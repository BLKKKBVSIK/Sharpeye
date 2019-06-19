//
// Created by Midori on 29/04/2019.
//

#include <Tracker.hpp>

#include "Tracker.hpp"
#include <android/log.h>


Tracker::Tracker() : ct(), trackers() {
}


std::map<int, cv::Rect2f> Tracker::addBoxes(cv::Mat const &frame, std::vector<cv::Rect2f> const &boxes) {
    float xMin, yMin, boxWidth, boxHeight;

    trackers = cv::MultiTracker::create();
    for (cv::Rect2f const &box: boxes) {
        xMin = box.x;
        yMin = box.y;
        boxWidth = box.width;
        boxHeight = box.height;
        __android_log_print(ANDROID_LOG_INFO, "JNIBoxes", "box.x=%f box.y=%f box.width=%f box.height=%f", box.x,
                            box.y, box.width, box.height);
        trackers->add(cv::TrackerMOSSE::create(), frame, cv::Rect2d(xMin, yMin, boxWidth, boxHeight));
    }
    return ct.update(boxes);
}

std::map<int, cv::Rect2f> Tracker::updateBoxes(cv::Mat const &frame) {
    std::vector<cv::Rect2f> boxes;
    boxes = getBoxesFromTracker(frame);
    std::string str = "boxes="+std::to_string(boxes.size());
    return ct.update(boxes);
}

std::vector<cv::Rect2f> Tracker::getBoxesFromTracker(cv::Mat const &frame) {
    trackers->update(frame);
    std::vector<cv::Rect2f> boxes;
    for (unsigned i = 0; i < trackers->getObjects().size(); i++) {
        cv::Rect2d object = trackers->getObjects()[i];
        boxes.emplace_back(object.x, object.y, object.width, object.height);
    }
    return boxes;
}
