#include "collisionpredictor.hpp"

CollisionPredictor::CollisionPredictor() = default;

CollisionPredictor::~CollisionPredictor() = default;

void CollisionPredictor::add_size_to_history(const int id, const double size) {
	this->objects_size_history[id].push_back(size);
}

double CollisionPredictor::minimum_distance(double speed) const {
	if (speed < 10) {
		return 0;
	}

	return this->APX_THRESHOLD + this->MAX_DELTA * (1 - (speed / this->APX_SPEED));
}

/*
** Return true if a collision is likely to happen
*/
bool CollisionPredictor::alert(const std::map<int, cv::Rect2f> &objects, cv::Mat frame, double speed) {
    int framewidth = frame.cols;
	double apx_distance;
	int mid_x;

	for (auto &[objectID, box]: objects) {
		if (box.width > (0.9 * framewidth))
			continue;

		mid_x = (int)(((box.x + box.width / 2) * 100) / framewidth);
		apx_distance = ((box.width * 100.0) / framewidth);

		this->add_size_to_history(objectID, apx_distance);

		if (apx_distance > this->minimum_distance(speed) && mid_x > this->MIN_X && mid_x < this->MAX_X)
			return true;
	}
	return false;
}
