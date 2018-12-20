cmake_minimum_required(VERSION 2.6)

SET(CTHRIFT_SRC
        cthrift_svr.cc
        cthrift_name_service.cc
        cthrift_transport.cc
        cthrift_client_worker.cc
        cthrift_client.cc
        cthrift_client_channel.cc
        )

SET(MNS_ZK_SRC
        util/cthrift_mns_imp.cc
        util/cthrift_config.cc
        util/cthrift_common.cc
        util/cthrift_ns_imp.cc
        util/cthrift_zk_client.cc
        util/zk_tools.cc
        util/zk_client.cc
        util/log4cplus.cc
        )

ADD_LIBRARY(cthrift STATIC ${CTHRIFT_SRC} ${MNS_ZK_SRC})
ADD_LIBRARY(cthrift_dynamic SHARED ${CTHRIFT_SRC} ${MNS_ZK_SRC})

SET_TARGET_PROPERTIES(cthrift_dynamic PROPERTIES OUTPUT_NAME "cthrift")

EXECUTE_PROCESS(COMMAND find ${CTHRIFT_SRC_PATH}/cthrift  -path ${CTHRIFT_SRC_PATH}/cthrift/util -prune -o  -name *.h -exec cp -t ${INCLUDE_CTHRIFT_OUTPUT_PATH} {} \;)
EXECUTE_PROCESS(COMMAND find ${CTHRIFT_SRC_PATH}/cthrift  -path ${CTHRIFT_SRC_PATH}/cthrift/util -prune -o  -name *.tcc -exec cp -t ${INCLUDE_CTHRIFT_OUTPUT_PATH} {} \;)

EXECUTE_PROCESS(COMMAND find ${CTHRIFT_SRC_PATH}/cthrift/util -name *.h -exec cp -t ${INCLUDE_UTIL_OUTPUT_PATH} {} \;)

if (NOT CMAKE_BUILD_NO_TESTS)
    ADD_SUBDIRECTORY(tests)
endif ()
