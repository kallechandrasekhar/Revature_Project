package junittest;

import org.junit.Before;
import org.junit.Test;

import revpay.DBconfig.DButil;
import revpay.Services.InvoiceService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.*;

public class InvoiceTest {

    private InvoiceService invoiceService;

    private final int businessId = 1; // make sure this user exists in app_users
    private final String customerEmail = "customer@test.com";

    @Before
    public void setup() throws Exception {

        invoiceService = new InvoiceService();
        Connection con = DButil.getConnection();

        // Clean old test invoices
        PreparedStatement ps =
                con.prepareStatement(
                        "DELETE FROM business_invoices WHERE business_id=? AND customer_email=?");
        ps.setInt(1, businessId);
        ps.setString(2, customerEmail);
        ps.executeUpdate();
    }

    @Test
    public void testCreateInvoice() throws Exception {

        invoiceService.createInvoice(
                businessId,
                customerEmail,
                "Item1, Item2",
                500.00,
                "NET 30"
        );

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT * FROM business_invoices WHERE business_id=? AND customer_email=?");
        ps.setInt(1, businessId);
        ps.setString(2, customerEmail);

        ResultSet rs = ps.executeQuery();

        assertTrue(rs.next());
        assertEquals("UNPAID", rs.getString("status"));
        assertEquals(500.00, rs.getDouble("amount"), 0.01);
    }

    @Test
    public void testMarkInvoicePaid() throws Exception {

        // create invoice first
        testCreateInvoice();

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT invoice_id FROM business_invoices WHERE business_id=? AND customer_email=?");
        ps.setInt(1, businessId);
        ps.setString(2, customerEmail);

        ResultSet rs = ps.executeQuery();
        rs.next();
        int invoiceId = rs.getInt("invoice_id");

        invoiceService.markInvoicePaid(invoiceId);

        PreparedStatement check =
                con.prepareStatement(
                        "SELECT status FROM business_invoices WHERE invoice_id=?");
        check.setInt(1, invoiceId);

        ResultSet rs2 = check.executeQuery();
        rs2.next();

        assertEquals("PAID", rs2.getString("status"));
    }

    @Test
    public void testMarkInvoiceUnpaid() throws Exception {

        // create invoice first
        testCreateInvoice();

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT invoice_id FROM business_invoices WHERE business_id=? AND customer_email=?");
        ps.setInt(1, businessId);
        ps.setString(2, customerEmail);

        ResultSet rs = ps.executeQuery();
        rs.next();
        int invoiceId = rs.getInt("invoice_id");

        invoiceService.markInvoiceUnpaid(invoiceId);

        PreparedStatement check =
                con.prepareStatement(
                        "SELECT status FROM business_invoices WHERE invoice_id=?");
        check.setInt(1, invoiceId);

        ResultSet rs2 = check.executeQuery();
        rs2.next();

        assertEquals("UNPAID", rs2.getString("status"));
    }
}

