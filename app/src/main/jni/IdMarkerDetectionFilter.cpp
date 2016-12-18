#include <float.h>

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/aruco.hpp>

#include "IdMarkerDetectionFilter.hpp"

using namespace mymensor;

IdMarkerDetectionFilter::IdMarkerDetectionFilter(int qtyVps, float realSize)
{
    qtVp = qtyVps;
    markerLength = realSize;
    dictionary = cv::aruco::getPredefinedDictionary(cv::aruco::PREDEFINED_DICTIONARY_NAME(cv::aruco::DICT_ARUCO_ORIGINAL));
    mCandidateSceneCorners.create(4, 1, CV_32FC2);
    // Assume no distortion.
    mDistCoeffs.zeros(4, 1, CV_64F);
    mTracking = false;
}

float *IdMarkerDetectionFilter::getPose()
{
    if (mTracking) {
        mLastValidPose[0]=mPose[0];
        mLastValidPose[1]=mPose[1];
        mLastValidPose[2]=mPose[2];
        mLastValidPose[3]=mPose[3];
        mLastValidPose[4]=mPose[4];
        mLastValidPose[5]=mPose[5];
        mLastValidPose[6]=mPose[6];
        return mPose;
    } else {
        if (lostTrackingCounter < 5){
            return mLastValidPose;
        } else {
            return NULL;
        }
    }
}

void IdMarkerDetectionFilter::apply(cv::Mat &src, int isHudOn, cv::Mat &cameraMatrix) {

    // Convert the scene from RGBA to RGB (ArUco requirement).
    cv::cvtColor(src, src, cv::COLOR_RGBA2RGB);

    std::vector<int> ids;

    cv::aruco::detectMarkers(src, dictionary, corners, ids);

    if (ids.size() > 0) {
        mTracking = true;
        if (ids.size()==1){
            cv::aruco::estimatePoseSingleMarkers(corners, markerLength, cameraMatrix, mDistCoeffs, mRVec, mTVec);
            mPose[0] = (float) mTVec[0](0);// X Translation
            mPose[1] = (float) mTVec[0](1);// Y Translation
            mPose[2] = (float) mTVec[0](2);// Z Translation
            mPose[3] = (float) mRVec[0](0);// X Rotation
            mPose[4] = (float) mRVec[0](1);// Y Rotation
            mPose[5] = (float) mRVec[0](2);// Z Rotation
            mPose[6] = (float) ((ids[0])/10); // marker currently being tracked
            lostTrackingCounter = 0;
            LOGD("POSE: Id#%f x=%f y=%f z=%f rx=%f ry=%f rz=%f",mPose[6], mPose[0],mPose[1],mPose[2],mPose[3]*180/3.141592,mPose[4]*180/3.141592,mPose[5]*180/3.141592);
            if (isHudOn==1) {
                cv::aruco::drawDetectedMarkers(src, corners, ids);
                cv::aruco::drawAxis(src, cameraMatrix, mDistCoeffs, mRVec[0], mTVec[0], markerLength * 0.5f);

            }
        }
    } else {
        mTracking = false;
        lostTrackingCounter++;
    }

}