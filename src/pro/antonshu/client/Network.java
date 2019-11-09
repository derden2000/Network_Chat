package pro.antonshu.client;

import pro.antonshu.server.StringHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static pro.antonshu.client.MessagePatterns.*;

public class Network {

    public Socket socket;
    public DataInputStream in;
    public DataOutputStream out;

    private String hostName;
    private int port;
    private MessageReciever messageReciever;

    private String login;

    private Thread receiverThread;

    public Network(String hostName, int port, MessageReciever messageReciever) {
        this.hostName = hostName;
        this.port = port;
        this.messageReciever = messageReciever;

        this.receiverThread = new Thread(new Runnable() { //входящий поток клиента
            @Override
            public void run() {
                try {
                    messageReciever.initClientsOnline(requestConnectedUserList());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String text = in.readUTF();
                        System.out.println("New message: " + text);
                        TextMessage textMessage = StringHandler.parseMessage(text);
                        if (textMessage!=null){
                            messageReciever.submitMessage(textMessage);
                            continue;
                        }

                        System.out.println("Connection message " + text);
                        String login = StringHandler.parseConnectedMessage(text);
                        if (login != null) {
                            messageReciever.userConnected(login);
                            continue;
                        }

                        System.out.println("Disconnection message " + text);
                        String loginDisconected = StringHandler.parseDisconnectedMessage(text);
                        if (loginDisconected != null) {
                            messageReciever.userDisconnected(loginDisconected);
                            continue;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        if (socket.isClosed()) {
                            break;
                        }
                    }
                }
            }
        });
    }


    public void authorize(String login, String password) throws IOException, AuthException {
        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        sendMessage(String.format(AUTH_PATTERN, login, password));
        String response = in.readUTF();
        if (response.equals(AUTH_SUCCESS_RESPONSE)) {
            this.login = login;
            receiverThread.start();
        } else {
            throw new AuthException();
        }
    }

    public void register (String login, String password) throws IOException {

        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        sendMessage(String.format(REG_PATTERN, login, password));
    }

    public void sendTextMessage(TextMessage message) {
        sendMessage(String.format(MESSAGE_SEND_PATTERN, message.getUserFrom(), message.getUserTo(), message.getText()));
    }

    private void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLogin() {
        return login;
    }

    public List<String> requestConnectedUserList() throws IOException {
        List<String> connectedUserList = new ArrayList<>();
        sendMessage(CONNECTED_USERS_REQUEST);
        String response = in.readUTF();
        connectedUserList = StringHandler.parseConnectedUsers(response);
        return connectedUserList;
    }

    public void close() {
        this.receiverThread.interrupt();
        sendMessage(String.format(DISCONNECT_SEND, login));
    }
}
