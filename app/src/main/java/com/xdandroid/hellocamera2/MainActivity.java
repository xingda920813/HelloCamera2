package com.xdandroid.hellocamera2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.xdandroid.hellocamera2.app.App;
import com.xdandroid.hellocamera2.app.BaseActivity;
import com.xdandroid.hellocamera2.util.BitmapUtils;
import com.xdandroid.hellocamera2.util.CommonUtils;
import com.xdandroid.hellocamera2.util.FrescoUtils;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity {

    @BindView(R.id.iv) SimpleDraweeView iv;
    @BindView(R.id.btn_takepicture) Button btnTakepicture;

    private File mFile;
    private boolean mHasSelectedOnce;

    @OnClick(R.id.iv)
    void onImageViewClick(View v) {
        if (mHasSelectedOnce) {
            View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog, null, false);
            SimpleDraweeView ivBig = (SimpleDraweeView) dialogView.findViewById(R.id.iv_big);
            FrescoUtils.load("file://" + mFile.toString()).into(ivBig);
            AlertDialog dialog = new AlertDialog
                    .Builder(MainActivity.this, R.style.Dialog_Translucent)
                    .setView(dialogView).create();
            ivBig.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        } else {
            takePicture();
        }
    }

    @OnClick(R.id.btn_takepicture)
    void onTakepictureClick(View v) {
        takePicture();
    }

    private void takePicture() {
        new TedPermission(App.app)
                .setRationaleMessage("我们需要使用您设备上的相机以完成拍照。\n当 Android 系统请求将相机权限授予 HelloCamera2 时，请选择『允许』。")
                .setDeniedMessage("如果您不对 HelloCamera2 授予相机权限，您将不能完成拍照。")
                .setRationaleConfirmText("确定")
                .setDeniedCloseButtonText("关闭")
                .setGotoSettingButtonText("设定")
                .setPermissionListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        Intent intent;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            intent = new Intent(MainActivity.this, Camera2Activity.class);
                        } else {
                            intent = new Intent(MainActivity.this, CameraActivity.class);
                        }
                        mFile = CommonUtils.createFile("mFile");
                        //文件保存的路径和名称
                        intent.putExtra("file", mFile.toString());
                        //拍照时的提示文本
                        intent.putExtra("hint", "");
                        //是否使用整个画面作为取景区域(全部为亮色区域)
                        intent.putExtra("hideBounds", false);
                        startActivityForResult(intent, App.REQUEST_TAKE_PHOTO);
                        /*调用系统相机进行拍照*/
                        /*Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        mFile = FileUtils.createFile("mFile");
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile));
                        startActivityForResult(intent, Constants.REQUEST_TAKE_PHOTO);*/
                    }

                    @Override
                    public void onPermissionDenied(ArrayList<String> arrayList) {

                    }
                }).setPermissions(new String[]{Manifest.permission.CAMERA})
                .check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != 200) return;
        if (requestCode == App.REQUEST_TAKE_PHOTO) {
            mFile = new File(data.getStringExtra("file"));
            Observable.just(mFile)
                    //将File解码为Bitmap
                    .flatMap((Func1<File, Observable<Bitmap>>) file -> Observable.create(
                            subscriber -> subscriber.onNext(BitmapUtils.decodeFile(file))))
                    //裁剪Bitmap
                    .flatMap((Func1<Bitmap, Observable<Bitmap>>) bitmap -> Observable.create(
                            subscriber -> subscriber.onNext(BitmapUtils.crop(bitmap))))
                    //将Bitmap写入文件
                    .flatMap((Func1<Bitmap, Observable<File>>) bitmap -> Observable.create(
                            subscriber -> subscriber.onNext(BitmapUtils.writeBitmapToFile(bitmap, "mFile"))))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(this.bindToLifecycle())
                    .subscribe(new Observer<File>() {
                        public void onCompleted() {
                        }

                        public void onError(Throwable e) {
                        }

                        public void onNext(File file) {
                            mFile = file;
                            Uri uri = Uri.parse("file://" + mFile.toString());
                            ImagePipeline imagePipeline = Fresco.getImagePipeline();
                            //清除该Uri的Fresco缓存. 若不清除，对于相同文件名的图片，Fresco会直接使用缓存而使得Drawee得不到更新.
                            imagePipeline.evictFromMemoryCache(uri);
                            imagePipeline.evictFromDiskCache(uri);
                            FrescoUtils.load("file://" + mFile.toString()).resize(240, 164).into(iv);
                            btnTakepicture.setText("重新拍照");
                            mHasSelectedOnce = true;
                        }
                    });
        }
    }

    @Override
    protected int getContentViewResId() {
        return R.layout.activity_main;
    }

    @Override
    protected void preInitData() {

    }
}
