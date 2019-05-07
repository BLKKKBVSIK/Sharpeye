//
// Created by Midori on 29/04/2019.
//

#include <Tracker.hpp>

#include "Tracker.hpp"
#include <android/log.h>


Tracker::Tracker() : ct(), trackers() {
}


std::map<int, cv::Rect> Tracker::addBoxes(cv::Mat const &frame, std::vector<cv::Rect> const &boxes) {
    int xMin, yMin, boxWidth, boxHeight;

    trackers = cv::MultiTracker::create();
    for (cv::Rect const &box: boxes) {
        xMin = box.x;
        yMin = box.y;
        boxWidth = box.width;
        boxHeight = box.height;

        trackers->add(cv::TrackerMOSSE::create(), frame, cv::Rect2d(xMin, yMin, boxWidth, boxHeight));
    }
    return ct.update(boxes);
}

std::map<int, cv::Rect> Tracker::updateBoxes(cv::Mat const &frame) {
    std::vector<cv::Rect> boxes;
    boxes = getBoxesFromTracker(trackers, frame);
    std::string str = "boxes="+std::to_string(boxes.size());
    return ct.update(boxes);
}

std::vector<cv::Rect>
Tracker::getBoxesFromTracker(cv::Ptr<cv::MultiTracker> const &trackers, cv::Mat const &frame) {
    trackers->update(frame);
    std::vector<cv::Rect> boxes;
    for (unsigned i = 0; i < trackers->getObjects().size(); i++) {
        cv::Rect2d object = trackers->getObjects()[i];
        boxes.emplace_back(object.x, object.y, object.width, object.height);
    }
    return boxes;
}
