package pro.antonshu.server;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import pro.antonshu.client.AuthException;
import pro.antonshu.client.TextMessage;
import pro.antonshu.server.auth.AuthService;
import pro.antonshu.server.auth.AuthServiceJdbcImpl;
import pro.antonshu.server.persistance.UserRepository;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pro.antonshu.client.MessagePatterns.AUTH_FAIL_RESPONSE;
import static pro.antonshu.client.MessagePatterns.AUTH_SUCCESS_RESPONSE;


public class ChatServer {

    private static Connection conn;

    private static UserRepository userRepository;

    private static AuthService authServiceJdbc;

    public static Map<String, ClientHandler> clientHandlerMap = Collections.synchronizedMap(new HashMap<>());

    static ExecutorService executorService;

    public volatile int numTasks;

    private static Logger logger = Logger.getLogger(ChatServer.class.getName());

    public static void main(String[] args) {

        //Log4j - 1.2.17
        PropertyConfigurator.configure("C:\\Users\\derde\\IdeaProjects\\Network Chat\\src\\resources\\log4j.properties");

        //JUL with file-parametrized properties
        /*try {
            LogManager.getLogManager().readConfiguration(ChatServer.class.
                    getResourceAsStream("jul.properties"));
        } catch (IOException e) {
            System.err.println("Could not setup logger configuration: " + e.toString());
        }*/

        //JUL with handmaded properties
        /*logger.setLevel(Level.ALL);
        logger.getParent().setLevel(Level.ALL);
        logger.getParent().getHandlers()[0].setLevel(Level.ALL);

        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler("log_file.log", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                final Date date = new Date();
                date.setTime(record.getMillis());
                if (record.getThrown() != null) {
                    return String.format("! %s %s %d %s %s%n", record.getLevel(), date,
                            record.getThreadID(), record.getMessage(), record.getThrown().getCause());
                } else {
                    return String.format("! %s %s %d %s%n", record.getLevel(), date, record.getThreadID(), record.getMessage());
                }
            }
        });
        logger.addHandler(fileHandler);*/

        try {
            conn = DriverManager.getConnection("jdbc:mysql://antonshu.pro:3306/network_chat_log" +
                            "?useUnicode=true" +
                            "&useJDBCCompliantTimezoneShift=true" +
                            "&useLegacyDatetimeCode=false" +
                            "&serverTimezone=UTC",
                    "server", "123");
            //Class clazz = User.class;
            userRepository = new UserRepository(conn, User.class);
            authServiceJdbc = new AuthServiceJdbcImpl(userRepository);
            executorService = Executors.newFixedThreadPool(2);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.error(sw.toString());//logger.log(Level.SEVERE, "Exception: ", e);
            return;
        }

        ChatServer chatServer = new ChatServer();
        chatServer.start(7777);
    }

    private void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server started!"); //System.out.println("Server started!");
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream inp = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                logger.info("New client connected!");//System.out.println("New client connected!");

