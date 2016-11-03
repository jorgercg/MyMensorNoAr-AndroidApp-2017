#include <jni.h>

#include "ImageDetectionFilter.hpp"

using namespace mymensor;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_mymensor_filters_ImageDetectionFilter_newSelf(
        JNIEnv *env, jclass clazz, jlong referenceImageBGRAddr,
        jdouble realSize)
{
    cv::Mat &referenceImageBGR =  *(cv::Mat *)referenceImageBGRAddr;
    ImageDetectionFilter *self = new ImageDetectionFilter(
            referenceImageBGR, realSize);
    return (jlong)self;
}

JNIEXPORT void JNICALL
Java_com_mymensor_filters_ImageDetectionFilter_deleteSelf(
        JNIEnv *env, jclass clazz, jlong selfAddr)
{
    if (selfAddr != 0)
    {
        ImageDetectionFilter *self = (ImageDetectionFilter *)selfAddr;
        delete self;
    }
}

JNIEXPORT jfloatArray JNICALL
Java_com_mymensor_filters_ImageDetectionFilter_getGLPose(
        JNIEnv *env, jclass clazz, jlong selfAddr)
{
    if (selfAddr == 0)
    {
        return NULL;
    }

    ImageDetectionFilter *self = (ImageDetectionFilter *)selfAddr;
    float *glPoseNative = self->getGLPose();
    if (glPoseNative == NULL)
    {
        return NULL;
    }

    jfloatArray glPoseJava = env->NewFloatArray(16);
    if (glPoseJava != NULL)
    {
        env->SetFloatArrayRegion(glPoseJava, 0, 16, glPoseNative);
    }
    return glPoseJava;
}

JNIEXPORT void JNICALL
Java_com_mymensor_filters_ImageDetectionFilter_apply(JNIEnv *env, jclass clazz, jlong selfAddr, jlong srcAddr, jlong projectionAddr)
{
    if (selfAddr != 0)
    {
        ImageDetectionFilter *self = (ImageDetectionFilter *)selfAddr;
        cv::Mat &src = *(cv::Mat *)srcAddr;
        cv::Mat &projection = *(cv::Mat *)projectionAddr;
        self->apply(src, projection);
    }
}

} // extern "C"
