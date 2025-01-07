package cdle.opencv;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;

public class FrameExtractorMapper extends Mapper<LongWritable, BytesWritable, LongWritable, BytesWritable> {

    static {
        System.out.println( "static in FrameExtractorMapper..." );
        System.out.flush();
    }

    @Override
    public void setup(Context context) throws IOException {
        URI[] pat = Job.getInstance( context.getConfiguration() ).getCacheFiles();

        System.out.printf( "FrameExtractorMapper#setup(%s)\n", context.toString() );
        
        System.out.printf( "FrameExtractorMapper#Existing files=%d\n", pat.length );
        
        for ( URI p : pat ) {
            System.out.println( "FrameExtractorMapper#Path: " + p.toString() );
        }
    }

    @Override
    public void map(LongWritable key, BytesWritable value, Context context) throws IOException, InterruptedException {
        //String framePath = value.toString();

        // Log or process frame (here, we simply log the frame path)
        context.write(key, value);
    }
}
