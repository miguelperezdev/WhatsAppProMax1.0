package ui;

import network.TCPConnection;
import network.TCPConnectionListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MainClient implements TCPConnectionListener {

    public static void main(String[] args) throws IOException {
        new MainClient();
    }

    private TCPConnection connection;

    private MainClient() {
        try {
            connection = TCPConnection.getInstance(this, new Socket("127.0.0.1", 5000));
            System.out.println("Cliente conectado. Escribe mensajes y presiona Enter:");
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String msg = console.readLine();
                if (msg.equalsIgnoreCase("exit")) {
                    connection.disconnect();
                    break;
                }
                connection.sendMessage(msg);
            }
        } catch (IOException e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }

    @Override
    public void onConnectionReady(TCPConnection connection) {
        System.out.println("Conexión lista: " + connection);
    }

    @Override
    public void onReceiveString(TCPConnection connection, String message) {
        System.out.println(message);
    }

    @Override
    public void onDisconnect(TCPConnection connection) {
        System.out.println("Conexión cerrada");
    }

    @Override
    public void onException(TCPConnection connection, Exception e) {
        System.out.println("Error: " + e.getMessage());
    }
}
