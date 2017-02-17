import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
import java.util.*;
import java.io.ObjectInputStream;

public class TotalFailMapper extends Mapper<LongWritable, Text, WTRKey,
                                            RequestReplyMatch> {

    private MessageDigest messageDigest;
    @Override
    public void setup(Context ctx) throws IOException, InterruptedException {
        // You probably need to do the same setup here you did
        // with the QFD writer
        super.setup(ctx);
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            // SHA-1 is required on all Java platforms, so this
            // should never occur
            throw new RuntimeException("SHA-1 algorithm not available");
        }
        // Now we are adding the salt...
        messageDigest.update(HashUtils.SALT.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void map(LongWritable lineNo, Text line, Context ctxt)
            throws IOException, InterruptedException {

        // The value in the input for the key/value pair is a Tor IP.
        // You need to then query that IP's source QFD to get
        // all cookies from that IP,
        // query the cookie QFDs to get all associated requests
        // which are by those cookies, and store them in a torusers QFD

        String srcip = line.toString();
        MessageDigest md = HashUtils.cloneMessageDigest(messageDigest);
        md.update(srcip.getBytes(StandardCharsets.UTF_8));
        byte[] hash = md.digest();
        byte[] hashBytes = Arrays.copyOf(hash, HashUtils.NUM_HASH_BYTES);
        String hashString = DatatypeConverter.printHexBinary(hashBytes);
        String fileName = "qfds/" + "srcIP" + "/" + "srcIP" + "_" + hashString;
        Path path = new Path(fileName);

        try {
            FileSystem fSystem = FileSystem.get(ctxt.getConfiguration());
            FSDataInputStream input = fSystem.open(path);
            ObjectInputStream ois = new ObjectInputStream(input);
            QueryFocusedDataSet qfd = (QueryFocusedDataSet) ois.readObject();
            ois.close();

            Set<RequestReplyMatch> rrMatches = qfd.getMatches();
            List<String> cookies = new ArrayList<>();

            for (RequestReplyMatch one : rrMatches) {
                if (Objects.equals(one.getSrcIp(), srcip)) {
                    cookies.add(one.getCookie());
                }
            }


            // deserialize each cookie got from above, store the corresponding qfd's requestReplyMatch into a List.

//            List reqRepMatches = new ArrayList<>();

            for (int i = 0; i < cookies.size(); i++) {
                // System.out.println("www");
                String cookie = cookies.get(i).toString();
                MessageDigest md2 = HashUtils.cloneMessageDigest(messageDigest);
                md2.update(cookie.getBytes(StandardCharsets.UTF_8));
                hash = md2.digest();
                hashBytes = Arrays.copyOf(hash, HashUtils.NUM_HASH_BYTES);
                hashString = DatatypeConverter.printHexBinary(hashBytes);
                String fileName2 = "qfds/" + "cookie" + "/" + "cookie" + "_" + hashString;
                path = new Path(fileName2);

                try {
                    fSystem = FileSystem.get(ctxt.getConfiguration());
                    input = fSystem.open(path);
                    ois = new ObjectInputStream(input);
                    qfd = (QueryFocusedDataSet) ois.readObject();
//                    reqRepMatches.add(qfd2.getMatches());
                    ois.close();  //where to close? close at end????
                    for (RequestReplyMatch match : qfd.getMatches()) {
                        if (Objects.equals(match.getCookie(), cookie)) {
                            MessageDigest md3 = HashUtils.cloneMessageDigest(messageDigest);
                            md3.update(match.getUserName().getBytes(StandardCharsets.UTF_8));
                            hash = md3.digest();
                            hashBytes = Arrays.copyOf(hash, HashUtils.NUM_HASH_BYTES);
                            hashString = DatatypeConverter.printHexBinary(hashBytes);
                            WTRKey key = new WTRKey("torusers", hashString);
                            ctxt.write(key, match);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
