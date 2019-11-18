#include "collisionpredictor.hpp"

CollisionPredictor::CollisionPredictor() = default;

CollisionPredictor::~CollisionPredictor() = default;

void CollisionPredictor::add_size_to_history(const int id, const double size) {
	this->objects_size_history[id].push_back(size);
}

bool CollisionPredictor::is_faster_than(const int id, cv::Rect2f box, cv::Mat frame) {
	unsigned long count = this->objects_size_history[id].size();
	int first = static_cast<int>(count > this->SAMPLE_SIZE ? count - this->SAMPLE_SIZE : count - 1);
	double growth = pow(this->objects_size_history[id][count - 1] / this->objects_size_history[id][first], 1.0 / count) - 1;
	growth *= 100;
	std::cout << "Average growth for object " << id << " is " << growth << "%" << std::endl;

    return true;
}

/*
** Return true if a collision is likely to happen
*/
bool CollisionPredictor::alert(const std::map<int, cv::Rect2f> &objects, cv::Mat frame) {
    int framewidth = frame.cols;
	double apx_distance;
	int mid_x;
	for (auto &[objectID, box]: objects) {
		if (box.width > (0.9 * framewidth))
			continue;
		mid_x = (int)(((box.x + box.width / 2) * 100) / framewidth);
		apx_distance = ((box.width * 100.0) / framewidth);

		this->add_size_to_history(objectID, apx_distance);

		if (apx_distance > this->APX_THRESHOLD && mid_x > this->MIN_X && mid_x < this->MAX_X && this->is_faster_than(objectID, box, frame))
			return true;
	}
	return false;
}
