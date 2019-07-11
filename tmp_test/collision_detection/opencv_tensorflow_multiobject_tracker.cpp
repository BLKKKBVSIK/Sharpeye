#include <string>
#include <vector>
#include <ctime>
#include <iostream>
#include <unistd.h>
#include <chrono>
#include <opencv2/opencv.hpp>
#include <opencv2/tracking.hpp>
#include <opencv2/imgproc.hpp>

#include "centroidtracker.hpp"
#include "opencvtracker.hpp"
#include "collisionpredictor.hpp"

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

void show_objects(std::map<int, cv::Rect2f> objects, cv::Mat frame, bool isDangerous) {
	for (auto &[objectID, box]: objects) {
		std::string text = std::to_string(objectID);
		cv::Scalar color = cv::Scalar(0, 255, 0);
		//cv::putText(frame, text, cv::Point(box.x + (box.width / 2.0) - 5, box.y + (box.height / 2.0)), cv::FONT_HERSHEY_SIMPLEX, 0.5, color, 2);
		//cv::rectangle(frame, cv::Rect(box.x, box.y, box.width, box.height), color, 2);
	}
	if (isDangerous) {
		cv::putText(frame, "WARNING!!!", cv::Point(frame.cols / 2, frame.rows / 2), cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0,0,255), 3);
	}
	// Show frame
	cv::imshow("Frame", frame);
}

int main(int argc, char **argv) {
	CentroidTracker centroid_tracker;
	OpenCVTracker opencv_tracker;
	CollisionPredictor collision_predictor;

	std::string videoPath = "videos/danger.mp4";
	cv::dnn::Net tensorflowNet = cv::dnn::readNetFromCaffe("model/MobileNetSSD_deploy.prototxt", "model/MobileNetSSD_deploy.caffemodel");
	cv::VideoCapture cap(videoPath);

	int frame_counter = 0;
	std::vector<cv::Rect2f> boxes;
	cv::Mat frame;
	std::map<int, cv::Rect2f> objects;
	bool isDangerous = false;

	std::chrono::high_resolution_clock::time_point t1;
	std::chrono::high_resolution_clock::time_point t2;
	std::chrono::duration<double, std::milli> time_span;
	double interval = 100.0 / 6.0;
	double towait = 0;

	while (cap.isOpened()) {
		t1 = std::chrono::high_resolution_clock::now();
		cap >> frame;
		if (frame.empty()) break;

		if ((frame_counter % 5) == 0) {
			boxes = getBoxesFromTensorflow(tensorflowNet, frame);
			// Update the opencv tracker if boxes are from tensorflow
			opencv_tracker.update(boxes, frame);
		} else {
			// Get boxes from the centroid tracker
			boxes = opencv_tracker.getBoxesFromTracker(frame);
		}
		// Update centroid tracker with the boxes
		objects = centroid_tracker.update(boxes);
		isDangerous = collision_predictor.isDangerous(objects, frame.cols);

		show_objects(objects, frame, isDangerous);

		if (cv::waitKey(1) == 'q') break;

		++frame_counter;

		t2 = std::chrono::high_resolution_clock::now();
		time_span = t2 - t1;
		towait = interval - time_span.count();
		if (towait > 0) {
			usleep(towait * 1000);
		}
	}

	return 0;
}
