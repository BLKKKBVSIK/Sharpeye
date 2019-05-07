#include <jni.h>
#include "Tracker.hpp"

extern "C" JNIEXPORT jlong JNICALL
Java_sharpeye_sharpeye_tracking_Tracker_createTracker(JNIEnv *env, jobject obj) {
    return reinterpret_cast<jlong>(new Tracker());
}

extern "C" JNIEXPORT void JNICALL
Java_sharpeye_sharpeye_tracking_Tracker_deleteTracker(JNIEnv *env, jobject obj, jlong ptr) {
    auto *tracker = reinterpret_cast<Tracker*>(ptr);
    delete tracker;
}

std::vector<cv::Rect> arrayListToRectVector(JNIEnv *env, jobject const &javaBoxes) {

    std::vector<cv::Rect> boxes;
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID getMethod = env->GetMethodID(arrayListClass, "get", "(I)Ljava/lang/Object;");
    jmethodID sizeMethod = env->GetMethodID(arrayListClass, "size", "()I");
    jclass rectClass = env->FindClass("org/opencv/core/Rect");
    jfieldID rectX = env->GetFieldID(rectClass, "x", "I");
    jfieldID rectY = env->GetFieldID(rectClass, "y", "I");
    jfieldID rectWidth = env->GetFieldID(rectClass, "width", "I");
    jfieldID rectHeight = env->GetFieldID(rectClass, "height", "I");
    int size = static_cast<int>(env->CallIntMethod(javaBoxes, sizeMethod));
    for (int i = 0; i < size; ++i) {
        jobject rectObject = env->CallObjectMethod(javaBoxes, getMethod, i);
        int x = env->GetIntField(rectObject, rectX);
        int y = env->GetIntField(rectObject, rectY);
        int width = env->GetIntField(rectObject, rectWidth);
        int height = env->GetIntField(rectObject, rectHeight);
        boxes.emplace_back(x, y, width, height);
    }
    env->DeleteLocalRef(arrayListClass);
    env->DeleteLocalRef(rectClass);
    return boxes;
}

jobject rectMapToHashMap(JNIEnv *env, std::map<int, cv::Rect> const &boxes) {
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapConstructor = env->GetMethodID(hashMapClass, "<init>", "()V");
    jobject hashMapObject = env->NewObject(hashMapClass, hashMapConstructor, "");
    jmethodID putMethod = env->GetMethodID(hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jclass integerClass = env->FindClass("java/lang/Integer");
    jmethodID integerConstructor = env->GetMethodID(integerClass, "<init>", "(I)V");
    jclass rectClass = env->FindClass("org/opencv/core/Rect");
    jmethodID rectConstructor = env->GetMethodID(rectClass, "<init>", "(IIII)V");
    for (auto const &it : boxes) {

        jobject integerObject = env->NewObject(integerClass, integerConstructor, it.first);
        jobject rectObject = env->NewObject(rectClass, rectConstructor,
                                            it.second.x, it.second.y,
                                            it.second.width, it.second.height);
        env->CallObjectMethod(hashMapObject, putMethod, integerObject, rectObject);
    }
    env->DeleteLocalRef(hashMapClass);
    env->DeleteLocalRef(integerClass);
    env->DeleteLocalRef(rectClass);
    return hashMapObject;
}

extern "C" JNIEXPORT jobject JNICALL
Java_sharpeye_sharpeye_tracking_Tracker_addBoxes(JNIEnv *env, jobject obj,
        jlong trackerAddr, jlong frameAddr, jobject javaBoxes) {
    auto *tracker = reinterpret_cast<Tracker*>(trackerAddr);
    auto *frame = reinterpret_cast<cv::Mat*>(frameAddr);
    std::vector<cv::Rect> boxes = arrayListToRectVector(env, javaBoxes);
    std::map<int, cv::Rect> boxesId = tracker->addBoxes(*frame, boxes);
    jobject hashMap = rectMapToHashMap(env, boxesId);
    return hashMap;
}

extern "C" JNIEXPORT jobject JNICALL
Java_sharpeye_sharpeye_tracking_Tracker_updateBoxes(JNIEnv *env, jobject obj,
        jlong trackerAddr, jlong frameAddr) {
    auto *tracker = reinterpret_cast<Tracker*>(trackerAddr);
    auto *frame = reinterpret_cast<cv::Mat*>(frameAddr);
    std::map<int, cv::Rect> boxes = tracker->updateBoxes(*frame);
    jobject hashMap = rectMapToHashMap(env, boxes);
    return hashMap;
}