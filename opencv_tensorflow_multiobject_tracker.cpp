#include <string>
#include <vector>
#include <ctime>
#include <iostream>
#include <opencv2/opencv.hpp>
#include <opencv2/tracking.hpp>
#include <opencv2/imgproc.hpp>

#include "centroidtracker.hpp"

std::string CLASSES[] = {"background", "aeroplane", "bicycle", "bird", "boat",
	"bottle", "bus", "car", "cat", "chair", "cow", "diningtable",
	"dog", "horse", "motorbike", "person", "pottedplant", "sheep",
	"sofa", "train", "tvmonitor"};

/*
** get boxes of the objects detected by the tensorflow neural network
*/
std::vector<cv::Rect2f> getBoxesFromTensorflow(cv::dnn::Net tensorflowNet, cv::Mat frame) {
	int rows = frame.rows;
	int cols = frame.cols;

	cv::Mat blob = cv::dnn::blobFromImage(frame, 0.007843, cv::Size(300, 300), cv::Scalar(127.5, 127.5, 127.5), false);
	tensorflowNet.setInput(blob, "data");

	cv::Mat detection = tensorflowNet.forward("detection_out");
	cv::Mat detectionMat(detection.size[2], detection.size[3], CV_32F, detection.ptr<float>());

    std::ostringstream ss;
	std::vector<cv::Rect2f> boxes;
    float score;
    int object_class, left, top, right, bottom;
    for (int i = 0; i < detectionMat.rows; i++)
    {
        float score = detectionMat.at<float>(i, 2);
        int object_class = static_cast<int>(detectionMat.at<float>(i, 1));

        if (score > 0.3 && CLASSES[object_class].compare("car") == 0)
        {
        	left = static_cast<int>(detectionMat.at<float>(i, 3) * cols);
        	top = static_cast<int>(detectionMat.at<float>(i, 4) * rows);
        	right = static_cast<int>(detectionMat.at<float>(i, 5) * cols);
        	bottom = static_cast<int>(detectionMat.at<float>(i, 6) * rows);
        	boxes.push_back(cv::Rect2f(left, top, right - left, bottom - top));
        }
    }

	return boxes;
}

/*
** get new boxes of the objects from the trackers
*/
std::vector<cv::Rect2f> getBoxesFromTracker(cv::Ptr<cv::MultiTracker> trackers, cv::Mat frame) {
	trackers->update(frame);

	std::vector<cv::Rect2f> boxes;
	for (unsigned i = 0; i < trackers->getObjects().size(); i++) {
		cv::Rect2d object = trackers->getObjects()[i];
		boxes.push_back(cv::Rect2f(object.x, object.y, object.width, object.height));
	}
	return boxes;
}

int main(int argc, char **argv) {
	std::string videoPath = "dashcam_boston.mp4";
	srand(1);

	CentroidTracker ct;
	cv::dnn::Net tensorflowNet = cv::dnn::readNetFromCaffe("MobileNetSSD_deploy.prototxt", "MobileNetSSD_deploy.caffemodel");
	cv::VideoCapture cap(videoPath);

	int frame_counter = 0;
	std::vector<cv::Rect2f> boxes;
	cv::Ptr<cv::MultiTracker> trackers = nullptr;
	std::time_t start = std::time(nullptr);
	cv::Mat frame;
	std::map<int, cv::Rect2f> objects;
	std::string text;

	int xmin, ymin, boxwidth, boxheight;

	while (cap.isOpened()) {
		cap >> frame;
		if (frame.empty()) break;

		// cv::resize(frame, frame, cv::Size(300, 300));

		if ((frame_counter % 30) == 0) {
			boxes = getBoxesFromTensorflow(tensorflowNet, frame);

			trackers = cv::MultiTracker::create();

			for (cv::Rect2f &box: boxes) {
				xmin = box.x;
				ymin = box.y;
				boxwidth = box.width;
				boxheight = box.height;
				
				trackers->add(cv::TrackerMOSSE::create(), frame, cv::Rect2d(xmin, ymin, boxwidth, boxheight));
			}
		} else {
			boxes = getBoxesFromTracker(trackers, frame);
		}

		// for (cv::Rect &box: boxes) {
		// 	cv::rectangle(frame, cv::Rect(box.x, box.y, box.width, box.height), cv::Scalar(0, 0, 255), 2);
		// }

		objects = ct.update(boxes);
		for (auto &[objectID, box]: objects) {
			text = "ID " + std::to_string(objectID);
			cv::Scalar color = cv::Scalar(50 * (objectID - 1) % 255, 50 * (objectID - 1) % 255, 50 * (objectID - 1) % 255);
			cv::putText(frame, text, cv::Point(box.x + (box.width / 2.0) - 10, box.y + (box.height / 2.0) - 10), cv::FONT_HERSHEY_SIMPLEX, 0.5, color, 2);
			cv::rectangle(frame, cv::Rect(box.x, box.y, box.width, box.height), color, 2);
			// cv::circle(frame, centroid, 4, cv::Scalar(0, 255, 0), -1);
		}

		// Show frame
		cv::imshow("Frame", frame);

		// Stop video if q is pressed
		if (cv::waitKey(1) == 'q') break;

		++frame_counter;
	}

	std::time_t now = std::time(nullptr);
	std::cout << float(frame_counter) / float(now - start) << " FPS" << std::endl;

	return 0;
}