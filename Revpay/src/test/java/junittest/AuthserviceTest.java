package junittest;

import org.junit.Before;
import org.junit.Test;
import revpay.DBconfig.DButil;
import revpay.Services.AuthService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.*;

public class AuthserviceTest {

    private AuthService auth;

    private final String email = "testuser@gmail.com";
    private final String phone = "9999999999";
    private final String password = "Test@123";
    private final String pin = "1234";

    @Before
    public void setup() throws Exception {

        auth = new AuthService();
        Connection con = DButil.getConnection();

        PreparedStatement ps =
                con.prepareStatement("SELECT user_id FROM app_users WHERE email=?");
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int userId = rs.getInt("user_id");

            //  DELETE CHILD TABLES FIRST (FK SAFE ORDER)

            PreparedStatement delRequests =
                    con.prepareStatement(
                            "DELETE FROM payment_requests WHERE from_user=? OR to_user=?");
            delRequests.setInt(1, userId);
            delRequests.setInt(2, userId);
            delRequests.executeUpdate();

            PreparedStatement delTxn =
                    con.prepareStatement(
                            "DELETE FROM payment_transactions WHERE sender_id=? OR receiver_id=?");
            delTxn.setInt(1, userId);
            delTxn.setInt(2, userId);
            delTxn.executeUpdate();

            PreparedStatement delNotify =
                    con.prepareStatement(
                            "DELETE FROM user_notifications WHERE user_id=?");
            delNotify.setInt(1, userId);
            delNotify.executeUpdate();

            PreparedStatement delWallet =
                    con.prepareStatement(
                            "DELETE FROM user_wallet WHERE user_id=?");
            delWallet.setInt(1, userId);
            delWallet.executeUpdate();

            //  DELETE PARENT LAST
            PreparedStatement delUser =
                    con.prepareStatement(
                            "DELETE FROM app_users WHERE user_id=?");
            delUser.setInt(1, userId);
            delUser.executeUpdate();
        }
    }

    @Test
    public void testRegisterPersonalUser() throws Exception {

        auth.registerPersonal(
                "Test User",
                email,
                phone,
                password,
                pin,
                "Q",
                "A"
        );

        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement("SELECT * FROM app_users WHERE email=?");
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();

        assertTrue(rs.next());
    }

    @Test
    public void testLoginSuccess() {

        auth.registerPersonal(
                "Login User",
                email,
                phone,
                password,
                pin,
                "Q",
                "A"
        );

        int userId = auth.login(email, password);

        assertTrue(userId > 0);
    }

    @Test
    public void testLoginFailWrongPassword() {

        auth.registerPersonal(
                "Fail User",
                email,
                phone,
                password,
                pin,
                "Q",
                "A"
        );

        int userId = auth.login(email, "Wrong@123");

        assertEquals(-1, userId);
    }

    @Test
    public void testRecoverPassword() {

        auth.registerPersonal(
                "Recover User",
                email,
                phone,
                password,
                pin,
                "fav?",
                "red"
        );

        boolean changed =
                auth.recoverPassword(email, "red", "NewPass@123");

        assertTrue(changed);

        int userId = auth.login(email, "NewPass@123");

        assertTrue(userId > 0);
    }
}
