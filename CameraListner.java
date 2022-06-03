import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CameraListner implements Runnable{
	
	public int port = 63001;
	public String multicastIp;
	public RIPTable table;
	public int cameraId;
	public HashMap<InetAddress, LocalTime> routerUpdates;
	
	/*
	 * initialize camera listener class
	 */
	public CameraListner(int port, String multicastIp, int cameraId, RIPTable table, HashMap<InetAddress, LocalTime> routerUpdates) {
		this.port = port;
		this.multicastIp = multicastIp;
		this.table = table;
		this.cameraId = cameraId;
		this.routerUpdates = routerUpdates;
	}
	
	/*
	 * convert byte array to integer
	 */
	public static int convertBytesToInt(byte[] bytes) {
		int result = 0;
		int size = bytes.length;
		for (int i=0; i<size; i++)
			result = result | ((bytes[i]&0xFF)<<(8*(size-1-i)));
		return result;
	}
	
	/*
	 * read the rip packet from the neighbour.
	 */
	public  ArrayList<Entry> readPacket(byte[] packet) throws UnknownHostException {
		
		// store all entries of neighbours routing table in an array list.
		ArrayList<Entry> entries = new ArrayList<Entry>();
		int numpackets = (packet.length - 4)/20;
		int counter = 4;
		for (int i=0; i<numpackets; i++) {
			byte[] entry = new byte[20];
			for (int j=counter, k=0; j<counter+20; j++, k++)
				entry[k] = packet[j];
			byte[] id = {entry[2], entry[3]};
			byte[] ipaddress = {entry[4], entry[5], entry[6], entry[7]};
			byte[] subnetmask = {entry[8], entry[9], entry[10], entry[11]};
			byte[] nextHop = {entry[12], entry[13], entry[14], entry[15]};
			byte[] metric = {entry[16], entry[17], entry[18], entry[19]};
			Entry oneEntry = new Entry(convertBytesToInt(id), InetAddress.getByAddress(ipaddress), 
					InetAddress.getByAddress(subnetmask), InetAddress.getByAddress(nextHop), convertBytesToInt(metric));
			entries.add(oneEntry);
			counter += 20;
		}
		return entries;
	}
	
	/*
	 * get sending processes ip address. Since this is a local implementation this is done manually.
	 */
	public static InetAddress getAddress(String id) throws UnknownHostException {
		String ip = "10.0."+id+".0";
		return InetAddress.getByName(ip);	
	}
	
	/*
	 * check the status of the update table.
	 */
	public void checkUpdates() throws UnknownHostException {
		ArrayList<InetAddress> toremove = new ArrayList<>();
		for (Map.Entry<InetAddress, LocalTime> element : routerUpdates.entrySet()) {
			LocalTime te = LocalTime.now();
			
			// change the cost to 16 if no update received from the neighbour for 5 seconds.
			if (LocalTime.now().compareTo(element.getValue().plusSeconds(5)) > 0) {
				table.updateMetric(element.getKey(), 16);
				table.printTable();
			}
			// delete the entry from the routing table if the neighbour is unreachable and no update was given for 10 seconds.
			if (LocalTime.now().compareTo(element.getValue().plusSeconds(10)) > 0) {
				table.deleteEntry(element.getKey());
				toremove.add(element.getKey());
				table.printTable();
			}
		}
		for(int i=0; i<toremove.size(); i++)
			routerUpdates.remove(toremove.get(i));
	}
	
	/*
	 * create a socket to receive packets from the neighbours.
	 */
	public void receivePacket() throws IOException {
		MulticastSocket socket = new MulticastSocket(port);
		InetAddress ip = InetAddress.getByName(multicastIp);
		InetSocketAddress group = new InetSocketAddress(ip, port);
		socket.joinGroup(group, NetworkInterface.getByInetAddress(ip));
		
		while(true) {
			try {
				byte[] buffer = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				
				socket.receive(packet);
				byte[] receivedpacket = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), packet.getOffset(), receivedpacket, 0, packet.getLength());
				ArrayList<Entry> entries = readPacket(receivedpacket);
				
				// get all the destination addresses from the neghbours routing table.
				InetAddress[] destAddresses = new InetAddress[table.table.size()];
				for (int i=0; i<destAddresses.length; i++)
					destAddresses[i] = InetAddress.getByAddress(table.table.get(i).ipaddress);
					
				boolean flag = false;
				InetAddress camip = InetAddress.getByName("127.0.0.1"); // this needs to be changes if not implemented on local machine
				if (convertBytesToInt(entries.get(0).routeTag) != -1)
					camip = getAddress(String.valueOf(convertBytesToInt(entries.get(0).routeTag)));
					routerUpdates.put(camip, LocalTime.now()); // update sending processes time.
				checkUpdates();
				
				for (int i=0; i<entries.size(); i++) {
					InetAddress destIp = InetAddress.getByAddress(entries.get(i).ipaddress);
					InetAddress subnet_mask = InetAddress.getByAddress(entries.get(i).subnet_mask);
					InetAddress nexthop = InetAddress.getByName("127.0.0.1");
					if (convertBytesToInt(entries.get(i).routeTag) != -1)
						nexthop = getAddress(String.valueOf(convertBytesToInt(entries.get(i).routeTag)));
					int metric = convertBytesToInt(entries.get(i).metric);
					// if entry not in routing table, add it.
					if (!Arrays.asList(destAddresses).contains(destIp)) {
						table.addEntry(cameraId, destIp, subnet_mask, nexthop, metric+1);
						flag = true;
					} else if (Arrays.asList(destAddresses).contains(destIp)) {
						// if entry already present, check for any cost change
						int index = Arrays.asList(destAddresses).indexOf(destIp);
						InetAddress next = InetAddress.getByAddress(table.table.get(index).nexthop);
						int cost = convertBytesToInt(table.table.get(index).metric);
						if(next == nexthop) {
							if (cost < metric+1 || cost > metric+1) {
								table.updateEntry(index, metric+1, next);
								flag = true;
							}
						}
						if ((cost==16 && metric+1<16)) {
							table.updateEntry(index, metric+1, nexthop);
							flag = true;
						}
					}
					
				}
				// if any changes to the routing table than print table.
				if (flag) {
					table.printTable();
					flag = false;
				}
				
				
				
			} catch(IOException ex) {
//				ex.printStackTrace();
			}
			if(false) break;
		}
		socket.leaveGroup(group, NetworkInterface.getByInetAddress(ip));
		socket.close();
		
		
	}
	
	@Override
	public void run() {
		try {
			receivePacket();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
