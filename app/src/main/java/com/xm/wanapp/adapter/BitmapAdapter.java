package com.xm.wanapp.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xm.baselibrary.bitmap.ImageCache;
import com.xm.baselibrary.bitmap.ImageReSize;
import com.xm.wanapp.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BitmapAdapter extends RecyclerView.Adapter<BitmapAdapter.BitmapViewHolder> {

    private Context context;

    public BitmapAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public BitmapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rv_item, null, false);
        ImageView imageView = view.findViewById(R.id.iv);
        BitmapViewHolder bitmapViewHolder = new BitmapViewHolder(view,imageView);
        return bitmapViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull BitmapViewHolder bitmapViewHolder, int i) {
        // 原始方法获取bitmap
//        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_mv_w);

        // 第一种优化
//        Bitmap bitmap = ImageResize.resizeBitmap(context, R.drawable.icon_mv, 80, 80, false);

        // 第二种优化
        Bitmap bitmap = ImageCache.getInstance().getBitmapFromMemory(String.valueOf(i));
        Log.e("leo", "使用内存缓存" + bitmap);
        if (bitmap == null) {
            //todo 这个地方不是很理解 为啥resuable的宽度比bitmap还小
            Bitmap reusable = ImageCache.getInstance().getResuable(60,60,1);
            bitmap = ImageReSize.resizeBitmap(context, R.drawable.one, 80, 80, reusable,false);
            Log.e("leo", "使用复用缓存" + reusable);

            bitmap = ImageCache.getInstance().getBitmapFromDisk(String.valueOf(i), reusable);
            Log.e("leo", "使用磁盘缓存" + reusable);

            if (bitmap == null) {
                // 网络获取
                bitmap = ImageReSize.resizeBitmap(context, R.drawable.one, 80, 80, reusable,false);
                //放入内存
                ImageCache.getInstance().putBitmap2Memeory(String.valueOf(i), bitmap);
                //放入磁盘
                ImageCache.getInstance().putBitmap2Disk(String.valueOf(i), bitmap);
            }

        }

        bitmapViewHolder.iv.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        return 1000;
    }

    class BitmapViewHolder extends RecyclerView.ViewHolder {
        private ImageView iv;

        public BitmapViewHolder(@NonNull View itemView, ImageView iv) {
            super(itemView);
            iv = itemView.findViewById(R.id.iv);
        }
    }
}
