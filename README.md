# intruder-detector
OpenCV Android project to detect human body and send alert accordingly

1. Download OpenCV [3.4.12 here](https://sourceforge.net/projects/opencvlibrary/files/3.4.12/opencv-3.4.12-android-sdk.zip/download).
2. Extract the folder and copy the contents of `OpenCV-android-sdk > sdk` into root directory `openCV3412` except for `build.gradle` file.
3. The downloaded `OpenCV-android-sdk` contains `apk` folder that you can install an apk from or you can install the one in root folder of this git repository: `adb install OpenCV_3.4.12_Manager_3.49_arm64-v8a.apk`.
4. Update the file `java > org > opencv > android > StaticHelper.java`. Change line 95 to `result = loadLibrary("opencv_java");` removing the 3 from `_java3`.
5. Rename all the files in nested folders of `openCV3412\native\libs`, removing the 3 from `..._java3.so`.


