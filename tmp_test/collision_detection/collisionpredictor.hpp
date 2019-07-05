#ifndef COLLISIONPREDICTOR_HPP
#define COLLISIONPREDICTOR_HPP

#include <map>
#include <opencv2/opencv.hpp>

class CollisionPredictor {
public:
	CollisionPredictor();
	~CollisionPredictor();

	bool isDangerous(const std::map<int, cv::Rect2f> &objects, const int framewidth);
};

#endif // COLLISIONPREDICTOR_HPP