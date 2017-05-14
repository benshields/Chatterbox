//package version4_manyMachines;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/**
 * This object tells connecting peers which address and port the listener is listening on.
 */
public class ListenerLocator {
    public static void main(String[] args) {
        System.out.println("Starting listener location service");
        Thread locator = new Thread(ListenerLocator::locate);
        Thread waiter = new Thread(()->waitForExit(locator));
        System.out.println("Listener status: noListener");
        locator.start();
        waiter.start();
    }

    private static void locate() {
        final int locatorPort = 35754;
        final int timeout = 3000; // three seconds by default, may change in future
        boolean listenerExists = false;
        ServerSocket locator = null;
        String listenerAddress = "";
        int listenerPort = -1; // the port that the listener is currently using

        try {
            locator = new ServerSocket(locatorPort); // setup locator server socket
            locator.setReuseAddress(true); // so that the application can be ran back to back
            locator.setSoTimeout(timeout); // set a timeout so thread periodically checks for interruption
        } catch (IOException e) {
            System.err.println("ERROR @ ListenerLocator : locate() locator = new ServerSocket(locatorPort) -> IOException");
            e.printStackTrace();
            System.exit(9);
        }

        boolean finishedLocating = false; // flag changed by thread interruption by waitForExit
        while (!finishedLocating) { // continuously answer requests to locate the listener port
            try {
                Socket newUser = locator.accept(); // accept a request
                ObjectOutputStream out = new ObjectOutputStream(newUser.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(newUser.getInputStream());
                boolean nextListener = in.readBoolean(); // check if user wants to locate listener or become listener
                if (nextListener) {
                    listenerAddress = in.readUTF(); // get the address the client wants to use to listen on
                    listenerPort = in.readInt(); // get the port the client wants to use to listen on
                } else {
                    System.out.println("Sending listener status");
                    out.writeBoolean(listenerExists); // send the listener status
                    out.flush(); // push it on through instead of waiting for buffer to fill
                    if (!listenerExists) { // the client will be listener and create a port and socket
                        System.out.println("Waiting for port number from new listener...");
                        listenerAddress = in.readUTF(); // get the address the client wants to use to listen on
                        listenerPort = in.readInt(); // get the port the client wants to use to listen on
                        listenerExists = true; // change listener status
                    } else { // a listener already exists
                        out.writeUTF(listenerAddress);  // tell the client which address they can reach the listener on
                        out.flush();
                        out.writeInt(listenerPort); // tell the client which port they can reach the listener on
                        out.flush();
                    }
                }
                System.out.println("Listener status: listenerExists on address "+listenerAddress+", port "+listenerPort);
            } catch (SocketTimeoutException e) { // accept has been given a timeout
                if (Thread.interrupted()) { // check if user wants the chat to end
                    finishedLocating = true;
                }
            } catch (IOException e) {
                System.err.println("ERROR @ ListenerLocator : locate() Socket newUser = locator.accept() -> IOException");
                e.printStackTrace();
                System.exit(10);
            }
        }
        System.out.println("Finished locating the listener, now exiting");
    }

    private static void waitForExit(Thread locator) {
        Scanner keyboard = new Scanner(System.in);
        String input;
        boolean finishedWaiting = false;
        while (!finishedWaiting) {
            input = keyboard.nextLine();
            if (input.equals("exit")) {
                locator.interrupt();
                finishedWaiting = true;
            } else {
                System.out.println("Please type 'exit' to kill process and end chat");
            }
        }
    }
}
