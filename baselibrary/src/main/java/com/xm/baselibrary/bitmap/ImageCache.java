package com.xm.baselibrary.bitmap;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.LruCache;

import com.xm.baselibrary.BuildConfig;
import com.xm.baselibrary.bitmap.disk.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 图片缓存，lrucache
 */
public class ImageCache {

    private static ImageCache instance;
    private DiskLruCache diskLruCache;

    private LruCache<String, Bitmap> lruCache;

    //复用的缓存池
    private Set<WeakReference<Bitmap>> reusablePool;

    /**
     * 单例。
     *
     * @return
     */
    public static ImageCache getInstance() {
        if (instance == null) {
            synchronized (ImageCache.class) {
                if (instance == null) {
                    instance = new ImageCache();
                }
            }
        }
        return instance;
    }

    public void init(Context context, String dir) {
        //复用池
        reusablePool = Collections.synchronizedSet(new HashSet<WeakReference<Bitmap>>());
        ActivityManager am = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        //todo 并没有看出来内存跟复用池有啥区别 而且多了一个复用池 那内存不是还得去分配空间吗?
        // 这个意义何在
        //  一种理解是lruCache是存的一个个真实对象 ，复用池存的是一块块内存 这个理解跟我后续的逻辑是冲突的 求解？
        lruCache = new LruCache<String, Bitmap>(memoryClass * 1024 * 1024 / 8) {
            //返回一张图片大小
            @Override
            protected int sizeOf(String key, Bitmap value) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    //系统给图片分配的大小
                    return value.getAllocationByteCount();
                }
                //图片在内存中大小
                return value.getByteCount();
            }

            //todo bitmap从内存移除后需要对bitmap回收
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (oldValue.isMutable()) {
                    //bitmap在3.0之前 缓存到native
                    //<8.0bitmap缓存到java
                    //8.0又缓存到native
                    //native需要 java中的话 需要手动调用recycle() JVM虚拟机会通过GC操作自动调用recycle()进行回收
                    //todo 问题1：我直接把我内存中不使用的图片加入到复用池里面 那我复用池不是还有这个图片吗？意义何在
                    // 我后续又不是使用这个相同图片，我只是需要这么大空间而已
                    // 问题2：为啥需要弱引用队列 我直接使用一个弱引用对象不就好了吗？
                    // 说法是弱引用通过gc清理后 会进入弱引用的引用队列中，也就是我如果在引用队列中看到这个弱引用
                    //  我们进行recycle()就好了 问题是 我不加入引用队列 gc应该也会清除他呀(gc不是说一碰到弱引用必定回收吗？) 为啥多此一举，还有我怎么在引用队列里面去判断
                    reusablePool.add(new WeakReference<Bitmap>(oldValue, getReferenceQueue()));
                }
                oldValue.recycle();
            }
        };
        try {
            diskLruCache = DiskLruCache.open(new File(dir), BuildConfig.VERSION_CODE, 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean shutDown;
    private ReferenceQueue<Bitmap> referenceQueue;

    private ReferenceQueue<Bitmap> getReferenceQueue() {
        if (referenceQueue == null) {
            referenceQueue = new ReferenceQueue<>();
            // todo 为啥需要开启线程
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!shutDown) {
                        Reference<? extends Bitmap> remove = null;
                        try {
                            remove = referenceQueue.remove();
                            Bitmap bitmap = remove.get();
                            if (bitmap != null && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }).start();
        }
        return referenceQueue;
    }

    /**
     * 加入磁盘缓存
     */
    public void putBitmap2Disk(String key,Bitmap bitmap) {
        DiskLruCache.Snapshot snapshot = null;
        OutputStream outputStream = null;
        try {
            snapshot = diskLruCache.get(key);
            if (snapshot == null) {
                //todo 这个干吗用的？
                DiskLruCache.Editor edit = diskLruCache.edit(key);
                if (edit != null) {
                    //todo 这个干吗用的
                    outputStream = edit.newOutputStream(0);
                    //todo 压缩格式
                    boolean compress = bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
                    //todo  commit是干嘛用的
                    edit.commit();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 从磁盘缓存中获取bitmap
     */
    public Bitmap getBitmapFromDisk(String key,Bitmap reusable) {
        DiskLruCache.Snapshot snapshot = null;
        Bitmap bitmap = null;
        try {
            snapshot = diskLruCache.get(key);
            if (snapshot == null) {
                return null;
            }
            //todo  为啥是0 以及为啥是输入流
            InputStream inputStream = snapshot.getInputStream(0);
            BitmapFactory.Options options = new BitmapFactory.Options();
            //todo 这俩行是为了复用 但是为啥做到的不太理解
            options.inMutable = true;
            options.inBitmap = reusable;
            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (bitmap != null) {
                //todo 内存缓存 为啥不是放到put方法里面而是get方法里面
                lruCache.put(key,bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (snapshot !=null) {
                snapshot.close();
            }
        }
        //todo 如果走到异常里面去了 还会走return方法吗？？？
        return bitmap;
    }

    /**
     * 把图片bitmap放入内存
     */
    public void putBitmap2Memeory(String key, Bitmap bitmap) {

        lruCache.put(key, bitmap);
    }

    /**
     * 从内存中取出图片
     */
    public Bitmap getBitmapFromMemory(String key) {

        return lruCache.get(key);
    }

    /**
     * 清空图片。
     */
    public void clearMemory() {
        lruCache.evictAll();
    }

    /**
     * 3.0之前不能复用
     * 3.0-4.4宽高一样 ，inSampleSize =1
     * 4.4以上只要小于等于就行了
     */
    public Bitmap getResuable(int w, int h, int inSampleSize) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return null;
        }
        Bitmap resuable = null;
        Iterator<WeakReference<Bitmap>> iterator = reusablePool.iterator();
        while (iterator.hasNext()) {
            Bitmap bitmap = iterator.next().get();
            if (bitmap != null) {
                if (checkInBitmap(bitmap, w, h, inSampleSize)) {
                    resuable = bitmap;
                    //todo 为啥需要移除 移除的话  那我下次不还是得加到复用池里 那我复用次数只有一次 那意义也不大呀
                    iterator.remove();
                    break;
                }
            } else {
                //todo 这个地方更不理解 如果我引入一张图片 不能复用的我都移除掉
                // 那我复用池只可能是一个bitmap或者是0个 那意义更加不存在了
                iterator.remove();
            }
        }
        return resuable;
    }

    /**
     * 校验bitmap是否满足条件
     */
    private boolean checkInBitmap(Bitmap bitmap, int w, int h, int inSampleSize) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return bitmap.getWidth() == w && bitmap.getHeight() == h && inSampleSize == 1;
        }
        if (inSampleSize > 1) {
            w /= inSampleSize;
            h /= inSampleSize;
        }
        //todo 这个地方我有个疑问 为啥 我即将放进去的图片的Config使用的是复用池bitmap的getConfig尺寸
        //如果我的Config比复用池里bitmap的getConfig尺寸大 这个地方不是有问题吗？
        int byteCount = w * h * getBytePerPixel(bitmap.getConfig());
        //图片内存 系统分配内存
        return byteCount <= bitmap.getAllocationByteCount();
    }

    /**
     * 通过像数格式计算每一个像素占用多少个字节。
     */
    private int getBytePerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        }
        return 2;
    }
}
