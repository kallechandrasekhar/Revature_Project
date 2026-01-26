package revpay.Services;

import revpay.DBconfig.DButil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoanService {

    private static final Logger logger =
            LogManager.getLogger(LoanService.class);

    //..APPLY PERSONAL LOAN ..//
    public void applyPersonalLoan(int userId, double amount, String purpose) {

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "INSERT INTO business_loans " +
                            "(user_id, loan_type, amount, repayment_amount, purpose, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, "PERSONAL");
            ps.setDouble(3, amount);
            ps.setDouble(4, amount);
            ps.setString(5, purpose);
            ps.setString(6, "PENDING");

            ps.executeUpdate();

            System.out.println(" Personal loan applied successfully");
            logger.info("Personal loan applied | userId={} amount={}", userId, amount);

        } catch (Exception e) {
            logger.error("Loan application failed | userId={}", userId, e);
            System.out.println(" Loan application failed");
        }
    }

    /* ================= APPLY BUSINESS LOAN ================= */
    public void applyBusinessLoan(int userId, double amount, String purpose) {

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "INSERT INTO business_loans " +
                            "(user_id, loan_type, amount, repayment_amount, purpose, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, "BUSINESS");
            ps.setDouble(3, amount);
            ps.setDouble(4, amount);
            ps.setString(5, purpose);
            ps.setString(6, "PENDING");

            ps.executeUpdate();

            System.out.println(" Business loan applied successfully");
            logger.info("Business loan applied | userId={} amount={}", userId, amount);

        } catch (Exception e) {
            logger.error("Business loan failed | userId={}", userId, e);
            System.out.println(" Business loan application failed");
        }
    }

    //..VIEW LOANS ..//
    public void viewLoans(int userId) {

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "SELECT loan_id, loan_type, amount, repayment_amount, status, purpose " +
                            "FROM business_loans WHERE user_id=?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            boolean found = false;
            System.out.println("\n----- Your Loans -----");

            while (rs.next()) {
                found = true;
                System.out.println(
                        "Loan ID: " + rs.getInt("loan_id") +
                                " | Type: " + rs.getString("loan_type") +
                                " | Amount: " + rs.getDouble("amount") +
                                " | Remaining: " + rs.getDouble("repayment_amount") +
                                " | Status: " + rs.getString("status") +
                                " | Purpose: " + rs.getString("purpose")
                );
            }

            if (!found) {
                System.out.println("No loans found");
            }

        } catch (Exception e) {
            logger.error("Failed to fetch loans | userId={}", userId, e);
        }
    }

    //.. REPAY LOAN ..//
    public void repayLoan(int loanId, double repayAmount) {

        logger.info("Loan repayment attempt | loanId={} repayAmount={}",
                loanId, repayAmount);

        try {
            Connection con = DButil.getConnection();

            String checkSql =
                    "SELECT repayment_amount FROM business_loans WHERE loan_id=?";
            PreparedStatement checkPs = con.prepareStatement(checkSql);
            checkPs.setInt(1, loanId);

            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {
                System.out.println(" Loan not found");
                return;
            }

            double remaining = rs.getDouble("repayment_amount");

            if (repayAmount <= 0 || repayAmount > remaining) {
                System.out.println(" Invalid repayment amount");
                return;
            }

            double newRemaining = remaining - repayAmount;

            if (newRemaining == 0) {
                String sql =
                        "UPDATE business_loans SET repayment_amount=0, status='CLOSED' WHERE loan_id=?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, loanId);
                ps.executeUpdate();

                System.out.println(" Loan fully cleared");
                logger.info("Loan cleared | loanId={}", loanId);

            } else {
                String sql =
                        "UPDATE business_loans SET repayment_amount=?, status='ACTIVE' WHERE loan_id=?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setDouble(1, newRemaining);
                ps.setInt(2, loanId);
                ps.executeUpdate();

                System.out.println(" Partial repayment successful. Remaining: " + newRemaining);
                logger.info("Partial repayment | loanId={} remaining={}", loanId, newRemaining);
            }

        } catch (Exception e) {
            logger.error("Loan repayment failed | loanId={}", loanId, e);
            System.out.println(" Loan repayment failed");
        }
    }
}
