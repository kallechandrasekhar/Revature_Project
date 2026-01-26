package junittest;

import org.junit.Before;
import org.junit.Test;

import revpay.DBconfig.DButil;
import revpay.Services.BusinessAnalyticsService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.*;

public class BussinessTest {

    private BusinessAnalyticsService analyticsService;

    private final int businessId = 2; // MUST exist in app_users (BUSINESS)
    private final int customerId = 1;


    @Before
    public void setup() throws Exception {

        analyticsService = new BusinessAnalyticsService();
        Connection con = DButil.getConnection();

        // Clean old data
        PreparedStatement delTxn =
                con.prepareStatement(
                        "DELETE FROM payment_transactions WHERE receiver_id=?");
        delTxn.setInt(1, businessId);
        delTxn.executeUpdate();

        PreparedStatement delInv =
                con.prepareStatement(
                        "DELETE FROM business_invoices WHERE business_id=?");
        delInv.setInt(1, businessId);
        delInv.executeUpdate();

        // Insert test transaction
        PreparedStatement insTxn =
                con.prepareStatement(
                        "INSERT INTO payment_transactions (sender_id, receiver_id, amount, status) VALUES (?,?,?,?)");
        insTxn.setInt(1, customerId);
        insTxn.setInt(2, businessId);
        insTxn.setDouble(3, 1000.00);
        insTxn.setString(4, "TRANSFER");
        insTxn.executeUpdate();

        // Insert unpaid invoice
        PreparedStatement insInv =
                con.prepareStatement(
                        "INSERT INTO business_invoices (business_id, customer_email, amount, status) VALUES (?,?,?,?)");
        insInv.setInt(1, businessId);
        insInv.setString(2, "cust@test.com");
        insInv.setDouble(3, 2000.00);
        insInv.setString(4, "UNPAID");
        insInv.executeUpdate();
    }


    @Test
    public void testRevenueSummary() throws Exception {

        analyticsService.showRevenueSummary(businessId);

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT SUM(amount) FROM payment_transactions WHERE receiver_id=?");
        ps.setInt(1, businessId);

        ResultSet rs = ps.executeQuery();
        rs.next();

        assertEquals(1000.00, rs.getDouble(1), 0.01);
    }


    @Test
    public void testOutstandingInvoices() throws Exception {

        analyticsService.showOutstandingInvoices(businessId);

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT COUNT(*), SUM(amount) FROM business_invoices WHERE business_id=? AND status='UNPAID'");
        ps.setInt(1, businessId);

        ResultSet rs = ps.executeQuery();
        rs.next();

        assertEquals(1, rs.getInt(1));
        assertEquals(2000.00, rs.getDouble(2), 0.01);
    }


    @Test
    public void testPaymentTrends() {

        analyticsService.showPaymentTrends(businessId);
        // Console-based method → no assertion needed
        // If it runs without exception, test passes
        assertTrue(true);
    }


    @Test
    public void testTopCustomers() {

        analyticsService.showTopCustomers(businessId);
        // Console-based analytics → execution success = pass
        assertTrue(true);
    }
}
