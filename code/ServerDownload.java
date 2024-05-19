import java.io.*;
import java.net.*;

/*
 * Implements R3) Download
 * Node to act as a server when transferring a file to a client
 */

public class ServerDownload {
    private final int port;
    private final String directory = "root_dir";
    private byte[] contents;
    
    public ServerDownload(int port) {
        this.port = port;
    }

    public void uploadFile() {
        try (ServerSocket ssocket = new ServerSocket(port);) {
            System.out.println("Server is listening on port " + port + "...");

            try (Socket socket = ssocket.accept()) {
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                System.out.println("Client connected");

                String filename = br.readLine();
                File dir = new File(directory);
                String path = getAbsPath(dir, filename);

                if (path == null) {
                    throw new FileNotFoundException("File does not exist: " + filename);
                }

                File file = new File(path);
                contents = new byte[(int) file.length()];

                System.out.println("transferring: " + file);
                System.out.println("size: " + file.length() + " bytes");       
            
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);

                bis.read(contents, 0, (int) file.length());
                os.write(contents, 0, (int) file.length());

                System.out.println("File has been sent");

                os.flush();
            } catch (IOException e) {
                System.err.println("Server exception: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Cannot start server: " + e.getMessage());
        }
    }

    public String getAbsPath(File dir, String filename) {
        File[] fileList = dir.listFiles();
        String path = null;
    
        for (File file : fileList) {
          if (file.isDirectory()) {
            path = getAbsPath(file, filename);
            if (path != null) {
                return path;
            }
          } else if (file.getName().equals(filename)) {
            try {
                return file.getCanonicalPath();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
          }
        }
    
        return path;
    }
}