//package version4_manyMachines;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Encapsulates the information users need to communicate with each other once connected.
 */
class Network {
    private User user; // the user that owns this Network object, used to respond to them
    private String username = null; // logging in will return the chosen username
    private String myAddress; // IP address that others can reach me at
    private String backAddress; // address of the user behind
    private String linkAddress; // address of the user ahead
    private int backPort; // to store the port of the user behind
    private int linkPort; // to store the port of the user ahead
    private Socket backSocket; // used to receive from previous user in the loop
    private Socket linkSocket; // used to talk to next user in the loop
    private ObjectOutputStream netOut; // stream for sending network signals
    private ObjectInputStream netIn; // stream for receiving network signals

    private final String locatorAddress = "10.192.144.94"; // address used to reach the locator
    private final int locatorPort = 35754; // port used to contact the listener locator
    private boolean isListener = false;
    private String listenerAddress;
    private int listenerPort; // port used if acting as listener
    private ServerSocket listenerSocket; // used to listen for new users logging in
    private ObjectOutputStream listenerOut; // TODO: needed?
    private ObjectInputStream listenerIn; // TODO: needed?

    Network() {
        try {
            myAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        //myPort = generatePortNumber();
        //backPort = generatePortNumber();
    }

    ///////////////////////////////////
    //        User's methods         //
    ///////////////////////////////////
    /**
     * Connect the user to the chat, have them choose a username, and update their list of all users online now.
     * Then assume the role of listener until another user joins
     */
    @SuppressWarnings("unchecked")
    void login(User _user) {
        this.user = _user;
        findListener(locatorAddress, locatorPort);
        if (isListener) { // first user to connect to chat
            //linkPort = myPort; // next user to connect to me
            Thread listener = becomeFirstListener(); // wait for next user
            try {
                listener.join(); // after they join and we setup netIn & netOut channels, can receive
                new Thread(()->receive(this)).start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            contactListener();
            new Thread(()->receive(this)).start();
        }
    }

    void logout() {
        System.out.println("Logging out...");
        try {
            // pass listener role forwards, have next node begin network update process
            netOut.writeUTF("logout");
            netOut.flush();
            netOut.writeUTF(backAddress);
            netOut.writeInt(backPort);
            netOut.flush();
            // pass along the address and port of my back node
            netOut.writeUTF(netIn.readUTF()); // address
            netOut.writeInt(netIn.readInt()); // port
            netOut.flush();
            // await confirmation to logout
            netIn.readUTF();
            // cleanup?
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    ///////////////////////////////////
    //   Socket and stream methods   //
    ///////////////////////////////////
    /**
     * Uses server/client concept to setup a connection between two processes
     */
    private Socket establishSockets(String address, int portNum, ObjectOutputStream optionalOutStream) {
        System.out.println("\tSTARTING Network : establishSockets("+address+", "+portNum+")");
        ServerSocket serverSock = null;
        Socket clientSock = null;
        try { // attempt to become server first
            if (address.equals(myAddress)) { // become server
                System.out.println("\tStarting connecting as server...");
                serverSock = new ServerSocket(portNum);
                // serverSock.setReuseAddress(true); // TODO: I don't think I should reuse general addresses

                if (optionalOutStream != null) { // used when connecting with next link node to establish new netOut
                    System.out.println("\tSending address " + address + " to waiting user");
                    optionalOutStream.writeUTF(address);
                    optionalOutStream.flush();
                    System.out.println("\tSending port number " + portNum + " to waiting user");
                    optionalOutStream.writeInt(portNum); // send my port number back, to be sent ahead to next user
                    optionalOutStream.flush();
                }

                System.out.println("\tWaiting for client connection on " + address + ", port " + portNum + "...");
                clientSock = serverSock.accept(); // wait for & accept client
                if (portNum != locatorPort) {
                    serverSock.close(); // no longer needed // FIXME: research when it's okay to close a socket
                } else {
                    //isListener = true; // TODO: pretty sure this is deprecated with listenerLocator
                    // listenerSocket = serverSock; TODO: pretty sure this is deprecated with listenerLocator
                    //isFirstUser = true;
                }
            } else { // become the client
                System.out.println("\tConnecting as client on address "+address+", port "+portNum);
                clientSock = new Socket(address, portNum);
            }
        } catch (IOException e) {
           // System.out.println("\tFailed to connect as a server, attempting to connect as client");
            System.err.println("\tFailed to connect to address+"+address+", port "+portNum+", please try again");
            e.printStackTrace(); // TODO: maybe not needed
            System.exit(1); // TODO: maybe not needed
        }
//        if (serverSock == null) { // must become client to connect
//            try {
//                System.out.println("\tConnecting as client on port "+portNum);
//                String host = InetAddress.getLocalHost().getHostAddress();
//                clientSock = new Socket(host, portNum);
//            } catch (IOException e) {
//                System.err.println("\tFailed to connect as peer to port "+portNum+", please try again");
//                e.printStackTrace(); // TODO: maybe not needed
//                System.exit(1); // TODO: maybe not needed
//            }
//        }
        System.out.println("\tFINISHED Network : establishSockets("+address+", "+portNum+")");
        return clientSock;
    }

    // attempt to establish the network streams
    private void establishStreams(Socket sock, boolean in, boolean out) {
        System.out.println("\tSTARTING Network : establishStreams(in = "+in+", out = "+out+")");
        if (out) { netOut = createOutStream(sock); } // out before in
        if (in) { netIn = createInStream(sock); } // or else it will hang
        System.out.println("\tFINISHED Network : establishStreams(in = "+in+", out = "+out+")");
    }

    private static ObjectOutputStream createOutStream(Socket sock) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(sock.getOutputStream());
        } catch (IOException e) {
            System.err.println("ERROR @ Network : createOutStream("+sock+")");
            e.printStackTrace(); // TODO: maybe not needed
            System.exit(2); // TODO: maybe not needed
        }
        return out;
    }

    private static ObjectInputStream createInStream(Socket sock) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(sock.getInputStream());
        } catch (IOException e) {
            System.err.println("ERROR @ Network : createInStream("+sock+")");
            e.printStackTrace(); // TODO: maybe not needed
            System.exit(2); // TODO: maybe not needed
        }
        return in;
    }


