import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RIPPacket {
	
	public static byte[] command;
	public static byte[] version;
	public static byte[] mustbezero;
	public static byte[][] ripEntries;
	public static byte[] packet;
	
	/*
	 * convert integer to byte array
	 */
	public static byte[] convertIntToByte(int value, int size) {
		byte[] bytes = new byte[size];
		for (int i=0; i<size; i++)
			bytes[i] = (byte)(value>>(8*(size-1-i)));
		
		return bytes;
	}
	
	/*
	 * store the entry in one single byte array of length 20.
	 */
	public static byte[] convertToBytes(Entry entry) {
		byte[] byteEntry = new byte[20];
		System.arraycopy(entry.address_family_id, 0, byteEntry, 0, 2);
		System.arraycopy(entry.routeTag, 0, byteEntry, 2, 2);
		System.arraycopy(entry.ipaddress, 0, byteEntry, 4, 4);
		System.arraycopy(entry.subnet_mask, 0, byteEntry, 8, 4);
		System.arraycopy(entry.nexthop, 0, byteEntry, 12, 4);
		System.arraycopy(entry.metric, 0, byteEntry, 16, 4);
		
		return byteEntry;
		
	}
	
	/*
	 * initialize rip packet class
	 */
	RIPPacket(int command, ArrayList<Entry> entries) {
		this.command = convertIntToByte(command, 1);
		this.version = convertIntToByte(2, 1);
		this.mustbezero = convertIntToByte(0, 2);
		this.ripEntries = new byte[entries.size()][20];
		
		// store all rip entries in a 2D byte array
		for (int i=0; i<entries.size(); i++) {
			ripEntries[i] = convertToBytes(entries.get(i));
		}
	}
	
	/*
	 * create a rip packet
	 */
	public static void creatPacket() {
		packet = new byte[4+20*(ripEntries.length)];
		packet[0] = command[0];
		packet[1] = version[0];
		packet[2] = mustbezero[0];
		packet[3] = mustbezero[1];
		int counter = 4;
		for (int i=0; i<ripEntries.length; i++) {
			for (int j=counter, k=0; j<20+counter; j++, k++)
				packet[j] = ripEntries[i][k];
			counter += 20;
		}
	}
	
	

}
