import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class QFDWriterReducer extends Reducer<WTRKey, RequestReplyMatch, NullWritable, NullWritable> {

    @Override
    public void reduce(WTRKey key, Iterable<RequestReplyMatch> values,
                       Context ctxt) throws IOException, InterruptedException {

        // The input will be a WTR key and a set of matches.

        // You will want to open the file named
        // "qfds/key.getName()/key.getName()_key.getHashBytes()"
        // using the FileSystem interface for Hadoop.

        // EG, if the key's name is srcIP and the hash is 2BBB,
        // the filename should be qfds/srcIP/srcIP_2BBB

        // Some useful functionality:

        // FileSystem.get(ctxt.getConfiguration())
        // gets the interface to the filesysstem
        // new Path(filename) gives a path specification
        // hdfs.create(path, true) will create an
        // output stream pointing to that file

        Set<RequestReplyMatch> mm = new HashSet<>();
        for (RequestReplyMatch rrm : values) {
            mm.add(new RequestReplyMatch(rrm));
        }

        QueryFocusedDataSet qfd  = new QueryFocusedDataSet(key.getName(), key.getHashBytes(), mm);
        String fileName = "qfds/" + key.getName() + "/" + key.getName() + "_" + key.getHashBytes();
        FileSystem fs = FileSystem.get(ctxt.getConfiguration());
        Path p = new Path(fileName);
        FSDataOutputStream fsOut = fs.create(p,true);
//        FileOutputStream fileOut = fs.create(p, true);
        ObjectOutputStream out = new ObjectOutputStream(fsOut);
        out.writeObject(qfd);
        fsOut.close();
        out.close();


//        FileSystem fs = FileSystem.get(ctxt.getConfiguration());
////        Path path = Paths.get(fileName);
//        fs.create(path, true);
    }
}
