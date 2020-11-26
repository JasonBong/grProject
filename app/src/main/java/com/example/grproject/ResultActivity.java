package com.example.grproject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
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

    private static final int MY_PERMISSION_CAMERA = 1111;
    private static final int REQUEST_TAKE_PHOTO = 2222;
    private static final int REQUEST_TAKE_ALBUM = 3333;
    private static final int REQUEST_IMAGE_CROP = 4444;
    private static final int REQUEST_RESULT = 5555;

    private final static String TAG ="ResultActivity";
    private boolean isOpenCvLoaded = false;

    public native void CropImage(long addrImage, long addrResult,int[] pos);
    public native void BoxFilterImg(long addrImage, long addrResult);
    TextView resultText,textView;
    TessBaseAPI tessBaseAPI;

    Button cropBtn;//이미지 자르기 버튼
    Button recoBtn;//텍스트 인식 버튼

    Uri imageUri;
    Uri photoURI, albumURI;

    //이미지 정보
    int[] pos = new int[6];
    String imgFileName = "drug1.jpg";
    private ImageView imageView;
    private static final int REQUEST_CODE = 0;

    String mCurrentPhotoPath;
    Bitmap rotatedBitmap;
    Bitmap recoginPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        cropBtn = findViewById(R.id.cropBtn);
        recoBtn = findViewById(R.id.recoginBtn);

        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.tv);

        openMyImageFile(); // 사진찍기에서 최초 사진파일을 이미지뷰로 출력

        //이미지 자르기 버튼
        cropBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, REQUEST_TAKE_ALBUM);
                imageView.setImageBitmap(rotatedBitmap);
            }
        });

        //텍스트 인식 버튼
        recoBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                recoBtn.setText("인식 중");
                Mat ori = new Mat();
                Mat res = new Mat();//결과물

                Utils.bitmapToMat(recoginPicture,ori);//이미지 복사
                Mat gray = new Mat();
                Utils.bitmapToMat(recoginPicture, gray);

                Bitmap grayBitmap = Bitmap.createBitmap(gray.cols(), gray.rows(), null);
                // 윗 부분 오류발생하면 마지막 param null 대신 Bitmap.Config.ARGB_8888
                //Utils.matToBitmap(gray, grayBitmap);

                BoxFilterImg(ori.getNativeObjAddr(),res.getNativeObjAddr());
                Utils.matToBitmap(res,grayBitmap);

                imageView.setImageBitmap(grayBitmap);
                //recoginPicture = GetBinaryBitmap(recoginPicture);
                new AsyncTess().execute(grayBitmap);
            }
        });

        //테서렉
        tessBaseAPI = new TessBaseAPI();
        String dir = getFilesDir() + "/tesseract";
        if(checkLanguageFile(dir+"/tessdata"))
            tessBaseAPI.init(dir, "eng");
    }

    public void openMyImageFile(){
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures");
        File imageFile = new File(storageDir, "drug1.jpg");
        mCurrentPhotoPath = imageFile.getAbsolutePath();

        rotatedBitmap = null;

        File file = new File(mCurrentPhotoPath);
        //textView.setText(imgFileName);
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
            recoginPicture = rotatedBitmap;
            imageView.setImageBitmap(rotatedBitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        double dblack=GetColorDistance(c,Color.BLACK)*0.4;
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
        switch (requestCode) {
            case REQUEST_TAKE_ALBUM:
                if (resultCode == Activity.RESULT_OK) {

                    if (data.getData() != null) {
                        try {
                            File albumFile = null;
                            File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures");
                            albumFile = new File(storageDir,"drug1.jpg");
                            photoURI = data.getData();
                            albumURI = Uri.fromFile(albumFile);
                            cropImage();
                        } catch (Exception e) {
                            Log.e("TAKE_ALBUM_SINGLE ERROR", e.toString());
                        }
                    }
                }
                break;

            case REQUEST_IMAGE_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    galleryAddPic();
                    imageView.setImageURI(albumURI);
                    try {
                        recoginPicture=MediaStore.Images.Media.getBitmap(getContentResolver(),albumURI);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }

    }

    private void galleryAddPic(){
        Log.i("galleryAddPic", "Call");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        // 해당 경로에 있는 파일을 객체화(새로 파일을 만든다는 것으로 이해하면 안 됨)
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show();

    }
    public void cropImage(){
        Log.i("cropImage", "Call");
        Log.i("cropImage", "photoURI : " + photoURI + " / albumURI : " + albumURI);

        Intent cropIntent = new Intent("com.android.camera.action.CROP");

        // 50x50픽셀미만은 편집할 수 없다는 문구 처리 + 갤러리, 포토 둘다 호환하는 방법
        cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropIntent.setDataAndType(photoURI, "image/*");
        //cropIntent.putExtra("outputX", 200); // crop한 이미지의 x축 크기, 결과물의 크기
        //cropIntent.putExtra("outputY", 200); // crop한 이미지의 y축 크기
        cropIntent.putExtra("aspectX", 1); // crop 박스의 x축 비율, 1&1이면 정사각형
        cropIntent.putExtra("aspectY", 1); // crop 박스의 y축 비율
        cropIntent.putExtra("scale", true);
        cropIntent.putExtra("output", albumURI); // 크랍된 이미지를 해당 경로에 저장
        startActivityForResult(cropIntent, REQUEST_IMAGE_CROP);
    }
    //테서렉트 관련
    boolean checkLanguageFile(String dir)
    {
        File file = new File(dir);
        if(!file.exists() && file.mkdirs())
            createFiles(dir);
        else if(file.exists()){
            String filePath = dir + "/eng.traineddata";  //테서렉트 학습된 데이터
            File langDataFile = new File(filePath);
            if(!langDataFile.exists())
                createFiles(dir);
        }
        return true;
    }

    private void createFiles(String dir)  //테서렉트 파일 이용
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

    private class AsyncTess extends AsyncTask<Bitmap, Integer, String> { //테서렉트 실행 시
        @Override
        protected String doInBackground(Bitmap... mRelativeParams) {
            tessBaseAPI.setImage(mRelativeParams[0]);
            return tessBaseAPI.getUTF8Text(); //이미지에서 추출된 문자
        }

        protected void onPostExecute(String result) {
            String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
            result = result.replaceAll(match, " ");
            result = result.replaceAll(" ", "");

            resultText.setText("인식된 텍스트 : "+result);
            recoBtn.setText("텍스트 인식하기");

            Intent intent = new Intent(getApplicationContext(),PillResultActivity.class);
            String str = result;
            intent.putExtra("recoginText",result);
            startActivity(intent);
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