                User user = null;
                try {
                    String authMessage = inp.readUTF();
                    if (StringHandler.isReg(authMessage)) {
                        UserRepository.insert(StringHandler.parseRegMessage(authMessage));
                    }
                    else {
                        user = checkAuthentication(authMessage);
                    }

                } catch (IOException | SQLException ex) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    logger.error(sw.toString());//logger.log(Level.SEVERE, "Exception :", ex);//ex.printStackTrace();
                } catch (AuthException ex) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    logger.error(sw.toString());//logger.log(Level.SEVERE, "Exception: ", ex);
                    out.writeUTF(AUTH_FAIL_RESPONSE);
                    out.flush();
                    socket.close();
                }

                if (user != null && authServiceJdbc.authUser(user) && numTasks<2) {
                    logger.info(String.format("User %s authorized successful!", user.getLogin()));//System.out.printf("User %s authorized successful!%n", user.getLogin());
                    subcribe(user.getLogin(), socket);
                    out.writeUTF(AUTH_SUCCESS_RESPONSE);
                    out.flush();
                } else {
                    if (user != null) {
                        logger.warn(String.format("Wrong authorization for user %s", user.getLogin()));//logger.warning(String.format("Wrong authorization for user %s", user.getLogin()));//System.out.printf("Wrong authorization for user %s%n", user.getLogin());
                    }
                    out.writeUTF(AUTH_FAIL_RESPONSE);
                    out.flush();
                    socket.close();
                }
            }
        } catch (IOException ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(sw.toString());//logger.log(Level.SEVERE, "Ecveption: ", ex);//ex.printStackTrace();
        }
    }

    private void subcribe(String login, Socket socket) throws IOException {
        numTasks++;
        if (!clientHandlerMap.containsKey(login)) {
            clientHandlerMap.put(login, new ClientHandler(login, socket, this));
            sendUserConnectedMessage(login);
        }
        else {
            sendMessage(new TextMessage("Server", login, "Пользователь уже зарегистрирован"));
        }
    }

    public void unsubscribe(String login) throws IOException {
        numTasks--;
        serverSendDisconnectLogin(login);
        clientHandlerMap.remove(login);
    }

    private void sendUserConnectedMessage(String login) throws IOException {
        for (ClientHandler clientHandler : clientHandlerMap.values()) {
            if (!clientHandler.getLogin().equals(login)) {
                logger.info(String.format("Sending connect notification to %s about %s", clientHandler.getLogin(), login));
                //System.out.printf("Sending connect notification to %s about %s%n", clientHandler.getLogin(), login);
                clientHandler.sendConnectedMessage(login);
            }
        }
    }


    private User checkAuthentication(String authMessage) throws AuthException, SQLException {
        String[] authParts = authMessage.split(" ");
        if (authParts.length != 3 || !authParts[0].equals("/auth")) {
            logger.warn(String.format("Incorrect authorization message %s", authMessage));//logger.warning(String.format("Incorrect authorization message %s%n", authMessage));
            //System.out.printf("Incorrect authorization message %s%n", authMessage);
            throw new AuthException();
        } else {
            return new User(-1, authParts[1], authParts[2]);
        }
    }

    public void sendMessage(TextMessage message) throws IOException {
        if (message.getUserTo().equals("All")){
            for (Map.Entry<String, ClientHandler> pair: clientHandlerMap.entrySet()) {
                if (!message.getUserFrom().equals(pair.getKey())) {
                    ClientHandler userToClientHandler = pair.getValue();
                    userToClientHandler.sendMessage(message);
                }
            }
        }
        else if (clientHandlerMap.containsKey(message.getUserTo())) {
            ClientHandler userToClientHandler = clientHandlerMap.get(message.getUserTo());
            userToClientHandler.sendMessage(message);
        }
        else {
            ClientHandler userToClientHandler = clientHandlerMap.get(message.getUserFrom());
            String error = String.format("Пользователя %s нет в сети", message.getUserTo());
            TextMessage messageError = new TextMessage("Server", message.getUserFrom(), error);
            userToClientHandler.sendMessage(messageError);
        }
    }

    public void serverSendConnectedUserList(String login) throws IOException {
        ClientHandler userToClientHandler = clientHandlerMap.get(login);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ClientHandler> pair: clientHandlerMap.entrySet()) {
            if (!login.equals(pair.getKey())) {
                String name = pair.getKey().toLowerCase();
                builder.append(name + " ");
            }
        }
        String sendList = builder.toString();
        userToClientHandler.sendConnectedUsersList(sendList);
    }

    public void serverSendDisconnectLogin(String login) throws IOException {
        for (Map.Entry<String, ClientHandler> pair: clientHandlerMap.entrySet()) {
            if (!login.equals(pair.getKey())) {
                ClientHandler userToClientHandler = pair.getValue();
                userToClientHandler.sendDisconnectedLogin(login);
            }
        }
    }
}
