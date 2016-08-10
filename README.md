# HelloCamera2
Android 自定义相机 ： 可定制的UI, 压缩到指定分辨率级别, 使用代码自动裁剪指定区域, 大图预览, Camera / Camera2 API 的使用.

![Alt text](https://raw.githubusercontent.com/xingda920813/HelloCamera2/master/screenshot_main.png)

![Alt text](https://raw.githubusercontent.com/xingda920813/HelloCamera2/master/screenshot_camera.png)

![Alt text](https://raw.githubusercontent.com/xingda920813/HelloCamera2/master/screenshot_preview.png)

可选系统相机、自定义相机 (Camera API)、自定义相机 (Camera2 API)

详见 Sample App 的代码及注释.

### 系统相机 :

使用 FileProvider 指定文件保存路径，支持 Android N，target API 24 时不会出现 FileUriExposedException.

拍照完成后，文件已被保存到FileProvider指定的目录和文件名 :

(1) 按面积压缩照片到1920x1080(约207万像素)像素数，保持宽高比不变，图像不会被拉伸变形;

(2) 智能旋转，若图片的宽小于高，则逆时针旋转90°; 保证图片的宽始终大于高;

(3) 将图像写入文件.

### 自定义相机 (Camera API / Camera2 API) :

特性 :

```
在 Android Kitkat 及以前版本的 Android 中, 使用 Camera API;
在 Android Lollipop 及以后版本的 Android 中, 使用 Camera2 API.

使用算法寻找最佳预览/照片尺寸
自动闪光灯控制
连续对焦模式
```

通过 startActivityForResult 启动 CameraActivity / Camera2Activity，传入 4 个 Extra :

```
//文件保存的路径和名称
intent.putExtra("file", mFile.toString());

//拍照时的提示文本
intent.putExtra("hint", "请将证件放入框内。将裁剪图片，只保留框内区域的图像");

//是否使用整个画面作为取景区域(全部为亮色区域)
intent.putExtra("hideBounds", false);

//最大允许的拍照尺寸（像素数）
intent.putExtra("maxPicturePixels", 3840 * 2160);
```
拍照完成后，在 onActivityResult 的 Intent 里，存有一个名为 file 的 String Extra，即为文件被保存的目录和文件名 :

(1) 按面积压缩照片到1920x1080(约207万像素)像素数，保持宽高比不变，图像不会被拉伸变形;

(2) 按取景框（亮色区域）进行裁剪，只保留框内区域的图像;

(3) 将图像写入文件.

### 算法说明 :

#### 确定使用的预览尺寸 :
从sizeArray中找到满足16:9比例，且不超过1920x1080的最大Size.

若找不到，则选择满足16:9比例的最大Size（像素数可能超过1920x1080).

若仍找不到，返回最大Size.

预览尺寸不要超过1920x1080，否则相机带宽吃紧，这也是Camera2 API的要求.

#### 确定使用的照片尺寸 :
从sizeArray中找到满足16:9比例，且不超过maxPicturePixels指定的像素数的最大Size.

若找不到，则选择满足16:9比例的最大Size（像素数可能超过maxPicturePixels).

若仍找不到，返回最大Size.

maxPicturePixels通过Intent的Extra传入.

#### 压缩到指定分辨率级别 :
将图片文件压缩到所需的大小，返回位图对象.

若原图尺寸小于需要压缩到的尺寸，则返回原图.

该方法通过计算分辨率面积之比得到压缩比，考虑了图片的宽高比可能与maxPicturePixels中指定的分辨率的宽高比不同的情况，因此不存在图片方向、宽高比的问题.

#### 裁剪指定区域 :
将拍照得到的图片按照取景框（亮色区域）的范围进行裁剪.

对于1280x720的屏幕，裁剪起始点为坐标(52, 80)，裁剪后，位图尺寸为896x588.（由layout xml定义的布局计算得到）

以上参数将按照图片的实际大小，进行等比例换算。

设备有无虚拟导航栏均能裁剪到正确的区域。

#### 将Bitmap写入文件 :
将Bitmap写入文件，文件位于内置存储上该应用包名目录的cache子目录中.

JPEG为硬件加速，O(1)时间复杂度，而PNG为O(n)，速度要慢很多，WEBP不常用

90%的品质已高于超精细(85%)的标准，已非常精细

#### FrescoUtils :
```
/**
* 适配AutoLayout的辅助方法.
* 将控件的width(dp), height(dp)传入resize(int, int)，即可将图片向下缩放(DownScale)到与控件一样大.
*/
FrescoInner FrescoInner.resize(int widthDp, int heightDp);

/**
* 增加了对 DraweeView 的 wrap_content / ViewGroup.LayoutParams.WRAP_CONTENT 属性的支持.
*/
void FrescoInner.into(SimpleDraweeView v);
```
