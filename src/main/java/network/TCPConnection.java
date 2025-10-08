package network;

import java.io.*;
import java.net.*;

public class TCPConnection {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private TCPConnectionListener listener;
    private boolean connected;
    private Thread listenerThread;

    // Constructor con Listener
    public TCPConnection(Socket socket, TCPConnectionListener listener) throws IOException {
        this.socket = socket;
        this.listener = listener;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.connected = true;

        startListening();
    }

    // Constructor para cliente que se conecta a servidor
    public TCPConnection(TCPConnectionListener listener, String ip, int port) throws IOException {
        this(new Socket(ip, port), listener);
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                // Notificar que la conexión está lista
                if (listener != null) {
                    listener.onConnectionReady(this);
                }

                String message;
                while (connected && (message = reader.readLine()) != null) {
                    if (listener != null) {
                        listener.onReceiveString(this, message);
                    }
                }
            } catch (IOException e) {
                if (connected && listener != null) {
                    listener.onException(this, e);
                }
            } finally {
                disconnect();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void sendMessage(String message) {
        if (connected && writer != null) {
            writer.println(message);
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }

            // Notificar desconexión
            if (listener != null) {
                listener.onDisconnect(this);
            }
        } catch (IOException e) {
            if (listener != null) {
                listener.onException(this, e);
            }
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getRemoteAddress() {
        if (socket != null) {
            return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
        return "Disconnected";
    }

    @Override
    public String toString() {
        return getRemoteAddress();
    }
}