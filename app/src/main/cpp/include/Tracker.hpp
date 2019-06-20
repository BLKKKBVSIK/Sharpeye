//
// Created by Midori on 29/04/2019.
//

#ifndef MYAPPLICATION2_TRACKER_HPP
#define MYAPPLICATION2_TRACKER_HPP


#include <opencv2/tracking.hpp>
#include "centroidtracker.hpp"

class Tracker {

private:

    CentroidTracker             ct;
    cv::Ptr<cv::MultiTracker>   trackers;

    std::vector<cv::Rect2f>       getBoxesFromTracker(cv::Mat const &frame);

public:
    Tracker();

    std::map<int, cv::Rect2f>     addBoxes(cv::Mat const &frame, std::vector<cv::Rect2f> const &boxes);

    std::map<int, cv::Rect2f>     updateBoxes(cv::Mat const &frame);


};


#endif //MYAPPLICATION2_TRACKER_HPP
