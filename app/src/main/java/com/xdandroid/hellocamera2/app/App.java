package com.xdandroid.hellocamera2.app;

import android.app.*;
import android.os.*;

import com.facebook.drawee.backends.pipeline.*;
import com.facebook.imagepipeline.core.*;

public class App extends Application {

    /**
     * 启动照相Intent的requestCode.
     */
    public static final int TAKE_PHOTO_CUSTOM = 100;
    public static final int TAKE_PHOTO_SYSTEM = 200;
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
