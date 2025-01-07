package cdle.opencv;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

public class ImageMapper extends Mapper<LongWritable, BytesWritable, Text, LongWritable> {

    private ImageAnnotator imageAnnotator;

    static {
        Utils.loadNative();
    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        try {
            // Get configuration paths for YOLO model and class names
            Configuration conf = context.getConfiguration();
            String modelConfigPath = conf.get("yolo.model.config");
            String modelWeightsPath = conf.get("yolo.model.weights");
            String classNamesPath = conf.get("yolo.class.names");
            String outputPath = conf.get("image.output.path");

            // Initialize ImageAnnotator
            this.imageAnnotator = new ImageAnnotator(modelConfigPath, modelWeightsPath, classNamesPath, outputPath);
        } catch (Exception e) {
            throw new IOException("Failed to initialize ImageAnnotator", e);
        }
    }

    @Override
    protected void map(LongWritable key, BytesWritable value, Context context)
            throws IOException, InterruptedException {
        try {
            // Convert BytesWritable to Mat
            byte[] imageData = value.getBytes();
            MatOfByte matOfByte = new MatOfByte(imageData);
            Mat image = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);

            if (!image.empty()) {
                // Perform marking and possession detection

                String imagePath = String.format("frame_%d", key.get());
                String possession = imageAnnotator.annotateAndDetect(imagePath, image);

                // Emit the result
                context.write(new Text(possession), new LongWritable(1));
            } else {
                context.write(new Text("Failed to decode image"), key);
                context.getCounter("ImageProcessing", "FailedImages").increment(1);
            }
        } catch (Exception e) {
            context.write(new Text("Error processing image: " + e.getMessage()), key);
            context.getCounter("ImageProcessing", "Errors").increment(1);
        }
    }
}
