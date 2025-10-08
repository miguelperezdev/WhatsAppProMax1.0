package service;

import model.User;
import model.Group;
import model.Message;
import model.AudioMessage;
import persistence.ChatHistory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {
    private Map<String, User> onlineUsers;
    private Map<String, Group> groups;
    private ChatHistory chatHistory;

    public ChatManager() {
        this.onlineUsers = new ConcurrentHashMap<>();
        this.groups = new ConcurrentHashMap<>();
        this.chatHistory = new ChatHistory();
    }

    // ==================== GESTIÓN DE USUARIOS ====================

    /**
     * Login de un usuario al sistema
     */
    public boolean loginUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String cleanUsername = username.trim();

        if (onlineUsers.containsKey(cleanUsername)) {
            return false; // Usuario ya conectado
        }

        User user = new User(cleanUsername);
        onlineUsers.put(cleanUsername, user);
        System.out.println("✅ Usuario conectado: " + cleanUsername);
        return true;
    }

    /**
     * Logout de un usuario
     */
    public void logoutUser(String username) {
        if (username != null) {
            onlineUsers.remove(username);
            System.out.println("❌ Usuario desconectado: " + username);
        }
    }

    /**
     * Verifica si un usuario está online
     */
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    /**
     * Obtiene lista de usuarios online
     */
    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers.keySet());
    }

    /**
     * Obtiene un usuario por nombre
     */
    public User getUser(String username) {
        return onlineUsers.get(username);
    }

    // ==================== GESTIÓN DE GRUPOS ====================

    /**
     * Crea un nuevo grupo
     */
    public boolean createGroup(String groupName, String creator) {
        if (groupName == null || groupName.trim().isEmpty() || creator == null) {
            return false;
        }

        String cleanGroupName = groupName.trim();

        if (groups.containsKey(cleanGroupName)) {
            System.out.println("❌ Grupo ya existe: " + cleanGroupName);
            return false;
        }

        if (!isUserOnline(creator)) {
            System.out.println("❌ Creador no está online: " + creator);
            return false;
        }

        Group group = new Group(cleanGroupName, creator);
        groups.put(cleanGroupName, group);
        System.out.println("✅ Grupo creado: " + cleanGroupName + " por " + creator);
        return true;
    }

    /**
     * Unirse a un grupo existente
     */
    public boolean joinGroup(String groupName, String username) {
        if (groupName == null || username == null) {
            return false;
        }

        Group group = groups.get(groupName);
        if (group == null) {
            System.out.println("❌ Grupo no existe: " + groupName);
            return false;
        }

        if (!isUserOnline(username)) {
            System.out.println("❌ Usuario no está online: " + username);
            return false;
        }

        boolean success = group.addMember(username);
        if (success) {
            System.out.println("✅ Usuario " + username + " se unió al grupo " + groupName);
        } else {
            System.out.println("❌ Usuario " + username + " ya está en el grupo " + groupName);
        }
        return success;
    }

    /**
     * Abandonar un grupo
     */
    public boolean leaveGroup(String groupName, String username) {
        if (groupName == null || username == null) {
            return false;
        }

        Group group = groups.get(groupName);
        if (group == null) {
            return false;
        }

        boolean success = group.removeMember(username);
        if (success) {
            System.out.println("✅ Usuario " + username + " abandonó el grupo " + groupName);

            // Si el grupo queda vacío, eliminarlo
            if (group.getMemberCount() == 0) {
                groups.remove(groupName);
                System.out.println("🗑️ Grupo eliminado por estar vacío: " + groupName);
            }
        }
        return success;
    }

    /**
     * Obtiene los miembros de un grupo
     */
    public List<String> getGroupMembers(String groupName) {
        Group group = groups.get(groupName);
        if (group != null) {
            return new ArrayList<>(group.getMembers());
        }
        return new ArrayList<>();
    }

    /**
     * Obtiene todos los grupos existentes
     */
    public List<String> getAllGroups() {
        return new ArrayList<>(groups.keySet());
    }

    /**
     * Verifica si un grupo existe
     */
    public boolean groupExists(String groupName) {
        return groups.containsKey(groupName);
    }

    /**
     * Obtiene información de un grupo específico
     */
    public Group getGroup(String groupName) {
        return groups.get(groupName);
    }

    // ==================== GESTIÓN DE MENSAJES ====================

    /**
     * Guarda un mensaje de texto en el historial
     */
    public void saveTextMessage(Message message) {
        if (message != null) {
            chatHistory.saveMessage(message);
            System.out.println("💾 Mensaje guardado: " + message.getFrom() + " -> " + message.getTo());
        }
    }

    /**
     * Guarda un mensaje de audio en el historial
     */
    public void saveAudioMessage(AudioMessage audioMessage) {
        if (audioMessage != null) {
            chatHistory.saveAudioMessage(audioMessage);
            System.out.println("💾 Audio guardado: " + audioMessage.getFrom() + " -> " + audioMessage.getTo() +
                    " (" + audioMessage.getAudioSize() + " bytes)");
        }
    }

    /**
     * Obtiene el historial de mensajes de texto
     */
    public List<Message> getMessageHistory(String target, boolean isGroup) {
        if (target == null) {
            return new ArrayList<>();
        }
        return chatHistory.loadMessages(target, isGroup);
    }

    /**
     * Obtiene el historial de mensajes de audio
     */
    public List<AudioMessage> getAudioMessageHistory(String target, boolean isGroup) {
        if (target == null) {
            return new ArrayList<>();
        }
        return chatHistory.loadAudioMessages(target, isGroup);
    }

    /**
     * Obtiene los datos de audio por ID
     */
    public byte[] getAudioData(String audioId) {
        return chatHistory.loadAudioData(audioId);
    }

    // ==================== ESTADÍSTICAS Y UTILIDADES ====================

    /**
     * Número de usuarios conectados
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    /**
     * Número de grupos activos
     */
    public int getGroupCount() {
        return groups.size();
    }

    /**
     * Obtiene información del estado del sistema
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("onlineUsers", getOnlineUserCount());
        status.put("activeGroups", getGroupCount());
        status.put("totalUsers", onlineUsers.size());
        status.put("totalGroups", groups.size());
        return status;
    }

    /**
     * Limpia todos los datos (útil para testing)
     */
    public void clearAllData() {
        onlineUsers.clear();
        groups.clear();
        System.out.println("🧹 Todos los datos han sido limpiados");
    }

    /**
     * Verifica si un usuario es miembro de un grupo
     */
    public boolean isUserInGroup(String username, String groupName) {
        Group group = groups.get(groupName);
        return group != null && group.hasMember(username);
    }

    /**
     * Obtiene los grupos a los que pertenece un usuario
     */
    public List<String> getUserGroups(String username) {
        List<String> userGroups = new ArrayList<>();
        for (Map.Entry<String, Group> entry : groups.entrySet()) {
            if (entry.getValue().hasMember(username)) {
                userGroups.add(entry.getKey());
            }
        }
        return userGroups;
    }
}