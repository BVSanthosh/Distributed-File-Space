import java.io.*;
import java.net.*;

/*
 * Implements R3) Download
 * Node to act as a client when receiving a file from a server
 */

public class ClientDownload {
    private final InetAddress server_addr;
    private final int server_port;
    private final String file;
    private final String path;
    private byte[] contents = new byte[1024];
    
    public ClientDownload(InetAddress server_addr, int server_port, String file, String path) {
        this.server_addr = server_addr;
        this.server_port = server_port;
        this.file = file;
        this.path = path;
    }

    public void downloadFile() {
        try (Socket socket = new Socket(server_addr, server_port)) {
            File parentDir = new File(path);
            File downloadFile = new File(parentDir, "downloaded_" + file);
            InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
            FileOutputStream fos = new FileOutputStream(downloadFile);
            PrintWriter w = new PrintWriter(os, true);

            System.out.println("Connected to server");

            w.println(file);

            int count = 0;
            while ((count = is.read(contents)) != -1) {
                fos.write(contents, 0, count);
            }

            System.out.println("File downloaded successfully");

            fos.close();
        } catch (IOException ex) {
            System.err.println("Client error: " + ex.getMessage());
        }
    }
}