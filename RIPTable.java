import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.ArrayList;

class Entry {
	
	public final byte[] address_family_id;
	public final byte[] routeTag;
	public byte[] ipaddress;
	public byte[] subnet_mask;
	public byte[] nexthop;
	public byte[] metric;
	
	/*
	 * Convert integer to byte array
	 */
	public static byte[] convertIntToByte(int value, int size) {
		byte[] bytes = new byte[size];
		for (int i=0; i<size; i++)
			bytes[i] = (byte)(value>>(8*(size-1-i)));
		
		return bytes;
	}
	
	/*
	 * Initialize the Entry class. This class stores the RIP entries.
	 */
	Entry(int id, InetAddress ipaddress, InetAddress subnet_mask, InetAddress nexthop, int metric) {
		this.address_family_id = convertIntToByte(2, 2);
		this.routeTag = convertIntToByte(id,2);
		this.ipaddress = ipaddress.getAddress();
		this.subnet_mask = subnet_mask.getAddress();
		this.nexthop = nexthop.getAddress();
		this.metric = convertIntToByte(metric, 4);
	}
	
	
}

public class RIPTable {
	
	public static ArrayList<Entry> table = new ArrayList<Entry>(); //Initialize routing table.
	
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
	 * Convert Integer to byte array.
	 */
	public static byte[] convertIntToByte(int value, int size) {
		byte[] bytes = new byte[size];
		for (int i=0; i<size; i++)
			bytes[i] = (byte)(value>>(8*(size-1-i)));
		
		return bytes;
	}
	
	/*
	 * Implementation of CIDR.
	 */
	public static int cidr(InetAddress subnetMask) {
		
		byte[] subnet = subnetMask.getAddress();
		int cidr = 0;
		boolean flag = false;
		for(byte b : subnet) {
			int max = 0x80;
			for (int i=0; i<8; i++) {
				int temp = b & max;
				if (temp == 0) {
					flag=true;
				}else {
					cidr++;
				}
			}
			max>>>=1;
		}
		return cidr;
	}
	
	/*
	 * add an entry to the routing table.
	 */
	public static void addEntry(int id, InetAddress ipaddress, InetAddress subnet_mask, InetAddress nexthop, int metric) {
		Entry entry = new Entry(id, ipaddress, subnet_mask, nexthop, metric);
		table.add(entry);
	}
	
	/*
	 * update an entry of the routing table where the nextHop is unchanged but the cost increases or decreases.
	 */
	public static void updateEntry(int index, int newMetric, InetAddress newHop) {
		Entry entry = table.get(index);
		entry.metric = convertIntToByte(newMetric, 4);
		entry.nexthop = newHop.getAddress();
	}
	
	/*
	 * Update an entry where destination is unreachable
	 */
	public static void updateMetric(InetAddress address, int newMetric) throws UnknownHostException {
		// store all nextHop addresses in an array
		InetAddress[] nextAddresses = new InetAddress[table.size()];
		for (int i=0; i<nextAddresses.length; i++)
			nextAddresses[i] = InetAddress.getByAddress(table.get(i).nexthop);
		//	change the cost of the path to 16
		for (int j=0; j<table.size(); j++) {
			if (nextAddresses[j].equals(address)) {
				Entry entry = table.get(j);
				entry.metric = convertIntToByte(newMetric, 4);
			}
		}
		
	}
	
	/*
	 * delete an entry if no response is received for more than 10 seconds.
	 */
	public static void deleteEntry(InetAddress address) throws UnknownHostException {
		// store all nexthop addresses in an array
		InetAddress[] nextAddresses = new InetAddress[table.size()];
		for (int i=0; i<nextAddresses.length; i++)
			nextAddresses[i] = InetAddress.getByAddress(table.get(i).nexthop);
		
		// get index of all entries to be deleted.
		ArrayList<Integer> indices = new ArrayList<>();
		for (int j=0; j<table.size(); j++) {
			if (nextAddresses[j].equals(address)) {
				indices.add(j);
			}
		}
		System.out.println(indices.size());
		for (int k=0; k<indices.size(); k++) {
			System.out.println(indices.get(k));
			table.remove((int)indices.get(k));
		}
	}
	
	/*
	 * Print the routing table
	 */
	public static void printTable() throws UnknownHostException {
		System.out.println("\nDestination Address\t\tNext Hop\t\tCost");
		System.out.println("--------------------------------------------------------------");
		for (int i=0; i<table.size(); i++) {
			Entry entry = table.get(i);
			String destination_ip = InetAddress.getByAddress(entry.ipaddress).getHostAddress();
			String next_hop = InetAddress.getByAddress(entry.nexthop).getHostAddress();
			int cost = convertBytesToInt(entry.metric);
			String sub = String.valueOf(cidr(InetAddress.getByAddress(entry.subnet_mask))); // implement cidr support
			
			System.out.println(destination_ip+"\\"+sub+"\t\t\t"+next_hop+"\\"+sub+"\t\t"+cost);
		}
	}
	

}
