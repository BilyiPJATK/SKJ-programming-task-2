import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CCS {
    private static final String DISCOVER_MESSAGE = "CCS DISCOVER";
    private static final String RESPONSE_MESSAGE = "CCS FOUND";

    // Shared statistics object
    private static final Statistics stats = new Statistics();

    private static ServerSocket serverSocket = null;
    private static DatagramSocket udpSocket = null;
    private static ExecutorService executor = null;
    private static volatile boolean isShuttingDown = false;

    public static void main(String[] args) {
        // Validate port argument as before
        if (args.length != 1) {
            System.err.println("Error: Please provide exactly one argument specifying the port number.");
            System.exit(1);
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
            if (port < 1024 || port > 65535) {
                throw new IllegalArgumentException("Port number must be between 1024 and 65535.");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Register shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down server...");
                cleanup(); // Ensure cleanup is called properly
                System.out.println("Server shutdown complete.");
                System.exit(0); // Only exit if everything is cleaned up correctly
            } catch (Exception e) {
                System.err.println("Error during shutdown: " + e.getMessage());
                System.exit(1); // Exit with error code if there’s a problem during cleanup
            }
        }));

        // Start UDP and TCP services
        executor = Executors.newCachedThreadPool();
        executor.submit(() -> startUDPService(port));
        executor.submit(CCS::startStatisticsThread); // Start statistics thread
        startTCPService(port);
    }

    private static void startUDPService(int port) {
        try (DatagramSocket udpSocket = new DatagramSocket(port)) {
            CCS.udpSocket = udpSocket;
            System.out.println("UDP Service Discovery started on port: " + port);

            byte[] buffer = new byte[1024];
            while (!isShuttingDown) {
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                System.out.println("Received UDP message: " + message);

                if (DISCOVER_MESSAGE.equals(message)) {
                    InetAddress clientAddress = receivePacket.getAddress();
                    int clientPort = receivePacket.getPort();

                    byte[] responseBytes = RESPONSE_MESSAGE.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddress, clientPort);
                    udpSocket.send(responsePacket);

                    System.out.println("Responded to discovery message from " + clientAddress + ":" + clientPort);
                } else {
                    System.out.println("Ignored invalid UDP message: " + message);
                }
            }
        } catch (Exception e) {
            if (!isShuttingDown) {
                System.err.println("Error in UDP Service: " + e.getMessage());
            }
        }
    }

    private static void startTCPService(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            CCS.serverSocket = serverSocket;
            System.out.println("TCP Server started on port: " + port);

            while (!isShuttingDown) {
                Socket clientSocket = serverSocket.accept();
                stats.incrementClientCount(); // Update statistics for a new client
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                executor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (!isShuttingDown) {
                System.err.println("Error in TCP Service: " + e.getMessage());
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String request;
            while ((request = in.readLine()) != null && !isShuttingDown) {
                stats.incrementRequestCount(); // Update total request count
                System.out.println("Received request: " + request);

                // Parse and process the request
                String[] parts = request.split(" ");
                if (parts.length != 3) {
                    out.println("ERROR");
                    stats.incrementInvalidCount(); // Update invalid request count
                    System.out.println("Invalid request format");
                    continue;
                }

                String operation = parts[0];
                int arg1, arg2;
                try {
                    arg1 = Integer.parseInt(parts[1]);
                    arg2 = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    out.println("ERROR");
                    stats.incrementInvalidCount(); // Update invalid request count
                    System.out.println("Invalid arguments: " + request);
                    continue;
                }

                int result;
                switch (operation) {
                    case "ADD":
                        result = arg1 + arg2;
                        stats.incrementOperationCount("ADD");
                        break;
                    case "SUB":
                        result = arg1 - arg2;
                        stats.incrementOperationCount("SUB");
                        break;
                    case "MUL":
                        result = arg1 * arg2;
                        stats.incrementOperationCount("MUL");
                        break;
                    case "DIV":
                        if (arg2 == 0) {
                            out.println("ERROR");
                            stats.incrementInvalidCount(); // Update invalid request count
                            System.out.println("Division by zero: " + request);
                            continue;
                        }
                        result = arg1 / arg2;
                        stats.incrementOperationCount("DIV");
                        break;
                    default:
                        out.println("ERROR");
                        stats.incrementInvalidCount(); // Update invalid request count
                        System.out.println("Invalid operation: " + operation);
                        continue;
                }

                out.println(result);
                stats.addResultSum(result); // Update result sum
                System.out.println("Response sent: " + result);
            }
        } catch (IOException e) {
            if (!isShuttingDown) {
                System.err.println("Client communication error: " + e.getMessage());
            }
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client disconnected.");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private static void startStatisticsThread() {
        while (!isShuttingDown) {
            try {
                Thread.sleep(10_000); // Wait 10 seconds
                stats.printStatistics();
            } catch (InterruptedException e) {
                if (!isShuttingDown) {
                    System.err.println("Statistics thread interrupted: " + e.getMessage());
                }
            }
        }
    }

    // Cleanup resources: close sockets and shutdown executor
    private static void cleanup() {
        isShuttingDown = true;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("ServerSocket closed.");
            }
        } catch (IOException e) {
            System.err.println("Error closing ServerSocket: " + e.getMessage());
        }

        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
            System.out.println("DatagramSocket closed.");
        }

        try {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate in the specified time.");
                    executor.shutdownNow(); // Force shutdown if it exceeds the time
                }
                System.out.println("ExecutorService shutdown.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt(); // Preserve the interrupt status
        }
    }
}
