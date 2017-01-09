package com.xdandroid.hellocamera2.util;

import android.hardware.*;
import android.support.annotation.*;

import java.util.*;

import rx.Observable;
import rx.*;
import rx.android.schedulers.*;
import rx.schedulers.*;

@SuppressWarnings("deprecation")
public class CameraUtils {

    /**
     * 寻找最佳尺寸的Subscription，用于在找到最佳尺寸后及时取消订阅，防止内存泄漏.
     */
    public Subscription subscription;

    /**
     * 找到最佳尺寸后的回调类.
     */
    @SuppressWarnings("deprecation")
    public abstract class OnBestSizeFoundCallback {

        /**
         * 先取消订阅，防止Observable再发出事件，避免该方法被多次回调.
         * 然后调用用户提供的onBestSizeFound(Camera.Size size).
         *
         * @param size 最佳尺寸的Camera.Size
         */
        void bestSizeJustFound(Camera.Size size) {
            if (subscription != null) subscription.unsubscribe();
            onBestSizeFound(size);
        }

        public abstract void onBestSizeFound(Camera.Size size);
    }

    @Nullable
    public static Camera getCamera() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return camera;
    }

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
    public static boolean isWide(Camera.Size size) {
        double ratio = ((double) size.width) / ((double) size.height);
        return ratio > 1.68 && ratio < 1.87;
    }

    /**
     * 从sizeArray中找到满足16:9比例，且不超过maxPicturePixels指定的像素数的最大Size.
     * 若找不到，则选择满足16:9比例的最大Size（像素数可能超过maxPicturePixels)，若仍找不到，返回最大Size。
     *
     * @param forTakingPicture 寻找拍照尺寸，而不是预览尺寸。
     *                         为true时，尺寸受到maxPicturePixels的限制；
     *                         false时，尺寸不超过1920x1080，否则相机带宽吃紧，这也是Camera2 API的要求.
     * @param sizeList         Camera.Parameters.getSupportedPreviewSizes()
     *                         或Camera.Parameters.getSupportedPictureSizes()返回的sizeList
     * @param callback         找到最佳尺寸后的回调.
     * @param maxPicturePixels 最大可接受的照片像素数
     */
    public void findBestSize(boolean forTakingPicture, List<Camera.Size> sizeList, OnBestSizeFoundCallback callback, long maxPicturePixels) {
        //满足16:9，但超过maxAcceptedPixels的过大Size
        List<Camera.Size> tooLargeSizes = new ArrayList<>();
        subscription = Observable
                .just(sizeList)
                //按面积由大到小排序
                .map(sizes -> {
                    Collections.sort(sizes, (lhs, rhs) -> -compare(lhs.width * lhs.height, rhs.width * rhs.height));
                    return sizes;
                })
                //一个一个地激发事件
                .flatMap(Observable::from)
                //非16:9的尺寸无视
                .filter(CameraUtils::isWide)
                .filter(size -> {
                    boolean notTooLarge;
                    if (forTakingPicture) {
                        //若是为了拍摄照片，则尺寸不要超过指定的maxPicturePixels.
                        notTooLarge = ((long) size.width) * ((long) size.height) <= maxPicturePixels;
                    } else {
                        //若只是为了预览，则尺寸不要超过1920x1080，否则相机带宽吃紧，这也是Camera2 API的要求.
                        notTooLarge = ((long) size.width) * ((long) size.height) <= 1920 * 1080;
                    }
                    if (!notTooLarge) tooLargeSizes.add(size);
                    return notTooLarge;
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(callback::bestSizeJustFound, throwable -> {
                            if (tooLargeSizes.size() > 0) {
                                callback.bestSizeJustFound(tooLargeSizes.get(0));
                            } else {
                                callback.bestSizeJustFound(sizeList.get(0));
                            }
                        }, () -> {
                            if (tooLargeSizes.size() > 0) {
                                callback.bestSizeJustFound(tooLargeSizes.get(0));
                            } else {
                                callback.bestSizeJustFound(sizeList.get(0));
                            }
                        }
                );
    }

    /**
     * 在FOCUS_MODE_AUTO模式下使用，触发一次自动对焦.
     *
     * @param camera Camera
     */
    public static void autoFocus(Camera camera) {
        try {
            camera.autoFocus(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
