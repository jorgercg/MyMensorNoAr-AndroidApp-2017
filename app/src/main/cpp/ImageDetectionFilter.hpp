#ifndef IMAGE_DETECTION_FILTER
#define IMAGE_DETECTION_FILTER

#include <vector>

#include <android/log.h>
#define  LOG_TAG    "NATIVE ImgDetFilter"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


#include <opencv2/core/core.hpp>
#include <opencv2/features2d/features2d.hpp>

namespace mymensor {

class ImageDetectionFilter
{
public:
    ImageDetectionFilter(std::vector<cv::Mat> &referenceImageBGR, int qtyVps, double realSize);
    float *getPose();
    void apply(cv::Mat &src, int isHudOn, int isSingleimage, cv::Mat &projection);

private:
    //void findPose(cv::Mat &projection);
    void draw(cv::Mat sceneCorners , cv::Mat src, int isHudOn);

    // The reference image (this detector's target).
    cv::Mat mReferenceImage;
    // Features of the reference image.
    std::vector<std::vector<cv::KeyPoint> > mReferenceKeypoints;
    std::vector<cv::KeyPoint>  localReferenceKeypoints;
    // Descriptors of the reference image's features.
    std::vector<cv::Mat> mReferenceDescriptors;
    cv::Mat localReferenceDescriptors;
    // The corner coordinates of the reference image, in pixels.
    cv::Mat mReferenceCorners;
    // The reference image's corner coordinates, in 3D, in real
    // units.
    std::vector<cv::Point3f> mReferenceCorners3D;

    // Features of the scene (the current frame).
    std::vector<cv::KeyPoint> mSceneKeypoints;
    // Descriptors of the scene's features.
    cv::Mat mSceneDescriptors;
    // Tentative corner coordinates detected in the scene, in
    // pixels.
    cv::Mat mCandidateSceneCorners;

    // A grayscale version of the scene.
    cv::Mat mGraySrc;
    // Tentative matches of scene features and reference features.
    std::vector<cv::DMatch> mMatches;

    // A feature detector, which finds features in images, and
    // descriptor extractor, which creates descriptors of features.
    cv::Ptr<cv::Feature2D> mFeatureDetectorAndDescriptorExtractor;
    // A descriptor matcher, which matches features based on their
    // descriptors.
    cv::Ptr<cv::DescriptorMatcher> mDescriptorMatcher;




    // Distortion coefficients of the camera's lens.
    cv::Mat mDistCoeffs;

    // The Euler angles of the detected target.
    cv::Mat mRVec;
    // The XYZ coordinates of the detected target.
    cv::Mat mTVec;
    // The rotation matrix of the detected target.
    cv::Mat mRotation;
    // The MyMensor Tracking vector of the detected marker.
    float mPose[7];
    float mLastValidPose[7];

    // Whether a marker is currently being tracked.
    bool mTracking;
    int lostTrackingCounter;

    CvRect rect;

    int qtVp;

    long int frame;

    int dampIndex;
    float mPose_0[8];
    float mPose_1[8];
    float mPose_2[8];
    float mPose_3[8];
    float mPose_4[8];
    float mPose_5[8];

    std::vector<cv::Point2f> goodReferencePoints;
    std::vector<cv::Point2f> goodScenePoints;

    const int xShift = 440; //440;
    const int yShift = 160; //160;
    const int rectWidth = 400; //400;
    const int rectHeight = 400; //400;


};

} // namespace mymensor

#endif // IMAGE_DETECTION_FILTER
