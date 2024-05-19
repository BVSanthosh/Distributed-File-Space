import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Scanner;
import java.io.File;

/*
 * Imlpements R2) Search
 * Allows each node to simultaneously send and receive search requests
 */

public class FileSpaceSearch implements Runnable{
  private final Configuration configuration;
  private final List<String[]> responses = new CopyOnWriteArrayList<>();
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
  private final MulticastGroup m;
  private final boolean isRequest;
  private final int serialNum = ThreadLocalRandom.current().nextInt(10000, 100000);
  private final int duration = 10;
  private String searchFile;
  private int resSerialNum;

  public FileSpaceSearch(MulticastGroup m, Configuration configuration, boolean isRequest) {
      this.m = m;
      this.configuration = configuration;
      this.isRequest = isRequest;
  }

  public void run() {
    for (int num = 0; num < duration; num++) {
      try {
        if (isRequest) {
          sendRequest();
        } else {
          sendResponse();
        }

        Thread.sleep(configuration.sleepTime); 
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); 
        System.out.println("Thread was interrupted, stopping task.");
        break;
      }
    }
  }

  private void sendRequest() {
    try {
        byte[] sendAdv = new byte[configuration.maximumMessageSize];
        String timestamp = sdf.format(new Date());
        String searchType = getSearchType();
        String header = configuration.id + ":" + serialNum + ":" + timestamp;
        String payload = "search-request:" + searchType + ":" + searchFile;
        String msg = header + ":" + payload + ":";

        sendAdv = msg.getBytes("US-ASCII");
        m.send_to_group(sendAdv);

        configuration.log.writeLog("tx-> " + msg, true);
    } catch (UnsupportedEncodingException e) {
        System.out.println("txBeacon(): " + e.getMessage());
    }
  }

  private void sendResponse() {
    try {
      byte[] receiveAdv = new byte[configuration.maximumMessageSize];
      String logRequest = "";

      m.receive_from_group(receiveAdv);

      logRequest = new String(receiveAdv, "US-ASCII");
      logRequest = logRequest.trim();

      if (!logRequest.isEmpty()) {
        configuration.log.writeLog("->rx " + logRequest, true);
        String[] searchNode = logRequest.split(":");
        String strSerialNum = String.valueOf(resSerialNum);
        String searchType = getSearchType();

        if ("search-result".equals(searchNode[3]) && configuration.id.equals(searchNode[4]) && strSerialNum.equals(searchNode[5])) {
          addResponse(searchNode);
        } else if ("search-error".equals(searchNode[3]) && configuration.id.equals(searchNode[4]) && strSerialNum.equals(searchNode[5])) {
          addResponse(searchNode);
        }

        if (searchNode[3].equals("search-request")) {
          int newSerialNum = ThreadLocalRandom.current().nextInt(10000, 100000);
          String time = sdf.format(new Date());
          String rheader = configuration.id + ":" + newSerialNum + ":" + time;
          String rpayload = "";
          String rMsg = "";
          byte[] response = new byte[configuration.maximumMessageSize];

          if (configuration.search.equals("none")) {
            rpayload = "search-error:" + searchNode[0] + ":" + searchNode[1];
          } else if (configuration.search.equals("path") && searchType.equals("filename")) {
            rpayload = "search-error:" + searchNode[0] + ":" + searchNode[1];
          } else {
            File dir = new File(configuration.rootDir);
            boolean result = searchFile(dir, searchNode);

            if (result) {
              rpayload = "search-result:" + searchNode[0] + ":" + searchNode[1] + ":" + searchNode[5];
            }
            else {
              rpayload = "search-error:" + searchNode[0] + ":" + searchNode[1];
            } 
          }

          rMsg = rheader + ":" + rpayload + ":";

          response = rMsg.getBytes("US-ASCII");
          m.send_to_group(response);
          configuration.log.writeLog("tx-> " + rMsg, true);
        } 
      }
    } catch (UnsupportedEncodingException e) {
        System.out.println("rxBeacon(): " + e.getMessage());
    }
  }

  private boolean searchFile(File dir, String[] searchNode) {
    File[] fileList = dir.listFiles();

    for (File file : fileList) {
      if (file.isDirectory()) {
        if (searchFile(file, searchNode)) {
          return true;
        }
      } else if ("filename".equals(searchNode[4]) && file.getName().equals(searchNode[5])) {
        return true;
      } else if ("path".equals(searchNode[4]) && file.getPath().equals(searchNode[5])) {
        return true;
      } 
    }

    return false;
  }

  public void setSearchFile(String file) {
    this.searchFile = file;
  }

  public String getSearchType() {
    if (searchFile.contains("/")) {
      return "path";
    } else {
      return "filename";
    }
  }

  public int getSerialNum() {
    return serialNum;
  }

  public void setSerialNum(int num) {
    resSerialNum = num;
  }

  public void addResponse(String[] resNode) {
    boolean found = false;
    for (String[] node : responses) {
      if (node[0].equals(resNode[0])) {
          found = true;
          break;
      }
    }

    if (!found) {
      responses.add(resNode);
    }
  }

  public List<String[]> getResponses() {
    return responses;
  }
}