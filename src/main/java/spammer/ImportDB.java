package spammer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 3. Загрузка базы спаммеров. [#279172]
 */
public class ImportDB {
    private Properties cfg;
    private String dump;

    public ImportDB(Properties cfg, String dump) {
        this.cfg = cfg;
        this.dump = dump;
    }

    /**
     * Читаем спикок пользователей.
     *
     * @return список.
     */
    public List<User> load() {
        List<User> users = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        try (BufferedReader rd = new BufferedReader(new FileReader(dump))) {
            rd.lines().forEach(lines::add);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String line : lines) {
            User user = new User(
                    line.split(";")[0], line.split(";")[1]
            );
            users.add(user);
        }
        return users;
    }

    /**
     * Записывем имя пользователя и его электронный адрес в базу данных.
     *
     * @param users список пользователей.
     */
    public void save(List<User> users) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            try (Connection cnt = DriverManager.getConnection(
                    cfg.getProperty("jdbc.url"),
                    cfg.getProperty("jdbc.username"),
                    cfg.getProperty("jdbc.password")
            )) {
                for (User user : users) {
                    try (PreparedStatement ps = cnt.prepareStatement("insert into users (name, email) values(?,?)")) {
                        ps.setString(1, user.name);
                        ps.setString(2, user.email);
                        ps.execute();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class User {
        String name;
        String email;

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }

    public static void main(String[] args) {
        Properties cfg = new Properties();
        try (FileInputStream in = new FileInputStream("./app.properties")) {
            cfg.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ImportDB db = new ImportDB(cfg, "./dump.txt");
        db.save(db.load());
    }
}
