#include <cmath>
#include <numeric>
#include <opencv2/opencv.hpp>
#include "centroidtracker.hpp"

CentroidTracker::CentroidTracker() {}

CentroidTracker::~CentroidTracker() {}

/*
** Adds a new object to the list of tracked objects
*/
void CentroidTracker::register_object(const cv::Point &centroid, const cv::Rect2f &box) {
	this->objects.insert(std::make_pair(this->nextObjectID, centroid));
	this->lastBoxes.insert(std::make_pair(this->nextObjectID, box));
	this->disappeared.insert(std::make_pair(this->nextObjectID, 0));
	++this->nextObjectID;
}

/*
** Removes an object from the list of tracked objects
*/
void CentroidTracker::deregister_object(const int objectID) {
	this->objects.erase(objectID);
	this->lastBoxes.erase(objectID);
	this->disappeared.erase(objectID);
}

/*
** Calculates the Euclidian distances between each centroids of the tracked objects and each centroids of the new boxes
*/
void CentroidTracker::compute_distances(const std::vector<cv::Point> &XA, const std::vector<cv::Point> &XB) {
	std::vector<std::vector<double>> dist(XA.size(), std::vector<double>(XB.size()));

	for (int i = 0; i < XA.size(); i++) {
		for (int j = 0; j < XB.size(); j++) {
			dist[i][j] = sqrt( pow(XB[j].x - XA[i].x, 2) + pow(XB[j].y - XA[i].y, 2) );
		}
	}

	this->dists = dist;
}

/*
** Returns a vector of the indexes of each row, sorted by the smallest distances in each row
*/
std::vector<int> CentroidTracker::sortRows() const {
	std::vector<double> mins(this->dists.size());
	for (int i = 0; i < this->dists.size(); i++) {
		mins[i] = *(std::min_element(this->dists[i].begin(), this->dists[i].end()));
	}
	std::vector<int> rows(mins.size());
	std::iota(rows.begin(), rows.end(), 0);
	auto comparator = [&mins](int a, int b){ return mins[a] < mins[b]; };
	std::sort(rows.begin(), rows.end(), comparator);

	return rows;
}

/*
** Returns a vector of the indexes of the smallest distance in each row, sorted by the smallest value
*/
std::vector<int> CentroidTracker::sortCols(const std::vector<int> rows) const {
	std::vector<int> mins(this->dists.size());
	for (int i = 0; i < this->dists.size(); i++) {
		mins[i] = std::distance(this->dists[i].begin(), std::min_element(this->dists[i].begin(), this->dists[i].end()));
	}

	std::vector<int> cols;
	for (auto &row: rows) {
		cols.push_back(mins[row]);
	}

	return cols;
}

/*
** Increments the disappeared counter of each object of the list of tracked objects
*/
std::map<int, cv::Point> CentroidTracker::allObjectsDisappeared(const std::vector<cv::Rect2f> &boxes) {
	std::vector<int> toDelete;
	for (auto &object: this->disappeared) {
		++(this->disappeared[object.first]);
		if (this->disappeared[object.first] > this->maxDisappeared) {
			toDelete.push_back(object.first);
		}
	}
	for (auto &objectID: toDelete) {
		this->deregister_object(objectID);
	}
	return this->objects;
}

/*
** Updates the centroid of the tracked objects
*/
void CentroidTracker::correlatePositions(
	const std::vector<int> &objectIDs,
	const std::vector<cv::Point> &objectCentroids,
	const std::vector<cv::Point> &inputCentroids,
	std::vector<int> &unusedRows,
	std::vector<int> &unusedCols,
	const std::vector<cv::Rect2f> &boxes) {

	std::vector<int> rows = this->sortRows();
	std::vector<int> cols = this->sortCols(rows);
	std::vector<int> usedRows;
	std::vector<int> usedCols;
	int objectID;
	for (unsigned int i = 0; i < rows.size(); i++) {
		if (std::find(usedRows.begin(), usedRows.end(), rows[i]) != usedRows.end() ||
			std::find(usedCols.begin(), usedCols.end(), cols[i]) != usedCols.end()) {
			continue;
		}
		objectID = objectIDs[rows[i]];
		this->objects[objectID] = inputCentroids[cols[i]];
		this->lastBoxes[objectID] = boxes[cols[i]];
		this->disappeared[objectID] = 0;
		usedRows.push_back(rows[i]);
		usedCols.push_back(cols[i]);
	}
	for (unsigned int i = 0; i < this->dists.size(); i++)
		if (std::find(usedRows.begin(), usedRows.end(), i) == usedRows.end()) unusedRows.push_back(i);
	for (unsigned int i = 0; i < this->dists[0].size(); i++)
		if (std::find(usedCols.begin(), usedCols.end(), i) == usedCols.end()) unusedCols.push_back(i);
}

/*
** Updates the centroids of the tracked objects and adds/removes objects to the list of tracked objects
*/
std::map<int, cv::Rect2f> CentroidTracker::update(const std::vector<cv::Rect2f> &boxes) {
	if (boxes.size() == 0) {
		this->allObjectsDisappeared(boxes);
		return this->lastBoxes;
	}

	std::vector<cv::Point> inputCentroids;
	for (auto &box: boxes)
		inputCentroids.push_back(cv::Point(box.x + (box.width / 2.0), box.y + (box.height / 2.0)));

	if (this->objects.size() == 0) {
		int i = 0;
		for (auto &centroid: inputCentroids) {
			this->register_object(centroid, boxes[i]);
		}
	} else {
		std::vector<int> objectIDs;
		std::vector<cv::Point> objectCentroids;

		for (auto &object: this->objects) {
			objectIDs.push_back(object.first);
			objectCentroids.push_back(object.second);
		}

		this->compute_distances(objectCentroids, inputCentroids);
		std::vector<int> unusedRows;
		std::vector<int> unusedCols;
		this->correlatePositions(objectIDs, objectCentroids, inputCentroids, unusedRows, unusedCols, boxes);

		int objectID;
		if (this->dists.size() >= this->dists[0].size()) {
			for (auto &row: unusedRows) {
				objectID = objectIDs[row];
				++(this->disappeared[objectID]);
				if (this->disappeared[objectID] > this->maxDisappeared) this->deregister_object(objectID);
			}
		} else {
			for (auto &col: unusedCols) {
				this->register_object(inputCentroids[col], boxes[col]);
			}
		}
	}
	return this->lastBoxes;
}