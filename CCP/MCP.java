package CCP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import org.json.JSONObject;

public class MCP {

    private InetAddress ccpAddress;
    private int ccpPort;
    private DatagramSocket mcpSocket;
    private int sequenceNumber = new Random().nextInt(29001) + 1000; // Sequence number between 1000 and 30000

    public MCP(String ccpIp, int ccpPort) throws SocketException, UnknownHostException {
        this.ccpAddress = InetAddress.getByName(ccpIp);
        this.ccpPort = ccpPort;
        this.mcpSocket = new DatagramSocket(2000); // MCP listens on port 2000
    }

    // Send initiation response to CCP initiation request
    public void sendInitiationAck(String ccpId) throws IOException {
        JSONObject message = new JSONObject();
        message.put("client_type", "MCP");
        message.put("message", "AKIN");
        message.put("client_id", ccpId);
        message.put("sequence_number", sequenceNumber++);

        sendMessage(message);
    }

    // Send status request (heartbeat) to CCP
    public void sendStatusRequest(String ccpId) throws IOException {
        JSONObject message = new JSONObject();
        message.put("client_type", "MCP");
        message.put("message", "STRQ");
        message.put("client_id", ccpId);
        message.put("sequence_number", sequenceNumber++);

        sendMessage(message);
    }

    // Generic method to send an EXEC command to CCP
    private void sendCommand(String action, String ccpId) throws IOException {
        JSONObject message = new JSONObject();
        message.put("client_type", "MCP");
        message.put("message", "EXEC");
        message.put("client_id", ccpId);
        message.put("sequence_number", sequenceNumber++);
        message.put("action", action);

        sendMessage(message);
    }

    // Command methods to cover CCP functionalities
    public void sendStopAndCloseCommand(String ccpId) throws IOException {
        sendCommand("STOPC", ccpId);
    }

    public void sendStopAndOpenCommand(String ccpId) throws IOException {
        sendCommand("STOPO", ccpId);
    }

    public void sendMoveForwardSlowCommand(String ccpId) throws IOException {
        sendCommand("FSLOWC", ccpId);
    }

    public void sendMoveForwardFastCommand(String ccpId) throws IOException {
        sendCommand("FFASTC", ccpId);
    }

    public void sendMoveBackwardSlowCommand(String ccpId) throws IOException {
        sendCommand("RSLOWC", ccpId);
    }

    public void sendDisconnectCommand(String ccpId) throws IOException {
        sendCommand("DISCONNECT", ccpId);
    }

    public void sendHazardCommand(String ccpId) throws IOException {
        sendCommand("HAZARD_DETECTED", ccpId);
    }

    // Send specific commands for LED and IR controls
    public void sendFlashLedCommand(String ccpId) throws IOException {
        sendCommand("FLASH_LED", ccpId);
    }

    public void sendIRLedOnCommand(String ccpId) throws IOException {
        JSONObject data = new JSONObject();
        data.put("status", "ON");
        sendCommandWithData("IRLD", ccpId, data);
    }

    public void sendIRLedOffCommand(String ccpId) throws IOException {
        JSONObject data = new JSONObject();
        data.put("status", "OFF");
        sendCommandWithData("IRLD", ccpId, data);
    }

    // Command with additional data
    private void sendCommandWithData(String action, String ccpId, JSONObject additionalData) throws IOException {
        JSONObject message = new JSONObject();
        message.put("client_type", "MCP");
        message.put("message", "EXEC");
        message.put("client_id", ccpId);
        message.put("sequence_number", sequenceNumber++);
        message.put("action", action);

        if (additionalData != null) {
            for (String key : additionalData.keySet()) {
                message.put(key, additionalData.get(key));
            }
        }

        sendMessage(message);
    }

    // Send status acknowledgment for CCP status updates
    public void sendStatusAck(String ccpId) throws IOException {
        JSONObject message = new JSONObject();
        message.put("client_type", "MCP");
        message.put("message", "AKST");
        message.put("client_id", ccpId);
        message.put("sequence_number", sequenceNumber++);

        sendMessage(message);
    }

    // Generic method to send messages
    private void sendMessage(JSONObject message) throws IOException {
        byte[] buffer = message.toString().getBytes("UTF-8");
        DatagramPacket packet = new DatagramPacket(
                buffer, buffer.length, ccpAddress, ccpPort);
        mcpSocket.send(packet);
        System.out.println("Sent message: " + message.toString());
    }

    // Receive incoming messages from CCP
    public void listenForMessages() {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                mcpSocket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received message: " + received);

                JSONObject message = new JSONObject(received);
                processReceivedMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Process received message based on its type
    private void processReceivedMessage(JSONObject message) throws IOException {
        String msgType = message.optString("message", null);
        String ccpId = message.optString("client_id", null);

        if (msgType == null || ccpId == null) {
            System.out.println("Invalid message received.");
            return;
        }

        switch (msgType) {
            case "CCIN": // Initiation from CCP
                sendInitiationAck(ccpId);
                break;
            case "STAT": // CCP Status Update
                sendStatusAck(ccpId);
                break;
            case "FSLOWC":
            case "FFASTC":
            case "RSLOWC":
            case "STOPC":
            case "STOPO":
            case "FLASH_LED":
            case "IRLD":
            case "HAZARD_DETECTED":
            case "DISCONNECT":
                // Handle action messages if needed
                System.out.println("Received action status from CCP: " + msgType);
                // You can implement additional logic here if necessary
                break;
            default:
                System.out.println("Unknown message type received: " + msgType);
        }
    }

    // Main method for testing all MCP commands
    public static void main(String[] args) {
        try {
            String ccpIp = "10.20.30.153"; // CCP IP address
            int ccpPort = 3000;         // CCP Port
            String ccpId = "BR12";      // Blade Runner ID

            MCP mcp = new MCP(ccpIp, ccpPort);

            // Start listening for responses from CCP in a separate thread
            new Thread(() -> mcp.listenForMessages()).start();

            // Give the CCP some time to start and connect
            Thread.sleep(2000);

            // // Send all commands to test CCP functionalities
            // mcp.sendStatusRequest(ccpId);               // Heartbeat check
            // Thread.sleep(1000);

            // mcp.sendMoveForwardSlowCommand(ccpId);      // Move forward slowly
            // Thread.sleep(1000);

            mcp.sendMoveForwardFastCommand(ccpId);      // Move forward fast
            Thread.sleep(1000);

            // mcp.sendMoveBackwardSlowCommand(ccpId);     // Move backward slowly
            // Thread.sleep(1000);

            // mcp.sendStopAndCloseCommand(ccpId);         // Stop and close doors
            // Thread.sleep(1000);

            // mcp.sendStopAndOpenCommand(ccpId);          // Stop and open doors
            // Thread.sleep(1000);

            // mcp.sendFlashLedCommand(ccpId);             // Flash LED
            // Thread.sleep(1000);

            // mcp.sendIRLedOnCommand(ccpId);              // Turn IR LED on
            // Thread.sleep(1000);

            // mcp.sendIRLedOffCommand(ccpId);             // Turn IR LED off
            // Thread.sleep(1000);

            // mcp.sendHazardCommand(ccpId);               // Simulate hazard detected
            // Thread.sleep(1000);

            // mcp.sendDisconnectCommand(ccpId);           // Disconnect command

            // Keep the main thread alive to continue listening
            Thread.sleep(5000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
