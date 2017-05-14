//package version4_manyMachines;

import java.util.LinkedList;
import java.util.Scanner;

/**
 * Handles all of the commands and request a user could make, as well as
 * the information they would care about.
 */
class User {
    LinkedList<String> allOnlineUsers; /* the user can see all the names of
                                          other users who are online */
    LinkedList<String> allConversations; /* the user can see the names of all
                                            the conversations that are going on */
    LinkedList<Conversation> myConversations; // that this user is a part of
    String username; // this user's username
    Network network; /* the user needs one of these for network types of requests
                        like logging in, logging out, joining conversations, etc. */

    User() {
        network = new Network();
        network.login(this);
    }

    void chat() {
        Scanner keyboard = new Scanner(System.in);
        while (true) {
            System.out.println("Online users:");
            for (String name : allOnlineUsers) {
                System.out.println("\t"+name);
            }
            System.out.println("To send a message, enter recipient name, then enter body, or 'exit' at any time to stop");
            String recipient = keyboard.nextLine();
            if (!allOnlineUsers.contains(recipient)) {
                System.out.println("User not found, please try again");
                continue;
            }
            if (recipient.equals("exit")) break;
            String body = keyboard.nextLine();
            if (body.equals("exit")) break;
            network.send(body, recipient, network);
        }
    }

    void addNewUser(String name) {
        if (!allOnlineUsers.contains(name))
            allOnlineUsers.addLast(name);
    }

    void allUsersUpdate(LinkedList<String> newOnlineUsers) {
        allOnlineUsers = newOnlineUsers;
        // maybe update the screen or something
        // or at least a primitive print message
    }

    LinkedList<String> getAllUsernames() {
        return allOnlineUsers;
    }

    void logout() {
        System.out.println("Logging out...");
        network.logout();
        System.out.println("Good bye!");
    }
}
