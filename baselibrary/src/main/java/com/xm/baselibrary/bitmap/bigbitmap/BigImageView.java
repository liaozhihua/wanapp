package com.xm.baselibrary.bitmap.bigbitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.Nullable;

public class BigImageView extends View implements GestureDetector.OnGestureListener, View.OnTouchListener {

    private BitmapFactory.Options options;
    private BitmapRegionDecoder regionDecoder;
    private int imageWidth;
    private int imageHeight;
    private int viewHeight;
    private int viewWidth;
    private Rect rect;
    private float scale;
    private GestureDetector gestureDetector;
    private Scroller scroller;

    public BigImageView(Context context) {
        this(context,null,0);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public BigImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        rect = new Rect();
        options = new BitmapFactory.Options();
        //手势
        gestureDetector = new GestureDetector(context, this);
        // 将触摸事件交给手势处理
        setOnTouchListener(this);
        // 滑动帮助
        scroller = new Scroller(context);
    }

    public void setImage(InputStream is) {
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is,null,options);
        imageWidth = options.outWidth;
        imageHeight = options.outHeight;
        //todo 这是为了复用 可是这是怎么复用的？为啥需要复用？
        options.inMutable = true;
        //todo 这个叫什么？
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        //todo 这个地方设置为false有啥意义？
        options.inJustDecodeBounds = false;
        try {
            //false 是为了不共享图片源
            regionDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    requestLayout();


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //todo 这个值会不会为0 ？？什么情况下值一定不为0
        viewHeight = getMeasuredHeight();
        viewWidth = getMeasuredWidth();
        if (regionDecoder == null) {
            return;
        }
        rect.left = 0;
        rect.top = 0;
        rect.right = imageWidth;
        //缩放因子
        scale = viewWidth /(float) imageWidth;
        rect.bottom =(int) (viewHeight / scale);
        //第一种方式优化
        options.inSampleSize = calcuteInSampleSize(imageWidth, imageHeight, viewWidth, viewHeight);
        //todo  第二种方式优化
//        float temp = 1.0f / mScale;
//        if (temp > 1) {
//        todo  Math.pow 意义
//            mOptions.inSampleSize = (int) Math.pow(2, (int) (temp));
//        } else {o
//            mOptions.inSampleSize = 1;
//        }
        Log.e("Leo", "============缩放后=========");
        Log.e("Leo", "inSampleSize = " + options.inSampleSize);
        Log.e("Leo", "mScale = " + scale);
        Log.e("Leo", "图片宽 = " + imageWidth + ",高 = " + imageHeight);
        Log.e("Leo", "view 宽 = " + viewWidth + ",高 = " + viewHeight);
    }

    /**
     * @param w    图片宽
     * @param h    图片高
     * @param maxW View 宽
     * @param maxH View 高
     * @return
     */
    private static int calcuteInSampleSize(int w, int h, int maxW, int maxH) {
        int inSampleSize = 1;
        if (w > maxW && h > maxH) {
            inSampleSize = 2;
            while (w / inSampleSize > maxW && h / inSampleSize > maxH) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    Bitmap bitmap = null;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (regionDecoder == null) {
            return;
        }
        //todo  这个赋值的意义何在
        options.inBitmap = bitmap;
        bitmap = regionDecoder.decodeRegion(rect,options);
        Log.e("leo", "图片大小 " + bitmap.getByteCount());
        // 没有优化：44338752，1.优化：2770200，2优化：692064

        //todo  这个的意义是干嘛的？
        Matrix matrix = new Matrix();
        matrix.setScale(scale,scale);
        //        matrix.setScale(mScale * mOptions.inSampleSize, mScale * mOptions.inSampleSize);
        canvas.drawBitmap(bitmap,matrix,null);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // 如果滑动还没有停止 强制停止
        if (!scroller.isFinished()) {
            scroller.forceFinished(true);
        }
        //继续接收后续事件
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //todo 改变加载图片的区域  什么情况下distanceY为正
        rect.offset(0, (int) distanceY);
        //bottom大于图片高了， 或者 top小于0了
        //todo  这个地方要注意怎使用
        if (rect.bottom>imageHeight) {
            rect.bottom = imageHeight;
            rect.top = imageHeight - (int) (viewHeight/scale);
        }
        if (rect.top < 0) {
            rect.top = 0;
            rect.bottom = (int) (viewHeight / scale);
        }
        // 重绘 todo 重绘会走什么?
        invalidate();
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        /**todo  不太理解意义何在
         * startX: 滑动开始的x坐标
         * velocityX: 以每秒像素为单位测量的初始速度
         * minX: x方向滚动的最小值
         * maxX: x方向滚动的最大值
         */
        scroller.fling(0, rect.top, 0, (int) -velocityY, 0, 0,
                0, imageHeight - (int) (viewHeight / scale));
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //todo 事件交给手势处理(为啥可以做到这样)
        return gestureDetector.onTouchEvent(event);
    }

    /**
     * 获取计算结果并且重绘
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        //todo  这个方法作用是？？？
        //已经计算结束 return
        if (scroller.isFinished()) {
            return;
        }
        //true 表示当前动画未结束
        if (scroller.computeScrollOffset()) {
            rect.top = scroller.getCurrY();
            rect.bottom = rect.top + (int) (viewHeight / scale);
            invalidate();
        }
    }
}
