import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Implements R1) Discovery
 * Allows a node to concurrently send and receive advertisements
 */

public class NodeSearch implements Runnable{
    private final Configuration configuration;
    private final List<String[]> discoveredNodes = new CopyOnWriteArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
    private final MulticastGroup m;
    private final boolean isSender;
    private final int duration = 10;

    public NodeSearch(MulticastGroup m, Configuration configuration, boolean isSender) {
        this.m = m;
        this.configuration = configuration;
        this.isSender = isSender;
    }

    public void run() {
        for (int num = 0; num < duration; num++) {
            try {
                if (isSender) {
                    sendAdvertisement();
                } else {
                    receiveAdvertisement();
                }
                Thread.sleep(configuration.sleepTime); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
                System.out.println("Thread was interrupted, stopping task.");
                break;
            }
        }
    }

    private void sendAdvertisement() {
        try {
            byte[] sendAdv = new byte[configuration.maximumMessageSize];
            int serialNum = ThreadLocalRandom.current().nextInt(10000, 100000);
            String timestamp = sdf.format(new Date());
            String header = configuration.id + ":" + serialNum + ":" + timestamp;
            String payload = "advertisement:" + configuration.mPort + ":search=" + configuration.search;
            String msg = header + ":" + payload+ ":";

            sendAdv = msg.getBytes("US-ASCII");

            m.send_to_group(sendAdv);
            configuration.log.writeLog("tx-> " + msg, true);
            
        } catch (UnsupportedEncodingException e) {
            System.out.println("txBeacon(): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception in sendAdvertisement: " + e.getMessage());
        }
    }

    private void receiveAdvertisement() {
        try {
            byte[] receiveAdv = new byte[configuration.maximumMessageSize];
            String logRequest = "";

            m.receive_from_group(receiveAdv);

            logRequest = new String(receiveAdv, "US-ASCII");
            logRequest = logRequest.trim();

            if (!logRequest.isEmpty()) {
            configuration.log.writeLog("->rx " + logRequest, true);
            String[] newNode = logRequest.split(":");

                if (newNode[3].equals("advertisement")) {
                    boolean found = false;
                    for (String[] node : discoveredNodes) {
                        if (node[0].equals(newNode[0])) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        discoveredNodes.add(newNode);
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("rxBeacon(): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception in receiveAdvertisement: " + e.getMessage());
        }
    }

    public List<String[]> getDiscoveredNodes() {
        return discoveredNodes;
    }
}