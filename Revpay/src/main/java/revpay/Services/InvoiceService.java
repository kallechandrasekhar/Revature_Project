package revpay.Services;

import revpay.DBconfig.DButil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class InvoiceService {

    //.. CREATE INVOICE ..//
    public void createInvoice(
            int businessId,
            String customerEmail,
            String items,
            double amount,
            String paymentTerms
    ) throws Exception {

        Connection con = DButil.getConnection();

        String sql =
                "INSERT INTO business_invoices " +
                        "(business_id, customer_email, items, amount, payment_terms, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, businessId);
        ps.setString(2, customerEmail);
        ps.setString(3, items);
        ps.setDouble(4, amount);
        ps.setString(5, paymentTerms);
        ps.setString(6, "UNPAID");

        ps.executeUpdate();
        System.out.println("Invoice created successfully");
    }

    //.. MARK PAID ..//
    public void markInvoicePaid(int invoiceId) throws Exception {

        Connection con = DButil.getConnection();

        String sql =
                "UPDATE business_invoices SET status='PAID' WHERE invoice_id=?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, invoiceId);
        ps.executeUpdate();

        System.out.println("Invoice marked as PAID");
    }

    //.. MARK UNPAID.. //
    public void markInvoiceUnpaid(int invoiceId) throws Exception {

        Connection con = DButil.getConnection();

        String sql =
                "UPDATE business_invoices SET status='UNPAID' WHERE invoice_id=?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, invoiceId);
        ps.executeUpdate();

        System.out.println("Invoice marked as UNPAID");
    }

    //..VIEW INVOICES ..//
    public void viewInvoices(int businessId) throws Exception {

        Connection con = DButil.getConnection();

        String sql =
                "SELECT invoice_id, customer_email, amount, status " +
                        "FROM business_invoices " +
                        "WHERE business_id=? " +
                        "ORDER BY invoice_id DESC";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, businessId);

        ResultSet rs = ps.executeQuery();

        boolean found = false;
        while (rs.next()) {
            found = true;
            System.out.println(
                    "Invoice ID: " + rs.getInt("invoice_id") +
                            " | Customer: " + rs.getString("customer_email") +
                            " | Amount: " + rs.getDouble("amount") +
                            " | Status: " + rs.getString("status")
            );
        }

        if (!found) {
            System.out.println("No invoices found");
        }
    }

    //..REQUIRED BY ProjectDemo ..//
    public void manageInvoices(int businessId) throws Exception {

        Scanner sc = new Scanner(System.in);

        System.out.println("\n--- Invoice Management ---");
        System.out.println("1. Create Invoice");
        System.out.println("2. View Invoices");
        System.out.println("3. Mark Invoice PAID");
        System.out.println("4. Mark Invoice UNPAID");
        System.out.print("Choose option: ");

        int choice = sc.nextInt();
        sc.nextLine();

        if (choice == 1) {
            System.out.print("Customer Email: ");
            String email = sc.nextLine();
            System.out.print("Items: ");
            String items = sc.nextLine();
            System.out.print("Amount: ");
            double amount = sc.nextDouble();
            sc.nextLine();
            System.out.print("Payment Terms: ");
            String terms = sc.nextLine();

            createInvoice(businessId, email, items, amount, terms);
        }
        else if (choice == 2) {
            viewInvoices(businessId);
        }
        else if (choice == 3) {
            System.out.print("Invoice ID: ");
            markInvoicePaid(sc.nextInt());
        }
        else if (choice == 4) {
            System.out.print("Invoice ID: ");
            markInvoiceUnpaid(sc.nextInt());
        }
    }
}
