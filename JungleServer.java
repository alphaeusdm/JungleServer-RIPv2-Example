import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class JungleServer {
	
	public static void main(String args[]) {
		if (args.length > 0) {
			int port = Integer.parseInt(args[0]);
			new JungleServer().run(port);
		} else {
			System.out.println("Enter Jungle Server Port.....");
		}
	}
	
	/*
	 * create and run repeater socket
	 */
	public void run(int port) {
		DatagramSocket server = null;
		try {
			System.out.println("Starting Jungle Server......");
			server = new DatagramSocket(port);
			
			
			byte[] receivedata = new byte[1024];
			DatagramPacket packet = new DatagramPacket(receivedata, receivedata.length);
			while(true) {
				server.receive(packet);
				byte[] data = packet.getData();
				
				DatagramPacket sendPacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
				server.send(sendPacket);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (server != null)
				server.close();
		}
		
	}

}
