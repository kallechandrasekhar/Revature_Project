package revpay.Services;

import revpay.DBconfig.DButil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class NotificationService {

    private static final Logger logger =
            LogManager.getLogger(NotificationService.class);

    public void notifyUser(int userId, String message) {

        logger.info("Sending notification | userId={}", userId);

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "INSERT INTO user_notifications (user_id, message, is_read) " +
                            "VALUES (?,?,?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setBoolean(3, false);

            ps.executeUpdate();

            logger.info("Notification sent successfully | userId={}", userId);

        } catch (Exception e) {
            logger.error("Failed to send notification | userId={}", userId, e);
        }
    }

    public void viewNotifications(int userId) {

        logger.info("Fetching all notifications | userId={}", userId);

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "SELECT * FROM user_notifications WHERE user_id=?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            boolean found = false;

            while (rs.next()) {
                found = true;

                logger.info(
                        "Notification | id={} message={} read={}",
                        rs.getInt("id"),
                        rs.getString("message"),
                        rs.getBoolean("is_read")
                );
            }

            if (!found) {
                logger.warn("No notifications found | userId={}", userId);
            }

        } catch (Exception e) {
            logger.error("Failed to fetch notifications | userId={}", userId, e);
        }
    }

    public void viewUnreadNotifications(int userId) {

        logger.info("Fetching unread notifications | userId={}", userId);

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "SELECT * FROM user_notifications " +
                            "WHERE user_id=? AND is_read=false";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            boolean found = false;

            while (rs.next()) {
                found = true;

                logger.info(
                        "Unread notification | id={} message={}",
                        rs.getInt("id"),
                        rs.getString("message")
                );
            }

            if (!found) {
                logger.info("No unread notifications | userId={}", userId);
            }

        } catch (Exception e) {
            logger.error("Failed to fetch unread notifications | userId={}", userId, e);
        }
    }

    public void markAsRead(int notificationId) {

        logger.info("Marking notification as read | notificationId={}", notificationId);

        try {
            Connection con = DButil.getConnection();

            String sql =
                    "UPDATE user_notifications SET is_read=true WHERE id=?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, notificationId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                logger.info("Notification marked as read | notificationId={}", notificationId);
            } else {
                logger.warn("Notification not found | notificationId={}", notificationId);
            }

        } catch (Exception e) {
            logger.error(
                    "Failed to mark notification as read | notificationId={}",
                    notificationId, e
            );
        }
    }
}

