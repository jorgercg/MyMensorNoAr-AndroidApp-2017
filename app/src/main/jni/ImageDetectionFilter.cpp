#include <float.h>

#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>

#include "ImageDetectionFilter.hpp"

using namespace mymensor;

ImageDetectionFilter::ImageDetectionFilter(std::vector<cv::Mat> &referenceImageGray, int qtyVps, double realSize)
{
    qtVp = qtyVps;

    // All reference images are with the same size

    int cols = referenceImageGray[0].cols;
    int rows = referenceImageGray[0].rows;

    // Store the reference image's corner coordinates, in pixels.
    mReferenceCorners.create(4, 1, CV_32FC2);
    mReferenceCorners.at<cv::Vec2f>(0, 0)[0] = 0.0f;
    mReferenceCorners.at<cv::Vec2f>(0, 0)[1] = 0.0f;
    mReferenceCorners.at<cv::Vec2f>(1, 0)[0] = cols;
    mReferenceCorners.at<cv::Vec2f>(1, 0)[1] = 0.0f;
    mReferenceCorners.at<cv::Vec2f>(2, 0)[0] = cols;
    mReferenceCorners.at<cv::Vec2f>(2, 0)[1] = rows;
    mReferenceCorners.at<cv::Vec2f>(3, 0)[0] = 0.0f;
    mReferenceCorners.at<cv::Vec2f>(3, 0)[1] = rows;

    // Compute the image's width and height in real units, based
    // on the specified real size of the image's smaller dimension.
    float aspectRatio = (float)cols /(float)rows;
    float halfRealWidth;
    float halfRealHeight;
    if (cols > rows) {
        halfRealHeight = 0.5f * realSize;
        halfRealWidth = halfRealHeight * aspectRatio;
    } else {
        halfRealWidth = 0.5f * realSize;
        halfRealHeight = halfRealWidth / aspectRatio;
    }

    // Define the real corner coordinates of the printed image
    // so that it normally lies in the xy plane (like a painting
    // or poster on a wall).
    // That is, +z normally points out of the page toward the
    // viewer.
    mReferenceCorners3D.push_back(cv::Point3f(-halfRealWidth, -halfRealHeight, 0.0f));
    mReferenceCorners3D.push_back(cv::Point3f( halfRealWidth, -halfRealHeight, 0.0f));
    mReferenceCorners3D.push_back(cv::Point3f( halfRealWidth,  halfRealHeight, 0.0f));
    mReferenceCorners3D.push_back(cv::Point3f(-halfRealWidth,  halfRealHeight, 0.0f));

    // Create the feature detector, descriptor extractor, and
    // descriptor matcher.
    mFeatureDetectorAndDescriptorExtractor = cv::ORB::create(800,1.2f,8,31,0,2,cv::ORB::FAST_SCORE,31,20);
    //mDescriptorMatcher = cv::DescriptorMatcher::create("BruteForce-HammingLUT");

    for (int i=0; i<qtVp; i++){
        // Detect the reference features and compute their descriptors.
        mReferenceImage = referenceImageGray[i];
        mFeatureDetectorAndDescriptorExtractor->detect(mReferenceImage, localReferenceKeypoints);
        mFeatureDetectorAndDescriptorExtractor->compute(mReferenceImage, localReferenceKeypoints, localReferenceDescriptors);
        mReferenceDescriptors.push_back(localReferenceDescriptors);
        mReferenceKeypoints.push_back(localReferenceKeypoints);
    }
    mCandidateSceneCorners.create(4, 1, CV_32FC2);
    // Assume no distortion.
    mDistCoeffs.zeros(4, 1, CV_64F);
    mTracking = false;

    frame=0;
}

float *ImageDetectionFilter::getPose()
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

void ImageDetectionFilter::apply(cv::Mat &src, int isHudOn, int isSingleImage, cv::Mat &cameraMatrix) {

    LOGD("Frame=%d Remainder=%d",frame,frame%6);

    cv::FlannBasedMatcher matcher(new cv::flann::LshIndexParams(6, 12, 0)); //matcher(new cv::flann::LshIndexParams(6, 12, 1));
    // Convert the scene to grayscale.
    cv::cvtColor(src, mGraySrc, cv::COLOR_RGBA2GRAY);
    // Get only the center of the image to be used for detection.
    rect = CvRect(xShift, yShift, rectWidth, rectHeight);
    mGraySrc = mGraySrc(rect);
    // Detect the scene features, compute their descriptors,
    // and match the scene descriptors to reference descriptors.
    mFeatureDetectorAndDescriptorExtractor->detect(mGraySrc, mSceneKeypoints);
    mFeatureDetectorAndDescriptorExtractor->compute(mGraySrc, mSceneKeypoints, mSceneDescriptors);
    if (frame>9) frame=0;
    int k = frame * 3;
    int kmax = k + 2;
    if (k>=qtVp) {
        frame = 0;
        k = frame * 3;
        kmax = k + 2;
    }
    do {
        LOGD("k=%d kmax=%d",k,kmax);
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
                            if (isSingleImage == 0){
                                mPose[6] = (float) (k + 1); // VP currently being tracked
                            } else {
                                mPose[6] = (float) (isSingleImage);
                            }
                            mTracking = true;
                            lostTrackingCounter = 0;
                            LOGD("POSE: VP#%f x=%f y=%f z=%f rx=%f ry=%f rz=%f",mPose[6], mPose[0]+xShift,mPose[1]+yShift,mPose[2],mPose[3],mPose[4],mPose[5]);
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
    } while (( k < (kmax+1) ) && (k < qtVp) && (!mTracking));
    frame++;
}

