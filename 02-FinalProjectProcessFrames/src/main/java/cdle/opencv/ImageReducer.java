package cdle.opencv;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class ImageReducer extends Reducer<Text, LongWritable, Text, LongWritable> {

    @Override
    protected void reduce(Text key, Iterable<LongWritable> values, Context context)
            throws IOException, InterruptedException {

        // Sum up all possession counts for the given team
        long totalPossessionTime = 0;
        for (LongWritable value : values) {
            totalPossessionTime += value.get();
        }

        // Emit the aggregated result
        context.write(key, new LongWritable(totalPossessionTime));
    }
}
