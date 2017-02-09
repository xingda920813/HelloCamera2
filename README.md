# HelloCamera2

[中文 README](https://github.com/xingda920813/HelloCamera2/blob/master/README_zh.md)

Android Custom Camera: Customizable UI, Compressing To Specific Resolution Level, Clipping Specific Zone Programmatically, Picture Preview, Usage of Camera and Camera2 API.

![Alt text](https://raw.githubusercontent.com/xingda920813/HelloCamera2/master/screenshot_main.png)

![Alt text](https://raw.githubusercontent.com/xingda920813/HelloCamera2/master/screenshot_camera.png)

![Alt text](https://raw.githubusercontent.com/xingda920813/HelloCamera2/master/screenshot_preview.png)

You can choose system camera, custom camera (Camera API) and custom camera (Camera2 API).

Please refer to codes and comments of the sample app.

### System camera:

Using FileProvider to specify the file save path, supports Android N, no FileUriExposedException will be thrown when targeting API 24+.

After taking picture, the file is already saved to the path with the file name FileProvider specified before:

(1) Compress the picture to 1920x1080 (approximately 2070k pixels) pixels by area, keeping the aspect ratio unchanged, so the image will not be distorted;

(2) Intelligent rotation, if the picture's width is less than height, then counterclockwise rotate of 90°, to ensure that the picture's width is always greater than height;

(3) Save the image to file.

### Custom camera (Camera API / Camera2 API):

Characters:

```
In Android Kitkat and lower, use Camera API;
In Android Lollipop and higher, use Camera2 API.

Find the best preview / pictrue size using algorithm;
Automatic flash control;
Continuous focus mode;
```

Start CameraActivity / Camera2Activity by calling startActivityForResult，passing 4 extras:

```
//The path and the file name to save
intent.putExtra("file", mFile.toString());

//Hint text when taking picture
intent.putExtra("hint", "Please put documents into the box. The picture will be cropped, leaving only the area within the frame.");

//Whether to use the entire screen as the framing area (all the screen is bright area)
intent.putExtra("hideBounds", false);

//Maximum allowed picture size (pixels)
intent.putExtra("maxPicturePixels", 3840 * 2160);
```

After taking picture, in the Intent parameter of onActivityResult, there is an extra named "file", which is the path and the file name the file was saved to:

(1) Compress the picture to 1920x1080 (approximately 2070k pixels) pixels by area, keeping the aspect ratio unchanged, so the image will not be distorted;

(2) Crop the image by framing area (bright area), leaving only the image in the frame;

(3) Save the image to file.

### Description of the algorithm:

#### Determines the preview size:

Find the max size in the sizeArray, which meets the 16: 9 ratio, and no more than 1920x1080.

If not found, select the maximum size that meets the 16: 9 aspect ratio (the number of pixels may exceed 1920x1080).

If it is still not found, return the maximum Size.

Preview size should not be more than 1920x1080, otherwise the camera bandwidth will be tight. This is also the requirements of Camera2 API.

#### Determines the picture size:

Find the maximum Size from the sizeArray that satisfies the 16: 9 aspect ratio and does not exceed the number of pixels specified by maxPicturePixels.

If not found, select the maximum Size that meets the 16: 9 aspect ratio (the number of pixels may exceed maxPicturePixels).

If it is still not found, return the maximum Size.

maxPicturePixels is passed into by the extra of Intent.

#### Compressed to the specified resolution level:

Compresses the image file to the desired size and returns a Bitmap object.

If the original image size is smaller than the size to be compressed, return to the original.

In this method, the compression ratio is calculated by calculating the area ratio of the resolution. Considering that the aspect ratio of the picture may be different from the resolution specified in maxPicturePixels, there is no problem of picture orientation and aspect ratio.

#### Crop the specified area:

Crop the taken picture by framing area (bright area).

For a 1280x720 screen, the starting point for the crop is the point(52, 80). After cropping, the bitmap size is 896x588. (Calculated from the layout defined by layout xml)

The above parameters will be scale converted by the actual size of the picture.

No matter the device has a virtual navigation bar or not, the image can be cropped to the correct area.

#### Write Bitmap to file:

Write Bitmap to file. File is located at /data/data/${package-name}/cache/.

Compression to JPEG is hardware accelerated with O(1) time complexity, while compression to PNG is O(n) time complexity.

90% of the quality has been higher than the ultra-fine (85%) quality

#### FrescoUtils :
```
/**
* Helper method to fit AutoLayout.
* pass width(dp) and height(dp) of the image widget to resize(int, int) to downscale the image to the same size as the image widget.
*/
FrescoInner FrescoInner.resize(int widthDp, int heightDp);

/**
* Adds support of wrap_content / ViewGroup.LayoutParams.WRAP_CONTENT for DraweeView.
*/
void FrescoInner.into(SimpleDraweeView v);
```
