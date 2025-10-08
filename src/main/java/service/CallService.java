package service;

import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import util.AudioRecorder;
import util.AudioPlayer;
import network.TCPConnection;

public class CallService {
    private AudioRecorder audioRecorder;
    private AudioPlayer audioPlayer;
    private Map<String, CallSession> activeCalls;
    private boolean isInCall = false;
    private String currentCallId;
    private TCPConnection connection;

    public enum CallState {
        IDLE, CALLING, IN_CALL, ENDING
    }

    private CallState currentState = CallState.IDLE;

    public CallService() {
        this.audioRecorder = new AudioRecorder();
        this.audioPlayer = new AudioPlayer();
        this.activeCalls = new HashMap<>();
        System.out.println("CallManager inicializado");
    }

    public void setConnection(TCPConnection connection) {
        this.connection = connection;
    }

    public boolean startCall(String callId, String targetUser) {
        if (isInCall) {
            System.out.println("Ya hay una llamada activa: " + currentCallId);
            return false;
        }

        try {
            currentCallId = callId;
            currentState = CallState.CALLING;
            isInCall = true;

            CallSession session = new CallSession(callId, targetUser);
            activeCalls.put(callId, session);

            System.out.println("Iniciando llamada: " + callId + " con " + targetUser);

            audioPlayer.startPlayingForCall();

            audioRecorder.startRecordingForCall(audioData -> {
                sendAudioPacket(callId, audioData);
            });

            currentState = CallState.IN_CALL;
            System.out.println("Llamada conectada - ID: " + callId);

            return true;

        } catch (Exception e) {
            System.out.println("Error iniciando llamada: " + e.getMessage());
            currentState = CallState.IDLE;
            isInCall = false;
            return false;
        }
    }

    public void endCall(String callId) {
        if (!isInCall || !callId.equals(currentCallId)) {
            System.out.println("No hay llamada activa con ID: " + callId);
            return;
        }

        try {
            currentState = CallState.ENDING;
            audioRecorder.stopRecording();
            audioPlayer.stopPlaying();
            activeCalls.remove(callId);
            isInCall = false;
            currentCallId = null;
            currentState = CallState.IDLE;
            System.out.println("Llamada terminada: " + callId);
        } catch (Exception e) {
            System.out.println("Error terminando llamada: " + e.getMessage());
        }
    }

    public void receiveAudioPacket(String callId, byte[] audioData) {
        if (isInCall && callId.equals(currentCallId)) {
            audioPlayer.addAudioData(audioData);
            System.out.println("Audio recibido para llamada: " + callId);
        }
    }

    private void sendAudioPacket(String callId, byte[] audioData) {
        if (connection != null && connection.isConnected()) {
            String encoded = Base64.getEncoder().encodeToString(audioData);
            String message = "[CALL_AUDIO]:" + callId + ":" + encoded;
            connection.sendMessage(message);
        } else {
            System.out.println("Conexión no disponible - Audio no enviado");
        }
    }

    public boolean isInCall() {
        return isInCall;
    }

    public CallState getCurrentState() {
        return currentState;
    }

    public String getCurrentCallId() {
        return currentCallId;
    }

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
