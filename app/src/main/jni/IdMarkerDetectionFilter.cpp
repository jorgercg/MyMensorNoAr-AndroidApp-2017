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
        if (lostTrackingCounter < 20){
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
            mPose[6] = (float) ids[0]; // marker currently being tracked
            lostTrackingCounter = 0;
            LOGD("POSE: Id#%f x=%f y=%f z=%f rx=%f ry=%f rz=%f",mPose[6], mPose[0],mPose[1],mPose[2],mPose[3],mPose[4],mPose[5]);
            if (isHudOn==1) {
                cv::aruco::drawDetectedMarkers(src, corners, ids);
                cv::aruco::drawAxis(src, cameraMatrix, mDistCoeffs, mRVec[0], mTVec[0], markerLength * 0.5f);
                //draw(src,isHudOn);
            }
        }
    } else {
        mTracking = false;
    }


    /*
    int k = 0;
    do {
        cv::Mat localReferenceDescriptors;
        mReferenceDescriptors[k].copyTo(localReferenceDescriptors);
        matcher.match(mSceneDescriptors, localReferenceDescriptors, mMatches);
        //mDescriptorMatcher->match(mSceneDescriptors, localReferenceDescriptors, mMatches);
        //LOGD("Searching for VP: %d    Matches Found: %d", (k + 1), mMatches.size());
        // Attempt to find the target image's 3D pose in the scene.
        if (mMatches.size() >= 4) {
            // There are sufficient matches to find the pose.
            // Calculate the max and min distances between keypoints.
            float maxDist = 0.0f;
            float minDist = FLT_MAX;
            for (int i = 0; i < mMatches.size(); i++) {
                cv::DMatch match = mMatches[i];
                float dist = match.distance;
                if (dist < minDist) {
                    minDist = dist;
                }
                if (dist > maxDist) {
                    maxDist = dist;
                }
            }
            // The thresholds for minDist are chosen subjectively
            // based on testing. The unit is not related to pixel
            // distances; it is related to the number of failed tests
            // for similarity between the matched descriptors.
            localReferenceKeypoints = mReferenceKeypoints[k];
            if (minDist <= 25.0) {
                // Identify "good" keypoints based on match distance.
                double maxGoodMatchDist = 1.75 * minDist;
                for (int i = 0; i < mMatches.size(); i++) {
                    cv::DMatch match = mMatches[i];
                    if (match.distance < maxGoodMatchDist) {
                        goodReferencePoints.push_back(localReferenceKeypoints[match.trainIdx].pt);
                        goodScenePoints.push_back(mSceneKeypoints[match.queryIdx].pt);
                        //LOGD("Good Match from VP = %d  match.imgIdx %d  match.distance %f", (k+1), match.imgIdx, match.distance);
                    }
                }
                if (goodReferencePoints.size() > 6 && goodScenePoints.size() > 6) {
                    // There are sufficient good points to find the pose.
                    // Find the homography.
                    //LOGD("goodReferencePoints.size(): %d  goodScenePoints.size(): %d ", goodReferencePoints.size(), goodScenePoints.size());
                    cv::Mat homography = cv::findHomography(goodReferencePoints, goodScenePoints);
                    // Use the homography to project the reference corner
                    // coordinates into scene coordinates.
                    //LOGD("mReferenceCorners.cols: %d  mCandidateSceneCorners.cols: %d homography.cols: %d", mReferenceCorners.dims, mCandidateSceneCorners.dims, homography.cols);
                    if (((mCandidateSceneCorners.dims)+1)==homography.cols){
                        cv::perspectiveTransform(mReferenceCorners, mCandidateSceneCorners, homography);
                        // Check whether the corners form a convex polygon. If not,
                        // (that is, if the corners form a concave polygon), the
                        // detection result is invalid because no real perspective can
                        // make the corners of a rectangular image look like a concave
                        // polygon!
                        goodReferencePoints.clear();
                        goodScenePoints.clear();
                        if (cv::isContourConvex(mCandidateSceneCorners)) {
                            // Find the target's Euler angles and XYZ coordinates.
                            //LOGD("mCandidateSceneCorners.type() = %d", mCandidateSceneCorners.type());
                            cv::solvePnP(mReferenceCorners3D, mCandidateSceneCorners, cameraMatrix,
                                         mDistCoeffs, mRVec, mTVec, 0);
                            mPose[0] = (float) mTVec.at<double>(0);// X Translation
                            mPose[1] = (float) mTVec.at<double>(1);// Y Translation
                            mPose[2] = (float) mTVec.at<double>(2);// Z Translation
                            mPose[3] = (float) mRVec.at<double>(0);// X Rotation
                            mPose[4] = (float) mRVec.at<double>(1);// Y Rotation
                            mPose[5] = (float) mRVec.at<double>(2);// Z Rotation
                            mPose[6] = (float) (k + 1); // VP currently being tracked
                            mTracking = true;
                            lostTrackingCounter = 0;
                            LOGD("POSE: VP#%f x=%f y=%f z=%f rx=%f ry=%f rz=%f",mPose[6], mPose[0]+440,mPose[1]+160,mPose[2],mPose[3],mPose[4],mPose[5]);
                            draw(mCandidateSceneCorners, src, isHudOn);
                        } else {
                            mTracking = false;
                        }
                    } else {
                        mTracking = false;
                    }
                } else {
                    mTracking = false;
                }
            } else {
                mTracking = false;
            }
        }
        k++;
        if (!mTracking) lostTrackingCounter++;
    } while ((k < qtVp) && (!mTracking));

    */


}

/*

void IdMarkerDetectionFilter::draw(cv::Mat src, int isHudOn)
{
    if (((mTracking) || ((!mTracking)&&(lostTrackingCounter<12)))&&(isHudOn==1)) {
        // The target has been found.
        LOGD("isHudOn= %d",isHudOn);
        // Draw the rectangle guide in SeaMate green.
        cv::rectangle(src, rect,cv::Scalar(168.0,207.0,69.0), 8);
        cv::line(src, cv::Point2d(640.0,160.0), cv::Point2d(640.0,120.0), cv::Scalar(168.0,207.0,69.0), 8);
        cv::line(src, cv::Point2d(640.0,120.0), cv::Point2d(620.0,140.0), cv::Scalar(168.0,207.0,69.0), 8);
        cv::line(src, cv::Point2d(640.0,120.0), cv::Point2d(660.0,140.0), cv::Scalar(168.0,207.0,69.0), 8);
    }
}

*/