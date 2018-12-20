cmake_minimum_required(VERSION 2.8.10)

project(muduo-download NONE)

include(ExternalProject)
ExternalProject_Add(muduo
        GIT_REPOSITORY    https://github.com/chenshuo/muduo.git
        GIT_TAG           v1.1.0
        SOURCE_DIR        "${PROJECT_BINARY_DIR}/muduo"
        PATCH_COMMAND patch -p1 < ${CMAKE_SOURCE_DIR}/patch/0001-add-patch-for-http.patch
        CMAKE_COMMAND      cmake
        CMAKE_ARGS        "-DCMAKE_BUILD_NO_EXAMPLES=1 -DCMAKE_CXX_FLAGS=-fPIC"
        )
