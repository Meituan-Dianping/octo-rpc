#!/usr/bin/env bash
#!/bin/sh

# ------------------------------------
# default jvm args
# ------------------------------------
JVM_ARGS="-server -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Djava.io.tmpdir=/tmp -Djava.net.preferIPv6Addresses=false"
JVM_GC="-XX:+DisableExplicitGC -XX:+PrintGCDetails -XX:+PrintHeapAtGC -XX:+PrintTenuringDistribution -XX:+UseConcMarkSweepGC -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps"
JVM_GC=$JVM_GC" -XX:CMSFullGCsBeforeCompaction=0 -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=60 -XX:+UseCMSInitiatingOccupancyOnly"
JVM_HEAP="-XX:SurvivorRatio=8 -XX:NewRatio=3 -XX:PermSize=256m -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError -XX:ReservedCodeCacheSize=128m -XX:InitialCodeCacheSize=128m"
JVM_SIZE="-Xmx3g -Xms3g"


# ------------------------------------
# appkey and application name of your service
# ------------------------------------
APPKEY=com.meituan.octo.dorado.benchmark.client
MODULE=dorado-benchmark-client

# -----------------------------------------------------------------------------
# if permission limited, create directory firstly and use chown to change owner
# or modify the directory
# -----------------------------------------------------------------------------
WORK_PATH=/opt/apps/$APPKEY
LOG_PATH=/opt/logs/$APPKEY
START_LOG_PATH=/tmp/$MODULE.start.log
JAR_NAME=$MODULE.jar

function generateJar() {
    cd ../
    mvn clean -U package
    cd -
}

function init() {
    if [ ! -d $WORK_PATH  ];then
	    mkdir -p $WORK_PATH
	fi
    if [ ! -d $LOG_PATH  ];then
	    mkdir -p $LOG_PATH
    fi

    cp ../target/$JAR_NAME $WORK_PATH/
    cd $WORK_PATH
}


function run() {
	EXEC="exec"

	EXEC_JAVA="$EXEC java $JVM_ARGS $JVM_SIZE $JVM_HEAP $JVM_GC"
	EXEC_JAVA=$EXEC_JAVA" -Xloggc:$LOG_PATH/$MODULE.gc.log -XX:ErrorFile=$LOG_PATH/$MODULE.vmerr.log -XX:HeapDumpPath=$LOG_PATH/$MODULE.heaperr.log"

    if [ ! -f $WORK_PATH/$JAR_NAME  ];then
    	echo "There is no jar at $WORK_PATH !!"
    else
        ${EXEC_JAVA} -jar $JAR_NAME #> $START_LOG_PATH 2>&1 &
        echo "*****************************************************************"
        echo "Your $MODULE is running!\nCheck the start log at: $START_LOG_PATH"
    fi

}


# ------------------------------------
# actually work
# ------------------------------------
#generateJar
init
run