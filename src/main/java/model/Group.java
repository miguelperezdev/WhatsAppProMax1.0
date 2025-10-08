package model;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private final String name;
    private final List<User> members;
    private final List<Message> messages;

    public Group(String name) {
        this.name = name;
        this.members = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    public String getName() { return name; }
    public List<User> getMembers() { return members; }
    public List<Message> getMessages() { return messages; }

    public void addMember(User user) {
        if (!members.contains(user)) members.add(user);
    }

    public void addMessage(Message msg) {
        messages.add(msg);
    }

    @Override
    public String toString() {
        return "Grupo: " + name + " (" + members.size() + " miembros)";
    }
}
