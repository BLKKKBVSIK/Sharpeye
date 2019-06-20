cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

set(SOURCES
        src/native-lib.cpp
        src/centroidtracker.cpp
        src/Tracker.cpp)

set(OPENCV_PATH ${PROJECT_SOURCE_DIR}/../../../../openCVLibrary410)

set(OPENCV_SHARED ${OPENCV_PATH}/libs/${ANDROID_ABI}/libopencv_java4.so)

include_directories(include ${OPENCV_PATH}/src/main/cpp/include)

add_library(lib_opencv SHARED IMPORTED)
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION
        ${OPENCV_SHARED})

add_library(native-lib SHARED ${SOURCES})


target_link_libraries(native-lib lib_opencv log)

add_custom_command(TARGET native-lib POST_BUILD COMMAND
        ${CMAKE_COMMAND} -E copy ${OPENCV_SHARED} ${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
