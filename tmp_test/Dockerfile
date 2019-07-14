FROM debian:jessie
MAINTAINER Baptiste Veyssiere baptiste.veyssiere@epitech.eu

ENV PKG_CONFIG_PATH=/usr/local/lib/pkgconfig
ENV TERM=xterm-256color

WORKDIR /repositories

RUN echo "deb http://ftp.us.debian.org/debian unstable main contrib non-free" >> /etc/apt/sources.list.d/unstable.list && \
    apt-get update && \
    apt upgrade -y && \
    apt-get -y install build-essential pkg-config git cmake qt5-default vim && \
    apt-get -y install checkinstall yasm gfortran libpng-dev && \
    apt-get -y install software-properties-common libjasper1 libjpeg-dev && \
    apt-get -y install libtiff-dev libavcodec-dev libavformat-dev && \
    apt-get -y install libswscale-dev libdc1394-22-dev && \
    apt-get install -t unstable g++-8 -y && \
    apt-get install -y libxine2-dev libv4l-dev

WORKDIR /usr/include/linux

RUN ln -s -f ../libv4l1-videodev.h videodev.h && \
    apt-get -y install libgstreamer1.0-dev libgstreamer-plugins-base1.0-dev && \
    apt-get -y install libgtk2.0-dev libtbb-dev qt5-default && \
    apt-get -y install libatlas-base-dev && \
    apt-get -y install libfaac-dev libmp3lame-dev libtheora-dev && \
    apt-get -y install libvorbis-dev libxvidcore-dev && \
    apt-get -y install libopencore-amrnb-dev libopencore-amrwb-dev && \
    apt-get -y install libavresample-dev && \
    apt-get -y install x264 v4l-utils && \
    git clone https://github.com/opencv/opencv.git /repositories/opencv && \
    git clone https://github.com/opencv/opencv_contrib.git /repositories/opencv_contrib && \
    mkdir -p /repositories/installation/OpenCV-master

WORKDIR /repositories/opencv/build

RUN cmake -D CMAKE_BUILD_TYPE=RELEASE \
            -D CMAKE_INSTALL_PREFIX=/repositories/installation/OpenCV-master \
            -D INSTALL_C_EXAMPLES=ON \
            -D WITH_TBB=ON \
            -D WITH_V4L=ON \
        -D WITH_QT=ON \
        -D WITH_OPENGL=ON \
        -D OPENCV_EXTRA_MODULES_PATH=/repositories/opencv_contrib/modules \
        -D BUILD_EXAMPLES=ON .. && \
    make -j4 && \
    make install

RUN echo $'# Package Information for pkg-config \n\
 \n\
prefix=/usr \n\
exec_prefix=${prefix} \n\
libdir=${exec_prefix}/lib \n\
includedir_old=${prefix}/include/opencv4/opencv \n\
includedir_new=${prefix}/include/opencv4 \n\
 \n\
Name: OpenCV \n\
Description: Open Source Computer Vision Library \n\
Version: 4.1.0 \n\
Libs: -L${exec_prefix}/lib -lopencv_gapi -lopencv_stitching -lopencv_aruco -lopencv_bgsegm -lopencv_bioinspired -lopencv_ccalib -lopencv_dnn_objdetect -lopencv_dpm -lopencv_face -lopencv_fuzzy -lopencv_hfs -lopencv_img_hash -lopencv_line_descriptor -lopencv_quality -lopencv_reg -lopencv_rgbd -lopencv_saliency -lopencv_stereo -lopencv_structured_light -lopencv_phase_unwrapping -lopencv_superres -lopencv_optflow -lopencv_surface_matching -lopencv_tracking -lopencv_datasets -lopencv_text -lopencv_dnn -lopencv_plot -lopencv_videostab -lopencv_video -lopencv_xfeatures2d -lopencv_shape -lopencv_ml -lopencv_ximgproc -lopencv_xobjdetect -lopencv_objdetect -lopencv_calib3d -lopencv_features2d -lopencv_highgui -lopencv_videoio -lopencv_imgcodecs -lopencv_flann -lopencv_xphoto -lopencv_photo -lopencv_imgproc -lopencv_core \n\
Libs.private: -ldl -lm -lpthread -lrt \n\
Cflags: -I${includedir_new}' > /usr/lib/pkgconfig/opencv4.pc

WORKDIR /repositories/installation/OpenCV-master/

RUN cp -r share/* /usr/share && \
    cp -r lib/* /usr/lib && \
    cp -r bin/* /usr/bin && \
    cp -r include/* /usr/include

WORKDIR /playground
