package com.xdandroid.hellocamera2;

import android.hardware.*;
import android.view.*;
import android.widget.*;

import com.jakewharton.rxbinding.view.*;
import com.xdandroid.hellocamera2.app.*;
import com.xdandroid.hellocamera2.util.*;
import com.xdandroid.hellocamera2.view.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import butterknife.*;
import rx.Observable;
import rx.android.schedulers.*;
import rx.schedulers.*;

/**
 * Camera API. Android KitKat 及以前版本的 Android 使用 Camera API.
 */
public class CameraActivity extends BaseCameraActivity {

    @BindView(R.id.fl_camera_preview) FrameLayout flCameraPreview;
    @BindView(R.id.iv_camera_button) ImageView ivCameraButton;
    @BindView(R.id.tv_camera_hint) TextView tvCameraHint;
    @BindView(R.id.view_camera_dark0) View viewDark0;
    @BindView(R.id.view_camera_dark1) LinearLayout viewDark1;

    private File file;
    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.Size mLowPreviewSize, mLowPictureSize;

    /**
     * 预览的最佳尺寸是否已找到
     */
    private volatile boolean previewBestFound;

    /**
     * 拍照的最佳尺寸是否已找到
     */
    private volatile boolean pictureBestFound;

    /**
     * finish()是否已调用过
     */
    private volatile boolean finishCalled;

    @Override
    protected int getContentViewResId() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return R.layout.activity_camera;
    }

    @Override
    protected void preInitData() {
        file = new File(getIntent().getStringExtra("file"));
        tvCameraHint.setText(getIntent().getStringExtra("hint"));
        if (getIntent().getBooleanExtra("hideBounds", false)) {
            viewDark0.setVisibility(View.INVISIBLE);
            viewDark1.setVisibility(View.INVISIBLE);
        }
        initCamera();
        RxView.clicks(ivCameraButton)
                //防止手抖连续多次点击造成错误
                .throttleFirst(2, TimeUnit.SECONDS)
                .compose(this.bindToLifecycle())
                .subscribe(aVoid -> {
                    if (mCamera == null) return;
                    mCamera.takePicture(null, null, (data, camera) -> Observable.create((Observable.OnSubscribe<Integer>) subscriber -> {
                        try {
                            if (file.exists()) file.delete();
                            FileOutputStream fos = new FileOutputStream(file);
                            fos.write(data);
                            try {
                                fos.close();
                            } catch (Exception ignored) {}
                            subscriber.onNext(200);
                        } catch (Exception e) {
                            subscriber.onError(e);
                        }
                    }).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .compose(CameraActivity.this.bindToLifecycle())
                            .subscribe(integer -> {
                                setResult(integer, getIntent().putExtra("file", file.toString()));
                                finishCalled = true;
                                finish();
                            }, throwable -> {
                                throwable.printStackTrace();
                                mCamera.startPreview();
                            }));
                });
    }

    private void initCamera() {
        Observable.create((Observable.OnSubscribe<Camera>) subscriber -> subscriber.onNext(CameraUtils.getCamera()))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.bindToLifecycle())
                .subscribe(camera -> {
                    if (camera == null) {
                        Toast.makeText(App.app, "相机开启失败，再试一次吧", Toast.LENGTH_LONG).show();
                        finishCalled = true;
                        finish();
                        return;
                    }
                    mCamera = camera;
                    mPreview = new CameraPreview(CameraActivity.this, mCamera, (throwable, showToast) -> {
                        if (showToast) Toast.makeText(App.app, "开启相机预览失败，再试一次吧", Toast.LENGTH_LONG).show();
                        finishCalled = true;
                        finish();
                    });
                    flCameraPreview.addView(mPreview);
                    initParams();
                });
    }

    private void initParams() {
        Camera.Parameters params = mCamera.getParameters();
        mLowPreviewSize = params.getPreviewSize();
        mLowPictureSize = params.getPictureSize();
        //若相机支持自动开启/关闭闪光灯，则使用. 否则闪光灯总是关闭的.
        List<String> flashModes = params.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        }
        previewBestFound = false;
        pictureBestFound = false;
        //寻找最佳预览尺寸，即满足16:9的不超过1920x1080的最大尺寸; 若找不到，则使用不超过1920x1080的最大尺寸.
        CameraUtils previewUtils = new CameraUtils();
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        previewUtils.findBestSize(false, previewSizes, previewUtils.new OnBestSizeFoundCallback() {
            @Override
            public void onBestSizeFound(Camera.Size size) {
                previewBestFound = true;
                params.setPreviewSize(size.width, size.height);
                if (pictureBestFound) initFocusParams(params);
            }
        });
        //寻找最佳拍照尺寸，即满足16:9的最大尺寸; 若找不到，则使用最大尺寸.
        CameraUtils pictureUtils = new CameraUtils();
        List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();
        pictureUtils.findBestSize(true, pictureSizes, pictureUtils.new OnBestSizeFoundCallback() {
            @Override
            public void onBestSizeFound(Camera.Size size) {
                pictureBestFound = true;
                params.setPictureSize(size.width, size.height);
                if (previewBestFound) initFocusParams(params);
            }
        });
    }

    private void initFocusParams(Camera.Parameters params) {
        //若支持连续对焦模式，则使用.
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            setParameters(params);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            //进到这里，说明不支持连续对焦模式，退回到点击屏幕进行一次自动对焦.
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            setParameters(params);
            //点击屏幕进行一次自动对焦.
            flCameraPreview.setOnClickListener(v -> CameraUtils.autoFocus(mCamera));
            //4秒后进行第一次自动对焦，之后每隔8秒进行一次自动对焦.
            Observable.timer(4, TimeUnit.SECONDS)
                    .flatMap(aLong -> {
                        CameraUtils.autoFocus(mCamera);
                        return Observable.interval(8, TimeUnit.SECONDS).compose(this.bindToLifecycle());
                    })
                    .compose(this.bindToLifecycle())
                    .subscribe(aLong -> CameraUtils.autoFocus(mCamera));
        }
    }

    private void setParameters(Camera.Parameters params) {
        try {
            mCamera.setParameters(params);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                params.setPreviewSize(mLowPreviewSize.width, mLowPreviewSize.height);
                mCamera.setParameters(params);
            } catch (Exception e1) {
                e1.printStackTrace();
                try {
                    params.setPictureSize(mLowPictureSize.width, mLowPictureSize.height);
                    mCamera.setParameters(params);
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onBackPressed() {
        finishCalled = true;
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mCamera.stopPreview();
            mCamera.setPreviewDisplay(null);
            mCamera.release();
            mCamera = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!finishCalled) finish();
    }
}
