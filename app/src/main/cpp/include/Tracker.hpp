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

    std::vector<cv::Rect>       getBoxesFromTracker(cv::Ptr<cv::MultiTracker> const &trackers, cv::Mat const &frame);

public:
    Tracker();

    std::map<int, cv::Rect>     addBoxes(cv::Mat const &frame, std::vector<cv::Rect> const &boxes);

    std::map<int, cv::Rect>     updateBoxes(cv::Mat const &frame);


};


#endif //MYAPPLICATION2_TRACKER_HPP
