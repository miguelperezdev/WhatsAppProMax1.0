package network;

import java.io.*;
import java.net.Socket;

public class TCPConnection extends Thread {

    private static TCPConnection instance;
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final TCPConnectionListener eventListener;
    private volatile boolean running = true;

    // 🔒 Constructor privado (Singleton)
    private TCPConnection(TCPConnectionListener eventListener, Socket socket) throws IOException {
        this.eventListener = eventListener;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        start();
    }

    // 🧠 Método estático para obtener instancia única
    public static synchronized TCPConnection getInstance(TCPConnectionListener eventListener, Socket socket) throws IOException {
        if (instance == null) {
            instance = new TCPConnection(eventListener, socket);
        }
        return instance;
    }

    // 📨 Enviar mensaje a través del socket
    public synchronized void sendMessage(String message) {
        try {
            out.write(message + "\r\n");
            out.flush();
        } catch (IOException e) {
            eventListener.onException(this, e);
            disconnect();
        }
    }

    // 🔄 Escuchar mensajes entrantes
    @Override
    public void run() {
        try {
            eventListener.onConnectionReady(this);
            while (running) {
                String message = in.readLine();
                if (message == null) break;
                eventListener.onReceiveString(this, message);
            }
        } catch (IOException e) {
            eventListener.onException(this, e);
        } finally {
            eventListener.onDisconnect(this);
        }
    }

    // 🔚 Cerrar conexión
    public synchronized void disconnect() {
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            eventListener.onException(this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection: " + socket.getInetAddress() + ":" + socket.getPort();
    }
}
