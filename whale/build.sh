#!/bin/sh

set -x

if [ $# -ne 1 ] || [ "$1" != "only_lib" -a "$1" != "with_example" -a "$1" != "with_test" -a "$1" != "clean" -a "$1" != "init" ]
then
echo "build.sh + <only_lib/with_example/with_test/clean/init>, no argument means all"
exit
fi

BUILD_NO_EXAMPLES=${BUILD_NO_EXAMPLES:-0}
BUILD_NO_TESTS=${BUILD_NO_TESTS:-0}

SOURCE_DIR=`pwd`
CPU_CORE_NUM=`cat /proc/cpuinfo | grep processor | wc -l`


    if [ "$1" == "clean" ]
    then
        cd tool
        sh -x clear4git.sh
        cd -
        exit
    fi

    BUILD_NO_EXAMPLES=1
    BUILD_NO_TESTS=1

    if [ "$1" == "with_example" ]
    then
       BUILD_NO_EXAMPLES=0
    elif [ "$1" == "with_test" ]
    then
        BUILD_NO_TESTS=0
    elif [ "$1" == "init" ]
    then
        cmake -DCMAKE_BUILD_INIT=1 $SOURCE_DIR && make -j"$CPU_CORE_NUM"
        cd tool
        sh -x clear4git.sh
        cd -
       exit
    fi


  cmake \
          -DCMAKE_BUILD_NO_EXAMPLES=$BUILD_NO_EXAMPLES \
          -DCMAKE_BUILD_NO_TESTS=$BUILD_NO_TESTS \
           $SOURCE_DIR \
           && make -j"$CPU_CORE_NUM"

