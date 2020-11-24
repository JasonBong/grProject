package com.example.grproject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
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
    String imgFileName = "drug1.jpg";
    private ImageView imageView;
    private ImageView imageView2;
    private static final int REQUEST_CODE = 0;

    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imageView = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.imageView2);
        textView = findViewById(R.id.textView);
        resultText = findViewById(R.id.tv);
        Button button = findViewById(R.id.button);

        Intent intent = getIntent();

        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures");

        File imageFile = new File(storageDir, "drug1.jpg");
        mCurrentPhotoPath = imageFile.getAbsolutePath();

        Bitmap rotatedBitmap;

        File file = new File(mCurrentPhotoPath);
        textView.setText(imgFileName);
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));

            ExifInterface ei = new ExifInterface(mCurrentPhotoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;
            }

            imageView2.setImageBitmap(rotatedBitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( !isOpenCvLoaded )
                    return;

               // Intent intent = new Intent();
                //intent.setType("image/*");
               // intent.setAction(Intent.ACTION_GET_CONTENT);
                //startActivityForResult(intent, REQUEST_CODE);

                    /*
                    String fpath = getRealPathFromURI(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    textView.setText(fpath + " " + file_title);

                    File file = new File(fpath+file_title);
                    InputStream is = new FileInputStream(file);
                    buf1 = new BufferedInputStream(is);

                    Bitmap bitmap = BitmapFactory.decodeStream(buf1); // 이미지 원본 불러옴
                    //

                    */

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



            }
        });

        //테서렉트
        tessBaseAPI = new TessBaseAPI();
        String dir = getFilesDir() + "/tesseract";
        if(checkLanguageFile(dir+"/tessdata"))
            tessBaseAPI.init(dir, "eng");
    }

    private Bitmap GetBinaryBitmap(Bitmap bitmap_src) {
        Bitmap bitmap_new=bitmap_src.copy(bitmap_src.getConfig(), true);
        for(int x=0; x<bitmap_new.getWidth(); x++) {
            for (int y = 0; y < bitmap_new.getHeight(); y++) {
                int color = bitmap_new.getPixel(x, y);
                color = GetNewColor(color);
                bitmap_new.setPixel(x, y, color);
            }
        }
        return bitmap_new;
    }

    private int GetNewColor(int c) {
        double dwhite=GetColorDistance(c,Color.WHITE);
        double dblack=GetColorDistance(c,Color.BLACK)*0.3;
        if(dwhite<=dblack) {
            return Color.WHITE;
        }
        else {
            return Color.BLACK;
        }
    }

    private double GetColorDistance(int c1, int c2) {
        int db= Color.blue(c1)-Color.blue(c2);
        int dg=Color.green(c1)-Color.green(c2);
        int dr=Color.red(c1)-Color.red(c2);
        double d=Math.sqrt(  Math.pow(db, 2) + Math.pow(dg, 2) +Math.pow(dr, 2)  );
        return d;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    InputStream in = getContentResolver().openInputStream(data.getData());

                    Bitmap img = BitmapFactory.decodeStream(in);
                    in.close();

                    Mat oriImg = new Mat();
                    Mat cropImg = new Mat();

                    Utils.bitmapToMat(img,oriImg); // 이미지 복사했고
                    CropImage(oriImg.getNativeObjAddr(),cropImg.getNativeObjAddr(),pos);
                    Bitmap image1 = Bitmap.createBitmap(cropImg.cols(),cropImg.rows(),Bitmap.Config.ARGB_8888);

                    //좌표 확인
                    String test = String.format("%d %d",cropImg.cols(),cropImg.rows());
                    textView.setText(test);

                    Utils.matToBitmap(cropImg,image1);
                    image1 = GetBinaryBitmap(image1);
                    imageView2.setImageBitmap(image1);

                    new AsyncTess().execute(image1); // 테서렉트API 실행
                    //imageView.setImageBitmap(img);
                } catch (Exception e) {

                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "사진 선택 취소", Toast.LENGTH_LONG).show();
            }
        }
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
            String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
            result = result.replaceAll(match, " ");
            result = result.replaceAll(" ", "");

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
