//package version4_manyMachines;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;

/**
 * Users make and get requests with a Conversation to send and receive messages
 * with a single other user, or in a group chat setting
 */
class Conversation {
    private LinkedList<String> participants;
    private LinkedList<Message> messages;
    private ObjectOutputStream messageOut; // stream for sending messages
    private ObjectInputStream messageIn; // stream for receiving messages

    /**
     * Called when a user creates a new conversation
     * @param firstUser: name of that user
     */
    Conversation(String firstUser) {
        participants = new LinkedList<>();
        participants.addLast(firstUser);
        messages = new LinkedList<>();
        // do something about the ObjectStreams
    }

    /**
     * Gets run a thread that returns after this message gets sent
     */
    void sendMessage() {

    }

    /**
     * A thread continuously runs this until the conversation is left
     */
    void receiveMessage() {

    }
}
