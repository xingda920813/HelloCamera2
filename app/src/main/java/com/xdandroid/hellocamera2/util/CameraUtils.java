package com.xdandroid.hellocamera2.util;

import android.hardware.Camera;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class CameraUtils {

    /**
     * 寻找最佳尺寸的Subscription，用于在找到最佳尺寸后及时取消订阅，防止内存泄漏.
     */
    private Subscription subscription;

    /**
     * 找到最佳尺寸后的回调类.
     */
    public abstract class OnBestSizeFoundCallback {

        /**
         * 先取消订阅，防止Observable再发出事件，避免该方法被多次回调.
         * 然后调用用户提供的onBestSizeFound(Camera.Size size).
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
    private static boolean isWide(Camera.Size size) {
        double ratio = ((double) size.width) / ((double) size.height);
        return ratio > 1.68 && ratio < 1.87;
    }

    /**
     * 从sizeList中找到满足16:9比例的最大尺寸(宽*高).
     * 若是为了寻找实时预览所用的尺寸(forTakingPicture = false)，符合条件的尺寸还将受到最大1920x1080的限制.
     * 若找不到，返回最大尺寸（不一定满足16:9）.
     * @param forTakingPicture 若为true, 此次调用是寻找最终拍摄的照片所用的尺寸;
     *                         若为false, 此次调用是为了寻找实时预览所用的尺寸, 符合条件的尺寸还将受到最大1920x1080的限制.
     * @param sizeList Camera.Parameters.getSupportedPreviewSizes()或Camera.Parameters.getSupportedPictureSizes()返回的sizeList
     * @param callback 找到最佳尺寸后的回调.
     */
    public void findBestSize(boolean forTakingPicture, List<Camera.Size> sizeList, OnBestSizeFoundCallback callback) {
        subscription = Observable.just(sizeList)
                //先按照面积从大到小排序
                .map(sizes -> {
                    Collections.sort(sizes,
                            (lhs, rhs) -> -compare(lhs.width * lhs.height, rhs.width * rhs.height));
                    return sizes;
                })
                //一个一个地激发事件
                .flatMap(Observable::from)
                //若是为了拍摄照片，那么越清晰越好，直接返回true，表示所有的尺寸都可以通过筛选进入下一步;
                //若只是为了预览，则尺寸不要超过1920x1080，否则相机带宽吃紧。这也是Camera2 API的要求.
                .filter(size -> forTakingPicture || size.width <= 1920 && size.height <= 1080)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(size -> {
                            if (isWide(size)) callback.bestSizeJustFound(size);
                        }, throwable -> callback.bestSizeJustFound(sizeList.get(0)),
                        () -> callback.bestSizeJustFound(sizeList.get(0)));
    }

    /**
     * 在FOCUS_MODE_AUTO模式下使用，触发一次自动对焦.
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
