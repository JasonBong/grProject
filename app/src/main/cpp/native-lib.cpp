#include <jni.h>
#include <string>

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