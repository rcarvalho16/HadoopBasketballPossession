#!/bin/bash

function usage() {
    echo "Invalid arguments!"
    echo "Usage:"
    echo "$0 <File System type> <Num_Reducers>"
    echo ""
    echo "Where <File System type> can be:"
    echo "  local - local file system (file://)"
    echo "  HDFS  - HDFS file system (hdfs://)"
    echo ""
    echo "And <Num_Reducers> must be a positive integer."
    exit
}

# Validate arguments
if [[ $# -lt 2 ]]; then
    usage
else
    # Validate file system type
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

    # Validate number of reducers
    if ! [[ $2 =~ ^[0-9]+$ ]] || [[ $2 -lt 0 ]]; then
        echo "Error: <Num_Reducers> must be a positive integer."
        usage
    fi
    NUM_REDUCERS=$2
fi

# Paths and directories
VIDEO_FOLDER=OpenCV/VideoFrames
INPUT=${BASE_DIRECTORY}/output/${VIDEO_FOLDER}
INPUT_DIRECTORY=${FILE_SYSTEM_TYPE}${INPUT}

OUTPUT_FOLDER=OpenCV/AnnotatedFrames
OUTPUT=${BASE_DIRECTORY}/output/${OUTPUT_FOLDER}
OUTPUT_DIRECTORY=${FILE_SYSTEM_TYPE}${OUTPUT}

# Create output directory
if [ "${FILE_SYSTEM_TYPE}" == "file://" ]; then
    # Local file system
    CMD="rm -rf ${OUTPUT}"
    echo "${CMD}"
    ${CMD}
    CMD="mkdir -p ${OUTPUT}"
    echo "${CMD}"
    ${CMD}
    CMD="sudo chmod -R 777 ${OUTPUT}"
    echo "${CMD}"
    ${CMD}
else
    # HDFS
    CMD="hadoop fs -mkdir -p ${INPUT}"
    echo "${CMD}"
    ${CMD}
    LOCAL_INPUT=file://${HOME}/examples/input/${VIDEO_FOLDER}
    echo "LOCAL_INPUT: ${LOCAL_INPUT}"
    CMD="hadoop fs -cp -f ${LOCAL_INPUT}/*.* ${INPUT}"
    echo "${CMD}"
    ${CMD}
    
    CMD="hadoop fs -rm -f -r ${OUTPUT}"
    echo "${CMD}"
    ${CMD}
fi

# Paths
LIBRARY_PATH="../NativeLibs"
OPENCV_JAR_BASE_PATH="./target/libs/opencv-4.9.0-0.jar"
OPENCV_NATIVE_LIB_PATH=`realpath ${LIBRARY_PATH}/libopencv_java490.so`

JAR_FILE_BASE_PATH="./target/02-ProcessFrames-2020.2021.SemInv.jar"
JAR_FILE=`realpath "${JAR_FILE_BASE_PATH}"`
OPENCV_JAR=`realpath "${OPENCV_JAR_BASE_PATH}"`

JOB_CONFIG="-conf myconf.xml"
JOB_OPTIONS="${JOB_CONFIG} -files file://${OPENCV_NATIVE_LIB_PATH} -libjars file://${OPENCV_JAR}"

ARGS="${JOB_OPTIONS} ${INPUT_DIRECTORY} ${OUTPUT_DIRECTORY} ${NUM_REDUCERS}"

# echo ""
# echo "Absolute path of Input: $(realpath ${INPUT})"
# echo "Absolute path of Output: $(realpath ${OUTPUT})"
# echo ""

echo ""
echo "Exporting classpath..."
CMD="export HADOOP_CLASSPATH=${JAR_FILE}"
echo ${CMD}
${CMD}
echo ""

# Execute the command
echo "JAR_FILE: ${JAR_FILE}"
echo ""
echo "Running Hadoop job..."
CMD="hadoop jar ${JAR_FILE} ${ARGS}"
echo ${CMD}
echo ""
${CMD}
