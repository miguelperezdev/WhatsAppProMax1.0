package ui;

import model.AudioMessage;
import model.Message;
import network.TCPConnection;
import network.TCPConnectionListener;
import service.ChatManager;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer implements TCPConnectionListener {

    private final ChatManager chatManager;
    private final Map<String, TCPConnection> userConnections = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        new MainServer(port);
    }

    private MainServer(int port) {
        this.chatManager = new ChatManager();
        System.out.println("SERVIDOR DE CHAT INICIADO EN PUERTO " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    new TCPConnection(serverSocket.accept(), this);
                } catch (IOException e) {
                    System.err.println("Error al aceptar conexión: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo iniciar el servidor en el puerto " + port, e);
        }
    }

    @Override
    public synchronized void onConnectionReady(TCPConnection connection) {
        System.out.println("Nueva conexión establecida: " + connection.getRemoteAddress());
    }

    @Override
    public synchronized void onDisconnect(TCPConnection connection) {
        String user = findUserByConnection(connection);
        if (user != null) {
            userConnections.remove(user);
            chatManager.logoutUser(user);
            broadcastObject("type:system_message|content:El usuario " + user + " se ha desconectado.");
            System.out.println("Usuario desconectado: " + user);
        }
    }

    @Override
    public synchronized void onReceiveObject(TCPConnection connection, Object object) {
        if (object instanceof String command) {
            processCommand(connection, command);
        } else if (object instanceof AudioMessage audioMessage) {
            handleAudioMessage(audioMessage);
        }
    }

    @Override
    public synchronized void onException(TCPConnection connection, Exception e) {
        System.err.println("Excepción en " + connection.getRemoteAddress() + ": " + e.getMessage());
        onDisconnect(connection);
    }

    private void processCommand(TCPConnection connection, String command) {
        Map<String, String> data = parseCommand(command);
        String type = data.get("type");
        if (type == null) return;

        switch (type) {
            case "login" -> handleLogin(connection, data);
            case "logout" -> onDisconnect(connection);
            case "private_message" -> handlePrivateMessage(data);
            case "group_message" -> handleGroupMessage(data);
            case "create_group" -> handleCreateGroup(data);
            case "join_group" -> handleJoinGroup(data);
            case "get_online_users" -> handleGetOnlineUsers(data);
            case "get_groups" -> handleGetGroups(data);
            case "call_start" -> handleCallStart(data);
            case "call_accept" -> handleCallAccept(data);
            case "call_end" -> handleCallEnd(data);
            default -> System.out.println("Comando desconocido: " + type);
        }
    }

    private void handleLogin(TCPConnection connection, Map<String, String> data) {
        String username = data.get("username");
        if (username != null && chatManager.loginUser(username)) {
            userConnections.put(username, connection);
            sendObjectToUser(username, "type:login_success");
            broadcastObject("type:system_message|content:" + username + " se ha conectado.");
            System.out.println("Usuario conectado: " + username);
        } else {
            connection.sendObject("type:login_error|message:Nombre de usuario inválido o en uso.");
            connection.disconnect();
        }
    }

    private void handlePrivateMessage(Map<String, String> data) {
        String from = data.get("from");
        String to = data.get("to");
        String content = data.get("content");
        if (from == null || to == null || content == null) return;

        chatManager.saveTextMessage(new Message(from, to, content, false));
        String msg = String.format("type:private_message|from:%s|content:%s", from, content);
        sendObjectToUser(to, msg);
        sendObjectToUser(from, msg);
    }

    private void handleGroupMessage(Map<String, String> data) {
        String from = data.get("from");
        String group = data.get("group");
        String content = data.get("content");
        if (from == null || group == null || content == null) return;

        if (!chatManager.groupExists(group)) {
            sendObjectToUser(from, "type:error|message:El grupo no existe.");
            return;
        }

        chatManager.saveTextMessage(new Message(from, group, content, true));
        String msg = String.format("type:group_message|from:%s|group:%s|content:%s", from, group, content);
        for (String member : chatManager.getGroupMembers(group)) {
            sendObjectToUser(member, msg);
        }
    }

    private void handleAudioMessage(AudioMessage audioMessage) {
        System.out.println("Audio recibido de " + audioMessage.getFrom() + " para " + audioMessage.getTo());
        chatManager.saveAudioMessage(audioMessage);

        if (audioMessage.isGroupMessage()) {
            if (chatManager.groupExists(audioMessage.getTo())) {
                for (String member : chatManager.getGroupMembers(audioMessage.getTo())) {
                    sendObjectToUser(member, audioMessage);
                }
            }
        } else {
            sendObjectToUser(audioMessage.getTo(), audioMessage);
            sendObjectToUser(audioMessage.getFrom(), audioMessage);
        }
    }

    private void handleCreateGroup(Map<String, String> data) {
        String groupName = data.get("group_name");
        String creator = data.get("creator");
        if (groupName != null && creator != null && chatManager.createGroup(groupName, creator)) {
            broadcastObject("type:system_message|content:Grupo '" + groupName + "' creado por " + creator);
        } else {
            sendObjectToUser(creator, "type:error|message:No se pudo crear el grupo '" + groupName + "'.");
        }
    }

    private void handleJoinGroup(Map<String, String> data) {
        String groupName = data.get("group_name");
        String username = data.get("username");
        if (groupName != null && username != null && chatManager.joinGroup(groupName, username)) {
            broadcastObject("type:system_message|content:" + username + " se unió al grupo " + groupName);
        } else {
            sendObjectToUser(username, "type:error|message:No se pudo unir al grupo '" + groupName + "'.");
        }
    }

    private void handleGetOnlineUsers(Map<String, String> data) {
        String user = data.get("username");
        if (user != null) {
            String list = String.join(",", chatManager.getOnlineUsers());
            sendObjectToUser(user, "type:online_users|users:" + list);
        }
    }

    private void handleGetGroups(Map<String, String> data) {
        String user = data.get("username");
        if (user != null) {
            String list = String.join(",", chatManager.getAllGroups());
            sendObjectToUser(user, "type:groups_list|groups:" + list);
        }
    }

    private void handleCallStart(Map<String, String> data) {
        String from = data.get("from");
        String to = data.get("to");
        String isGroupStr = data.get("isGroup");
        String udpPortStr = data.get("udpPort");

        if (from == null || to == null || udpPortStr == null) return;

        boolean isGroup = "true".equalsIgnoreCase(isGroupStr);
        int callerUdpPort = Integer.parseInt(udpPortStr);

        TCPConnection callerConn = userConnections.get(from);
        if (callerConn == null) return;
        String callerIp = callerConn.getSocket().getInetAddress().getHostAddress();

        System.out.println("Intento de llamada de " + from + " a " + to + " | UDP: " + callerIp + ":" + callerUdpPort);

        String incomingCallMsg = String.format(
                "type:incoming_call|from:%s|to:%s|isGroup:%s|callerIp:%s|callerUdpPort:%d",
                from, to, isGroup, callerIp, callerUdpPort
        );

        if (isGroup) {
            if (!chatManager.groupExists(to)) {
                sendObjectToUser(from, "type:error|message:Grupo no existe para llamada.");
                return;
            }
            for (String member : chatManager.getGroupMembers(to)) {
                if (!member.equals(from)) {
                    sendObjectToUser(member, incomingCallMsg);
                }
            }
        } else {
            if (!chatManager.isUserOnline(to)) {
                sendObjectToUser(from, "type:error|message:El usuario no está en línea.");
                return;
            }
            sendObjectToUser(to, incomingCallMsg);
        }
        sendObjectToUser(from, "type:call_waiting|to:" + to);
    }

    private void handleCallAccept(Map<String, String> data) {
        String from = data.get("from");
        String to = data.get("to");
        String udpPortStr = data.get("udpPort");

        if (from == null || to == null || udpPortStr == null) {
            System.out.println("Datos incompletos en call_accept");
            return;
        }

        int receiverUdpPort = Integer.parseInt(udpPortStr);

        TCPConnection receiverConn = userConnections.get(from);
        if (receiverConn == null) {
            System.out.println("Conexión del receptor '" + from + "' no encontrada.");
            return;
        }
        String receiverIp = receiverConn.getSocket().getInetAddress().getHostAddress();

        System.out.println("Llamada aceptada: " + from + " <-> " + to);
        System.out.println("   Info UDP de '" + from + "': " + receiverIp + ":" + receiverUdpPort);

        sendObjectToUser(to, String.format(
                "type:call_accepted|from:%s|receiverIp:%s|receiverUdpPort:%d",
                from, receiverIp, receiverUdpPort
        ));
    }

    private void handleCallEnd(Map<String, String> data) {
        String from = data.get("from");
        String callId = data.get("callId");
        System.out.println("Llamada finalizada por " + from + " (ID: " + callId + ")");
    }

    private void sendObjectToUser(String username, Object object) {
        TCPConnection conn = userConnections.get(username);
        if (conn != null && conn.isConnected()) {
            conn.sendObject((Serializable) object);
        }
    }

    private void broadcastObject(Object object) {
        for (TCPConnection conn : userConnections.values()) {
            if (conn.isConnected()) {
                conn.sendObject((Serializable) object);
            }
        }
    }

    private String findUserByConnection(TCPConnection connection) {
        for (Map.Entry<String, TCPConnection> entry : userConnections.entrySet()) {
            if (entry.getValue().equals(connection)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Map<String, String> parseCommand(String command) {
        Map<String, String> result = new HashMap<>();
        for (String part : command.split("\\|")) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                result.put(kv[0], kv[1]);
            }
        }
        return result;
    }
}