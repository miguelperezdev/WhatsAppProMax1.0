package persistence;

import model.Message;
import java.io.*;
import java.util.List;

public class ChatHistory {

    private final String folder = "history/";

    public ChatHistory() {
        File dir = new File(folder);
        if (!dir.exists()) dir.mkdirs();
    }

    public void saveHistory(String groupName, List<Message> messages) {
        File file = new File(folder + groupName + ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            for (Message m : messages) {
                writer.write(m.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error guardando historial: " + e.getMessage());
        }
    }

    public void loadHistory(String groupName) {
        File file = new File(folder + groupName + ".txt");
        if (!file.exists()) {
            System.out.println("No hay historial previo.");
            return;
        }
        System.out.println("Historial de " + groupName + ":");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error leyendo historial: " + e.getMessage());
        }
    }
}
