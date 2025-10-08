package ui;

import service.ChatManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ChatMenu {

    public static void main(String[] args) throws IOException {
        ChatManager manager = new ChatManager();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("=== CHAT - GESTIÓN DE GRUPOS ===");
        String currentUser = null;

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
                    if (currentUser != null) {
                        manager.createGroup(reader.readLine(), currentUser);
                    } else {
                        System.out.println("Debes crear un usuario primero");
                    }
                }
                case "3" -> {
                    System.out.print("Nombre del grupo: ");
                    String g = reader.readLine();
                    if (currentUser != null) {
                        manager.addUserToGroup(g, currentUser);
                    } else {
                        System.out.println("Debes crear un usuario primero");
                    }
                }
                case "4" -> {
                    System.out.print("Grupo destino: ");
                    String g = reader.readLine();
                    System.out.print("Mensaje: ");
                    if (currentUser != null) {
                        manager.sendMessage(g, currentUser, reader.readLine());
                    } else {
                        System.out.println("Debes crear un usuario primero");
                    }
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