void ImageDetectionFilter::draw(cv::Mat sceneCorners, cv::Mat src, int isHudOn)
{
    if (((mTracking) || ((!mTracking)&&(lostTrackingCounter<12)))&&(isHudOn==1)) {
        // The target has been found.
        LOGD("isHudOn= %d",isHudOn);
        // Outline the found target in SeaMate blue.
        cv::line(src, cv::Point2d((sceneCorners.at<cv::Vec2f>(0, 0)[0])+xShift,(sceneCorners.at<cv::Vec2f>(0, 0)[1])+yShift), cv::Point2d((sceneCorners.at<cv::Vec2f>(1, 0)[0])+xShift,(sceneCorners.at<cv::Vec2f>(1, 0)[1])+yShift), cv::Scalar(0.0,175.0,239.0), 8);
        cv::line(src, cv::Point2d((sceneCorners.at<cv::Vec2f>(1, 0)[0])+xShift,(sceneCorners.at<cv::Vec2f>(1, 0)[1])+yShift), cv::Point2d((sceneCorners.at<cv::Vec2f>(2, 0)[0])+xShift,(sceneCorners.at<cv::Vec2f>(2, 0)[1])+yShift), cv::Scalar(0.0,175.0,239.0), 8);
        cv::line(src, cv::Point2d((sceneCorners.at<cv::Vec2f>(2, 0)[0])+xShift,(sceneCorners.at<cv::Vec2f>(2, 0)[1])+yShift), cv::Point2d((sceneCorners.at<cv::Vec2f>(3, 0)[0])+xShift,(sceneCorners.at<cv::Vec2f>(3, 0)[1])+yShift), cv::Scalar(0.0,175.0,239.0), 8);
        cv::line(src, cv::Point2d((sceneCorners.at<cv::Vec2f>(3, 0)[0])+xShift,(sceneCorners.at<cv::Vec2f>(3, 0)[1])+yShift), cv::Point2d((sceneCorners.at<cv::Vec2f>(0, 0)[0])+xShift,(sceneCorners.at<cv::Vec2f>(0, 0)[1])+yShift), cv::Scalar(0.0,175.0,239.0), 8);
        cv::line(src, cv::Point2d((((sceneCorners.at<cv::Vec2f>(0, 0)[0])+xShift)+((sceneCorners.at<cv::Vec2f>(1, 0)[0])+xShift))/2,(sceneCorners.at<cv::Vec2f>(0, 0)[1])+yShift),
                      cv::Point2d((((sceneCorners.at<cv::Vec2f>(0, 0)[0])+xShift)+((sceneCorners.at<cv::Vec2f>(1, 0)[0])+xShift))/2,(sceneCorners.at<cv::Vec2f>(1, 0)[1])+yShift-40),
                      cv::Scalar(0.0,175.0,239.0), 8);
        cv::line(src, cv::Point2d((((sceneCorners.at<cv::Vec2f>(0, 0)[0])+xShift)+((sceneCorners.at<cv::Vec2f>(1, 0)[0])+xShift))/2,(sceneCorners.at<cv::Vec2f>(0, 0)[1])+yShift-40),
                 cv::Point2d((((sceneCorners.at<cv::Vec2f>(0, 0)[0])+xShift)+((sceneCorners.at<cv::Vec2f>(1, 0)[0])+xShift))/2-20,(sceneCorners.at<cv::Vec2f>(1, 0)[1])+yShift-20),
                 cv::Scalar(0.0,175.0,239.0), 8);
        cv::line(src, cv::Point2d((((sceneCorners.at<cv::Vec2f>(0, 0)[0])+xShift)+((sceneCorners.at<cv::Vec2f>(1, 0)[0])+xShift))/2,(sceneCorners.at<cv::Vec2f>(0, 0)[1])+yShift-40),
                 cv::Point2d((((sceneCorners.at<cv::Vec2f>(0, 0)[0])+xShift)+((sceneCorners.at<cv::Vec2f>(1, 0)[0])+xShift))/2+20,(sceneCorners.at<cv::Vec2f>(1, 0)[1])+yShift-20),
                 cv::Scalar(0.0,175.0,239.0), 8);
        // Draw the rectangle guide in SeaMate green.
        cv::rectangle(src, rect,cv::Scalar(168.0,207.0,69.0), 8);
        cv::line(src, cv::Point2d(640.0,160.0), cv::Point2d(640.0,120.0), cv::Scalar(168.0,207.0,69.0), 8);
        cv::line(src, cv::Point2d(640.0,120.0), cv::Point2d(620.0,140.0), cv::Scalar(168.0,207.0,69.0), 8);
        cv::line(src, cv::Point2d(640.0,120.0), cv::Point2d(660.0,140.0), cv::Scalar(168.0,207.0,69.0), 8);
    }
}
