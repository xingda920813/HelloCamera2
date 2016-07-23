package com.xdandroid.hellocamera2.util;

import android.os.*;
import android.support.annotation.*;
import android.util.Size;

import java.util.*;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Utils {

    /**
     * 比较两个int值的大小.
     * @param lhs 左侧值
     * @param rhs 右侧值
     * @return lhs < rhs, -1; lhs = rhs, 0; lhs > rhs, 1;
     */
    private static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    /**
     * 判断是否是16：9的Size, 允许误差5%.
     * @param size Size
     * @return 是否是16：9的Size
     */
    private static boolean isWide(Size size) {
        double ratio = ((double) size.getWidth()) / ((double) size.getHeight());
        return ratio > 1.68 && ratio < 1.87;
    }

    /**
     * 从sizeArray中找到满足16:9比例的最大尺寸(宽*高).
     * 若找不到，返回最大尺寸（不一定满足16:9）.
     * @param sizeArray StreamConfigurationMap.getOutputSizes(ImageFormat.JPEG)得到的sizeArray
     * @return 满足16:9比例的最大尺寸(宽*高); 若找不到，返回最大尺寸（不一定满足16:9）.
     */
    public static Size findBestSize(Size[] sizeArray) {
        List<Size> immutableSizeList = Arrays.asList(sizeArray);
        //Arrays.asList返回的List是不可变的，需重新包装为java.util.ArrayList.
        List<Size> sizeList = new ArrayList<>(immutableSizeList);
        Collections.sort(sizeList,
                (lhs, rhs) -> -compare(lhs.getWidth() * lhs.getHeight(), rhs.getWidth() * rhs.getHeight()));
        for (Size size : sizeList) {
            if (isWide(size)) return size;
        }
        return sizeList.get(0);
    }
}
