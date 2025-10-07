package service;

import java.util.HashMap;
import java.util.Map;
import util.AudioRecorder;
import util.AudioPlayer;

public class CallService {
    private AudioRecorder audioRecorder;
    private AudioPlayer audioPlayer;
    private Map<String, CallSession> activeCalls;
    private boolean isInCall = false;
    private String currentCallId;

    public enum CallState {
        IDLE, CALLING, IN_CALL, ENDING
    }

    private CallState currentState = CallState.IDLE;

    public CallService() {
        this.audioRecorder = new AudioRecorder();
        this.audioPlayer = new AudioPlayer();
        this.activeCalls = new HashMap<>();
        System.out.println(" CallManager inicializado");
    }

    // Iniciar llamada
    public boolean startCall(String callId, String targetUser) {
        if (isInCall) {
            System.out.println(" Ya hay una llamada activa: " + currentCallId);
            return false;
        }

        try {
            currentCallId = callId;
            currentState = CallState.CALLING;
            isInCall = true;

            // Crear sesión de llamada
            CallSession session = new CallSession(callId, targetUser);
            activeCalls.put(callId, session);

            System.out.println(" Iniciando llamada: " + callId + " con " + targetUser);

            // Iniciar reproducción de audio recibido
            audioPlayer.startPlayingForCall();

            // Iniciar grabación y envío de audio
            audioRecorder.startRecordingForCall(audioData -> {
                // TODO: Aquí se integraría con TCPConnection para enviar paquetes
                sendAudioPacket(callId, audioData);
            });

            currentState = CallState.IN_CALL;
            System.out.println(" Llamada conectada - ID: " + callId);

            return true;

        } catch (Exception e) {
            System.out.println(" Error iniciando llamada: " + e.getMessage());
            currentState = CallState.IDLE;
            isInCall = false;
            return false;
        }
    }

    // Terminar llamada
    public void endCall(String callId) {
        if (!isInCall || !callId.equals(currentCallId)) {
            System.out.println(" No hay llamada activa con ID: " + callId);
            return;
        }

        try {
            currentState = CallState.ENDING;

            // Detener grabación y reproducción
            audioRecorder.stopRecording();
            audioPlayer.stopPlaying();

            // Remover sesión
            activeCalls.remove(callId);

            // Resetear estado
            isInCall = false;
            currentCallId = null;
            currentState = CallState.IDLE;

            System.out.println(" Llamada terminada: " + callId);

        } catch (Exception e) {
            System.out.println(" Error terminando llamada: " + e.getMessage());
        }
    }

    // Recibir audio de otra persona (llamado por la capa de red)
    public void receiveAudioPacket(String callId, byte[] audioData) {
        if (isInCall && callId.equals(currentCallId)) {
            audioPlayer.addAudioData(audioData);
            System.out.println("📦 Audio recibido para llamada: " + callId);
        }
    }

    // Enviar paquete de audio (se integrará con TCPConnection)
    private void sendAudioPacket(String callId, byte[] audioData) {
        // TODO: Integrar con TCPConnection para envío real
        System.out.println(" Enviando " + audioData.length + " bytes de audio - Llamada: " + callId);

    }

    // Getters de estado
    public boolean isInCall() {
        return isInCall;
    }

    public CallState getCurrentState() {
        return currentState;
    }

    public String getCurrentCallId() {
        return currentCallId;
    }

    // Clase interna para manejo de sesiones
    private static class CallSession {
        String callId;
        String targetUser;
        long startTime;

        public CallSession(String callId, String targetUser) {
            this.callId = callId;
            this.targetUser = targetUser;
            this.startTime = System.currentTimeMillis();
        }
    }
}

