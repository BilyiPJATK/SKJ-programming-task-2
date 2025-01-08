import java.io.*;
import java.net.Socket;

public class ArithmeticClient {
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

        try (Socket socket = new Socket("localhost", port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to server on port: " + port);

            String input;
            while (true) {
                System.out.print("Enter request (e.g., ADD 5 3) or type 'exit' to quit: ");
                input = console.readLine();

                if ("exit".equalsIgnoreCase(input)) {
                    System.out.println("Exiting...");
                    break;
                }

                out.println(input); // Send request to the server
                String response = in.readLine(); // Receive response from the server

                System.out.println("Server response: " + response);
            }
        } catch (IOException e) {
            System.err.println("Error: Unable to connect to server. " + e.getMessage());
        }
    }
}
