package com.xdandroid.hellocamera2.util;

import android.os.*;
import android.support.annotation.*;
import android.util.Size;

import java.util.*;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Utils {

    /**
     * 比较两个int值的大小.
     *
     * @param lhs 左侧值
     * @param rhs 右侧值
     * @return lhs < rhs, -1; lhs = rhs, 0; lhs > rhs, 1;
     */
    public static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    /**
     * 判断是否是16：9的Size, 允许误差5%.
     *
     * @param size Size
     * @return 是否是16：9的Size
     */
    public static boolean isWide(Size size) {
        double ratio = ((double) size.getWidth()) / ((double) size.getHeight());
        return ratio > 1.68 && ratio < 1.87;
    }

    /**
     * 从sizeArray中找到满足16:9比例，且不超过maxPicturePixels指定的像素数的最大Size.
     * 若找不到，则选择满足16:9比例的最大Size（像素数可能超过maxPicturePixels)，若仍找不到，返回最大Size。
     *
     * @param sizeArray        StreamConfigurationMap.getOutputSizes(ImageFormat.JPEG)得到的sizeArray
     * @param maxPicturePixels 最大可接受的照片像素数
     * @return 找到满足16:9比例，且不超过maxPicturePixels指定的像素数的最大Size
     */
    public static Size findBestSize(Size[] sizeArray, long maxPicturePixels) {
        //满足16:9，但超过maxAcceptedPixels的过大Size
        List<Size> tooLargeSizes = new ArrayList<>();
        List<Size> immutableSizeList = Arrays.asList(sizeArray);
        //Arrays.asList返回的List是不可变的，需重新包装为java.util.ArrayList.
        List<Size> sizeList = new ArrayList<>(immutableSizeList);
        //按面积由大到小排序
        Collections.sort(sizeList, (lhs, rhs) -> -compare(lhs.getWidth() * lhs.getHeight(), rhs.getWidth() * rhs.getHeight()));
        for (Size size : sizeList) {
            //非16:9的尺寸无视
            if (!isWide(size)) continue;
            boolean notTooLarge = ((long) size.getWidth()) * ((long) size.getHeight()) <= maxPicturePixels;
            if (!notTooLarge) {
                tooLargeSizes.add(size);
                continue;
            }
            return size;
        }
        if (tooLargeSizes.size() > 0) {
            return tooLargeSizes.get(0);
        } else {
            return sizeList.get(0);
        }
    }
}
