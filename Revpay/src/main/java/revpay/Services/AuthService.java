package revpay.Services;

import revpay.DAO.UserDAO;
import revpay.DBconfig.DButil;
import revpay.Security.PasswordUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {

    private static final Logger logger =
            LogManager.getLogger(AuthService.class);

    private UserDAO userDAO = new UserDAO();
    private static final int MAX_FAILED_ATTEMPTS = 3;

    public int login(String loginInput, String password) {

        logger.info("Login attempt for input {}", loginInput);

        try {
            ResultSet rs = userDAO.getUserByEmailOrPhone(loginInput);

            if (rs == null || !rs.next()) {
                logger.warn("Login failed – user not found for input {}", loginInput);
                return -1;
            }

            int userId = rs.getInt("user_id");
            String passwordHash = rs.getString("password_hash");
            int failedAttempts = rs.getInt("failed_attempts");
            boolean locked = rs.getBoolean("locked");

            if (locked) {
                logger.warn("Login attempt on locked account | userId={}", userId);
                return -1;
            }

            if (!PasswordUtil.verifyPassword(password, passwordHash)) {

                failedAttempts++;
                updateFailedAttempts(userId, failedAttempts);

                if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                    lockAccount(userId);
                    logger.warn("Account locked due to multiple failures | userId={}", userId);
                } else {
                    logger.warn("Invalid password | userId={}", userId);
                }
                return -1;
            }

            resetFailedAttempts(userId);

            logger.info("Login successful | userId={}", userId);
            return userId;

        } catch (Exception e) {
            logger.error("Login error for input {}", loginInput, e);
            return -1;
        }
    }
    public String getUserType(int userId) {
        try {
            Connection con = DButil.getConnection();
            PreparedStatement ps =
                    con.prepareStatement(
                            "SELECT user_type FROM app_users WHERE user_id=?");
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                logger.info("Fetched user type for userId {}", userId);
                return rs.getString("user_type");
            }

        } catch (Exception e) {
            logger.error("Failed to fetch user type | userId={}", userId, e);
        }
        return "PERSONAL";
    }
    public void registerPersonal(String name, String email, String phone,
                                 String password, String pin,
                                 String securityQuestion, String securityAnswer) {

        logger.info("Registering PERSONAL user | email={}", email);

        registerUser(
                name, email, phone, password, pin,
                "PERSONAL", null, null, null,
                securityQuestion, securityAnswer
        );

        logger.info("PERSONAL user registered successfully | email={}", email);
    }
    public void registerBusiness(String name, String email, String phone,
                                 String password, String pin,
                                 String businessName, String taxId, String address,
                                 String securityQuestion, String securityAnswer) {

        logger.info("Registering BUSINESS user | email={}", email);

        registerUser(
                name, email, phone, password, pin,
                "BUSINESS", businessName, taxId, address,
                securityQuestion, securityAnswer
        );

        logger.info("BUSINESS user registered successfully | email={}", email);
    }
    private void registerUser(String name, String email, String phone,
                              String password, String pin,
                              String type,
                              String businessName, String taxId, String address,
                              String question, String answer) {

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "INSERT INTO app_users " +
                            "(full_name, email, phone, password_hash, transaction_pin, user_type, " +
                            "business_name, tax_id, address, security_question, security_answer, " +
                            "failed_attempts, locked) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,0,false)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, PasswordUtil.hashPassword(password));
            ps.setString(5, PasswordUtil.hashPassword(pin));
            ps.setString(6, type);
            ps.setString(7, businessName);
            ps.setString(8, taxId);
            ps.setString(9, address);
            ps.setString(10, question);
            ps.setString(11, answer);

            ps.executeUpdate();

        } catch (Exception e) {
            logger.error("User registration failed | email={}", email, e);
        }
    }
    public boolean recoverPassword(String email, String answer, String newPassword) {

        logger.info("Password recovery attempt | email={}", email);

        try {
            Connection con = DButil.getConnection();
            PreparedStatement ps =
                    con.prepareStatement(
                            "UPDATE app_users SET password_hash=? " +
                                    "WHERE email=? AND security_answer=?");

            ps.setString(1, PasswordUtil.hashPassword(newPassword));
            ps.setString(2, email);
            ps.setString(3, answer);

            boolean updated = ps.executeUpdate() > 0;

            if (updated) {
                logger.info("Password updated successfully | email={}", email);
            } else {
                logger.warn("Password recovery failed | email={}", email);
            }

            return updated;

        } catch (Exception e) {
            logger.error("Password recovery error | email={}", email, e);
            return false;
        }
    }
    private void lockAccount(int userId) throws Exception {
        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "UPDATE app_users SET locked=true WHERE user_id=?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    private void updateFailedAttempts(int userId, int attempts) throws Exception {
        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "UPDATE app_users SET failed_attempts=? WHERE user_id=?");
        ps.setInt(1, attempts);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    private void resetFailedAttempts(int userId) throws Exception {
        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "UPDATE app_users SET failed_attempts=0 WHERE user_id=?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }
}
