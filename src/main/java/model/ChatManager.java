package model;

import persistence.ChatHistory;
import java.util.ArrayList;
import java.util.List;

public class ChatManager {
    private final List<User> users = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    private final ChatHistory history = new ChatHistory();

    public User createUser(String name) {
        User u = new User(name);
        users.add(u);
        return u;
    }

    public Group createGroup(String name) {
        Group g = new Group(name);
        groups.add(g);
        return g;
    }

    public void addUserToGroup(String groupName, User user) {
        Group group = findGroup(groupName);
        if (group != null) group.addMember(user);
    }

    public void sendMessage(String groupName, User sender, String content) {
        Group group = findGroup(groupName);
        if (group != null) {
            Message m = new Message(sender.getName(), content);
            group.addMessage(m);
            history.saveHistory(groupName, List.of(m));
        }
    }

    public void showHistory(String groupName) {
        history.loadHistory(groupName);
    }

    public void showGroups() {
        for (Group g : groups) System.out.println(g);
    }

    private Group findGroup(String name) {
        return groups.stream().filter(g -> g.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
