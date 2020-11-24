#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <vector>
#include <android/log.h>
#include <opencv2/opencv.hpp>

using namespace cv;

/*
extern "C"
JNIEXPORT void JNICALL
Java_com_example_grproject_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                jlong mat_addr_input,
                                jlong mat_addr_result) {

    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);

}
*/
/*
extern "C"
JNIEXPORT void JNICALL
Java_com_example_grproject_MainActivity_imageprocessing(JNIEnv *env, jobject thiz,
                                                        jlong input_image, jlong output_image,
                                                        jint th1, jint th2) {

    Mat &img_input = *(Mat *) input_image;
    Mat &img_output = *(Mat *) output_image;

    cvtColor( img_input, img_output, COLOR_RGB2GRAY);
    blur( img_output, img_output, Size(5,5) );
    Canny( img_output, img_output, th1, th2);
}
*/

extern "C"
JNIEXPORT void JNICALL
Java_com_example_grproject_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                         jlong mat_addr_input,
                                                         jlong mat_addr_result) {
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);

    blur( matResult, matResult, Size(5,5));

    Canny( matResult, matResult, 50, 150);
}

//테스트용
extern "C"
JNIEXPORT void JNICALL
Java_com_example_grproject_ResultActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                           jlong mat_addr_input,
                                                           jlong mat_addr_result) {
    // TODO: implement ConvertRGBtoGray()
    Mat &matInput = *(Mat *)mat_addr_input;
    Mat &matResult = *(Mat *)mat_addr_result;

    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_example_grproject_ResultActivity_CropImage(JNIEnv *env, jobject thiz, jlong addr_image,
                                                    jlong addr_result, jintArray pos) {
    // TODO: implement CropImage()

    Mat &matInput = *(Mat *)addr_image;
    Mat &matResult = *(Mat *)addr_result;
    jint *int_buf;
    int size;
    int sum,i;
    size = env->GetArrayLength(pos);
    int_buf = env->GetIntArrayElements(pos,NULL);

    int x1 = int_buf[0];//left
    int y1 = int_buf[1];//top

    int x2 = int_buf[2];//right
    int y2 = int_buf[3];//bottom

    resize(matInput,matInput,Size(1440,2560),0,0);
    Rect rect(x1,y1,300,300);
    matResult = matInput(rect);

    //이미지 전처리
    //resize(matResult,matResult,size(3,3),1.2, 1.2,INTER_CUBIC);
    cvtColor(matResult,matResult, COLOR_BGR2GRAY);


}