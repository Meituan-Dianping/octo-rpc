INCLUDE_DIRECTORIES(SYSTEM echo)
INCLUDE_DIRECTORIES(SYSTEM ${INCLUDE_OUTPUT_PATH})
LINK_DIRECTORIES(${LIBRARY_OUTPUT_PATH})

SET(ECHO_THRIFT_SRCS
        echo/echo_constants.cpp
        echo/echo_types.cpp
        echo/Echo.cpp
        )

ADD_EXECUTABLE(cthrift_svr_example ${ECHO_THRIFT_SRCS} cthrift_svr_example.cc)

TARGET_LINK_LIBRARIES(cthrift_svr_example -Xlinker "-(" libcthrift.a
        libmns_sdk.a ${MUDUO_BASE_LIBRARY} ${MUDUO_NET_LIBRARY} ${MUDUO_HTTP_LIBRARY}
        ${ZOOKEEPER_LIBRARY} liboctoidl.a  pthread rt ${ZLIB_LIBRARY} ${LOG4CPLUS_LIBRARY}
        ${THRIFT_LIBRARY} -Xlinker "-)")

ADD_EXECUTABLE(cthrift_cli_example ${ECHO_THRIFT_SRCS} cthrift_cli_example.cc)

TARGET_LINK_LIBRARIES(cthrift_cli_example -Xlinker "-(" libcthrift.a
        libmns_sdk.a ${MUDUO_BASE_LIBRARY} ${MUDUO_NET_LIBRARY} ${MUDUO_HTTP_LIBRARY}
        ${ZOOKEEPER_LIBRARY} liboctoidl.a  pthread rt ${ZLIB_LIBRARY} ${LOG4CPLUS_LIBRARY}
        ${THRIFT_LIBRARY} -Xlinker "-)")

ADD_EXECUTABLE(cthrift_async_example ${ECHO_THRIFT_SRCS} cthrift_async_example.cc)

TARGET_LINK_LIBRARIES(cthrift_async_example -Xlinker "-(" libcthrift.a
        libmns_sdk.a  ${MUDUO_BASE_LIBRARY} ${MUDUO_NET_LIBRARY} ${MUDUO_HTTP_LIBRARY}
        ${ZOOKEEPER_LIBRARY} liboctoidl.a  pthread rt ${ZLIB_LIBRARY} ${LOG4CPLUS_LIBRARY}
        ${THRIFT_LIBRARY} -Xlinker "-)")


EXECUTE_PROCESS(COMMAND find ${CTHRIFT_SRC_PATH}/example -name conf.json -exec cp -t ${EXECUTABLE_OUTPUT_PATH} {} \;)
EXECUTE_PROCESS(COMMAND find ${CTHRIFT_SRC_PATH}/example -name log4cplus.conf -exec cp -t ${EXECUTABLE_OUTPUT_PATH} {} \;)
