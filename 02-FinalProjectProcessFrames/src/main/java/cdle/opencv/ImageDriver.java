package cdle.opencv;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class ImageDriver extends Configured implements Tool {

    @Override
    public int run(String[] args) throws Exception {
        if (args.length <= 2) {
            System.err.println("Usage: ImageDriver <input path> <output path> <num reducers>");
            return -1;
        }

        // Create configuration
        Configuration conf = getConf();
        conf.set("image.output.path", args[1]);

        // Create job
        Job job = Job.getInstance(conf);
        job.setJarByClass(ImageDriver.class);
        job.setJobName("Ball Possession Analysis");

        // Set input/output paths
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1] + "_logs"));

        // Set input/output formats
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        // Set mapper
        job.setMapperClass(ImageMapper.class);

        // Set reducer
        job.setReducerClass(ImageReducer.class);

        // Set output key/value classes
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        // No reducer needed
        job.setNumReduceTasks(Integer.parseInt(args[2]));
        // job.setNumReduceTasks(2);

        // Run the job and wait for completion
        boolean success = job.waitForCompletion(true);
        if (success) {
            Path fullPath = new Path(args[1]);
            Path parentPath = fullPath.getParent();

            // Append the desired text file name
            String finalOutputPath = new Path(parentPath, "Possession.txt").toString();

            mergeOutputFiles(conf, args[1] + "_logs", finalOutputPath);
            return 0;
        } else {
            return 1;
        }
    }

    private void mergeOutputFiles(Configuration conf, String outputDirPath, String finalOutputPath) throws Exception {
        // Use the local filesystem
        FileSystem localFs = FileSystem.getLocal(conf);
        Path outputDir = new Path(outputDirPath);
        Path finalOutputFile = new Path(finalOutputPath);

        // Create the final output file
        try (FSDataOutputStream outputStream = localFs.create(finalOutputFile)) {
            // List all part files and merge them
            FileStatus[] outputFiles = localFs.listStatus(outputDir);
            for (FileStatus file : outputFiles) {
                if (file.getPath().getName().startsWith("part-r-")) {
                    try (FSDataInputStream inputStream = localFs.open(file.getPath())) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }

        // Optionally delete the intermediate output directory
        localFs.delete(outputDir, true);
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new ImageDriver(), args);
        System.exit(exitCode);
    }
}
