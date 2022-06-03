import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.HashMap;

public class Camera {
	
	/*
	 * Get ip address for the camera with the given id. This is done only for the program to be tested locally.
	 */
	public static InetAddress getCameraIp(String id) throws UnknownHostException {
		String ip = "10.0."+id+".0";
		return InetAddress.getByName(ip);
		
	}
	
	public static void main(String args[]) throws UnknownHostException, InterruptedException {
		// get id of the camera and port and ip of the jungle cloud.
		String id = args[0];
		String destip = args[1];
		String destport = args[2];
		
		// store the update times for the neighbouring routers.
		HashMap<InetAddress, LocalTime> routerUpdates = new HashMap<InetAddress, LocalTime>();
		InetAddress cameraip = getCameraIp(id);
		InetAddress subnet = InetAddress.getByName("255.255.255.0"); // subnet is kept constant.
		RIPTable cameraTable = new RIPTable();
		
		// add entry for the camera itself with the cost of 0.
		cameraTable.addEntry(Integer.parseInt(id), cameraip, subnet, cameraip, 0);
		cameraTable.printTable();
		
		try {
			// initialize the jungle cloud thread. This thread checks connection to jungle cloud.
			Thread jungleserver = new Thread(new JungleClient(InetAddress.getByName(destip), Integer.parseInt(destport), 4455, cameraTable, routerUpdates));
			jungleserver.start();
			
			// initialize the listener thread. This thread listens for the packets from its neighbours.
			Thread receiver = new Thread(new CameraListner(63001, "230.230.230.230", Integer.parseInt(id), cameraTable, routerUpdates));
			receiver.start();
			
			// initializethe sender thread. This thread sends packets to its neighbours via multicast.
			Thread sender = new Thread(new CameraSender(63001, "230.230.230.230", Integer.parseInt(id), cameraTable));
			sender.start();
			
			while(true) {Thread.sleep(1000);}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
