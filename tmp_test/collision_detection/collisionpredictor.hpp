#ifndef COLLISIONPREDICTOR_HPP
#define COLLISIONPREDICTOR_HPP

#include <map>
#include <vector>
#include <math.h>
#include <opencv2/opencv.hpp>

class CollisionPredictor {
public:
	CollisionPredictor();
	~CollisionPredictor();

	bool isDangerous(const std::map<int, cv::Rect2f> &objects, const int framewidth, cv::Mat frame);

private:
	void add_size_to_history(const int id, const double size);
	bool is_faster_than(const int id, cv::Rect2f box, cv::Mat frame);

	std::map<int, std::vector<double>>	objects_size_history;
	const int 							SAMPLE_SIZE = 30;
	const int 							APX_THRESHOLD = 18;
	const int 							GROWTH_THRESHOLD = 1;
	const int 							MIN_X = 20;
	const int 							MAX_X = 80;
};

#endif // COLLISIONPREDICTOR_HPP
