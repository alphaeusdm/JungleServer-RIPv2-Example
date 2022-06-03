import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.HashMap;

public class JungleClient implements Runnable{
	
	public String message = "Are you Live";
	InetAddress destinationIp;
	int destinationPort;
	int sourcePort;
	RIPTable table;
	public HashMap<InetAddress, LocalTime> routerUpdates;
	boolean flag = false;
	
	/*
	 * initialize client process to communicate with jungle cloud
	 */
	public JungleClient(InetAddress destinationIp, int destinationPort, int sourcePort, RIPTable table, HashMap<InetAddress, LocalTime> routerUpdates) {
		this.destinationIp = destinationIp;
		this.destinationPort = destinationPort;
		this.sourcePort = sourcePort;
		this.table = table;
		this.routerUpdates = routerUpdates;
	}
	
	/*
	 * create socket and check if jungle cloud is reachable;
	 */
	public void repeater() {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(sourcePort);
			byte[] senddata = message.getBytes();
			DatagramPacket sendpacket = new DatagramPacket(senddata, senddata.length, destinationIp, destinationPort);
			socket.send(sendpacket);
			
			socket.setSoTimeout(1000);
			
			byte[] receivedata = new byte[1024];
			DatagramPacket receivepacket = new DatagramPacket(receivedata, receivedata.length);
			socket.receive(receivepacket);
			
			// add entry to the routing table
			if (!flag) {
				table.addEntry(-1, destinationIp, InetAddress.getByName("255.255.255.0"), destinationIp, 1);
				table.printTable();
				flag = true;
			}
			routerUpdates.put(destinationIp, LocalTime.now()); // update neighbours update time.
			
		} catch (SocketTimeoutException e) {
			try {
				table.updateMetric(destinationIp, 16); // if unreachable, update cost to 16
				flag = false;
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (socket != null)
				socket.close();
		}
	}
	
	@Override
	public void run() {
		while(true) {
			repeater();
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
