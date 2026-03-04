package revpay.DBconfig;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DButil {

    private static final Logger logger =
            LogManager.getLogger(DButil.class);

    private static final String URL = "jdbc:mysql://localhost:3306/revpay";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static Connection getConnection() {
        try {
            Connection con =
                    DriverManager.getConnection(URL, USER, PASSWORD);

            logger.info("Database connection established");
            return con;

        } catch (Exception e) {
            logger.error("Database connection failed", e);
            return null;
        }
    }
}


