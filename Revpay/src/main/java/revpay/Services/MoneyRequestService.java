package revpay.Services;

import revpay.DBconfig.DButil;
import revpay.DAO.TransactionDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MoneyRequestService {

    private final WalletService walletService = new WalletService();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    //.. SEND REQUEST ..//

    public void sendRequest(int senderId, int receiverId, double amount) throws Exception {
        requestMoney(senderId, receiverId, amount);
        System.out.println("Money request sent successfully");
    }

    //..CORE DB METHOD ..//
    public void requestMoney(int fromUser, int toUser, double amount) throws Exception {

        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }

        Connection con = DButil.getConnection();

        String sql =
                "INSERT INTO payment_requests (from_user, to_user, amount, status) " +
                        "VALUES (?, ?, ?, 'PENDING')";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, fromUser);
        ps.setInt(2, toUser);
        ps.setDouble(3, amount);

        ps.executeUpdate();
    }

    //.. VIEW REQUESTS ..//
    public void viewRequests(int userId) throws Exception {

        Connection con = DButil.getConnection();

        String sql =
                "SELECT from_user, amount, status " +
                        "FROM payment_requests WHERE to_user=?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        boolean found = false;
        System.out.println("\n--- Incoming Money Requests ---");

        while (rs.next()) {
            found = true;
            System.out.println(
                    "From User: " + rs.getInt("from_user") +
                            " | Amount: " + rs.getDouble("amount") +
                            " | Status: " + rs.getString("status")
            );
        }

        if (!found) {
            System.out.println("No requests found");
        }
    }

    //.. REQUIRED METHOD ..//
    public void respondToRequest(int requestId, boolean approve) throws Exception {

        Connection con = DButil.getConnection();

        // 🔹 Get request details
        String fetchSql =
                "SELECT from_user, to_user, amount " +
                        "FROM payment_requests " +
                        "WHERE from_user=? AND status='PENDING' LIMIT 1";

        PreparedStatement fetchPs = con.prepareStatement(fetchSql);
        fetchPs.setInt(1, requestId);

        ResultSet rs = fetchPs.executeQuery();

        if (!rs.next()) {
            System.out.println("No pending request found");
            return;
        }

        int fromUser = rs.getInt("from_user");
        int toUser = rs.getInt("to_user");
        double amount = rs.getDouble("amount");

        if (approve) {
            // WALLET TRANSFER
            walletService.withdraw(toUser, amount);
            walletService.deposit(fromUser, amount);

            // INSERT TRANSACTION (THIS FIXES HISTORY)
            transactionDAO.addTransaction(
                    toUser,
                    fromUser,
                    amount,
                    "REQUEST_TRANSFER",
                    "Money request approved"
            );
        }

        // UPDATE REQUEST STATUS
        String updateSql =
                "UPDATE payment_requests SET status=? " +
                        "WHERE from_user=? AND status='PENDING' LIMIT 1";

        PreparedStatement updatePs = con.prepareStatement(updateSql);
        updatePs.setString(1, approve ? "APPROVED" : "REJECTED");
        updatePs.setInt(2, requestId);

        updatePs.executeUpdate();

        System.out.println(
                approve
                        ? "Request APPROVED and money transferred"
                        : "Request REJECTED"
        );
    }
}
