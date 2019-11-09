package pro.antonshu.client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class MessageBackup<T extends TextMessage> implements Serializable {

    private ArrayList<T> messageList;

    private transient Path backUpFile; //не вижу смысла сохранять это поле при сериализации

    public MessageBackup(Path path) {
        if (Files.exists(path)) {
            backUpFile = path;
            messageList = readData(path); //присваиваю только List, потому что поле backUpFile не сериализуется
        } else {
            backUpFile = Paths.get(String.valueOf(path));
            messageList = new ArrayList<>();
        }
    }

    public MessageBackup() {
        messageList = new ArrayList<>();
    }

    public ArrayList<T> getMessageList() {
        return messageList;
    }

    private ArrayList<T> readData(Path file) {
        MessageBackup<T> res = new MessageBackup<T>();
        try (ObjectInputStream out = new ObjectInputStream(
                new BufferedInputStream(
                        new FileInputStream(String.valueOf(file))))) {
            res = (MessageBackup<T>) out.readObject();       //Unchecked Cast - можно ли решить???
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return res.getMessageList();
    }

    public void writeToFile() throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(String.valueOf(this.backUpFile))))) {
            out.writeObject(this);
        }
    }

    public void addToList (T textMessage) {
        if (messageList.size() == 100) {
            messageList.remove(0);
            messageList.add(textMessage);
        } else {
            messageList.add(textMessage);
        }
    }
}