    ///////////////////////////////////
    //        Listener methods       //
    ///////////////////////////////////
    /**
     * Request listener status from listenerLocator, then either become the listener or get the listener's port
     */
    private void findListener(String locatorAddressString, int locatorPortNum) {
        System.out.println("Contacting listener locator...");
        Socket locatorSocket = establishSockets(locatorAddressString, locatorPortNum, null);
        System.out.println("Opening output stream to locator...");
        ObjectOutputStream locatorOut = createOutStream(locatorSocket);
        System.out.println("Opening input stream to locator...");
        ObjectInputStream locatorIn = createInStream(locatorSocket);
        try {
            locatorOut.writeBoolean(false); // not trying to become the new listener
            locatorOut.flush();
            System.out.println("Requesting listener status from locator...");
            isListener = !locatorIn.readBoolean(); // locator sends listenerExists
            System.out.println("isListener = "+isListener);
            if (!isListener) { // listener exists
                System.out.print("Listener is on IP address ");
                listenerAddress = locatorIn.readUTF();
                System.out.print(listenerAddress);
                System.out.print(", on port ");
                listenerPort = locatorIn.readInt();
                System.out.println(listenerPort);
            } else { // listener does not exist, so create a port to listen on
                System.out.print("Becoming the listener ");
                listenerAddress = myAddress;
                System.out.print("on IP address "+listenerAddress);
                locatorOut.writeUTF(listenerAddress);
                locatorOut.flush();
                listenerPort = generatePortNumber(); // make a port number
                System.out.print(", on port ");
                locatorOut.writeInt(listenerPort); // send it back to the locator
                locatorOut.flush();
                System.out.println(listenerPort);
            }
        } catch (IOException e) {
            System.err.println("ERROR @ Network : findListener() -> IOException");
            e.printStackTrace();
            System.exit(12);
        }
        System.out.println();
    }

