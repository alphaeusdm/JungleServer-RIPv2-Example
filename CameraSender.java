import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class CameraSender implements Runnable{
	public int port = 63001;
	public String multicastIp;
	public int cameraId;
	public RIPTable table;
	
	/*
	 * initialize camera sender class
	 */
	public CameraSender(int port, String multicastIp, int cameraId, RIPTable table) {
		this.port = port;
		this.multicastIp = multicastIp;
		this.cameraId = cameraId;
		this.table = table;
	}
	
	/*
	 * initialize the socket and send packets.
	 */
	public void sendPacket(int command) throws IOException {
		
		// initialize socket
		DatagramSocket socket = new DatagramSocket();
		InetAddress group = InetAddress.getByName(multicastIp);
		RIPPacket rippacket = new RIPPacket(command, table.table);
		rippacket.creatPacket(); // convert routing table entries to rip packet
		
		// send the packet via multicast.
		DatagramPacket packet = new DatagramPacket(rippacket.packet, rippacket.packet.length, group, port);
		socket.send(packet);
		socket.close();
	}
	
	/*
	 * run the multicast process every 5 seconds
	 */
	@Override
	public void run() {
		while(true) {
			try {
				sendPacket(2);
				Thread.sleep(5000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

}
