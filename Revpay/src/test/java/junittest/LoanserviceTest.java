package junittest;

import org.junit.Before;
import org.junit.Test;

import revpay.DBconfig.DButil;
import revpay.Services.LoanService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.*;

public class LoanserviceTest {

    private LoanService loanService;

    private final int userId = 1; // MUST exist in app_users
    private final double loanAmount = 100000.00;


    @Before
    public void setup() throws Exception {

        loanService = new LoanService();
        Connection con = DButil.getConnection();

        // Clean existing loans for test user
        PreparedStatement ps =
                con.prepareStatement("DELETE FROM business_loans WHERE user_id=?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }


    @Test
    public void testApplyPersonalLoan() throws Exception {

        loanService.applyPersonalLoan(userId, loanAmount, "Medical expenses");

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT * FROM business_loans WHERE user_id=? AND loan_type='PERSONAL'");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        assertTrue(rs.next());
        assertEquals("PENDING", rs.getString("status"));
        assertEquals(loanAmount, rs.getDouble("amount"), 0.01);
        assertEquals(loanAmount, rs.getDouble("repayment_amount"), 0.01);
    }


    @Test
    public void testApplyBusinessLoan() throws Exception {

        loanService.applyBusinessLoan(userId, 500000.00, "Business expansion");

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT * FROM business_loans WHERE user_id=? AND loan_type='BUSINESS'");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        assertTrue(rs.next());
        assertEquals("PENDING", rs.getString("status"));
    }


    @Test
    public void testPartialLoanRepayment() throws Exception {

        // apply loan first
        loanService.applyPersonalLoan(userId, loanAmount, "Education");

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT loan_id FROM business_loans WHERE user_id=?");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        rs.next();
        int loanId = rs.getInt("loan_id");

        loanService.repayLoan(loanId, 40000.00);

        PreparedStatement check =
                con.prepareStatement(
                        "SELECT repayment_amount, status FROM business_loans WHERE loan_id=?");
        check.setInt(1, loanId);

        ResultSet rs2 = check.executeQuery();
        rs2.next();

        assertEquals(60000.00, rs2.getDouble("repayment_amount"), 0.01);
        assertEquals("ACTIVE", rs2.getString("status"));
    }

    @Test
    public void testFullLoanRepayment() throws Exception {

        loanService.applyPersonalLoan(userId, 50000.00, "Travel");

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT loan_id FROM business_loans WHERE user_id=?");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        rs.next();
        int loanId = rs.getInt("loan_id");

        loanService.repayLoan(loanId, 50000.00);

        PreparedStatement check =
                con.prepareStatement(
                        "SELECT repayment_amount, status FROM business_loans WHERE loan_id=?");
        check.setInt(1, loanId);

        ResultSet rs2 = check.executeQuery();
        rs2.next();

        assertEquals(0.0, rs2.getDouble("repayment_amount"), 0.01);
        assertEquals("CLOSED", rs2.getString("status"));
    }
}

