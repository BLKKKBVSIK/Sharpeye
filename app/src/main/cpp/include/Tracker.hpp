#ifndef MYAPPLICATION2_TRACKER_HPP
#define MYAPPLICATION2_TRACKER_HPP


#include <opencv2/tracking.hpp>
#include "centroidtracker.hpp"
#include "collisionpredictor.hpp"

class Tracker {

private:

    CentroidTracker             ct;
    cv::Ptr<cv::MultiTracker>   trackers;
    CollisionPredictor          cp;
    bool                        dangerous;

    std::vector<cv::Rect2f>       getBoxesFromTracker(cv::Mat const &frame);

public:
    Tracker();

    std::map<int, cv::Rect2f>     addBoxes(cv::Mat const &frame, std::vector<cv::Rect2f> const &boxes);

    std::map<int, cv::Rect2f>     updateBoxes(cv::Mat const &frame, double speed);

    bool                          isDangerous() const;


};


#endif //MYAPPLICATION2_TRACKER_HPP
