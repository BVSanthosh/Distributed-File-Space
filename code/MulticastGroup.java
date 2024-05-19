import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;

/*
 * Implements IPv6 multicast
 * code adopted from the Beacon example code from week 5
 */

public class MulticastGroup {
    MulticastSocket mSocket;
    InetAddress mInetAddr;
    InetSocketAddress mGroup;
    Configuration config;
    NetworkInterface nif = null;
    InetAddress ipv6 = null;

    MulticastGroup(Configuration config){
        this.config = config;

        try{
            mSocket = new MulticastSocket(config.mPort);
            mSocket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, config.loopback);
            mSocket.setReuseAddress(config.reuseAddr); 
            mSocket.setTimeToLive(config.mTTL); 
            mSocket.setSoTimeout(config.soTimeout);
            ipv6 = InetAddress.getLocalHost();
            nif = NetworkInterface.getByInetAddress(ipv6);

            mInetAddr = InetAddress.getByName(config.mAddr6);
            mGroup = new InetSocketAddress(mInetAddr, config.mPort);


        } catch (SocketException e) {
            System.out.println("MulticastEndpoint(): " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Problem: " + e.getMessage());
        }
    }

    public void join_group(){
        try {
            if (mGroup != null) {
                mSocket.joinGroup(mGroup, nif);
                config.log.writeLog("joined IPv6 multicast group " + mGroup.toString(), true);
            }
        } catch (IOException e) {
            System.out.println("MulticastEndpoint.join(): " + e.getMessage());
        }
    }

    public boolean send_to_group(byte[] b){
        DatagramPacket data;
        boolean sent = false;

        try {
            data = new DatagramPacket(b, b.length, mGroup);
            mSocket.send(data);
            sent = true;
        } catch (SocketTimeoutException e) {
            System.out.println("MulticastEndpoint.tx(): timeout on send - " + e.getMessage());
        } catch (SocketException e) {
            System.out.println("MulticastEndpoint.tx(): " + e.getMessage());
        } catch (IOException e) {
            System.out.println("MulticastEndpoint.tx(): " + e.getMessage());
        }

        return sent;
    }

    public void receive_from_group(byte[] b){
        try {
            DatagramPacket data = new DatagramPacket(b, b.length);
            mSocket.receive(data);
        } catch (SocketTimeoutException e) {
            System.out.println("MulticastEndpoint.rx(): timeout on receive - " + e.getMessage());
        } catch (SocketException e) {
            System.out.println("MulticastEndpoint.rx(): " + e.getMessage());
        } catch (IOException e) {
            System.out.println("MulticastEndpoint.rx(): " + e.getMessage());
        }
    }

    public void leave_group(){
        try {
            if (mGroup != null) {
              mSocket.leaveGroup(mGroup, nif);
              config.log.writeLog("left IPv6 multicast group", true);
            }
            mSocket.close();
        } catch (IOException e) {
            System.out.println("MulticastEndpoint.leave(): " + e.getMessage());
        }
    }
}
