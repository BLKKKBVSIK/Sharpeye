cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

set(SOURCES
        src/native-lib.cpp
        src/centroidtracker.cpp
        src/Tracker.cpp)

include_directories(include)

add_library(lib_opencv SHARED IMPORTED)
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION
        ${PROJECT_SOURCE_DIR}/../../../../openCVLibrary410/libs/${ANDROID_ABI}/libopencv_java4.so)

add_library(native-lib SHARED ${SOURCES})

# TODO change to generic path to OpenCV_dir
set( OpenCV_DIR "<OpenCV-4.1.0-android-sdk path>/sdk/native/jni" )
find_package( OpenCV REQUIRED )

target_link_libraries(native-lib opencv_java)

get_target_property(opencv_src opencv_java IMPORTED_LOCATION_RELEASE )
add_custom_command(TARGET native-lib POST_BUILD COMMAND
        ${CMAKE_COMMAND} -E copy ${opencv_src} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        )