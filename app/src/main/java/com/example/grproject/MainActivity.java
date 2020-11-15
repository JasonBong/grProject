package com.example.grproject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.CAMERA;


public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    //test
    private static final String TAG = "opencv";
    private Mat matInput;
    private Mat matResult;

    private Mat matInput1, matInput2;
    private Mat matMask = null, matNotMask = null;
    Rect rect = new Rect();
    private int step = -1;

    private CameraBridgeViewBase mOpenCvCameraView;

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);


    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
        mOpenCvCameraView.setOnTouchListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();

        if (matResult == null)
            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        if (matInput1 == null)
            matInput1 = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        if (matInput2 == null)
            matInput2 = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        if (matNotMask == null)
            matNotMask = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        if (step == 1) {
            Imgproc.circle(matInput, new Point(rect.x, rect.y), 20, new Scalar(0, 255, 0, 255), -1);
            return matInput;
        } else if (step == 2) {
            if (matMask != null) {
                Core.bitwise_and(matInput, matMask, matInput1);
                ConvertRGBtoGray(matInput1.getNativeObjAddr(), matInput1.getNativeObjAddr());

                if (matMask == null || matNotMask == null) {
                    return matInput;
                }

                Core.bitwise_not(matMask, matNotMask);
                Core.bitwise_and(matInput, matNotMask, matInput2);
                Imgproc.cvtColor(matInput1, matInput1, Imgproc.COLOR_GRAY2RGBA);
                Core.bitwise_or(matInput1, matInput2, matResult);
                return matResult;
            }
        }
        return matInput;
    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase : cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override

    public boolean onTouch(View v, MotionEvent event) {
        int width = matInput.cols();
        int height = matInput.rows();

        // 카메라 뷰와 이미지 좌표 맞추기
        int xOffset = (mOpenCvCameraView.getWidth() - width) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - height) / 2;
        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;


        if (x < 0 || y < 0 || x >= width || y >= height) {// 터치가 범위 벗어난 경우
            step = -1;
            rect.x = rect.y = rect.width = rect.height = 0;
            matMask = null;
            return false;
        }
        if (step == 2) { // ROI 설정된 상태에서 또 터치하면 설정 취소
            step = -1;
            rect.x = rect.y = rect.width = rect.height = 0;
            matMask = null;
            return false;
        }
        if ((step == -1) && ((rect.x == 0 && rect.y == 0) || (rect.width != 0 && rect.height != 0))) { //첫번째 클릭
            step = 1;
            matMask = null;
            rect.x = x;
            rect.y = y;
            rect.width = rect.height = 0;
        } else if (step == 1) {  //두번째 클릭
            rect.width = x - rect.x;
            rect.height = y - rect.y;
            if (rect.width <= 0 || rect.height <= 0) {  // 잘못 두번쨰 좌표 선택한 경우 취소
                step = -1;
                rect.x = rect.y = rect.width = rect.height = 0;
                matMask = null;
                return false;
            }
            step = 2;
            matMask = null;
            matMask = Mat.zeros(matInput.size(), matInput.type());
            matMask.submat(rect).setTo(Scalar.all(255));
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        } else {
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }


}