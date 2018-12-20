IF (ENABLE_COVERAGE)
    SET(CMAKE_CXX_FLAGS "-g -Wall -O0 -fPIC -DMUDUO_STD_STRING -DHAVE_NETINET_IN_H -DGTEST")
ELSE ()
    SET(CMAKE_CXX_FLAGS "-g -Wall -O2 -fPIC -DMUDUO_STD_STRING -DHAVE_NETINET_IN_H -DGTEST")
ENDIF ()
SET(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake")
MESSAGE(STATUS "CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH}")

set(EXECUTABLE_OUTPUT_PATH "")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}  -fprofile-arcs -ftest-coverage")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS}  -fprofile-arcs -ftest-coverage")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS}  -fprofile-arcs -ftest-coverage-lgcov")

IF (ENABLE_COVERAGE)
    include(CodeCoverage)
    APPEND_COVERAGE_COMPILER_FLAGS()
ENDIF ()
MESSAGE(STATUS "CMAKE_CXX_FLAGS ${CMAKE_CXX_FLAGS}")

option(CTHRIFT_DOWNLOAD_GTEST "Download and build a fresh copy of googletest. Requires Internet access." ON)
set(CTHRIFT_SYSTEM_GTEST_SOURCE_DIR "" CACHE PATH "System googletest source directory.")

include(module)

if (CTHRIFT_DOWNLOAD_GTEST)
    include(SetupGtest)
elseif (CTHRIFT_SYSTEM_GTEST_SOURCE_DIR)
    add_subdirectory("${CTHRIFT_SYSTEM_GTEST_SOURCE_DIR}" "${PROJECT_BINARY_DIR}/system-googletest-build")
else ()
    message(FATAL_ERROR "Googletest is not available")
endif ()

INCLUDE_DIRECTORIES(SYSTEM echo)
INCLUDE_DIRECTORIES(SYSTEM ${INCLUDE_OUTPUT_PATH})

SET(ECHO_THRIFT_SRCS
        echo/echo_constants.cpp
        echo/echo_types.cpp
        echo/Echo.cpp
        )

INCLUDE_DIRECTORIES(SYSTEM ${GTEST_INCLUDE_DIR})
INCLUDE_DIRECTORIES(SYSTEM ${INCLUDE_OUTPUT_PATH})
INCLUDE_DIRECTORIES(SYSTEM ${CMAKE_SOURCE_DIR}/googletest-src/googletest/include/)
LINK_DIRECTORIES(${LIBRARY_OUTPUT_PATH})
set(unit_echo ${ECHO_THRIFT_SRCS})

add_library(util_unit_test_main STATIC ${unit_echo} main.cc)
add_library(util_unit_test_main_async STATIC ${unit_echo})


set(TEST_FILE_LIST
        "cthrift_client_unittest.cc"
        "cthrift_transport_unittest.cc"
        "cthrift_common_unittest.cc"
        "cthrift_sg_agent_unittest.cc"
        "cthrift_util_zk_tools_unittest.cc"
        "cthrift_channel_unittest.cc"
        )

foreach (TEST_FILE_NAME ${TEST_FILE_LIST})
    get_filename_component(test_name ${TEST_FILE_NAME} NAME_WE)
    add_executable(${test_name} ${TEST_FILE_NAME})
    target_link_libraries(${test_name} util_unit_test_main
            -Xlinker "-(" libcthrift.a libmns_sdk.a ${MUDUO_BASE_LIBRARY} ${MUDUO_NET_LIBRARY} ${MUDUO_HTTP_LIBRARY}
            ${ZOOKEEPER_LIBRARY} liboctoidl.a  pthread rt ${ZLIB_LIBRARY} ${LOG4CPLUS_LIBRARY}
            ${THRIFT_LIBRARY}
            gtest -Xlinker "-)"
            )
    cthrift_add_test(${test_name} ${test_name} CONFIGURATIONS Debug)
endforeach ()


add_executable(main_async_unittest main_async.cc cthrift_client_worker_unittest.cc)
target_link_libraries(main_async_unittest util_unit_test_main_async
        -Xlinker "-(" libcthrift.a libmns_sdk.a ${MUDUO_BASE_LIBRARY} ${MUDUO_NET_LIBRARY} ${MUDUO_HTTP_LIBRARY}
        ${ZOOKEEPER_LIBRARY} liboctoidl.a  pthread rt ${ZLIB_LIBRARY} ${LOG4CPLUS_LIBRARY}
        ${THRIFT_LIBRARY}
        gtest -Xlinker "-)"
        )
cthrift_add_test(main_async_unittest main_async_unittest CONFIGURATIONS Debug)


add_executable(main_svr_unittest main_svr.cc cthrift_server_unittest.cc)
target_link_libraries(main_svr_unittest util_unit_test_main_async
        -Xlinker "-(" libcthrift.a libmns_sdk.a ${MUDUO_BASE_LIBRARY} ${MUDUO_NET_LIBRARY} ${MUDUO_HTTP_LIBRARY}
        ${ZOOKEEPER_LIBRARY} liboctoidl.a  pthread rt ${ZLIB_LIBRARY} ${LOG4CPLUS_LIBRARY}
        ${THRIFT_LIBRARY}
        gtest -Xlinker "-)"
        )
cthrift_add_test(main_svr_unittest main_svr_unittest CONFIGURATIONS Debug)


add_executable(main_all_unittest main_all.cc ${TEST_FILE_LIST} cthrift_client_worker_unittest.cc)
target_link_libraries(main_all_unittest util_unit_test_main_async
        -Xlinker "-(" libcthrift.a libmns_sdk.a ${MUDUO_BASE_LIBRARY} ${MUDUO_NET_LIBRARY} ${MUDUO_HTTP_LIBRARY}
        ${ZOOKEEPER_LIBRARY} liboctoidl.a  pthread rt ${ZLIB_LIBRARY} ${LOG4CPLUS_LIBRARY}
        ${THRIFT_LIBRARY} gtest -Xlinker "-)"
        )
cthrift_add_test(main_all_unittest main_all_unittest CONFIGURATIONS Debug)
