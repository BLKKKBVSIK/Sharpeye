#include "collisionpredictor.hpp"

CollisionPredictor::CollisionPredictor() {}

CollisionPredictor::~CollisionPredictor() {}

void CollisionPredictor::add_size_to_history(const int id, const double size) {
	// if (this->objects_size_history.count(id) < 1) {
	// 	std::vector<int> v;
	// 	this->objects_size_history[id] = v;
	// }
	this->objects_size_history[id].push_back(size);
}

bool CollisionPredictor::is_faster_than(const int id, cv::Rect2f box, cv::Mat frame) {
	int count = this->objects_size_history[id].size();
	int first = count > this->SAMPLE_SIZE ? count - this->SAMPLE_SIZE : count - 1;
	double growth = pow(this->objects_size_history[id][count - 1] / this->objects_size_history[id][first], 1.0 / count) - 1;
	growth *= 100;
	std::cout << "Average growth for object " << id << " is " << growth << "%" << std::endl;

	std::string text = std::to_string(growth);
	cv::Scalar color = cv::Scalar(0, 0, 255);
	cv::putText(frame, text, cv::Point(box.x + (box.width / 2.0) - 60, box.y + (box.height / 2.0)), cv::FONT_HERSHEY_SIMPLEX, 1, color, 2);

	return growth > this->GROWTH_THRESHOLD ? true : false;
}

/*
** Return true if a collision is likely to happen
*/
bool CollisionPredictor::isDangerous(const std::map<int, cv::Rect2f> &objects, const int framewidth, cv::Mat frame) {
	double apx_distance;
	int mid_x, mid_y;
	for (auto &[objectID, box]: objects) {
		if (box.width > (0.9 * framewidth))
			continue;
		mid_x = (double)(((box.x + box.width / 2) * 100) / framewidth);
		apx_distance = (double)((box.width * 100.0) / framewidth);

		this->add_size_to_history(objectID, apx_distance);

		if (apx_distance > this->APX_THRESHOLD && mid_x > this->MIN_X && mid_x < this->MAX_X && this->is_faster_than(objectID, box, frame))
			return true;
	}
	return false;
}
