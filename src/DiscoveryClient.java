import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryClient {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Error: Please provide the server's port number as an argument.");
            System.exit(1);
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Port number must be an integer.");
            System.exit(1);
            return;
        }

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            clientSocket.setBroadcast(true);

            String message = "CCS DISCOVER";
            byte[] sendData = message.getBytes();

            // Send discovery message
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddress, port);
            clientSocket.send(sendPacket);
            System.out.println("Sent discovery message: " + message);

            // Wait for response
            byte[] buffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            clientSocket.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            System.out.println("Received response: " + response);
        } catch (Exception e) {
            System.err.println("Error in client: " + e.getMessage());
        }
    }
}
