package ui;

import network.TCPConnection;
import network.TCPConnectionListener;
import service.CallService;
import util.AudioRecorder;
import util.AudioPlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MainClient implements TCPConnectionListener {

    public static void main(String[] args) throws IOException {
        new MainClient();
    }

    private TCPConnection connection;
    private CallService callService; // Tu parte
    private AudioRecorder audioRecorder; // Tu parte
    private AudioPlayer audioPlayer; // Tu parte

    private MainClient() {
        try {
            initializeAudioSystem();

            connection = TCPConnection.getInstance(this, new Socket("127.0.0.1", 5000));
            System.out.println("Cliente conectado.");

            showMenu();

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String msg = console.readLine();

                if (msg.equalsIgnoreCase("exit")) {
                    cleanup();
                    connection.disconnect();
                    break;
                }

                if (processAudioCommands(msg, console)) {
                    continue;
                }

                connection.sendMessage(msg);
            }
        } catch (IOException e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }

    private void initializeAudioSystem() {
        try {
            callService = new CallService();
            audioRecorder = new AudioRecorder();
            audioPlayer = new AudioPlayer();
            System.out.println(" Sistema de audio inicializado correctamente");
        } catch (Exception e) {
            System.out.println(" Error inicializando audio: " + e.getMessage());
        }
    }

    private void showMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println(" CHAT CON SISTEMA DE AUDIO Y LLAMADAS");
        System.out.println("=".repeat(50));
        System.out.println("Comandos disponibles:");
        System.out.println(" Texto normal: Envía mensaje de chat");
        System.out.println(" /grabar <nombre>: Graba nota de voz");
        System.out.println(" /reproducir <nombre>: Reproduce nota de voz");
        System.out.println(" /llamar <usuario>: Inicia llamada");
        System.out.println(" /colgar: Termina llamada");
        System.out.println(" /estado: Ver estado de llamadas");
        System.out.println(" exit: Salir del chat");
        System.out.println("=".repeat(50));
        System.out.println("Escribe un mensaje o comando:");
    }

    // Tu parte: Procesar comandos de audio
    private boolean processAudioCommands(String msg, BufferedReader console) {
        try {
            String[] parts = msg.split(" ", 2);
            String command = parts[0].toLowerCase();

            switch (command) {
                case "/grabar":
                    if (parts.length < 2) {
                        System.out.println(" Uso: /grabar <nombre>");
                        return true;
                    }
                    System.out.println(" Grabando '" + parts[1] + "' por 5 segundos... ¡Habla ahora!");
                    audioRecorder.recordVoiceNote(parts[1], 5);
                    System.out.println(" Nota de voz guardada. Usa /reproducir " + parts[1]);
                    return true;

                case "/reproducir":
                    if (parts.length < 2) {
                        System.out.println(" Uso: /reproducir <nombre>");
                        return true;
                    }
                    System.out.println(" Reproduciendo '" + parts[1] + "'...");
                    audioPlayer.playVoiceNote(parts[1]);
                    return true;

                case "/llamar":
                    if (parts.length < 2) {
                        System.out.println(" Uso: /llamar <usuario>");
                        return true;
                    }
                    if (callService.isInCall()) {
                        System.out.println(" Ya estás en una llamada. Usa /colgar primero");
                        return true;
                    }
                    String callId = "call_" + System.currentTimeMillis();
                    boolean started = callService.startCall(callId, parts[1]);
                    if (started) {
                        System.out.println(" Llamada iniciada con " + parts[1]);
                        System.out.println(" Puedes hablar - usa /colgar para terminar");
                        // Notificar al servidor sobre la llamada
                        connection.sendMessage("[CALL_START]:" + parts[1] + ":" + callId);
                    }
                    return true;

                case "/colgar":
                    if (!callService.isInCall()) {
                        System.out.println(" No hay llamada activa");
                        return true;
                    }
                    String currentCall = callService.getCurrentCallId();
                    callService.endCall(currentCall);
                    System.out.println(" Llamada terminada");
                    // Notificar al servidor
                    connection.sendMessage("[CALL_END]:" + currentCall);
                    return true;

                case "/estado":
                    System.out.println(" Estado del sistema:");
                    System.out.println("    En llamada: " + callService.isInCall());
                    System.out.println("    Estado: " + callService.getCurrentState());
                    if (callService.isInCall()) {
                        System.out.println("    ID Llamada: " + callService.getCurrentCallId());
                    }
                    System.out.println("    Grabando: " + audioRecorder.isRecording());
                    System.out.println("    Reproduciendo: " + audioPlayer.isPlaying());
                    return true;

                default:
                    return false; // No es comando de audio, procesar normal
            }
        } catch (Exception e) {
            System.out.println(" Error procesando comando: " + e.getMessage());
            return true;
        }
    }

    private void cleanup() {
        try {
            if (callService != null && callService.isInCall()) {
                callService.endCall(callService.getCurrentCallId());
            }
            if (audioRecorder != null) {
                audioRecorder.stopRecording();
            }
            if (audioPlayer != null) {
                audioPlayer.stopPlaying();
            }
            System.out.println(" Recursos de audio liberados");
        } catch (Exception e) {
            System.out.println("⚠ Error liberando recursos: " + e.getMessage());
        }
    }

    @Override
    public void onConnectionReady(TCPConnection connection) {
        System.out.println(" Conexión TCP lista: " + connection);
    }

    @Override
    public void onReceiveString(TCPConnection connection, String message) {
        if (message.startsWith("[AUDIO]") || message.startsWith("[CALL]")) {
            processIncomingAudioMessage(message);
        } else {
            System.out.println(" " + message);
        }
    }

    private void processIncomingAudioMessage(String message) {
        try {
            if (message.startsWith("[CALL_START]")) {
                System.out.println(" Llamada entrante detectada");
            } else if (message.startsWith("[CALL_END]")) {
                System.out.println(" Llamada terminada por el otro usuario");
            } else if (message.startsWith("[AUDIO]")) {
                System.out.println(" Audio recibido");
            }
        } catch (Exception e) {
            System.out.println(" Error procesando audio: " + e.getMessage());
        }
    }

    @Override
    public void onDisconnect(TCPConnection connection) {
        cleanup();
        System.out.println(" Conexión cerrada");
    }

    @Override
    public void onException(TCPConnection connection, Exception e) {
        System.out.println(" Error de conexión: " + e.getMessage());
    }
}
