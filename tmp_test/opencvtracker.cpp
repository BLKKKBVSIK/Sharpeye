#include "opencvtracker.hpp"

OpenCVTracker::OpenCVTracker() {}

OpenCVTracker::~OpenCVTracker() {}

/*
** get new boxes of the objects from the trackers
*/
std::vector<cv::Rect2f> OpenCVTracker::getBoxesFromTracker(const cv::Mat &frame) {
	this->trackers->update(frame);

	std::vector<cv::Rect2f> boxes;
	for (unsigned i = 0; i < this->trackers->getObjects().size(); i++) {
		cv::Rect2d object = this->trackers->getObjects()[i];
		boxes.push_back(cv::Rect2f(object.x, object.y, object.width, object.height));
	}
	return boxes;
}

void OpenCVTracker::update(const std::vector<cv::Rect2f> &boxes, const cv::Mat &frame) {
    this->trackers = cv::MultiTracker::create();
    for (const cv::Rect2f &box: boxes) {
        this->trackers->add(cv::TrackerMOSSE::create(), frame, cv::Rect2d(box.x, box.y, box.width, box.height));
    }
}