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

	bool isDangerous(const std::map<int, cv::Rect2f> &objects, const int framewidth);

private:
	void add_size_to_history(const int id, const double size);
	bool is_faster_than(const int id);

	std::map<int, std::vector<double>>	objects_size_history;
	int 								SAMPLE_SIZE = 30;
	int 								APX_THRESHOLD = 18;
	int 								GROWTH_THRESHOLD = 1;
	int 								MIN_X = 20;
	int 								MAX_X = 80;
};

#endif // COLLISIONPREDICTOR_HPP
