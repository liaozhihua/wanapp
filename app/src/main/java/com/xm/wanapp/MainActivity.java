package com.xm.wanapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import com.xm.baselibrary.bitmap.bigbitmap.BigImageView;
import com.xm.wanapp.adapter.BitmapAdapter;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        initView();
//        decodeBitmap();
//        ImageCache.getInstance().init(this, Environment.getExternalStorageDirectory() + "/bitmap");
        initBigImage();
        EventBus.getDefault().register(this);
    }

    private void initBigImage() {
        InputStream stream = null;
        BigImageView bigImage = findViewById(R.id.big_image);
        try {
            stream = getAssets().open("world.jpg");
            bigImage.setImage(stream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化 recyclerView
     */
    private void initView() {
        RecyclerView rv = findViewById(R.id.rv);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(linearLayoutManager);

        BitmapAdapter bitmapAdapter = new BitmapAdapter(this);
        rv.setAdapter(bitmapAdapter);
    }

    /**
     * 解析图片
     */
    private void decodeBitmap() {
        //todo 图片大小注意事项 好像跟是不是jpg png 没有关系  这个只决定在磁盘中大小 内存中大小 只由
        // 图片宽高以及config决定 再测试相同图片 在x  xx  xxx 获取到的大小是否一样
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.one);
        Log.e("leo", "decodeBitmap: " + bitmap.getWidth() + "X" + bitmap.getHeight() + "x"
                + bitmap.getConfig() + ",内存总大小" + bitmap.getByteCount());
        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.two);
        Log.e("leo", "decodeBitmap: " + bitmap1.getWidth() + "X" + bitmap1.getHeight() + "x"
                + bitmap1.getConfig() + ",内存总大小" + bitmap1.getByteCount());
        Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.one);
        Log.e("leo", "decodeBitmap: " + bitmap2.getWidth() + "X" + bitmap2.getHeight() + "x"
                + bitmap2.getConfig() + ",内存总大小" + bitmap2.getByteCount());
    }
}
