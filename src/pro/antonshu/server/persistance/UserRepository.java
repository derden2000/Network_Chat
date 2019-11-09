package pro.antonshu.server.persistance;

import org.apache.log4j.Logger;
import pro.antonshu.server.User;
import pro.antonshu.server.annotations.AutoIncrement;
import pro.antonshu.server.annotations.PrimaryKey;
import pro.antonshu.server.annotations.Table;
import pro.antonshu.server.annotations.Unique;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository<T> {

    private static Connection conn = null;

    private static Class<?> clazz;

    private static Logger logger = Logger.getLogger(UserRepository.class.getName());

    public UserRepository(Connection conn, Class<?> clazz) {
        this.conn = conn;
        this.clazz = clazz;
        createTable(conn);
        /*PreparedStatement prepareStatement = null;
        try {
            prepareStatement = conn.prepareStatement("create table if not exists users (" +
                    "id int auto_increment primary key," +
                    " login varchar(25)," +
                    " password varchar(25)," +
                    " unique index uq_login(login));");
            prepareStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }*/
    }

    public static void insert(User user) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("insert into " + clazz.getAnnotation(Table.class).tableName()+"(");
        for (Field fld : clazz.getDeclaredFields()) {
            if (fld.isAnnotationPresent(AutoIncrement.class)) {
                continue;
            }
            if (fld.isAnnotationPresent(pro.antonshu.server.annotations.Field.class)) {
                pro.antonshu.server.annotations.Field fldAnnotation = fld.getAnnotation(pro.antonshu.server.annotations.Field.class);
                sb.append(fldAnnotation.name() + ", ");
            }
        }
        sb.deleteCharAt(sb.length()-1);
        sb.deleteCharAt(sb.length()-1);
        sb.append(") values (?, ?)");
        String request = sb.toString();

        /*try (Statement stmt = conn.createStatement()) {
            logger.info(request);
            stmt.execute(request);
        } catch (SQLException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.error(sw.toString());
        }*/

        logger.info(request);
        PreparedStatement prepareStatement = conn.prepareStatement(request/*"insert into userz(login, password) values (?, ?)"*/);
        prepareStatement.setString(1, user.getLogin());
        prepareStatement.setString(2, user.getPassword());
        prepareStatement.execute();
    }

    public static User findByLogin(String Login) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("select");
        for (Field fld : clazz.getDeclaredFields()) {
            if (!fld.isAnnotationPresent(pro.antonshu.server.annotations.Field.class)) {
                continue;
            }
            pro.antonshu.server.annotations.Field fldAnnotation = fld.getAnnotation(pro.antonshu.server.annotations.Field.class);
            sb.append(" " +fldAnnotation.name()+",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(" from " + clazz.getAnnotation(Table.class).tableName()+" where login = ?");
        String requestFromDB = sb.toString();
        logger.info(requestFromDB);

        try (PreparedStatement stmt = conn.prepareStatement(requestFromDB/*"select id, login, password from users where login = ?"*/)) {
            stmt.setString(1, Login);
            ResultSet rSet = stmt.executeQuery();

            if (rSet.next()) {
                    return new User(rSet.getInt(1), rSet.getString(2), rSet.getString(3));
            }
        }
        return new User(-1, "", "");
    }

    public List<User> getAllUsers() throws SQLException {
        ArrayList<User> result = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet resultSet = stmt.executeQuery("select * from " + clazz.getAnnotation(Table.class).tableName()/*users"*/)
        )
        {
            while (resultSet.next()) {
            result.add(new User(resultSet.getInt(1), resultSet.getString(2),resultSet.getString(3)));
            }
        }

        return result;
    }

    private void createTable(Connection conn) {
        StringBuilder sb = new StringBuilder();
        if (!clazz.isAnnotationPresent(Table.class)) {
            throw new IllegalStateException("No Table annotation");
        }
        String tableName = clazz.getAnnotation(Table.class).tableName();
        tableName = tableName.isEmpty() ? clazz.getSimpleName() : tableName;
        sb.append("create table if not exists " + tableName + "(");

        for (Field fld : clazz.getDeclaredFields()) {
            if (!fld.isAnnotationPresent(pro.antonshu.server.annotations.Field.class)) {
                continue;
            }
            pro.antonshu.server.annotations.Field fldAnnotation = fld.getAnnotation(pro.antonshu.server.annotations.Field.class);
            String fieldName = fldAnnotation.name().isEmpty() ? fld.getName() : fldAnnotation.name();
            String fieldType = null;
            Class<?> type = fld.getType();
            if (type == int.class) {
                fieldType = "int";
            } else if (type == String.class) {
                fieldType = "varchar(25)";
            }
            boolean isPrimaryKey = fld.isAnnotationPresent(PrimaryKey.class);
            boolean isAutoIncrement = fld.isAnnotationPresent(AutoIncrement.class);

            sb.append(fieldName + " " + fieldType + (isAutoIncrement ? " auto_increment" : "")+ (isPrimaryKey ? " primary key" : "") + ",");
        }

        for (Field fld : clazz.getDeclaredFields()) {
            if (!fld.isAnnotationPresent(Unique.class)) {
                continue;
            }
            sb.append("unique index uq_" + fld.getName() + "(" + fld.getName() + "),");

            sb.deleteCharAt(sb.length() - 1);
            sb.append(");");
            String request = sb.toString();

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(request);
                logger.info(request);
            } catch (SQLException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                logger.error(sw.toString());
            }
        }
    }
}
