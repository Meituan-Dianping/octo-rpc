#!/usr/bin/env bash

rm -rf ../cmake_install.cmake ../CMakeCache.txt ../CMakeFiles ../Makefile ../build/ ../install_manifest.txt
rm -rf ../cthrift/cmake_install.cmake ../cthrift/CMakeCache.txt .
./cthrift/CMakeFiles ../cthrift/Makefile
rm -rf ../cthrift/tests/cmake_install.cmake ../cthrift/tests/CMakeCache.txt ../cthrift/tests/CMakeFiles ../cthrift/tests/Makefile
rm -rf ../example/cmake_install.cmake ../example/CMakeCache.txt ../example/CMakeFiles ../example/Makefile

find ../ -name .*.sw* -exec rm -rf {} \;
