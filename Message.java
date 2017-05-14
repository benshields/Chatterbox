//package version4_manyMachines;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * This encapsulates the object of text that users send to each other
 */
class Message implements Serializable {
    private String senderUsername; // who sent this message
    private String recipientUsername; // intended receiver or group
    private String text; // the body of the message
    private Timestamp timestamp; // the time at which the user attempts to send this

    Message(String senderUsername, String recipientUsername) {
        this.senderUsername = senderUsername;
        this.recipientUsername = recipientUsername;
    }

    /**
     * Called when the user finishes writing the body of a message.
     * @param text: The body of this message
     */
    void setText(String text) {
        this.text = text;
    }

    /**
     * Called when just about to send this message.
     * @param milliTime: The user can pass in the SystemTimeMillis()
     */
    void setTimestamp(long milliTime) {
        this.timestamp = new Timestamp(milliTime);
    }

    /**
     * The network calls this to see if it should be given to a user or
     * sent on ahead to next user.
     * @param check: The name of the user who is checking this message
     * @return Whether checking user is the intended receiver or not
     */
    boolean isRecipient(String check) {
        return check.equals(recipientUsername);
    }

    /**
     * TODO: this needs some work eventually
     * @return The formatted message to be displayed in textual chat view
     */
    String format() {
        return "From: senderUsername\n"+"To: recipientUsername\n"+text+"\n@ "+timestamp;
    }
}
