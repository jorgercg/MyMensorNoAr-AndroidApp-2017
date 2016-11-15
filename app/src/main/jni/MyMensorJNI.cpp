#include <jni.h>

#include "ImageDetectionFilter.hpp"
#include "VpConfigureFilter.hpp"

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
Java_com_mymensor_filters_ImageDetectionFilter_getPose(
        JNIEnv *env, jclass clazz, jlong selfAddr)
{
    if (selfAddr == 0)
    {
        return NULL;
    }

    ImageDetectionFilter *self = (ImageDetectionFilter *)selfAddr;
    float *poseNative = self->getPose();
    if (poseNative == NULL)
    {
        return NULL;
    }

    jfloatArray poseJava = env->NewFloatArray(6);
    if (poseJava != NULL)
    {
        env->SetFloatArrayRegion(poseJava, 0, 6, poseNative);
    }
    return poseJava;
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

JNIEXPORT jlong JNICALL
Java_com_mymensor_filters_VpConfigFilter_newSelf(JNIEnv *env, jclass clazz,
                                                 jlong referenceImageBGRAddr, jdouble realSize) {
    cv::Mat &referenceImageBGR =  *(cv::Mat *)referenceImageBGRAddr;
    VpConfigureFilter *self = new VpConfigureFilter(referenceImageBGR, realSize);
    return (jlong)self;
}

JNIEXPORT void JNICALL
Java_com_mymensor_filters_VpConfigFilter_deleteSelf(JNIEnv *env, jclass clazz, jlong selfAddr) {
    if (selfAddr != 0)
    {
        VpConfigureFilter *self = (VpConfigureFilter *)selfAddr;
        delete self;
    }
}

JNIEXPORT jfloatArray JNICALL
Java_com_mymensor_filters_VpConfigFilter_getPose__J(JNIEnv *env, jclass clazz, jlong selfAddr) {
    if (selfAddr == 0)
    {
        return NULL;
    }

    VpConfigureFilter *self = (VpConfigureFilter *)selfAddr;
    float *poseNative = self->getPose();
    if (poseNative == NULL)
    {
        return NULL;
    }

    jfloatArray poseJava = env->NewFloatArray(6);
    if (poseJava != NULL)
    {
        env->SetFloatArrayRegion(poseJava, 0, 6, poseNative);
    }
    return poseJava;
}

JNIEXPORT void JNICALL
Java_com_mymensor_filters_VpConfigFilter_apply__JJJ(JNIEnv *env, jclass type, jlong selfAddr, jlong srcAddr, jlong projectionAddr)
{
    if (selfAddr != 0)
    {
        VpConfigureFilter *self = (VpConfigureFilter *)selfAddr;
        cv::Mat &src = *(cv::Mat *)srcAddr;
        cv::Mat &projection = *(cv::Mat *)projectionAddr;
        self->apply(src, projection);
    }
}



} // extern "C"
