package com.xdandroid.hellocamera2.util;

import com.xdandroid.hellocamera2.app.*;

import java.io.*;

public class CommonUtils {

    /**
     * dp转px
     * @param dpValue dp
     * @return px
     */
    static int dp2px(float dpValue) {
        final float scale = App.app.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 创建File对象，对应于data/data/${packageName}/cache/fileName.
     * @param fileName 文件名
     * @return File
     */
    public static File createImageFile(String fileName) {
        File dir = new File(App.app.getCacheDir(), "images");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, fileName);
    }
}
