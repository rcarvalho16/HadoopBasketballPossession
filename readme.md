# Basketball Video Analysis with Hadoop MapReduce

This repository contains a project focused on analyzing basketball game footage to calculate ball possession times for teams and individual players. The analysis is implemented using **Hadoop MapReduce**, and the video frames are processed to detect ball possession using **OpenCV** and **YOLO** object detection models.

## Project Overview

The project involves:
- Analyzing basketball game videos to detect and compute the ball possession time for teams and players.
- Leveraging **Hadoop MapReduce** for distributed processing of video frames.
- Utilizing **OpenCV** for image decoding and **YOLO** for object detection to identify bounding boxes for players, teams, and the ball.

The final output provides detailed statistics about:
1. Team possession times.
2. Player possession times.
3. Ball in-air time.

## Features

- **Distributed Video Processing:** Efficiently processes large basketball video datasets using Hadoop MapReduce.
- **Frame-based Analysis:** Extracts and analyzes individual video frames to detect ball possession.
- **Accurate Object Detection:** Identifies ball, players, and teams using YOLO object detection.
- **Custom Input and Output Format:** Inputs Sequence Files and outputs possession data as `("frame_xxx", team)` key-value pairs.

## Implementation Details

### 0. First Job (01-FinalProjectExtractFrames)
Read an input video using a custom record reader class (VideoRecordReader) and outputs the frames into a SequenceFile

### 1. Input Format
The input is a **Sequence File** containing `(LongWritable, BytesWritable)` pairs:
- **Key:** Frame index.
- **Value:** Raw image data (frame).

### 2. Mapper Logic
Each mapper:
- Decodes the raw image data using OpenCV.
- Applies YOLO to detect bounding boxes of the ball, players, and teams.
- Determines ball possession based on the bounding box overlap of the ball and players.
- Outputs key-value pairs in the format: ("frame_xxx", team)

### 3. Reducer Logic
The reducer aggregates frame-based outputs to:
- Calculate team possession times.
- Aggregate individual player possession durations.

### 4. Output Format
The final results are stored in a structured file that includes:
- Total team possession times.
- Total player possession times.
- Ball in-air durations.

## Requirements

- **Hadoop**: Version 3.3.0 or later.
- **Java**: JDK 8 or later.
- **YOLO**: Pre-trained weights and configurations for object detection.
- **OpenCV**: Version 4.5.0 or later.

## How to Run

1. **Set up Hadoop**:
 - Install and configure a Hadoop cluster.
 - Place the input Sequence File in HDFS.

2. **Compile the Project**:
 - Use Maven to build the Java project:
   ```bash
   mvn clean package
   ```
3. **Run 01-FinalProjectExtractFrames `run.sh` script**:
4. **Run 02-FinalProjectProcessFrames `run.sh` script**
  - Notice the `run.sh` might need to be changed to work properly

## Dependencies

- OpenCV (`opencv-java`)
- YOLO (`Darknet`)
- Hadoop Libraries

## Future Work

- Enhance player identification for more granular analysis.
- Train specific classifiers to identify players dynamically, instead of being by shirt color

Note: Examples of annotated images can be found under ./images
  - This working example was done using footage of NBA 2k20 as it was the highest quality footage of basketball games found.
