package pro.antonshu.server;

import pro.antonshu.server.annotations.*;

@Table(tableName = "USERS")
public class User {

    @AutoIncrement()
    @PrimaryKey()
    @Field(name = "id")
    private int id;

    @Unique()
    @Field(name = "login")
    private String login;

    @Field(name = "password")
    private String password;

    public User(int id, String login, String password) {
        this.id = id;
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "User{" +
                "login='" + login + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
