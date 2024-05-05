import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ServerTransceiver implements Runnable {
    // Shared list of connections
    public static ArrayList<ServerTransceiver> connections = new ArrayList<>();
    private Socket socket; //client socket
    private BufferedReader bufferedReader; // Used to read from the socket
    private BufferedWriter bufferedWriter; // Used to write to the socket
    private String clientUsername = "Unauthenticated";
    public static DataAccessObject dao = new DataAccessObject(); // Used to access the database

    public ServerTransceiver(Socket socket) {
        try  {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("ServerTransceiver IOException");
            closeEverything();
        }
    }

    // Write a message to every active connection managed by a ServerTransceiver
    public void broadcastMessage(String message) {
        for (ServerTransceiver connection: connections) {
            try {
                connection.flushMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Removes this particular connection from the shared list
    // of connections before letting all connections know who has left.
    public void removeConnection() {
        connections.remove(this);
        System.out.println("SERVER: " + clientUsername + " has left the chat!");
        System.out.println("Remaining Connections: " + printConnections());
        if (!this.clientUsername.equals("Unauthenticated")) {
            broadcastMessage("SERVER: " + clientUsername + " has left the chat!");
        }
    }

    // Removes this ServerTransceiver from the shared list of connections;
    // Shuts down the client socket, buffered writer, and buffered reader.
    public void closeEverything() {
        removeConnection();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void manageLogin() {
        try {
            String user = bufferedReader.readLine();
            String pass = bufferedReader.readLine();
            System.out.println("User:" + user);
            EventFlag validation = dao.validateUser(user, pass);
            if (validation.equals(EventFlag.VALID)) {
                acknowledgeCredentials(user);
            } else {
                // Just send the error message
                flushMessage(String.valueOf(validation.ordinal()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void manageAccountCreation() {
        try {
            String user = bufferedReader.readLine();
            String pass = bufferedReader.readLine();
            EventFlag validation = dao.createUser(user,pass);
            if (validation.equals(EventFlag.VALID)) {
                acknowledgeCredentials(user);
            } else {
                //Just send the error message.
                flushMessage(String.valueOf(validation.ordinal()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acknowledgeCredentials(String user) throws IOException {
        clientUsername = user;
        flushMessage(String.valueOf(EventFlag.VALID.ordinal()));
        connections.add(this);
        System.out.println(printConnections());
        broadcastMessage("SERVER: " + clientUsername + " had entered the chat!");
    }

    private void flushMessage(String message) throws IOException {
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    private static int parseStringToOrdinal(String stringToParse) {
        try {
            return Integer.parseInt(stringToParse);
        } catch(NumberFormatException e) {
            return 0; // 0 == INVALID
        }
    }

    private static String printConnections() {
        String output = "[";
        for (ServerTransceiver connection: connections) {
            output = output.concat(connection.clientUsername + ",");
        }
        output = output.substring(0,output.length() - 1).concat("]");
        return output;
    }

    @Override
    public void run() {
        String messageFromClient;
        while(!socket.isClosed()) {
            try { //BLOCKING OPERATION
                messageFromClient = bufferedReader.readLine();
                if (messageFromClient == null) {
                    socket.close();
                }
                EventFlag event = EventFlag.values()[parseStringToOrdinal(messageFromClient)];
                System.out.println("Received: " + messageFromClient);
                switch (event) {
                    case LOGIN -> manageLogin();
                    case CREATE_ACCOUNT -> manageAccountCreation();
                    case SHUTDOWN -> closeEverything();
                    default -> broadcastMessage(messageFromClient);
                }
            } catch (IOException e) {
                closeEverything();
                break;
            }
        }
    }
}
