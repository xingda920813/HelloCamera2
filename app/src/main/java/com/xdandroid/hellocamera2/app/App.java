package com.xdandroid.hellocamera2.app;

import android.app.Application;
import android.os.Handler;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

public class App extends Application {

    /**
     * 启动照相Intent的requestCode.
     */
    public static final int REQUEST_TAKE_PHOTO = 100;
    /**
     * 主线程Handler.
     */
    public static Handler mHandler;
    /**
     * Application对象.
     */
    public static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        mHandler = new Handler();
        Fresco.initialize(this, ImagePipelineConfig
                .newBuilder(this)
                .setDownsampleEnabled(true)
                .build());
    }
}
