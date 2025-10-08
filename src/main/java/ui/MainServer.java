package ui;

import network.TCPConnection;
import network.TCPConnectionListener;
import service.ChatManager;
import model.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class MainServer implements TCPConnectionListener {

    private final ArrayList<TCPConnection> connections = new ArrayList<>();
    private final Map<String, TCPConnection> userConnections = new HashMap<>();
    private final ChatManager chatManager;
    private boolean running;

    public static void main(String[] args) {
        int port = 5000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Puerto inválido, usando puerto por defecto: 5000");
            }
        }
        new MainServer(port);
    }

    private MainServer(int port) {
        this.chatManager = new ChatManager();
        this.running = true;

        System.out.println("=== SERVIDOR DE CHAT INICIADO ===");
        System.out.println("Puerto: " + port);
        System.out.println("Esperando conexiones...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                try {
                    new TCPConnection(serverSocket.accept(), this);
                } catch (IOException e) {
                    if (running) {
                        System.out.println("Error aceptando conexión: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo iniciar el servidor", e);
        }
    }

    @Override
    public synchronized void onConnectionReady(TCPConnection connection) {
        connections.add(connection);
        System.out.println("Conexión establecida: " + connection.getRemoteAddress());
        connection.sendMessage("type:server_ready|message:Conexión al servidor establecida");
    }

    @Override
    public synchronized void onReceiveString(TCPConnection connection, String message) {
        System.out.println("Mensaje recibido de " + connection.getRemoteAddress() + ": " + message);
        processMessage(connection, message);
    }

    private void processMessage(TCPConnection connection, String message) {
        try {
            // Parsear mensaje simple formato "key:value|key:value"
            Map<String, String> messageMap = parseMessage(message);
            String type = messageMap.get("type");

            if (type == null) {
                System.out.println("Mensaje sin tipo: " + message);
                return;
            }

            switch (type) {
                case "login":
                    handleLogin(connection, messageMap);
                    break;
                case "text_message":
                    handleTextMessage(messageMap);
                    break;
                case "create_group":
                    handleCreateGroup(messageMap);
                    break;
                case "join_group":
                    handleJoinGroup(messageMap);
                    break;
                case "leave_group":
                    handleLeaveGroup(messageMap);
                    break;
                case "get_online_users":
                    handleGetOnlineUsers(messageMap);
                    break;
                case "get_groups":
                    handleGetGroups(messageMap);
                    break;
                case "private_message":
                    handlePrivateMessage(messageMap);
                    break;
                case "group_message":
                    handleGroupMessage(messageMap);
                    break;
                default:
                    System.out.println("Tipo de mensaje desconocido: " + type);
                    broadcastToAll("type:error|message:Tipo de mensaje desconocido: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            connection.sendMessage("type:error|message:Error procesando mensaje: " + e.getMessage());
        }
    }

    private Map<String, String> parseMessage(String message) {
        Map<String, String> result = new HashMap<>();
        String[] pairs = message.split("\\|");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                result.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return result;
    }

    private String buildMessage(Map<String, String> data) {
        StringJoiner joiner = new StringJoiner("|");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            joiner.add(entry.getKey() + ":" + entry.getValue());
        }
        return joiner.toString();
    }

    private void handleLogin(TCPConnection connection, Map<String, String> message) {
        String username = message.get("username");

        if (username == null || username.trim().isEmpty()) {
            connection.sendMessage("type:login_error|message:Nombre de usuario requerido");
            return;
        }

        if (chatManager.loginUser(username)) {
            userConnections.put(username, connection);

            // Notificar a todos
            broadcastToAll("type:user_joined|username:" + username);
            broadcastToAll("type:system_message|content:Usuario " + username + " se ha conectado");

            // Enviar confirmación al usuario
            connection.sendMessage("type:login_success|username:" + username + "|message:Login exitoso");

            // Enviar lista actualizada de usuarios
            broadcastUserList();

            System.out.println("Usuario conectado: " + username);
        } else {
            connection.sendMessage("type:login_error|message:Usuario " + username + " ya está conectado");
        }
    }

    private void handleTextMessage(Map<String, String> message) {
        String from = message.get("from");
        String content = message.get("content");

        if (from == null || content == null) return;

        // Crear y guardar mensaje
        Message msg = new Message(from, "all", content, false);
        chatManager.saveTextMessage(msg);

        // Broadcast a todos los usuarios
        Map<String, String> response = new HashMap<>();
        response.put("type", "text_message");
        response.put("from", from);
        response.put("content", content);
        response.put("timestamp", msg.getTimestamp().toString());

        broadcastToAll(buildMessage(response));
        System.out.println("Mensaje público de " + from + ": " + content);
    }

    private void handlePrivateMessage(Map<String, String> message) {
        String from = message.get("from");
        String to = message.get("to");
        String content = message.get("content");

        if (from == null || to == null || content == null) return;

        if (chatManager.isUserOnline(to)) {
            Message msg = new Message(from, to, content, false);
            chatManager.saveTextMessage(msg);

            Map<String, String> response = new HashMap<>();
            response.put("type", "private_message");
            response.put("from", from);
            response.put("to", to);
            response.put("content", content);
            response.put("timestamp", msg.getTimestamp().toString());

            String responseStr = buildMessage(response);

            // Enviar al remitente y al destinatario
            sendToUser(from, responseStr);
            sendToUser(to, responseStr);

            System.out.println("Mensaje privado de " + from + " para " + to + ": " + content);
        } else {
            sendToUser(from, "type:error|message:Usuario " + to + " no está conectado");
        }
    }

    private void handleGroupMessage(Map<String, String> message) {
        String from = message.get("from");
        String groupName = message.get("group");
        String content = message.get("content");

        if (from == null || groupName == null || content == null) return;

        if (chatManager.groupExists(groupName)) {
            Message msg = new Message(from, groupName, content, true);
            chatManager.saveTextMessage(msg);

            // Enviar a todos los miembros del grupo
            for (String member : chatManager.getGroupMembers(groupName)) {
                if (!member.equals(from) && chatManager.isUserOnline(member)) {
                    Map<String, String> response = new HashMap<>();
                    response.put("type", "group_message");
                    response.put("from", from);
                    response.put("group", groupName);
                    response.put("content", content);
                    response.put("timestamp", msg.getTimestamp().toString());

                    sendToUser(member, buildMessage(response));
                }
            }

            System.out.println("Mensaje de grupo de " + from + " en " + groupName + ": " + content);
        } else {
            sendToUser(from, "type:error|message:El grupo " + groupName + " no existe");
        }
    }

    private void handleCreateGroup(Map<String, String> message) {
        String groupName = message.get("group_name");
        String creator = message.get("creator");

        if (groupName == null || creator == null) return;

        boolean success = chatManager.createGroup(groupName, creator);

        Map<String, String> response = new HashMap<>();
        response.put("type", "group_creation");
        response.put("success", String.valueOf(success));
        response.put("group_name", groupName);

        if (success) {
            response.put("message", "Grupo creado exitosamente");
            broadcastGroupList();
            broadcastToAll("type:system_message|content:Se ha creado el grupo: " + groupName);
        } else {
            response.put("message", "Error creando grupo - ya existe");
        }

        sendToUser(creator, buildMessage(response));
    }

    private void handleJoinGroup(Map<String, String> message) {
        String groupName = message.get("group_name");
        String username = message.get("username");

        if (groupName == null || username == null) return;

        boolean success = chatManager.joinGroup(groupName, username);

        Map<String, String> response = new HashMap<>();
        response.put("type", "group_join");
        response.put("success", String.valueOf(success));
        response.put("group_name", groupName);

        if (success) {
            response.put("message", "Te has unido al grupo: " + groupName);
            broadcastGroupList();
            broadcastToAll("type:system_message|content:" + username + " se ha unido al grupo " + groupName);
        } else {
            response.put("message", "Error uniéndose al grupo");
        }

        sendToUser(username, buildMessage(response));
    }

    private void handleLeaveGroup(Map<String, String> message) {
        String groupName = message.get("group_name");
        String username = message.get("username");

        if (groupName == null || username == null) return;

        boolean success = chatManager.leaveGroup(groupName, username);

        Map<String, String> response = new HashMap<>();
        response.put("type", "group_leave");
        response.put("success", String.valueOf(success));
        response.put("group_name", groupName);

        if (success) {
            response.put("message", "Has abandonado el grupo: " + groupName);
            broadcastGroupList();
        } else {
            response.put("message", "Error abandonando el grupo");
        }

        sendToUser(username, buildMessage(response));
    }

    private void handleGetOnlineUsers(Map<String, String> message) {
        String username = message.get("username");
        if (username == null) return;

        Map<String, String> response = new HashMap<>();
        response.put("type", "online_users");

        StringJoiner usersJoiner = new StringJoiner(",");
        for (String user : chatManager.getOnlineUsers()) {
            usersJoiner.add(user);
        }
        response.put("users", usersJoiner.toString());

        sendToUser(username, buildMessage(response));
    }

    private void handleGetGroups(Map<String, String> message) {
        String username = message.get("username");
        if (username == null) return;

        Map<String, String> response = new HashMap<>();
        response.put("type", "groups_list");

        StringJoiner groupsJoiner = new StringJoiner(",");
        for (String group : chatManager.getAllGroups()) {
            groupsJoiner.add(group);
        }
        response.put("groups", groupsJoiner.toString());

        sendToUser(username, buildMessage(response));
    }

    private void broadcastUserList() {
        Map<String, String> message = new HashMap<>();
        message.put("type", "user_list_update");

        StringJoiner usersJoiner = new StringJoiner(",");
        for (String user : chatManager.getOnlineUsers()) {
            usersJoiner.add(user);
        }
        message.put("users", usersJoiner.toString());

        broadcastToAll(buildMessage(message));
    }

    private void broadcastGroupList() {
        Map<String, String> message = new HashMap<>();
        message.put("type", "group_list_update");

        StringJoiner groupsJoiner = new StringJoiner(",");
        for (String group : chatManager.getAllGroups()) {
            groupsJoiner.add(group);
        }
        message.put("groups", groupsJoiner.toString());

        broadcastToAll(buildMessage(message));
    }

    private void sendToUser(String username, String message) {
        TCPConnection connection = userConnections.get(username);
        if (connection != null && connection.isConnected()) {
            connection.sendMessage(message);
        }
    }

    private void broadcastToAll(String message) {
        for (TCPConnection connection : connections) {
            if (connection.isConnected()) {
                connection.sendMessage(message);
            }
        }
    }

    @Override
    public synchronized void onDisconnect(TCPConnection connection) {
        connections.remove(connection);

        // Buscar y remover usuario asociado a esta conexión
        String disconnectedUser = null;
        for (Map.Entry<String, TCPConnection> entry : userConnections.entrySet()) {
            if (entry.getValue().equals(connection)) {
                disconnectedUser = entry.getKey();
                break;
            }
        }

        if (disconnectedUser != null) {
            userConnections.remove(disconnectedUser);
            chatManager.logoutUser(disconnectedUser);
            broadcastToAll("type:user_left|username:" + disconnectedUser);
            broadcastToAll("type:system_message|content:Usuario " + disconnectedUser + " se ha desconectado");
            broadcastUserList();
            System.out.println("Usuario desconectado: " + disconnectedUser);
        }

        System.out.println("Conexión cerrada: " + connection.getRemoteAddress());
        System.out.println("Conexiones activas: " + connections.size());
    }

    @Override
    public synchronized void onException(TCPConnection connection, Exception e) {
        System.out.println("Error en conexión " + connection.getRemoteAddress() + ": " + e.getMessage());
    }

    public void stopServer() {
        running = false;
        broadcastToAll("type:server_shutdown|message:El servidor se está apagando");

        for (TCPConnection connection : connections) {
            connection.disconnect();
        }

        System.out.println("Servidor detenido");
    }
}