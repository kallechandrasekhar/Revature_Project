package junittest;

import org.junit.Before;
import org.junit.Test;
import revpay.DBconfig.DButil;
import revpay.Services.MoneyRequestService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertTrue;

public class MoneyRequestServiceTest {

    private MoneyRequestService service;
    private final int fromUser = 1;
    private final int toUser = 2;
    private final double amount = 500;

    @Before
    public void setup() throws Exception {
        service = new MoneyRequestService();
        Connection con = DButil.getConnection();
        PreparedStatement ps =
                con.prepareStatement("DELETE FROM payment_requests WHERE from_user=?");
        ps.setInt(1, fromUser);
        ps.executeUpdate();
    }

    @Test
    public void testRequestMoney() throws Exception {
        service.requestMoney(fromUser, toUser, amount);

        Connection con = DButil.getConnection();
        PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM payment_requests WHERE from_user=? AND to_user=?");
        ps.setInt(1, fromUser);
        ps.setInt(2, toUser);

        ResultSet rs = ps.executeQuery();
        assertTrue(rs.next());
    }
}
