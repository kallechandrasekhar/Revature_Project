package revpay.DAO;

import revpay.DBconfig.DButil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MoneyRequestDAO {

    private static final Logger logger =
            LogManager.getLogger(MoneyRequestDAO.class);

    public void createRequest(int from, int to, double amount) {
        try {
            Connection con = DButil.getConnection();
            PreparedStatement ps =
                    con.prepareStatement(
                            "INSERT INTO payment_requests (from_user, to_user, amount) VALUES (?,?,?)");

            ps.setInt(1, from);
            ps.setInt(2, to);
            ps.setDouble(3, amount);
            ps.executeUpdate();

            logger.info("Money request created from {} to {} for amount {}", from, to, amount);

        } catch (Exception e) {
            logger.error("Failed to create money request from {} to {}", from, to, e);
        }
    }

    public ResultSet getIncomingRequests(int userId) {
        try {
            Connection con = DButil.getConnection();
            PreparedStatement ps =
                    con.prepareStatement(
                            "SELECT * FROM payment_requests WHERE to_user=? AND status='PENDING'");
            ps.setInt(1, userId);

            logger.info("Fetching incoming money requests for user {}", userId);
            return ps.executeQuery();

        } catch (Exception e) {
            logger.error("Failed to fetch incoming requests for user {}", userId, e);
            return null;
        }
    }

    public void updateStatus(int requestId, String status) {
        try {
            Connection con = DButil.getConnection();
            PreparedStatement ps =
                    con.prepareStatement(
                            "UPDATE payment_requests SET status=? WHERE id=?");

            ps.setString(1, status);
            ps.setInt(2, requestId);
            ps.executeUpdate();

            logger.info("Money request {} updated to status {}", requestId, status);

        } catch (Exception e) {
            logger.error("Failed to update request {} to status {}", requestId, status, e);
        }
    }
}

