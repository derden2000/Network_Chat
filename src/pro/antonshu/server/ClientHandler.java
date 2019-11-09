package pro.antonshu.server;

import org.apache.log4j.Logger;
import pro.antonshu.client.TextMessage;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Future;

import static pro.antonshu.client.MessagePatterns.*;


public class ClientHandler {

    private final String login;
    private final Socket socket;
    private final DataInputStream inp;
    private final DataOutputStream out;
    private final Future handleThread;
    private ChatServer chatServer;
    private static Logger logger = Logger.getLogger(ClientHandler.class.getName());

    public ClientHandler(String login, Socket socket, ChatServer chatServer) throws IOException {
        this.login = login;
        this.socket = socket;
        this.inp = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.chatServer = chatServer;

        this.handleThread = chatServer.executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String msg = inp.readUTF();
                        logger.info(String.format("Message from user %s: %s", login, msg));//System.out.printf("Message from user %s: %s%n", login, msg);

                        String[] text = msg.split(" ");
                        if (text[0].equals(CONNECTED_USERS_REQUEST)) {
                            chatServer.serverSendConnectedUserList(login);
                        }
                        else if (text[0].equals(DISCONNECT)) {
                            chatServer.unsubscribe(login);
                        }
                        else {
                            TextMessage inText = StringHandler.parseMessage(msg);
                            if (inText.getUserTo() != null) {
                                chatServer.sendMessage(inText);
                            }
                        }
                    } catch (IOException e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        logger.error(sw.toString());//chatServer.logger.log(Level.SEVERE, "Ecveption: ", e)
                        break;
                    }
                }
            }
        });
    }

    public void sendMessage(TextMessage message) throws IOException {
        out.writeUTF(String.format(MESSAGE_SEND_PATTERN, message.getUserFrom(), message.getUserTo(), message.getText()));
    }

    public String getLogin() {
        return login;
    }

    public void sendConnectedMessage(String login) throws IOException {
        if (socket.isConnected()) {
            out.writeUTF(String.format(CONNECTED_SEND, login));
        }
    }

    public void sendConnectedUsersList(String toString) throws IOException {
        out.writeUTF(String.format(CONNECTED_USERS_LIST_SEND, toString));
    }

    public void sendDisconnectedLogin(String login) throws IOException {
        out.writeUTF(String.format(DISCONNECT_SEND, login));
    }
}
