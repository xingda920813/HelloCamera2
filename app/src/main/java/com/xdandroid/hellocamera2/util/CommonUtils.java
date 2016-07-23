package com.xdandroid.hellocamera2.util;

import com.xdandroid.hellocamera2.app.*;

import java.io.*;

public class CommonUtils {

    /**
     * dp转px
     * @param dipValue dp
     * @return px
     */
    static int dp2px(float dipValue) {
        final float scale = App.app.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 创建File对象，对应于外置存储上该应用包名目录的cache子目录中的fileName文件.
     * @param fileName 文件名
     * @return File
     */
    public static File createFile(String fileName) {
        String path = App.app.getExternalCacheDir() + "/" + fileName;
        return new File(path);
    }
}
