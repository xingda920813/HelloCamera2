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
@SuppressWarnings("deprecation")
public class CameraActivity extends BaseCameraActivity {

    @BindView(R.id.fl_camera_preview) FrameLayout mFlCameraPreview;
    @BindView(R.id.iv_camera_button) ImageView mIvCameraButton;
    @BindView(R.id.tv_camera_hint) TextView mTvCameraHint;
    @BindView(R.id.view_camera_dark0) View mViewDark0;
    @BindView(R.id.view_camera_dark1) LinearLayout mViewDark1;

    File mFile;
    Camera mCamera;
    CameraPreview mPreview;
    long mMaxPicturePixels;

    /**
     * 预览的最佳尺寸是否已找到
     */
    volatile boolean mPreviewBestFound;

    /**
     * 拍照的最佳尺寸是否已找到
     */
    volatile boolean mPictureBestFound;

    /**
     * finish()是否已调用过
     */
    volatile boolean mFinishCalled;

    @Override
    protected int getContentViewResId() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return R.layout.activity_camera;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void preInitData() {
        mFile = new File(getIntent().getStringExtra("file"));
        mTvCameraHint.setText(getIntent().getStringExtra("hint"));
        if (getIntent().getBooleanExtra("hideBounds", false)) {
            mViewDark0.setVisibility(View.INVISIBLE);
            mViewDark1.setVisibility(View.INVISIBLE);
        }
        mMaxPicturePixels = getIntent().getIntExtra("maxPicturePixels", 3840 * 2160);
        initCamera();
        RxView.clicks(mIvCameraButton)
              //防止手抖连续多次点击造成错误
              .throttleFirst(2, TimeUnit.SECONDS)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(aVoid -> {
                  if (mCamera == null) return;
                  mCamera.takePicture(null, null, (data, camera) -> Observable
                          .create((Observable.OnSubscribe<Integer>) subscriber -> {
                              try {
                                  if (mFile.exists()) mFile.delete();
                                  FileOutputStream fos = new FileOutputStream(mFile);
                                  fos.write(data);
                                  try {fos.close();} catch (Exception ignored) {}
                                  subscriber.onNext(200);
                              } catch (Exception e) {
                                  subscriber.onError(e);
                              }
                          }).subscribeOn(Schedulers.io())
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(integer -> {
                              setResult(integer, getIntent().putExtra("file", mFile.toString()));
                              mFinishCalled = true;
                              finish();
                          }, throwable -> {
                              throwable.printStackTrace();
                              mCamera.startPreview();
                          }));
              });
    }

    void initCamera() {
        Observable.create((Observable.OnSubscribe<Camera>) subscriber -> subscriber.onNext(CameraUtils
                .getCamera()))
                  .subscribeOn(Schedulers.newThread())
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(camera -> {
                      if (camera == null) {
                          Toast.makeText(App.sApp, "相机开启失败，再试一次吧", Toast.LENGTH_LONG).show();
                          mFinishCalled = true;
                          finish();
                          return;
                      }
                      mCamera = camera;
                      mPreview = new CameraPreview(CameraActivity.this, mCamera, (throwable, showToast) -> {
                          if (showToast) Toast.makeText(App.sApp, "开启相机预览失败，再试一次吧", Toast.LENGTH_LONG).show();
                          mFinishCalled = true;
                          finish();
                      });
                      mFlCameraPreview.addView(mPreview);
                      initParams();
                  });
    }

    void initParams() {
        Camera.Parameters params = mCamera.getParameters();
        //若相机支持自动开启/关闭闪光灯，则使用. 否则闪光灯总是关闭的.
        List<String> flashModes = params.getSupportedFlashModes();
        if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        mPreviewBestFound = false;
        mPictureBestFound = false;
        //寻找最佳预览尺寸，即满足16:9比例，且不超过1920x1080的最大尺寸;若找不到，则使用满足16:9的最大尺寸.
        //若仍找不到，使用最大尺寸;详见CameraUtils.findBestSize方法.
        CameraUtils previewUtils = new CameraUtils();
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        previewUtils.findBestSize(false, previewSizes, previewUtils.new OnBestSizeFoundCallback() {
            @Override
            public void onBestSizeFound(Camera.Size size) {
                mPreviewBestFound = true;
                params.setPreviewSize(size.width, size.height);
                if (mPictureBestFound) initFocusParams(params);
            }
        }, 1920 * 1080);
        //寻找最佳拍照尺寸，即满足16:9比例，且不超过maxPicturePixels指定的像素数的最大Size;若找不到，则使用满足16:9的最大尺寸.
        //若仍找不到，使用最大尺寸;详见CameraUtils.findBestSize方法.
        CameraUtils pictureUtils = new CameraUtils();
        List<Camera.Size> pictureSizes = params.getSupportedPictureSizes();
        pictureUtils.findBestSize(true, pictureSizes, pictureUtils.new OnBestSizeFoundCallback() {
            @Override
            public void onBestSizeFound(Camera.Size size) {
                mPictureBestFound = true;
                params.setPictureSize(size.width, size.height);
                if (mPreviewBestFound) initFocusParams(params);
            }
        }, mMaxPicturePixels);
    }

    void initFocusParams(Camera.Parameters params) {
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
            mFlCameraPreview.setOnClickListener(v -> CameraUtils.autoFocus(mCamera));
            //4秒后进行第一次自动对焦，之后每隔8秒进行一次自动对焦.
            Observable.timer(4, TimeUnit.SECONDS)
                      .flatMap(aLong -> {
                          CameraUtils.autoFocus(mCamera);
                          return Observable.interval(8, TimeUnit.SECONDS);
                      }).subscribe(aLong -> CameraUtils.autoFocus(mCamera));
        }
    }

    void setParameters(Camera.Parameters params) {
        try {
            mCamera.setParameters(params);
        } catch (Exception e) {
            //非常罕见的情况
            //个别机型在SupportPreviewSizes里汇报了支持某种预览尺寸，但实际是不支持的，设置进去就会抛出RuntimeException.
            e.printStackTrace();
            try {
                //遇到上面所说的情况，只能设置一个最小的预览尺寸
                params.setPreviewSize(1920, 1080);
                mCamera.setParameters(params);
            } catch (Exception e1) {
                //到这里还有问题，就是拍照尺寸的锅了，同样只能设置一个最小的拍照尺寸
                e1.printStackTrace();
                try {
                    params.setPictureSize(1920, 1080);
                    mCamera.setParameters(params);
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onBackPressed() {
        mFinishCalled = true;
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mCamera.stopPreview();
            mCamera.setPreviewDisplay(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mCamera.release();
            mCamera = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!mFinishCalled) finish();
    }
}
