package com.xdandroid.hellocamera2.util;

import android.graphics.*;

import com.xdandroid.hellocamera2.app.*;

import java.io.*;

public class BitmapUtils {

    /**
     * 将拍照得到的图片按照取景框（亮色区域）的范围进行裁剪.
     * 对于1280x720的屏幕，裁剪起始点为坐标(52, 80)，裁剪后，位图尺寸为896x588.（由layout xml定义的布局计算得到）
     * 以上参数将按照图片的实际大小，进行等比例换算。
     *
     * @param originalBitmap 拍照得到的Bitmap
     * @return 裁剪之后的Bitmap
     */
    public static Bitmap crop(Bitmap originalBitmap) {
        double originalWidth = originalBitmap.getWidth();
        double originalHeight = originalBitmap.getHeight();
        double scaleX = originalWidth / 1280;
        scaleX = scaleX * 1.04;
        double scaleY = originalHeight / 720;
        int x = (int) (52 * scaleX + 0.5);
        int y = (int) (80 * scaleY + 0.5);
        int width = (int) (896 * scaleX + 0.5);
        int height = (int) (588 * scaleY + 0.5);
        return Bitmap.createBitmap(originalBitmap, x, y, width, height);
    }

    /**
     * 若图片宽小于高，则逆时针旋转90° ; 否则，返回原图片.
     * 适用于调用系统相机进行拍照，且希望图片总是横向的场景。
     *
     * @param sourceBitmap 拍照得到的Bitmap
     * @return 若图片宽小于高，返回逆时针旋转90°后的Bitmap ; 否则，返回原Bitmap.
     */
    public static Bitmap rotate(Bitmap sourceBitmap) {
        int sourceWidth = sourceBitmap.getWidth();
        int sourceHeight = sourceBitmap.getHeight();
        if (sourceWidth >= sourceHeight) return sourceBitmap;
        int maxInWidthAndHeight = Math.max(sourceWidth, sourceHeight);
        Bitmap destBitmap = Bitmap.createBitmap(maxInWidthAndHeight, maxInWidthAndHeight, Bitmap.Config.ARGB_8888);
        Matrix m = new Matrix();
        m.setRotate(-90, maxInWidthAndHeight / 2, maxInWidthAndHeight / 2);
        Canvas canvas = new Canvas(destBitmap);
        canvas.drawBitmap(sourceBitmap, m, new Paint());
        return destBitmap;
    }

    /**
     * 将图片文件压缩到所需的大小，返回位图对象.
     * 若原图尺寸小于需要压缩到的尺寸，则返回原图.
     * 该方法通过分辨率面积得到inSampleSize，因此不存在图片方向问题.
     *
     * @param filePath
     * @param reqSquarePixels
     * @return
     */
    public static Bitmap compressToResolution(File filePath, long reqSquarePixels) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath.toString(), options);
        options.inSampleSize = calculateInSampleSizeBySquare(options, reqSquarePixels);
        options.inJustDecodeBounds = false;
        Bitmap fullBitmap = BitmapFactory.decodeFile(filePath.toString());
        return Bitmap.createScaledBitmap(fullBitmap, options.outWidth / options.inSampleSize, options.outHeight / options.inSampleSize, false);
    }

    /**
     * 计算压缩参数BitmapFactory.Options.inSampleSize.
     * 考虑了options的长宽比可能与req中的比例不同的情况.
     * 该方法通过分辨率面积得到inSampleSize，因此不存在图片方向问题.
     *
     * @param options         BitmapFactory.Options
     * @param reqSquarePixels 压缩后的分辨率面积
     * @return 计算得到的BitmapFactory.Options.inSampleSize
     */
    private static int calculateInSampleSizeBySquare(BitmapFactory.Options options, long reqSquarePixels) {
        long squarePixels = options.outWidth * options.outHeight;
        if (squarePixels <= reqSquarePixels) return 1;
        double powwedScale = ((double) squarePixels) / ((double) reqSquarePixels);
        double scale = Math.sqrt(powwedScale);
        double log = Math.log(scale) / Math.log(2);
        double logCeil = Math.ceil(log);
        return (int) Math.pow(2, logCeil);
    }

    /**
     * 将Bitmap写入文件，文件位于外置存储上该应用包名目录的cache子目录中.
     *
     * @param bitmap   要写入的Bitmap
     * @param fileName 文件名
     * @return 文件对应的File.
     */
    public static File writeBitmapToFile(Bitmap bitmap, String fileName) {
        File dir = new File(App.app.getCacheDir(), "images");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        FileOutputStream fos;
        try {
            if (file.exists()) file.delete();
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            bitmap.recycle();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
