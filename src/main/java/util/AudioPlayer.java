package util;

import javax.sound.sampled.*;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioPlayer {
    private static final int BUFFER_SIZE = 1024;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000, 16, 1, true, false);

    private SourceDataLine speakers;
    private boolean isPlaying = false;
    private Thread playbackThread;
    private BlockingQueue<byte[]> audioBuffer;

    public AudioPlayer() {
        this.audioBuffer = new LinkedBlockingQueue<>(50); // Buffer de 50 paquetes
    }

    // Reproducir nota de voz desde archivo
    public void playVoiceNote(String fileName) {
        try {
            File audioFile = new File("data/audio/" + fileName + ".wav");
            if (!audioFile.exists()) {
                System.out.println(" Archivo no encontrado: " + audioFile.getAbsolutePath());
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
            speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(AUDIO_FORMAT);
            speakers.start();

            System.out.println(" Reproduciendo nota de voz: " + fileName);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = audioStream.read(buffer)) != -1) {
                speakers.write(buffer, 0, bytesRead);
            }

            speakers.drain();
            speakers.close();
            audioStream.close();

            System.out.println(" Reproducción terminada");

        } catch (Exception e) {
            System.out.println(" Error reproduciendo: " + e.getMessage());
        }
    }

    // Iniciar reproducción para llamadas en tiempo real
    public void startPlayingForCall() {
        if (isPlaying) return;

        isPlaying = true;

        playbackThread = new Thread(() -> {
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
                speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(AUDIO_FORMAT);
                speakers.start();

                System.out.println("🔊 Reproducción de llamada iniciada...");

                while (isPlaying) {
                    try {
                        // Obtener audio del buffer (bloquea si está vacío)
                        byte[] audioData = audioBuffer.take();
                        speakers.write(audioData, 0, audioData.length);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                speakers.drain();
                speakers.stop();
                speakers.close();
                System.out.println(" Reproducción de llamada detenida");

            } catch (Exception e) {
                System.out.println(" Error en reproducción de llamada: " + e.getMessage());
            }
        });

        playbackThread.start();
    }

    // Agregar datos de audio para reproducir (llamado por CallManager)
    public void addAudioData(byte[] audioData) {
        if (isPlaying && audioData != null) {
            // Si el buffer está lleno, descarta el más viejo
            if (!audioBuffer.offer(audioData)) {
                audioBuffer.poll(); // Remover el más viejo
                audioBuffer.offer(audioData); // Agregar el nuevo
            }
        }
    }

    public void stopPlaying() {
        isPlaying = false;
        if (playbackThread != null) {
            playbackThread.interrupt();
        }
        audioBuffer.clear();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public int getBufferSize() {
        return audioBuffer.size();
    }
}

