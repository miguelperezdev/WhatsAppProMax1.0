package ui;

import network.TCPConnection;
import network.TCPConnectionListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class MainServer implements TCPConnectionListener {

    private final ArrayList<TCPConnection> connections = new ArrayList<>();

    public static void main(String[] args) {
        new MainServer();
    }

    private MainServer() {
        System.out.println("Servidor iniciado...");
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            while (true) {
                try {
                    newConnection(serverSocket);
                } catch (IOException e) {
                    System.out.println("Error aceptando conexión: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo iniciar el servidor", e);
        }
    }

    private void newConnection(ServerSocket serverSocket) throws IOException {
        TCPConnection connection = TCPConnection.getInstance(this, serverSocket.accept());
        connections.add(connection);
        System.out.println("Cliente conectado: " + connection);
    }

    @Override
    public synchronized void onConnectionReady(TCPConnection connection) {
        broadcast("Cliente conectado: " + connection);
    }

    @Override
    public synchronized void onReceiveString(TCPConnection connection, String message) {
        System.out.println("Mensaje recibido: " + message);
        broadcast(">> " + message);
    }

    @Override
    public synchronized void onDisconnect(TCPConnection connection) {
        connections.remove(connection);
        broadcast("Cliente desconectado: " + connection);
    }

    @Override
    public synchronized void onException(TCPConnection connection, Exception e) {
        System.out.println("Error en conexión: " + e.getMessage());
    }

    private void broadcast(String message) {
        for (TCPConnection c : connections) {
            c.sendMessage(message);
        }
    }
}
