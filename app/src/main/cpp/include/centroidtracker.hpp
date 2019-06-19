#ifndef CENTROIDTRACKER_HPP
#define CENTROIDTRACKER_HPP

#include <map>
#include <vector>

class CentroidTracker {
public:
	CentroidTracker();
	~CentroidTracker();
	std::map<int, cv::Rect2f> update(const std::vector<cv::Rect2f> &boxes);

private:
	void register_object(const cv::Point2d &centroid, const cv::Rect2f &box);
	void deregister_object(const int objectID);
	std::map<int, cv::Point2d> allObjectsDisappeared(const std::vector<cv::Rect2f> &boxes);
	void compute_distances(const std::vector<cv::Point2d> &XA, const std::vector<cv::Point2d> &XB);
	std::vector<int> sortRows() const;
	std::vector<int> sortCols(const std::vector<int> rows) const;
	void correlatePositions(const std::vector<int> &objectIDs, const std::vector<cv::Point2d> &objectCentroids,
		const std::vector<cv::Point2d> &inputCentroids, std::vector<int> &unusedRows, std::vector<int> &unusedCols,
		const std::vector<cv::Rect2f> &boxes);

	int nextObjectID = 0;
	int maxDisappeared = 100;
	std::map<int, cv::Point2d> objects;
	std::map<int, cv::Rect2f> lastBoxes;
	std::map<int, int> disappeared;
	std::vector<std::vector<double>> dists;
};

#endif // CENTROIDTRACKER_HPP