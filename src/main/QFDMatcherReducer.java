import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

import java.io.IOException;

public class QFDMatcherReducer extends Reducer<IntWritable, WebTrafficRecord, RequestReplyMatch, NullWritable> {

    @Override
    public void reduce(IntWritable key, Iterable<WebTrafficRecord> values,
                       Context ctxt) throws IOException, InterruptedException {

        // The input is a set of WebTrafficRecords for each key,
        // the output should be the WebTrafficRecords for
        // all cases where the request and reply are matched
        // as having the same
        // Source IP/Source Port/Destination IP/Destination Port
        // and have occured within a 10 second window on the timestamp.

        // One thing to really remember, the Iterable element passed
        // from hadoop are designed as READ ONCE data, you will
        // probably want to copy that to some other data structure if
        // you want to iterate mutliple times over the data.

        List<WebTrafficRecord> requestList = new ArrayList<WebTrafficRecord>();
        List<WebTrafficRecord> replyList = new ArrayList<WebTrafficRecord>();
        for (WebTrafficRecord wtr : values) {
            if (wtr.getUserName() == null) {
//            WebTrafficRecord permanent = new WebTrafficRecord(record);
                requestList.add(new WebTrafficRecord(wtr));
            } else {
                replyList.add(new WebTrafficRecord(wtr));
            }
        }

        for (int i = 0; i < requestList.size(); i++) {
            for (int k = 0; k < replyList.size(); k++) {
                if (requestList.get(i).tupleMatches(replyList.get(k))) {
                    if (Math.abs(requestList.get(i).getTimestamp() - replyList.get(k).getTimestamp()) < 10L) {
                        RequestReplyMatch rrm = new RequestReplyMatch(requestList.get(i), replyList.get(k));
                        ctxt.write(rrm, NullWritable.get());
                    }
                }
            }
        }

        // ctxt.write should be RequestReplyMatch and a NullWriteable
    }
}
