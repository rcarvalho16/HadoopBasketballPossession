package cdle.opencv;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/* key -> frame index
   value -> frame data as bytes */
public class VideoRecordReader extends RecordReader<LongWritable, BytesWritable> {

    static {
        Utils.loadNative();
    }

    private LongWritable key = new LongWritable();
    private BytesWritable value = new BytesWritable();

    private int currentFrameIndex = 0;
    private int frameStep;
    private int totalFrames;
    private int currentExtractedFrameCount = 0;
    private int extractedFrameCount = 0; // Tracks the number of extracted frames
    private VideoCapture videoCapture;
    private int maxFramesToExtract;
    private String outputDir;

    @Override
    public void initialize(InputSplit split, TaskAttemptContext context) throws IOException {
        Path videoPath = ((FileSplit) split).getPath();
        Configuration conf = context.getConfiguration();

        // Load video
        videoCapture = new VideoCapture(videoPath.toString());
        if (!videoCapture.isOpened()) {
            throw new IOException("Failed to open video: " + videoPath.toString());
        }
        this.maxFramesToExtract = conf.getInt("frames.max.count", 600);

        totalFrames = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
        int fps = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
        float interval = conf.getFloat("frames.interval.seconds", 1.0f);

        frameStep = Math.max(1, (int) (fps * interval)); // calculate frames to skip based on interval

        // Get the output directory from the configuration
        outputDir = conf.get("frames.output.dir", "/tmp/frames");
        File dir = new File(outputDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir);
        }
    }

    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        System.out.printf("FrameExtractorRecordReader#nextKeyValue()\n");

        // If we've already extracted the maximum number of frames, we're done
        if (currentExtractedFrameCount >= maxFramesToExtract) {
            return false;
        }

        // If there are no more frames in the video, stop processing
        int frameToRead = currentExtractedFrameCount * frameStep;
        if (frameToRead >= totalFrames) {
            return false; // no more frames in the video
        }

        // Set the frame position and read the frame
        videoCapture.set(Videoio.CAP_PROP_POS_FRAMES, currentFrameIndex);
        Mat frame = new Mat();
        if (!videoCapture.read(frame) || frame.empty()) {
            return false;
        }

        // Convert the frame to byte array using MatOfByte
        MatOfByte buffer = new MatOfByte();
        if (!Imgcodecs.imencode(".jpg", frame, buffer)) {
            throw new IOException("Failed to encode frame to JPEG");
        }
        byte[] frameBytes = buffer.toArray();

        // Save the frame to disk
        String frameFileName = String.format("%s/frame_%d.jpg", outputDir, currentFrameIndex);
        boolean saved = Imgcodecs.imwrite(frameFileName, frame);
        if (!saved) {
            throw new IOException("Failed to save frame to file: " + frameFileName);
        }

        System.out.printf("Saved frame to: %s\n", frameFileName);

        // Set key and value
        key.set(currentExtractedFrameCount);
        value.set(frameBytes, 0, frameBytes.length);

        // Update counters
        currentFrameIndex += frameStep;
        currentExtractedFrameCount++;
        return true;
    }

    @Override
    public LongWritable getCurrentKey() {
        return key;
    }

    @Override
    public BytesWritable getCurrentValue() {
        return value;
    }

    @Override
    public float getProgress() throws IOException, InterruptedException {
        return maxFramesToExtract == 0 ? 0f : (float) currentExtractedFrameCount / maxFramesToExtract;
    }

    @Override
    public void close() {
        if (videoCapture != null) {
            videoCapture.release();
        }
    }
}
