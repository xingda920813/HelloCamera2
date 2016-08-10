package com.xdandroid.hellocamera2.util;

import android.content.*;
import android.content.res.*;
import android.os.*;
import android.util.*;
import android.view.*;

import com.xdandroid.hellocamera2.app.*;

import java.io.*;

public class CommonUtils {

    /**
     * dp转px
     *
     * @param dpValue dp
     * @return px
     */
    public static int dp2px(float dpValue) {
        final float scale = App.app.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dp(float pxValue) {
        final float scale = App.app.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 判断设备是否具有虚拟导航栏
     * @return 设备是否具有虚拟导航栏
     */
    public static boolean hasNavigationBar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) return true;
        WindowManager wm = (WindowManager) App.app.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);
        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);
        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;
        return (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
    }

    /**
     * 得到以px为单位的虚拟导航栏高度，若设备没有虚拟导航栏，返回0.
     * @return 虚拟导航栏高度(px)，若设备没有虚拟导航栏，返回0.
     */
    public static int getNavigationBarHeightInPx() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) return dp2px(48);
        int navBarHeightInPx = 0;
        Resources rs = App.app.getResources();
        int id = rs.getIdentifier("navigation_bar_height", "dimen", "android");
        if (id > 0 && hasNavigationBar()) {
            navBarHeightInPx = rs.getDimensionPixelSize(id);
        }
        return navBarHeightInPx;
    }

    /**
     * 创建File对象，对应于data/data/${packageName}/cache/fileName.
     *
     * @param fileName 文件名
     * @return File
     */
    public static File createImageFile(String fileName) {
        File dir = new File(App.app.getCacheDir(), "images");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, fileName);
    }
}
