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

	bool alert(const std::map<int, cv::Rect2f> &objects, cv::Mat frame, double speed);

private:
	void add_size_to_history(const int id, const double size);
	double minimum_distance(double speed) const;

	std::map<int, std::vector<double>>	objects_size_history;
	const int 							APX_THRESHOLD = 15;
	const int                           MAX_DELTA = 25;
	const int                           APX_SPEED = 130;
	const int 							MIN_X = 21;
	const int 							MAX_X = 82;
};

#endif // COLLISIONPREDICTOR_HPP
