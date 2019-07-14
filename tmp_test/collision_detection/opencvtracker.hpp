#ifndef OPENCVTRACKER_HPP
#define OPENCVTRACKER_HPP

#include <opencv2/opencv.hpp>
#include <opencv2/tracking.hpp>

class OpenCVTracker {
public:
	OpenCVTracker();
	~OpenCVTracker();

	std::vector<cv::Rect2f> getBoxesFromTracker(const cv::Mat &frame);
	void update(const std::vector<cv::Rect2f> &boxes, const cv::Mat &frame);

private:
	cv::Ptr<cv::MultiTracker> trackers = nullptr;
};

#endif // OPENCVTRACKER_HPP
