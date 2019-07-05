#include "collisionpredictor.hpp"

CollisionPredictor::CollisionPredictor() {}

CollisionPredictor::~CollisionPredictor() {}

/*
** Return true if a collision is likely to happen
*/
bool CollisionPredictor::isDangerous(const std::map<int, cv::Rect2f> &objects, const int framewidth) {
	double apx_distance;
	int mid_x, mid_y;
	for (auto &[objectID, box]: objects) {
		if (box.width > (0.9 * framewidth))
			continue;
		mid_x = (double)(((box.x + box.width / 2) * 100) / framewidth);
		// std::cout << "mid_x = " << mid_x << std::endl;
		//mid_y = ((box.y + box.height / 2) * 100) / (double)framewidth;
		apx_distance = (double)((box.width * 100.0) / framewidth);
		// std::cout << "apx = " << apx_distance << std::endl;

		if (apx_distance > 18 && mid_x > 20 && mid_x < 80)
			return true;
	}
	return false;
}