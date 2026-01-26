package revpay.DAO;

import revpay.DBconfig.DButil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

public class TransactionDAO {

    private static final Logger logger =
            LogManager.getLogger(TransactionDAO.class);

    // ..INSERT TRANSACTION.. //
    public void addTransaction(
            Integer senderId,
            Integer receiverId,
            double amount,
            String status,
            String note
    ) {

        try (Connection con = DButil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO payment_transactions " +
                             "(sender_id, receiver_id, amount, status, note) " +
                             "VALUES (?,?,?,?,?)")) {

            if (senderId == null)
                ps.setNull(1, Types.INTEGER);
            else
                ps.setInt(1, senderId);

            if (receiverId == null)
                ps.setNull(2, Types.INTEGER);
            else
                ps.setInt(2, receiverId);

            ps.setDouble(3, amount);
            ps.setString(4, status);
            ps.setString(5, note);

            ps.executeUpdate();

            logger.info(
                    "Transaction recorded | sender={} receiver={} amount={}",
                    senderId, receiverId, amount
            );

        } catch (Exception e) {
            logger.error("Failed to insert transaction", e);
        }
    }

    //.. VIEW TRANSACTIONS..//
    public void viewAllTransactions(int userId) {

        logger.info("Fetching all transactions for user {}", userId);

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "SELECT sender_id, receiver_id, amount, status, created_at " +
                            "FROM payment_transactions " +
                            "WHERE sender_id=? OR receiver_id=? " +
                            "ORDER BY created_at DESC";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, userId);

            ResultSet rs = ps.executeQuery();

            boolean found = false;

            System.out.println("\n----- Transaction History -----");

            while (rs.next()) {
                found = true;
                System.out.println(
                        "From: " + rs.getInt("sender_id") +
                                " | To: " + rs.getInt("receiver_id") +
                                " | Amount: ₹" + rs.getDouble("amount") +
                                " | Status: " + rs.getString("status") +
                                " | Date: " + rs.getTimestamp("created_at")
                );
            }

            if (!found) {
                System.out.println("No transactions found");
            }

        } catch (Exception e) {
            logger.error("Failed to fetch transactions", e);
            System.out.println("Failed to fetch transaction history");
        }
    }
}
