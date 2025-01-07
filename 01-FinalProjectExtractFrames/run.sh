#!/bin/bash

function usage() {
    echo "Invalid arguments!"
    echo "Usage:"
    echo "$0 <File System type>"
    echo ""
    echo "Where <File System type> can be:"
    echo "  local - local file system (file://)"
    echo "  HDFS  - HDFS file system (hdfs://)"
    exit
}


if [[ $# -lt 1 ]]; then

    usage
else
    case "$1" in
        "local")
            FILE_SYSTEM_TYPE=file://
            BASE_DIRECTORY=${HOME}/examples
            ;;
        
        "HDFS")
            FILE_SYSTEM_TYPE=hdfs://
            BASE_DIRECTORY=/user/${USER}
            ;;
        
        *)

            usage
    esac
fi

source ./usage.sh

# Paths and directories
VIDEO_FOLDER=ProjectVideos
INPUT=${BASE_DIRECTORY}/input/videos/${VIDEO_FOLDER}
INPUT_DIRECTORY=${FILE_SYSTEM_TYPE}${INPUT}

OUTPUT_FOLDER=OpenCV/VideoFrames
OUTPUT=${BASE_DIRECTORY}/output/${OUTPUT_FOLDER}
OUTPUT_DIRECTORY=${FILE_SYSTEM_TYPE}${OUTPUT}

#echo "Input folder: " ${INPUT}
#echo "Input directory: " ${INPUT_DIRECTORY}
#echo "Output folder: " ${OUTPUT}
#echo "Output directory: " ${OUTPUT_DIRECTORY}

echo "Setting up directories and removing previous output..."
echo ""

if [ "${FILE_SYSTEM_TYPE}" == "file://" ]; then
    # Local file system
    CMD="rm -rf ${OUTPUT}"
    echo "${CMD}"
    ${CMD}
    # CMD="mkdir -p ${OUTPUT}"
    # echo "${CMD}"
    # ${CMD}
else
    # HDFS
    CMD="hadoop fs -mkdir -p ${INPUT}"
    echo "${CMD}"
    ${CMD}
    LOCAL_INPUT=file://${HOME}/examples/input/videos/${VIDEO_FOLDER}
    echo "LOCAL_INPUT: ${LOCAL_INPUT}"
    CMD="hadoop fs -cp -f ${LOCAL_INPUT}/*.* ${INPUT}"
    echo "${CMD}"
    ${CMD}
    
    CMD="hadoop fs -rm -f -r ${OUTPUT}"
    echo "${CMD}"
    ${CMD}
fi



LIBRARY_PATH="../NativeLibs"
OPENCV_JAR_BASE_PATH="./target/libs/opencv-4.9.0-0.jar"
OPENCV_NATIVE_LIB_PATH=`realpath ${LIBRARY_PATH}/libopencv_java490.so`
JAR_FILE_BASE_PATH="./target/01-FinalProjectExtractFrames-2020.2021.SemInv.jar"

JAR_FILE=`realpath "${JAR_FILE_BASE_PATH}"`
OPENCV_JAR=`realpath "${OPENCV_JAR_BASE_PATH}"`

JOB_CONFIG="-conf myconf.xml"

JOB_OPTIONS="${JOB_CONFIG} -files file://${OPENCV_NATIVE_LIB_PATH} -libjars file://${OPENCV_JAR}"

# Arguments for the Hadoop job

ARGS="${JOB_OPTIONS} ${INPUT_DIRECTORY} ${OUTPUT_DIRECTORY}"

echo ""
echo "Exporting classpath..."
CMD="export HADOOP_CLASSPATH=${JAR_FILE}"
echo ${CMD}
${CMD}
echo ""

echo "JARFILE: ${JAR_FILE}"
echo ""
echo "Running Hadoop job..."
CMD="hadoop jar ${JAR_FILE} ${ARGS}"
echo ${CMD}
echo ""
${CMD}


