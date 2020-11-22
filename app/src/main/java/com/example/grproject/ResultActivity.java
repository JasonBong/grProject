package com.example.grproject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.features2d.AgastFeatureDetector;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ResultActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private final static String TAG ="ResultActivity";
    private boolean isOpenCvLoaded = false;

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    public native void CropImage(long addrImage, long addrResult,int[] pos);

    TextView resultText,textView;
    TessBaseAPI tessBaseAPI;

    FileInputStream inputStream = null;
    BufferedInputStream buf1 = null;

    //내가 자를 이미지 pos
    int[] pos = new int[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        final ImageView imageView = findViewById(R.id.imageView);
        final ImageView imageView2 = findViewById(R.id.imageView2);
        textView = findViewById(R.id.textView);
        resultText = findViewById(R.id.tv);

        Intent intent = getIntent(); /*데이터 수신*/

        pos = intent.getExtras().getIntArray("pos");

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( !isOpenCvLoaded )
                    return;

                try {

                    String SAVE_FOLDER = "/Pictures";

                    Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            null,"bucket_display_name='Pictures'",null,null);


                    String targetDir = "/mnt/sdcard/Pictures/sdcard/emulater/0/Pictures";
                    File file = new File(targetDir+"/1.jpg");
                    InputStream is = new FileInputStream(file);
                    buf1 = new BufferedInputStream(is);

                    Bitmap bitmap = BitmapFactory.decodeStream(buf1); // 이미지 원본 불러옴
                    //
                    Bitmap image1; // 출력될 이미지
                    Mat oriImg = new Mat();
                    Mat cropImg = new Mat();

                    Utils.bitmapToMat(bitmap,oriImg); // 이미지 복사했고

                    CropImage(oriImg.getNativeObjAddr(),cropImg.getNativeObjAddr(),pos);

                    image1 = Bitmap.createBitmap(cropImg.cols(),cropImg.rows(),Bitmap.Config.ARGB_8888);

                    //좌표 확인
                    String test = String.format("%d %d",cropImg.cols(),cropImg.rows());
                    textView.setText(test);

                    Utils.matToBitmap(cropImg,image1);
                    imageView2.setImageBitmap(image1);

                    //너무 잘넘어오고
                    //String str = String.format("%d %d %d %d %d %d",pos[0],pos[1],pos[2],pos[3],pos[4],pos[5]);//left,top,right,bottom,width,height
                    //textView.setText(str);


                   /*
                    Toast.makeText(getApplicationContext(),"opencv시작",Toast.LENGTH_LONG).show();
                    InputStream is = getAssets().open("te1.png");
                    Bitmap bitmap = BitmapFactory.decodeStream(is); // 이미지를 셋팅하는 법ㅁ
                    imageView.setImageBitmap(bitmap);

                    Mat gray = new Mat();
                    Utils.bitmapToMat(bitmap, gray);

                    Imgproc.cvtColor(gray, gray, Imgproc.COLOR_RGBA2GRAY);

                    Bitmap grayBitmap = Bitmap.createBitmap(gray.cols(), gray.rows(), null);
                    Utils.matToBitmap(gray, grayBitmap); //  Mat 비트맵으로 변환

                    imageView2.setImageBitmap(grayBitmap);

                    Mat image = new Mat();
                    Mat result = new Mat();

                    Utils.bitmapToMat(grayBitmap,image);
                    canny(image.getNativeObjAddr(),result.getNativeObjAddr());
                    Bitmap resultBitmap = Bitmap.createBitmap(result.cols(),result.rows(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(result,resultBitmap);
                    imageView2.setImageBitmap(resultBitmap);
                    */

                    //new AsyncTess().execute(grayBitmap); // 테서렉트API 실행

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //테서렉트
        tessBaseAPI = new TessBaseAPI();
        String dir = getFilesDir() + "/tesseract";
        if(checkLanguageFile(dir+"/tessdata"))
            tessBaseAPI.init(dir, "eng");
    }

    //테서렉트 관련
    boolean checkLanguageFile(String dir)
    {
        File file = new File(dir);
        if(!file.exists() && file.mkdirs())
            createFiles(dir);
        else if(file.exists()){
            String filePath = dir + "/eng.traineddata";
            File langDataFile = new File(filePath);
            if(!langDataFile.exists())
                createFiles(dir);
        }
        return true;
    }

    private void createFiles(String dir)
    {
        AssetManager assetMgr = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = assetMgr.open("eng.traineddata");

            String destFile = dir + "/eng.traineddata";

            outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class AsyncTess extends AsyncTask<Bitmap, Integer, String> {
        @Override
        protected String doInBackground(Bitmap... mRelativeParams) {
            tessBaseAPI.setImage(mRelativeParams[0]);
            return tessBaseAPI.getUTF8Text(); //이미지에서 추출된 문자
        }

        protected void onPostExecute(String result) {
            resultText.setText(result);
            Toast.makeText(getApplicationContext(), ""+result, Toast.LENGTH_LONG).show(); //토스트로 뛰우기

        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            isOpenCvLoaded = true;
        }
    }
}
