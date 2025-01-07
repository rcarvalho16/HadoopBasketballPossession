package cdle.opencv;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class VideoFrameExtractorJob extends Configured implements Tool {

	public static void main(String[] args) throws Exception {

		System.exit(ToolRunner.run(new VideoFrameExtractorJob(), args));
	}

	@Override
    public int run(String[] args) throws Exception {
        // Load custom configuration
        Job job = Job.getInstance(getConf());
        job.setJarByClass(VideoFrameExtractorJob.class);

        // Load the native OpenCV library

        // Add the input path
        job.setInputFormatClass(VideoInputFormat.class);
        VideoInputFormat.addInputPath(job, new Path(args[0]));

        // Configure mapper
        job.setMapperClass(FrameExtractorMapper.class);
        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(BytesWritable.class);

        // Configure output format and output path
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        SequenceFileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(BytesWritable.class);
        // Add the OpenCV dependency (optional, used for class loading)
        //job.addFileToClassPath(new Path("file:///home/usermr/examples/Projects/08-Project/01-FinalProjectExtractFrames/target/libs/opencv-4.9.0-0.jar"));

        // Wait for the job to complete
        return job.waitForCompletion(true) ? 0 : 1;
    }
}
