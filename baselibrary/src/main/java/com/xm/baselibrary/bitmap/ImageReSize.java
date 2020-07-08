package com.xm.baselibrary.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageReSize {

    /**
     * 返回压缩图片。
     */
    public static Bitmap resizeBitmap(Context context,int resId, int maxWidth , int maxHeight, Bitmap resuable, boolean hasAlpha) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //设置为true后，再去解析，就只解析out参数
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(),resId,options);
        int w = options.outWidth;
        int h = options.outHeight;
        options.inSampleSize = calcuteInSampleSize(w,h,maxWidth,maxHeight);
        if (!hasAlpha) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        }
        options.inJustDecodeBounds = false;

        //复用,inMutable为true表示易变
        options.inMutable = true;
        //todo 这个地方不是直接把bitmap赋值了吗
        // 但是我bitmap不是复用池里面的图片呀 只是图片大小差不多或者说复用池图片所占空间比我bitmap大
        options.inBitmap = resuable;
        return BitmapFactory.decodeResource(context.getResources(),resId,options);
    }

    /**
     * 计算缩放系数。
     * @param w
     * @param h
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    private static int calcuteInSampleSize(int w, int h, int maxWidth, int maxHeight) {
        int samplSize =1;
        if (w > maxWidth && h>maxHeight) {
            samplSize = 2;
            while (w/samplSize>maxWidth && h/samplSize>maxHeight) {
                samplSize *= 2;
            }
        }
        return samplSize;
    }
}
