package ui;

import model.ChatManager;
import model.User;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ChatMenu {

    public static void main(String[] args) throws IOException {
        ChatManager manager = new ChatManager();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("=== CHAT - GESTIÓN DE GRUPOS ===");
        User currentUser = null;

        while (true) {
            System.out.println("\n1. Crear usuario");
            System.out.println("2. Crear grupo");
            System.out.println("3. Añadir usuario a grupo");
            System.out.println("4. Enviar mensaje");
            System.out.println("5. Ver historial de grupo");
            System.out.println("6. Ver grupos");
            System.out.println("0. Salir");
            System.out.print("> ");
            String option = reader.readLine();

            switch (option) {
                case "1" -> {
                    System.out.print("Nombre de usuario: ");
                    currentUser = manager.createUser(reader.readLine());
                    System.out.println("Usuario creado: " + currentUser);
                }
                case "2" -> {
                    System.out.print("Nombre del grupo: ");
                    manager.createGroup(reader.readLine());
                }
                case "3" -> {
                    System.out.print("Nombre del grupo: ");
                    String g = reader.readLine();
                    manager.addUserToGroup(g, currentUser);
                }
                case "4" -> {
                    System.out.print("Grupo destino: ");
                    String g = reader.readLine();
                    System.out.print("Mensaje: ");
                    manager.sendMessage(g, currentUser, reader.readLine());
                }
                case "5" -> {
                    System.out.print("Nombre del grupo: ");
                    manager.showHistory(reader.readLine());
                }
                case "6" -> manager.showGroups();
                case "0" -> {
                    System.out.println("Saliendo...");
                    return;
                }
                default -> System.out.println("Opción no válida.");
            }
        }
    }
}
