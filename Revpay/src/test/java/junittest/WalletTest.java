package junittest;



import org.junit.Before;
import org.junit.Test;

import revpay.DBconfig.DButil;
import revpay.Services.WalletService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.*;

public class WalletTest {

    private WalletService walletService;

    private final int userId = 1;     // MUST exist in app_users
    private final double initialBalance = 1000.00;

    @Before
    public void setup() throws Exception {

        walletService = new WalletService();
        Connection con = DButil.getConnection();

        // Ensure wallet row exists
        PreparedStatement insert =
                con.prepareStatement(
                        "INSERT IGNORE INTO user_wallet (user_id, balance) VALUES (?, ?)");
        insert.setInt(1, userId);
        insert.setDouble(2, initialBalance);
        insert.executeUpdate();

        // Reset balance before every test
        PreparedStatement reset =
                con.prepareStatement(
                        "UPDATE user_wallet SET balance=? WHERE user_id=?");
        reset.setDouble(1, initialBalance);
        reset.setInt(2, userId);
        reset.executeUpdate();

        // Clear old notifications
        PreparedStatement clearNotif =
                con.prepareStatement(
                        "DELETE FROM user_notifications WHERE user_id=?");
        clearNotif.setInt(1, userId);
        clearNotif.executeUpdate();
    }

    @Test
    public void testDeposit() throws Exception {

        walletService.deposit(userId, 500.00);

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT balance FROM user_wallet WHERE user_id=?");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        rs.next();

        assertEquals(1500.00, rs.getDouble("balance"), 0.01);
    }

    @Test
    public void testWithdrawSuccess() throws Exception {

        walletService.withdraw(userId, 400.00);

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT balance FROM user_wallet WHERE user_id=?");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        rs.next();

        assertEquals(600.00, rs.getDouble("balance"), 0.01);
    }

    @Test
    public void testWithdrawInsufficientBalance() throws Exception {

        walletService.withdraw(userId, 2000.00);

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT balance FROM user_wallet WHERE user_id=?");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        rs.next();

        // balance should remain unchanged
        assertEquals(initialBalance, rs.getDouble("balance"), 0.01);
    }

    @Test
    public void testLowBalanceNotification() throws Exception {

        // Withdraw to bring balance below LOW_BALANCE_LIMIT (500)
        walletService.withdraw(userId, 600.00);

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT * FROM user_notifications WHERE user_id=?");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        assertTrue(rs.next());
        assertTrue(rs.getString("message").contains("Low wallet balance"));
    }
}

