package network;

import java.io.*;
import java.net.*;

public class TCPConnection {
    private Socket socket;

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    private TCPConnectionListener listener;
    private boolean connected;
    private Thread listenerThread;

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public TCPConnection(Socket socket, TCPConnectionListener listener) throws IOException {
        this.socket = socket;
        this.listener = listener;
        this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        this.objectInputStream = new ObjectInputStream(socket.getInputStream());

        this.connected = true;
        startListening();
    }

    public TCPConnection(TCPConnectionListener listener, String ip, int port) throws IOException {
        this(new Socket(ip, port), listener);
    }


    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                if (listener != null) {
                    listener.onConnectionReady(this);
                }

                Object receivedObject;
                while (connected && (receivedObject = objectInputStream.readObject()) != null) {
                    if (listener != null) {

                        listener.onReceiveObject(this, receivedObject);
                    }
                }
            } catch (IOException | ClassNotFoundException e) { // --> CAMBIO: Se añade ClassNotFoundException
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


    public void sendObject(java.io.Serializable object) {
        if (connected && objectOutputStream != null) {
            try {
                objectOutputStream.writeObject(object);
                objectOutputStream.flush();
            } catch (IOException e) {
                if (listener != null) {
                    listener.onException(this, e);
                }
            }
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (objectOutputStream != null) objectOutputStream.close();
            if (objectInputStream != null) objectInputStream.close();
            if (socket != null) socket.close();
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }

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