    @SuppressWarnings("unchecked")
    private void contactListener() {
        System.out.println("Contacting listener...");
        LinkedList<String> allUsers = null;
        Socket listener = establishSockets(listenerAddress, listenerPort, null);
        ObjectOutputStream out = createOutStream(listener);
        ObjectInputStream in = createInStream(listener);
        try {
            //linkPort = in.readInt(); // get the port of the node for this networks outStream
            System.out.println("Receiving list of online users...");
            allUsers = (LinkedList<String>) in.readObject(); // get the list
        } catch (IOException e) {
            System.err.println("ERROR @ Network : login(), LinkedList<String> allUsers = (LinkedList<String>)netIn.readObject() -> IOException");
            e.printStackTrace(); // TODO: needed?
            System.exit(4); // TODO: needed?
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR @ Network : login(), LinkedList<String> allUsers = (LinkedList<String>)netIn.readObject() -> ClassNotFoundException");
            e.printStackTrace(); // TODO: needed?
            System.exit(5); // TODO: needed?
        }
        // get the user's choice of username
        System.out.print("Please enter a unique username: ");
        Scanner keyboard = new Scanner(System.in);
        boolean valid = false;
        while (!valid) {
            username = keyboard.nextLine();
            if (!allUsers.contains(username)) { valid = true; }
            else { System.out.println(username+" is already taken, please try again"); }
        }
        allUsers.addLast(username); // add the name to the list
        try {
            System.out.println("Sending chosen username to listener");
            out.writeUTF(username); // send name back
            out.flush();

            // join the loop
            System.out.println("Joining the loop");
            ObjectOutputStream forwardPortStream = out; // FIXME: I probably don't need this line, just have them always establishSockets(myPort, out)
//            if (in.readBoolean()) { // FIXME: does this logic even make sense???
//                System.out.println("Forwarding the port");
//                forwardPortStream = out;
//            } else {
//                System.out.println("Not forwarding the port");
//            }
            System.out.println("\tEstablishing link with next user");
            linkPort = generatePortNumber();
            //linkSocket = establishSockets(myPort, forwardPortStream); // start listening for next link node to connect
            linkSocket = establishSockets(myAddress, linkPort, forwardPortStream); // start listening for next link node to connect
            // code was here
            System.out.println("\tCreating netOut to send to next user");
            establishStreams(linkSocket, false, true); // update netOut

            // FIXME: strangely this doesn't work, receives wrong int value???
//            System.out.println("Establishing back with listener");
//            backPort = in.readInt();
//            System.out.println("Received backPort : "+backPort);
//            backSocket = establishSockets(backPort, null);
//            System.out.println("Creating netIn to receive from listener");
//            establishStreams(backSocket, true, false);
            System.out.println("\n\tEstablishing back with listener");
            backPort = generatePortNumber();
            //System.out.println("Received backPort : "+backPort);
            backSocket = establishSockets(myAddress, backPort, forwardPortStream);
            System.out.println("\tCreating netIn to receive from listener");
            establishStreams(backSocket, true, false);

            System.out.println("Streams established");
            // in.readBoolean(); // wait for confirmation that next user is waiting to update link
        } catch (IOException e) {
            System.err.println("ERROR @ Network : login(), netOut.writeUTF(name) -> IOException");
            e.printStackTrace(); // TODO: needed?
            System.exit(8); // TODO: needed?
        }

        // now attach netIn to the listener
        //netIn = in; // FIXME: maybe need a separate socket and whole procedure for this

        //linkSocket = establishSockets(linkPort); // establish a socket with it
        //establishStreams(linkSocket, true, true); // establish streams with it
        System.out.println("Telling user to update allUsernames");
        // FIXME: it's probably the new user's responsibility to share the new allUsers with all the other users, maybe including the listener that added this new user
        try {
            netOut.writeUTF("newUser");
            netOut.writeUTF(username);
            netOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        user.allUsersUpdate(allUsers); // tell the user that another peer has joined the chat
        becomeNextListener(locatorPort); // assume the responsibility FIXME: this is temporarily commented out, pass on listener responsibility
        System.out.println("Finished contacting listener\n");
    }

    private Thread becomeFirstListener() {
        System.out.println("Becoming the first listener");
        Thread listener = null;
        try {
            listenerSocket = new ServerSocket(listenerPort);
            LinkedList<String> allUsers = new LinkedList<>(); // make list
            // get the user's choice of username
            System.out.print("You are the first user, please enter a username: ");
            Scanner keyboard = new Scanner(System.in);
            username = keyboard.nextLine();
            allUsers.addLast(username); // add this user's own name
            listener = new Thread(()->listen(listenerSocket, allUsers, this));
            System.out.println();
            listener.start();
        } catch (IOException e) {
            System.err.println("ERROR @ Network : becomeListener using address "+myAddress+", port "+listenerPort);
            e.printStackTrace();
            System.exit(13);
        }
        return listener;
    }

    private void becomeNextListener(int locatorPortNum) {
        System.out.println("Beginning the process of becoming the next listener...");
        Socket locatorSocket = establishSockets(locatorAddress, locatorPortNum, null);
        System.out.println("Opening output stream to locator...");
        ObjectOutputStream locatorOut = createOutStream(locatorSocket);
        System.out.println("Opening input stream to locator...");
        ObjectInputStream locatorIn = createInStream(locatorSocket); // TODO: needed?
        try {
            locatorOut.writeBoolean(true); // signal that I'm becoming the new listener
            locatorOut.flush();
            System.out.print("Will be listening on address ");
            listenerAddress = myAddress;
            System.out.print(", on port ");
            listenerPort = generatePortNumber(); // make a port number
            System.out.println(listenerPort);
            System.out.println("Creating server socket for listening");
            listenerSocket = new ServerSocket(listenerPort); // make the socket for it
            System.out.println("Sending the locator the new address +"+listenerAddress);
            locatorOut.writeUTF(listenerAddress);
            locatorOut.flush();
            System.out.println("Sending the locator the new port "+listenerPort);
            locatorOut.writeInt(listenerPort); // send it back to the locator
            locatorOut.flush();
            System.out.println("Now listening\n");
            new Thread(()->listen(listenerSocket, null,this)).start();
        } catch (IOException e) {
            System.err.println("ERROR @ Network : findListener() -> IOException");
            e.printStackTrace();
            System.exit(12);
        }
    }

    private static void listen(ServerSocket sock, LinkedList<String> allUsernames, Network reference) {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        if (allUsernames == null) allUsernames = reference.user.getAllUsernames();
        //while (true) { // FIXME: shouldn't loop, should pass on the responsibility
            try {
                System.out.println("Listening for new users...");
                Socket newUser = sock.accept(); // TODO: add timeout and interrupt like in ListenerLocator
                System.out.println("New user connecting...");

                out = createOutStream(newUser); // FIXME: this was the old code
                // FIXME: this is the new code
                //reference.establishStreams(newUser, false, true);
                //out = reference.netOut;
                // FIXME: this is the new code

                in = createInStream(newUser);
                System.out.println("Sending new user all online users' names");
                out.writeObject(allUsernames); // send list
                out.flush();
            } catch (IOException e) {
                System.err.println("ERROR @ Network : login(), netOut.writeObject(allUsers) failed");
                e.printStackTrace(); // TODO: needed?
                System.exit(3); // TODO: needed?
            }
            try {
                System.out.println("Getting new user's name");
                allUsernames.addLast(in.readUTF()); // get their name

                // now grow the loop
                System.out.println("Growing the loop...");
                if (reference.netIn == null && reference.netOut == null) { // this user is the only user or first user, new user should become my netIn and netOut
                    System.out.println("New user should not forward port number");
                    out.writeBoolean(false); // tell the new user not to use the optional stream nor forward their port num to a waiting node
                    out.flush();

                    System.out.println("Establishing back with new user");
                    reference.backAddress = in.readUTF();
                    reference.backPort = in.readInt();
                    reference.backSocket = reference.establishSockets(reference.backAddress, reference.backPort, null);
                    System.out.println("Creating netIn to receive from new user");
                    reference.establishStreams(reference.backSocket, true, false);

                    // FIXME: strangely this doesn't work, new user receives wrong int value???
//                    System.out.println("Establishing link with new user");
//                    reference.linkPort = reference.generatePortNumber();
//                    reference.linkSocket = reference.establishSockets(reference.linkPort, out); // open socket and connect as client to new user
//                    System.out.println("Creating netOut to send to new user");
//                    reference.establishStreams(reference.linkSocket, false, true); // establish the stream with the new user
                    System.out.println("Establishing link with new user");
                    reference.linkAddress = in.readUTF();
                    reference.linkPort = in.readInt();
                    reference.linkSocket = reference.establishSockets(reference.linkAddress, reference.linkPort, null); // open socket and connect as client to new user
                    System.out.println("Creating netOut to send to new user");
                    reference.establishStreams(reference.linkSocket, false, true); // establish the stream with the new user

                    System.out.println("Streams established");
                } else {
                    // help new user setup link
                    System.out.println("Helping new user establish their link...");
                    System.out.println("New user should forward port number");
                    out.writeBoolean(true); // tell the new user not to use the optional stream and forward their port num to a waiting node
                    out.flush();
                    System.out.println("Signaling link node to update netIn");
                    reference.netOut.writeUTF("updateNetIn"); // signal next link node to update netIn
                    reference.netOut.flush();
                    //out.writeInt(reference.linkPort); // wat???
                    System.out.println("Getting IP address from new user and sending it to link");
                    reference.netOut.writeUTF(in.readUTF());
                    System.out.println("Getting port number from new user and sending it to link");
                    reference.netOut.writeInt(in.readInt()); // send new user's port number for next node to update netIn
                    reference.netOut.flush();

                    // now set netOut to send to new user, and have new user netIn receive from me
                    System.out.println("Helping new user connect their back to my link...");
                    reference.linkAddress = in.readUTF();
                    reference.linkPort = in.readInt();
                    reference.linkSocket = reference.establishSockets(reference.linkAddress, reference.linkPort, null);
                    reference.establishStreams(reference.linkSocket, false, true);
                    System.out.println("Finished helping new user join loop");
                }

                // now attach netOut to the new user

                //reference.netOut = out; // FIXME: maybe need a separate socket and a whole procedure for this... // FIXME: this was the old code

                reference.finishListening(allUsernames); // FIXME: uncomment later
                System.out.println("Telling user to update allUsernames");
                //reference.user.allUsersUpdate(allUsernames); // FIXME: this is handled in finishListening
            } catch (IOException e) {
                System.err.println("ERROR @ Network : login(), allUsers.addLast((String)netIn.readObject()) -> IOException");
                e.printStackTrace(); // TODO: needed?
                System.exit(6); // TODO: needed?
            }
        //} // FIXME: don't loop, pass on listener role
    }

    private void finishListening(LinkedList<String> allUsernames) {
        // tell the listenerLocator you're no longer the listener // FIXME: do I have to?
        isListener = false;
        this.user.allUsersUpdate(allUsernames);
        System.out.println("Finished listening\n");
    }

    ///////////////////////////////////
    //        Network methods        //
    ///////////////////////////////////
    private int generatePortNumber() {
        return ThreadLocalRandom.current().nextInt(30000, 39999+1);
    }

    // FIXME: maybe after an update, return from method and call again?
    private static void receive(Network reference) {
        ObjectInputStream in = reference.netIn; // used to store oldNet in stream while updating it
        ObjectOutputStream out = reference.netOut; // TODO: may need to to the above
        while (true) { // keep listening for stuff
            try {
                System.out.printf("\t___backPort: %d, linkPort: %d___\n", reference.backPort, reference.linkPort);
                String input = in.readUTF(); // read from input stream
                if (input.equals("updateNetIn")) { // need to update input stream
                    reference.backAddress = in.readUTF();
                    reference.backPort = in.readInt();
                    reference.backSocket = reference.establishSockets(reference.backAddress, reference.backPort, null); // read new input port and make a socket
                    reference.establishStreams(reference.backSocket, true, false); // update input stream
                    in = reference.netIn; // update local var to input stream
                } else if (input.equals("message")){ // passing a message
                    Message message = (Message)in.readObject(); // get the message
                    if (message.isRecipient(reference.username)) { // check if it's addressed here
                        System.out.println("\n-----------FORMATTING-----------");
                        System.out.println(message.format()); // if so, print out its contents
                        System.out.println("-----------FORMATTING-----------\n");
                    } else { // otherwise pass it on
                        reference.netOut.writeUTF("message"); // let the next node know what's coming
                        reference.netOut.writeObject(message); // then pass the message along
                        reference.netOut.flush();
                    }
                } else if (input.equals("logout")) {
                    reference.becomeNextListener(reference.locatorPort); // assume role of new listener
                    String backBackAddress = reference.netIn.readUTF(); // get back address of my back node
                    int backBackPort = reference.netIn.readInt(); // read in the identifying port they were on
                    reference.netOut.writeUTF("updateNetOut"+backBackAddress+backBackPort); // signal them to update
                    reference.netOut.flush();
                    reference.backAddress = reference.netIn.readUTF(); // get the new address to connect to
                    reference.backPort = reference.netIn.readInt(); // get the new port to connect to
                    reference.backSocket = reference.establishSockets(reference.backAddress, reference.backPort, null);
                    reference.establishStreams(reference.backSocket, true, false);
                } else if (input.equals("updateNetOut"+reference.myAddress+reference.linkPort)) {
                    int newLinkPort = reference.generatePortNumber();
                    ServerSocket newLinkSocket = new ServerSocket(newLinkPort);
                    reference.netOut.writeUTF(reference.myAddress);
                    reference.netOut.writeInt(newLinkPort);
                    reference.netOut.flush();
                    reference.linkSocket = newLinkSocket.accept();
                    reference.netOut.writeUTF("confirmed"); // let old link log out
                    // update information
                    reference.linkPort = newLinkPort;
                    reference.establishStreams(reference.linkSocket, false, true);
                } else if (input.equals("newUser")) {
                    String newName = reference.netIn.readUTF();
                    if (!newName.equals(reference.username)) {
                        reference.user.addNewUser(newName);
                        reference.netOut.writeUTF("newUser");
                        reference.netOut.writeUTF(newName);
                        reference.netOut.flush();
                    }
                }
            } catch (EOFException e) {
                //System.out.println("Finished reading a (network) message");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    void send(String body, String recipient, Network reference) {
        Message message = new Message(reference.username, recipient);
        message.setText(body);
        ObjectOutputStream out = reference.netOut;
        try {
            out.writeUTF("message");
            out.flush();
            message.setTimestamp(System.currentTimeMillis());
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

//    /**
//     * Connect the user to the chat
//     */
//    @SuppressWarnings("unchecked")
//    void login() {
//        Socket locatorSocket = establishSockets(locatorPort); // get network sockets for chat
//        establishStreams(linkSocket, true, true); // get network streams for chat
//        // handle user names
//        if (isFirstUser) { // make the list and send it, then get their name
//            LinkedList<String> allUsers = new LinkedList<>(); // make list
//            allUsers.addLast(firstUserName); // add this user's own name
//            try {
//                netOut.writeObject(allUsers); // send list
//            } catch (IOException e) {
//                System.err.println("ERROR @ Network : login(), netOut.writeObject(allUsers) failed");
//                e.printStackTrace(); // TODO: needed?
//                System.exit(3); // TODO: needed?
//            }
//            try {
//                allUsers.addLast((String)netIn.readObject()); // get their name
//            } catch (IOException e) {
//                System.err.println("ERROR @ Network : login(), allUsers.addLast((String)netIn.readObject()) -> IOException");
//                e.printStackTrace(); // TODO: needed?
//                System.exit(6); // TODO: needed?
//            } catch (ClassNotFoundException e) {
//                System.err.println("ERROR @ Network : login(), allUsers.addLast((String)netIn.readObject()) -> ClassNotFoundException");
//                e.printStackTrace(); // TODO: needed?
//                System.exit(7); // TODO: needed?
//            }
//        } else { // get the list, make unique name, add it to list, send name back
//            LinkedList<String> allUsers = null;
//            try {
//                allUsers = (LinkedList<String>) netIn.readObject(); // get the list
//            } catch (IOException e) {
//                System.err.println("ERROR @ Network : login(), LinkedList<String> allUsers = (LinkedList<String>)netIn.readObject() -> IOException");
//                e.printStackTrace(); // TODO: needed?
//                System.exit(4); // TODO: needed?
//            } catch (ClassNotFoundException e) {
//                System.err.println("ERROR @ Network : login(), LinkedList<String> allUsers = (LinkedList<String>)netIn.readObject() -> ClassNotFoundException");
//                e.printStackTrace(); // TODO: needed?
//                System.exit(5); // TODO: needed?
//            }
//            // get the user's choice of username
//            System.out.print("Please enter a unique username: ");
//            Scanner keyboard = new Scanner(System.in);
//            String name = null;
//            boolean valid = false;
//            while (!valid) {
//                name = keyboard.nextLine();
//                if (allUsers.contains(name)) { valid = true; }
//                else { System.out.println(name+" is already taken, please try again"); }
//            }
//            allUsers.addLast(name); // add the name to the list
//            try {
//                netOut.writeChars(name); // send name back
//            } catch (IOException e) {
//                System.err.println("ERROR @ Network : login(), netOut.writeChars(name) -> IOException");
//                e.printStackTrace(); // TODO: needed?
//                System.exit(8); // TODO: needed?
//            }
//        }
//    }