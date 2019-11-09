package pro.antonshu.client.swing;

import pro.antonshu.client.MessageBackup;
import pro.antonshu.client.MessageReciever;
import pro.antonshu.client.Network;
import pro.antonshu.client.TextMessage;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;


public class MainWindow extends JFrame implements MessageReciever {

    private final JList<TextMessage> messageList;

    private final DefaultListModel<TextMessage> messageListModel;

    private final TextMessageCellRenderer messageCellRenderer;

    private final JScrollPane scroll;

    private final JPanel sendMessagePanel;

    private final JButton sendButton;

    private final JTextField messageField;

    private final Network network;

    private final MessageBackup messageBackup;

    private final JList<String> userList;

    private final DefaultListModel<String> userListModel;


    public MainWindow() {
        setTitle(String.format("Сетевой чат"));
        setBounds(200,200, 500, 500);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        messageList = new JList<>();
        messageListModel = new DefaultListModel<>();
        messageCellRenderer = new TextMessageCellRenderer();
        messageList.setModel(messageListModel);
        messageList.setCellRenderer(messageCellRenderer);

        scroll = new JScrollPane(messageList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);

        sendMessagePanel = new JPanel();
        sendMessagePanel.setLayout(new BorderLayout());
        sendButton = new JButton("Отправить");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = messageField.getText();
                if (text != null && !text.trim().isEmpty()) {
                    TextMessage msg = new TextMessage(network.getLogin(), userList.getSelectedValue(), text);
                    messageListModel.add(messageListModel.size(), msg);
                    messageList.ensureIndexIsVisible(messageListModel.size() - 1);
                    messageField.setText(null);
                    network.sendTextMessage(msg);
                    messageBackup.addToList(msg);
                } else {
                    JOptionPane.showMessageDialog(MainWindow.this,
                            "Сообщение пустое. Введите новое сообщение",
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        sendMessagePanel.add(sendButton, BorderLayout.EAST);
        messageField = new JTextField();
        messageField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String text = messageField.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        TextMessage msg = new TextMessage(network.getLogin(), userList.getSelectedValue(), text);
                        messageListModel.add(messageListModel.size(), msg);
                        messageList.ensureIndexIsVisible(messageListModel.size() - 1);
                        messageField.setText(null);
                        network.sendTextMessage(msg);
                        messageBackup.addToList(msg);
                    } else {
                        JOptionPane.showMessageDialog(MainWindow.this,
                                "Сообщение пустое. Введите новое сообщение",
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        sendMessagePanel.add(messageField, BorderLayout.CENTER);

        userList = new JList<>();
        userListModel = new DefaultListModel<>();
        userList.setModel(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Object element = userList.getSelectedValue();
            }
        });
        userList.setPreferredSize(new Dimension(100, 0));
        add(userList, BorderLayout.WEST);

        add(sendMessagePanel, BorderLayout.SOUTH);
        setVisible(true);

        this.network = new Network("localhost", 7777, this);

        ChooseDialog choose = new ChooseDialog(this);
        choose.setVisible(true);

        if (!choose.getIsRegistered()) {
            RegisterDialog regDialog = new RegisterDialog(this, network);
            regDialog.setVisible(true);

            if (!regDialog.isRegistered()) {
                System.exit(0);
            }
        } else {
            LoginDialog loginDialog = new LoginDialog(this, network);
            loginDialog.setVisible(true);

            if (!loginDialog.isConnected()) {
                System.exit(0);
            }
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (network != null) {
                    network.close();
                }
                try {
                    messageBackup.writeToFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                super.windowClosing(e);
            }
        });

        setTitle("Сетевой чат. Пользователь " + network.getLogin());

        messageBackup = new MessageBackup(Paths.get(String.format("%s_message_backup.dat", network.getLogin())));
        initMessageArchive();

    }

    private void initMessageArchive() {
        if (messageBackup.getMessageList().size()!=0) {
            for (int i = 0; i < messageBackup.getMessageList().size(); i++) {
                messageListModel.add(messageListModel.size(), (TextMessage) messageBackup.getMessageList().get(i));
                messageList.ensureIndexIsVisible(messageListModel.size() - 1);
            }
        }
    }

    @Override
    public void submitMessage(TextMessage message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                messageListModel.add(messageListModel.size(), message);
                messageList.ensureIndexIsVisible(messageListModel.size() - 1);
                messageBackup.addToList(message);
            }
        });
    }

    @Override
    public void userConnected(String login) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int ix = userListModel.indexOf(login);
                if (ix == -1) {
                    userListModel.add(userListModel.size(), login.toLowerCase());
                }
            }
        });
    }

    @Override
    public void userDisconnected(String login) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int ix = userListModel.indexOf(login);
                if (ix >= 0) {
                    userListModel.remove(ix);
                }
            }
        });
    }

    @Override
    public void initClientsOnline(List<String> list) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (list.size()>0) {
                    for (String user : list) {
                        userListModel.addElement(user.toLowerCase());
                    }
                }
            }
        });
    }
}