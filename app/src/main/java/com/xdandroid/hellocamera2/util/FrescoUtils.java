package com.xdandroid.hellocamera2.util;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

public class FrescoUtils {

    private FrescoUtils() {
    }

    public static class FrescoInner {

        private FrescoInner() {
        }

        private boolean sizeFixed;
        private String url;
        private ResizeOptions resizeOptions;

        /**
         * 适配AutoLayout的辅助方法.
         * 在1280x720的设计图下，将控件的width(px), height(px)传入resize(int, int)，即可将图片DownScale到与控件一样大.
         * @param widthInPx 控件的width(px)
         * @param heightInPx 控件的height(px)
         * @return FrescoInner
         */
        public FrescoInner resize(int widthInPx, int heightInPx) {
            if (sizeFixed) {
                sizeFixed = false;
                Log.d("FrescoUtils", "You are trying to call resize(int widthInPx, int heightInPx) after calling widthOrHeightFixed(), while there are conflicts. " +
                        "Call to widthOrHeightFixed() is ignored.");
            }
            resizeOptions = new ResizeOptions(CommonUtils.dp2px(widthInPx / 2), CommonUtils.dp2px(heightInPx / 2));
            return this;
        }

        /**
         * 固定宽和高之中的一项，使另外一项根据图片实际的宽高比自动调整到刚好包裹内容.
         * @return FrescoInner
         */
        public FrescoInner widthOrHeightFixed() {
            if (resizeOptions != null) {
                resizeOptions = null;
                Log.d("FrescoUtils", "You are trying to call widthOrHeightFixed() after calling resize(int widthInPx, int heightInPx), while there are conflicts. " +
                        "Call to resize(int widthInPx, int heightInPx) is ignored.");
            }
            this.sizeFixed = true;
            return this;
        }

        public void into(SimpleDraweeView v) {
            if (TextUtils.isEmpty(url)) {
                v.setImageURI(Uri.EMPTY);
                return;
            }

            PipelineDraweeControllerBuilder controllerBuilder = Fresco.newDraweeControllerBuilder();

            ImageRequestBuilder requestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url));

            if (resizeOptions != null) {
                requestBuilder.setResizeOptions(resizeOptions);
            }

            /**
             * 对控件的宽和高在XML中均指定为wrap_content的支持.
             */
            if (v.getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT && v.getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                controllerBuilder.setControllerListener(new BaseControllerListener<ImageInfo>() {

                    void setDraweeSize(ImageInfo imageInfo) {
                        if (imageInfo == null) return;
                        ViewGroup.LayoutParams params = v.getLayoutParams();
                        params.width = imageInfo.getWidth();
                        params.height = imageInfo.getHeight();
                        v.setLayoutParams(params);
                    }

                    @Override
                    public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
                        setDraweeSize(imageInfo);
                    }

                    @Override
                    public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
                        setDraweeSize(imageInfo);
                    }
                });
            }

            /**
             * 对控件的宽和高之中的一项在XML中指定为wrap_content，另外一项为固定大小的支持.
             * 同时适用于widthOrHeightFixed()被调用的情况.
             */
            if (sizeFixed || v.getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT || v.getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                controllerBuilder.setControllerListener(new BaseControllerListener<ImageInfo>() {

                    void makeSizeFixed(ImageInfo imageInfo) {
                        if (imageInfo == null) return;
                        v.setAspectRatio((float) imageInfo.getWidth() / imageInfo.getHeight());
                    }

                    @Override
                    public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
                        makeSizeFixed(imageInfo);
                    }

                    @Override
                    public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
                        makeSizeFixed(imageInfo);
                    }
                });
            }

            v.setController(controllerBuilder.setImageRequest(requestBuilder.build()).build());
        }
    }

    public static FrescoInner load(String url) {
        FrescoInner frescoInner = new FrescoInner();
        frescoInner.url = url;
        return frescoInner;
    }
}
