package junittest;

import org.junit.Before;
import org.junit.Test;

import revpay.DBconfig.DButil;
import revpay.Services.NotificationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.*;

public class NotificationTest {

    private NotificationService notificationService;

    private final int userId = 1; // MUST exist in app_users
    private final String message = "Test notification message";


    @Before
    public void setup() throws Exception {

        notificationService = new NotificationService();
        Connection con = DButil.getConnection();

        // Clean old notifications for this user
        PreparedStatement ps =
                con.prepareStatement(
                        "DELETE FROM user_notifications WHERE user_id=?");
        ps.setInt(1, userId);
        ps.executeUpdate();
    }


    @Test
    public void testNotifyUser() throws Exception {

        notificationService.notifyUser(userId, message);

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT * FROM user_notifications WHERE user_id=?");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();

        assertTrue(rs.next());
        assertEquals(message, rs.getString("message"));
        assertFalse(rs.getBoolean("is_read"));
    }


    @Test
    public void testMarkAsRead() throws Exception {

        // create notification first
        notificationService.notifyUser(userId, message);

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement(
                        "SELECT id FROM user_notifications WHERE user_id=?");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        rs.next();
        int notificationId = rs.getInt("id");

        notificationService.markAsRead(notificationId);

        PreparedStatement check =
                con.prepareStatement(
                        "SELECT is_read FROM user_notifications WHERE id=?");
        check.setInt(1, notificationId);

        ResultSet rs2 = check.executeQuery();
        rs2.next();

        assertTrue(rs2.getBoolean("is_read"));
    }
}
