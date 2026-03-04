package revpay.DAO;

import revpay.DBconfig.DButil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDAO {

    private static final Logger logger =
            LogManager.getLogger(UserDAO.class);

    public String getPasswordHash(String email) {
        try {
            Connection con = DButil.getConnection();
            PreparedStatement ps =
                    con.prepareStatement(
                            "SELECT password_hash FROM app_users WHERE email=?");
            ps.setString(1, email);

            logger.info("Fetching password hash for email {}", email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("password_hash");
            }

        } catch (Exception e) {
            logger.error("Failed to fetch password hash for email {}", email, e);
        }
        return null;
    }

    public String getUserType(String email) {
        try {
            Connection con = DButil.getConnection();
            PreparedStatement ps =
                    con.prepareStatement(
                            "SELECT user_type FROM app_users WHERE email=?");
            ps.setString(1, email);

            logger.info("Fetching user type for email {}", email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("user_type");
            }

        } catch (Exception e) {
            logger.error("Failed to fetch user type for email {}", email, e);
        }
        return null;
    }

    public ResultSet getUserByEmailOrPhone(String input) {
        try {
            Connection con = DButil.getConnection();
            PreparedStatement ps =
                    con.prepareStatement(
                            "SELECT * FROM app_users WHERE email=? OR phone=?");
            ps.setString(1, input);
            ps.setString(2, input);

            logger.info("Fetching user by email/phone");
            return ps.executeQuery();

        } catch (Exception e) {
            logger.error("Failed to fetch user by email/phone", e);
            return null;
        }
    }
}
