#include <jni.h>
#include "Tracker.hpp"

#include <android/log.h>

extern "C" JNIEXPORT jlong JNICALL
Java_sharpeye_sharpeye_tracking_Tracker_createTracker(JNIEnv *env, jobject obj) {
    return reinterpret_cast<jlong>(new Tracker());
}

extern "C" JNIEXPORT void JNICALL
Java_sharpeye_sharpeye_tracking_Tracker_deleteTracker(JNIEnv *env, jobject obj, jlong ptr) {
    auto *tracker = reinterpret_cast<Tracker*>(ptr);
    delete tracker;
}

std::vector<cv::Rect2f> arrayListToRectVector(JNIEnv *env, jobject const &javaBoxes) {

    std::vector<cv::Rect2f> boxes;
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID getMethod = env->GetMethodID(arrayListClass, "get", "(I)Ljava/lang/Object;");
    jmethodID sizeMethod = env->GetMethodID(arrayListClass, "size", "()I");
    jclass rectClass = env->FindClass("sharpeye/sharpeye/tracking/Tracker$Rect2f");
    jfieldID rectX = env->GetFieldID(rectClass, "x", "F");
    jfieldID rectY = env->GetFieldID(rectClass, "y", "F");
    jfieldID rectWidth = env->GetFieldID(rectClass, "width", "F");
    jfieldID rectHeight = env->GetFieldID(rectClass, "height", "F");
    int size = static_cast<int>(env->CallIntMethod(javaBoxes, sizeMethod));
    for (int i = 0; i < size; ++i) {
        jobject rectObject = env->CallObjectMethod(javaBoxes, getMethod, i);
        float x = env->GetFloatField(rectObject, rectX);
        float y = env->GetFloatField(rectObject, rectY);
        float width = env->GetFloatField(rectObject, rectWidth);
        float height = env->GetFloatField(rectObject, rectHeight);
        boxes.emplace_back(x, y, width, height);
    }
    env->DeleteLocalRef(arrayListClass);
    env->DeleteLocalRef(rectClass);
    return boxes;
}

jobject rectMapToHashMap(JNIEnv *env, std::map<int, cv::Rect2f> const &boxes) {
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapConstructor = env->GetMethodID(hashMapClass, "<init>", "()V");
    jobject hashMapObject = env->NewObject(hashMapClass, hashMapConstructor, "");
    jmethodID putMethod = env->GetMethodID(hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jclass integerClass = env->FindClass("java/lang/Integer");
    jmethodID integerConstructor = env->GetMethodID(integerClass, "<init>", "(I)V");
    jclass rectClass = env->FindClass("sharpeye/sharpeye/tracking/Tracker$Rect2f");
    jmethodID rectConstructor = env->GetMethodID(rectClass, "<init>", "(FFFF)V");
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
    std::vector<cv::Rect2f> boxes = arrayListToRectVector(env, javaBoxes);
    std::map<int, cv::Rect2f> boxesAndIDs = tracker->addBoxes(*frame, boxes);
    jobject hashMap = rectMapToHashMap(env, boxesAndIDs);
    return hashMap;
}

extern "C" JNIEXPORT jobject JNICALL
Java_sharpeye_sharpeye_tracking_Tracker_updateBoxes(JNIEnv *env, jobject obj,
        jlong trackerAddr, jlong frameAddr) {
    auto *tracker = reinterpret_cast<Tracker*>(trackerAddr);
    auto *frame = reinterpret_cast<cv::Mat*>(frameAddr);
    std::map<int, cv::Rect2f> boxes = tracker->updateBoxes(*frame);
    jobject hashMap = rectMapToHashMap(env, boxes);
    return hashMap;
}