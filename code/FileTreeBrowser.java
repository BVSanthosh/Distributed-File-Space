/**
 * Browses a file tree with a simple text based output and navigation.
 *
 * @author   <a href="https://saleem.host.cs.st-andrews.ac.uk/">Saleem Bhatti</a>
 * @version  1.4, 06 October 2022
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public final class FileTreeBrowser {

  // user commands
  final static String quit     = new String(":quit");
  final static String help     = new String(":help");
  final static String services = new String(":services");
  final static String up       = new String("..");
  final static String list     = new String(".");
  final static String nodes    = new String(":nodes");
  final static String search   = new String(":search");
  final static String download = new String(":download");

  final static String propertiesFile = "filetreebrowser.properties";
  static Configuration configuration;
  static String rootPath = "";

  File   thisDir;  // this directory
  String thisDirName;  // name of this directory
  SimpleDateFormat sdf;

  public static String timestamp() {
    SimpleDateFormat sdf = new SimpleDateFormat(new String("yyyyMMdd-HHmmss.SSS"));
    return sdf.format(new Date());
  }

  /**
   * @param args : no args required
   */
  public static void main(String[] args) {

    configuration = new Configuration(propertiesFile);
    rootPath = getPathName(new File(configuration.rootDir));

    InputStream keyboard = System.in;
    String userCmd = new String(list);
    boolean quitBrowser = false;

    FileTreeBrowser ftb = new FileTreeBrowser(configuration.rootDir);
    ftb.printList();

    while(!quitBrowser) {

      System.out.print("\n[filename | '" + list + "' | '" + up + "' | '" + services + "' | '" + nodes + "' | '" + search + "' | '" + download + "' | '" + quit + "' | '" + help + "'] ");

      // what does the user want to do?
      while((userCmd = ByteReader.readLine(keyboard)) == null) {
        try { Thread.sleep(configuration.sleepTime); }
        catch (InterruptedException e) { } // Thread.sleep() - do nothing
      }

      // blank
      if (userCmd.isBlank()) { continue; }

      // quit
      if (userCmd.equalsIgnoreCase(quit)) { quitBrowser = true; }

      // help message
      else
      if (userCmd.equalsIgnoreCase(help)) { displayHelp(); }

      // service info
      else
      if (userCmd.equalsIgnoreCase(services)) { displayServices(); }

      // list files
      else
      if (userCmd.equalsIgnoreCase(list)) { ftb.printList(); }

      // move up directory tree
      else
      if (userCmd.equalsIgnoreCase(up)) {
        // move up to parent directory ...
        // but not above the point where we started!
        if (ftb.thisDirName.equals(rootPath)) {
          System.out.println("At root : cannot move up.\n");
        }
        else {
          String parent = ftb.thisDir.getParent();
          System.out.println("<<< " + parent + "\n");
          ftb = new FileTreeBrowser(parent);
        }
      }

      //  list discovered servers
      else
      if (userCmd.equalsIgnoreCase(nodes)) { nodes(); }

      else
      if (userCmd.equalsIgnoreCase(search)) { search(); }

      // download
      else
      if (userCmd.equalsIgnoreCase(download)) { download(); }

      else { // do something with pathname

        File f = ftb.searchList(userCmd);

        if (f == null) {
          System.out.println("Unknown command or filename: '" + userCmd + "'");
        }

        // act upon entered filename
        else {

          String pathName = getPathName(f);

          if (f.isFile()) { // print some file details
            System.out.println("file: " + pathName);
            System.out.println("size: " + f.length());
          }

          else
          if (f.isDirectory()) { // move into to the directory
            System.out.println(">>> " + pathName);
            ftb = new FileTreeBrowser(pathName);
          }

        } // (f == null)

      } // do something

    } // while(!quit)

  } // main()


  /**
   * Create a new FileTreeBrowser.
   *
   * @param pathName the pathname (directory) at which to start.
   */
  public FileTreeBrowser(String pathName) {
    if (pathName == "") { pathName = configuration.rootDir; }
    else // "." -- this directory, re-list only
    if (pathName.equals(list)) { pathName = thisDirName; }
    thisDir = new File(pathName);
    thisDirName = getPathName(thisDir);
  }


  /**
   * Print help message.
   */
  static void displayHelp() {

    String[] lines = {
      "--* Welcome to the simple FileTreeBrowser. *--",
      "* The display consists of:",
      "\t- The name of the current directory",
      "\t- The list of files (the numbers for the files are of no",
      "\t  significance, but may help you with debugging).",
      "* Files that are directories have trailing '" + File.separator + "'.",
      "* Use text entry to navigate the directory tree.",
      "\t.\t\tTo refresh the view of the current directory.",
      "\t..\t\tTo move up a directory level.",
      "\tfilename\tTo list file details (if it is a file) or to",
      "\t\t\tmove into that directory (if it is a directory name).",
      "\t:services\tTo list the services offered.",
      "\t:nodes\t\tTo list the other nodes discovered.",
      "\t:search\t\tTo list the nodes that have the specified filename or path.",
      "\t:download\tTo download a file.",
      "\t:quit\t\tTo quit the program.",
      "\t:help\t\tTo print this message."
    };

    for(int i = 0; i < lines.length; ++i)
      System.out.println(lines[i]);

    return;
  }

  /**
   * Print config information.
   */
  static void displayServices() {

    String services = ":";
    services += "id=" + configuration.id + ":";
    services += "timestamp=" + timestamp() + ":";
    services += "search=" + configuration.search + ",";
    services += "download=" + configuration.download;
    services += ":";

    System.out.println(services);
  }

  static List<String[]> discoveredNodes = new ArrayList<String[]>();

  static void nodes() {
    MulticastGroup m = new MulticastGroup(configuration);
    NodeSearch senderNode = new NodeSearch(m, configuration, true);
    NodeSearch receiverNode = new NodeSearch(m, configuration, false);
    Thread senderThread = new Thread(senderNode);
    Thread receiverThread = new Thread(receiverNode);

    System.out.println("+++ Initiating discovery protocol...");

    m.join_group();

    senderThread.start();
    receiverThread.start();

    try {
      senderThread.join();
      receiverThread.join();
    } catch (InterruptedException e) {
        System.out.println("Main thread interrupted: " + e.getMessage());
    }

    m.leave_group();

    discoveredNodes = receiverNode.getDiscoveredNodes();

    if (discoveredNodes.size() == 0) {
      System.out.println("+++ No available nodes");
      return;
    }
  
    int num = 0;
    System.out.println("\n+++ " + discoveredNodes.size() + " node(s) discovered:");
    for (String[] node : discoveredNodes) {
      System.out.print(num + "   ");
      System.out.print("id: " + node[0] + ", ");
      System.out.print("port: " + node[4] + ", ");
      System.out.print("message type: " + node[3] + ", ");
      System.out.print("search: " + node[5]);
      System.out.println();
      num++;
    }
    System.out.println("\n+++");
  }

  static List<String[]> responses = new ArrayList<String[]>();

  static void search() { 
    Scanner scanner = new Scanner(System.in);
    List<String[]> availableSearch = new ArrayList<String[]>();
    List<String[]> possibleSearch = new ArrayList<String[]>();

    if (discoveredNodes.size() == 0) {
      System.out.println("+++ no nodes discovered yet");
      nodes();
    }

    if (discoveredNodes.size() == 0) {
      System.out.println("+++ search functionality unavailable");
      return;
    }

    for (String[] node : discoveredNodes) {
      if (!node[5].equals("search=none")) {
        possibleSearch.add(node);
      }
    }

    if (possibleSearch.size() == 0) {
      System.out.println("+++ search functionality unavailable \n+++ discovered node(s) don't support search.");
      return;
    }

    int num = 0;
    System.out.println("\n+++ Search capabilities: ");
    for (String[] node : possibleSearch) {
      System.out.print(num + "   ");
      System.out.print("id: " + node[0] + ", ");
      System.out.print("search: " + node[5]);
      System.out.println();
      num++;
    }
    System.out.println("\n+++");

    System.out.println("+++ Enter the filename or path to search \n+++ Otherwise press ENTER to recieve search requests");
    String searchFile = scanner.nextLine();

    if (searchFile.isEmpty()) {
      MulticastGroup m = new MulticastGroup(configuration);
      FileSpaceSearch sendResponse = new FileSpaceSearch(m, configuration, false);
      Thread sendResponseThread = new Thread(sendResponse);

      System.out.println("+++ Initiating search protocol");

      sendResponse.setSearchFile(searchFile);

      m.join_group();

      sendResponseThread.start();

      try {
        sendResponseThread.join();
      } catch (InterruptedException e) {
          System.out.println("Main thread interrupted: " + e.getMessage());
      }

      m.leave_group();

    } else {
      MulticastGroup m = new MulticastGroup(configuration);
      FileSpaceSearch sendRequest = new FileSpaceSearch(m, configuration, true);
      FileSpaceSearch sendResponse = new FileSpaceSearch(m, configuration, false);
      Thread sendRequestThread = new Thread(sendRequest);
      Thread sendResponseThread = new Thread(sendResponse);

      System.out.println("+++ Initiating search protocol");

      sendRequest.setSearchFile(searchFile);
      sendResponse.setSearchFile(searchFile);

      m.join_group();

      sendRequestThread.start();

      int serialNum = sendRequest.getSerialNum();
      sendResponse.setSerialNum(serialNum);

      sendResponseThread.start();

      try {
        sendRequestThread.join();
        sendResponseThread.join();
      } catch (InterruptedException e) {
          System.out.println("Main thread interrupted: " + e.getMessage());
      }

      m.leave_group();

      responses = sendResponse.getResponses();

      for (String[] response : responses) {
        if ("search-result".equals(response[3])) {
          availableSearch.add(response);
        }
      }

      if (availableSearch.size() == 0) {
        System.out.println("+++ file not found");
        return;
      }

      num = 0;
      System.out.println("\n+++ response(s):");
      for (String[] response : responses) {
        System.out.print(num + "   ");
        System.out.print("id: " + response[0] + ", ");
        System.out.print("message type: " + response[3] + ", ");
        if("search-result".equals(response[3])) {System.out.print("search string: " + response[6]);}
        System.out.println();
        num++;
      }
      System.out.println("\n+++");
    }
  }

  static void download() { 
    Scanner scanner = new Scanner(System.in);
    List<String[]> availableDownloads = new ArrayList<String[]>();

    System.out.println("+++ Choose the functionality to perform: \n0: Initiate server for file upload \n1: Inititate client for file download");
    String option = scanner.nextLine();

    if (option.equals("0")) {
      ServerDownload server = new ServerDownload(configuration.mPort);
      server.uploadFile();

    } else if (option.equals("1")) {
      if (responses.size() == 0) {
        System.out.println("+++ Use :search to search for the file to download first");
        return;
      }

      for (String[] response : responses) {
        if ("search-result".equals(response[3])) {
          availableDownloads.add(response);
        }
      }

      if (availableDownloads.size() == 0) {
        System.out.println("+++ Specified file unavailable for download");
        return;
      }

      int num = 0;
      System.out.println("\n+++ available downloads: ");
      for (String[] availableDownload : availableDownloads) {
        System.out.print(num + "   ");
        System.out.print("id: " + availableDownload[0] + ", ");
        System.out.print("search string: " + availableDownload[6]);
        System.out.println();
        num++;
      }
      System.out.println("\n+++");

      System.out.println("+++ Choose a node to download from \nEnter the node number ");
      String nodeNum = scanner.nextLine();
      int index = Integer.valueOf(nodeNum);

      if (!(index >= 0 && index < availableDownloads.size())) {
        System.out.println("+++ Invalid input");
        return;
      }

      System.out.println("+++ Enter the path to download to");
      String downloadPath = scanner.nextLine();

      String[] node = availableDownloads.get(index);
      String hostname = getServerIp(node);
      int port = getServerport(node);
      InetAddress serverAddr = null;
      String filename = "";

      try {
          serverAddr = InetAddress.getByName(hostname);
          String ipAddress = serverAddr.getHostAddress();
          System.out.println("+++ hostname: " + hostname);
          System.out.println("+++ IP address: " + ipAddress);
          System.out.println("+++ port number: " + port);
      } catch (UnknownHostException e) {
          e.printStackTrace();
      }

      if (node[6].contains("/")) {
        String[] parts = node[6].split("/");
        filename = parts[parts.length - 1];
      }
      else{
        filename = node[6];
      }

      ClientDownload client = new ClientDownload(serverAddr, port, filename, downloadPath);
      client.downloadFile();
    } else {
      System.out.println("+++ Invalid input");
    }
  }

  static String getServerIp(String[] node) {
    String id = node[0];
    String[] parts = id.split("@");
    return parts[1];
  }

  static int getServerport(String[] node) {
    String id = node[0];
    try{
      for (String[] dNode : discoveredNodes) {
        if (id.equals(dNode[0])) {
          return Integer.valueOf(dNode[4]);
        }
      }
    } catch (NumberFormatException e) {
      System.out.println("Server port unavailable: " + e.getMessage());
    }

    return 0;
  }

  /**
   * List the names of all the files in this directory.
   */
  public void printList() {

    File[] fileList = thisDir.listFiles();

    System.out.println("\n+++  id: " + configuration.id);
    System.out.println("+++ dir: " + getPathName(thisDir));
    System.out.println("+++\tfilename:");
    for(int i = 0; i < fileList.length; ++i) {

      File f = fileList[i];
      String name = f.getName();
      if (f.isDirectory()) // add a trailing separator to dir names
          name = name + File.separator;
      System.out.println(i + "\t" + name);
    }
    System.out.println("+++");
  }

  String getParent() { return thisDir.getParent(); }

  /**
   * Search for a name in the list of files in this directory.
   *
   * @param name the name of the file to search for.
   */
  public File searchList(String name) {

    File found = null;

    File[] fileList = thisDir.listFiles();
    for(int i = 0; i < fileList.length; ++i) {

      if (name.equals(fileList[i].getName())) {
        found = fileList[i];
        break;
      }
    }

    return found;
  }

  /**
   * Get full pathname.
   *
   * @param f the File for which the pathname is required.
   */
  static public String getPathName(File f) {

    String pathName = null;

    try {
      pathName = f.getCanonicalPath();
    }
    catch (IOException e) {
      System.out.println("+++ FileTreeBrowser.pathname(): " + e.getMessage());
    }

    return pathName;
  }
}