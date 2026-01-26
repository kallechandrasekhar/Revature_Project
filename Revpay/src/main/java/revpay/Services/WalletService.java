package revpay.Services;

import revpay.DBconfig.DButil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class WalletService {

    private static final Logger logger =
            LogManager.getLogger(WalletService.class);

    private static final double LOW_BALANCE_LIMIT = 500.0;

    public void deposit(int userId, double amount) {

        logger.info("Deposit initiated | userId={} amount={}", userId, amount);

        if (amount <= 0) {
            logger.warn("Invalid deposit amount | userId={} amount={}", userId, amount);
            return;
        }

        try {
            Connection con = DButil.getConnection();

            //  TRYING TO UPDATE
            String updateSql =
                    "UPDATE user_wallet SET balance = balance + ? WHERE user_id=?";
            PreparedStatement updatePs = con.prepareStatement(updateSql);
            updatePs.setDouble(1, amount);
            updatePs.setInt(2, userId);

            int rows = updatePs.executeUpdate();

            // IF WALLET DOES NOT EXIST → CREATE IT
            if (rows == 0) {
                logger.info("Wallet not found, creating new wallet | userId={}", userId);

                String createSql =
                        "INSERT INTO user_wallet (user_id, balance) VALUES (?, ?)";
                PreparedStatement createPs = con.prepareStatement(createSql);
                createPs.setInt(1, userId);
                createPs.setDouble(2, amount);
                createPs.executeUpdate();
            }

            logger.info("Deposit successful | userId={} amount={}", userId, amount);

        } catch (Exception e) {
            logger.error("Deposit failed | userId={} amount={}", userId, amount, e);
        }
    }

    public void withdraw(int userId, double amount) {

        logger.info("Withdraw initiated | userId={} amount={}", userId, amount);

        if (amount <= 0) {
            logger.warn("Invalid withdrawal amount | userId={} amount={}", userId, amount);
            return;
        }

        try {
            Connection con = DButil.getConnection();

            String checkSql =
                    "SELECT balance FROM user_wallet WHERE user_id=?";
            PreparedStatement checkPs = con.prepareStatement(checkSql);
            checkPs.setInt(1, userId);

            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {
                logger.warn("Wallet not found | userId={}", userId);
                return;
            }

            double balance = rs.getDouble("balance");

            if (balance < amount) {
                logger.warn(
                        "Insufficient balance | userId={} balance={} requested={}",
                        userId, balance, amount
                );
                return;
            }

            String sql =
                    "UPDATE user_wallet SET balance = balance - ? WHERE user_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDouble(1, amount);
            ps.setInt(2, userId);
            ps.executeUpdate();

            double newBalance = balance - amount;

            logger.info(
                    "Withdrawal successful | userId={} amount={} remainingBalance={}",
                    userId, amount, newBalance
            );

            checkLowBalance(userId, newBalance);

        } catch (Exception e) {
            logger.error(
                    "Withdrawal failed | userId={} amount={}",
                    userId, amount, e
            );
        }
    }

    public void viewBalance(int userId) {

        logger.info("Fetching wallet balance | userId={}", userId);

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "SELECT balance FROM user_wallet WHERE user_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                logger.info(
                        "Wallet balance | userId={} balance={}",
                        userId, rs.getDouble("balance")
                );
            } else {
                logger.warn("Wallet not found | userId={}", userId);
            }

        } catch (Exception e) {
            logger.error("Failed to fetch wallet balance | userId={}", userId, e);
        }
    }

    private void checkLowBalance(int userId, double balance) {

        if (balance < LOW_BALANCE_LIMIT) {

            logger.warn(
                    "Low wallet balance detected | userId={} balance={}",
                    userId, balance
            );

            NotificationService notificationService = new NotificationService();
            notificationService.notifyUser(
                    userId,
                    "Low wallet balance: ₹" + balance
            );
        }
    }

    public void sendMoney(int fromUser, int toUser, double amount) throws Exception {

        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }

        Connection con = DButil.getConnection();

        String sql = "INSERT INTO payment_transactions " +
                "(sender_id, receiver_id, amount, status) VALUES (?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, fromUser);
        ps.setInt(2, toUser);
        ps.setDouble(3, amount);
        ps.setString(4, "TRANSFER");

        ps.executeUpdate();
    }
}
