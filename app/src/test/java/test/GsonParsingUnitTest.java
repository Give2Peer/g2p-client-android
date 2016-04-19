package test;

import com.google.gson.Gson;

import org.give2peer.karma.entity.PrivateProfile;
import org.give2peer.karma.entity.Server;
import org.give2peer.karma.exception.AuthorizationException;
import org.give2peer.karma.exception.MaintenanceException;
import org.give2peer.karma.exception.QuotaException;
import org.give2peer.karma.service.RestService;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class GsonParsingUnitTest
{

    @Test
    public void parsingPrivateProfileWithNoItems()
    {
        String json = "{\"user\":{\"id\":3,\"username\":\"gizko\",\"email\":\"gizko@give2peer.org\",\"created_at\":{\"date\":\"2016-04-07 22:11:48.000000\",\"timezone_type\":3,\"timezone\":\"Europe\\/Paris\"},\"karma\":3,\"level\":0},\"items\":[]}";

        Gson gson = new Gson();
        PrivateProfile pp = gson.fromJson(json, PrivateProfile.class);
        assertTrue(pp.user.getUsername().equals("gizko"));
        assertEquals(pp.user.getKarma(), 3);
        assertTrue(pp.items.isEmpty());
    }


    @Test
    public void parsingPrivateProfileResponse()
    {

        // This can't be done that way because org.apache.httpclient is mocked it seems
        // Anyways, it's not an encoding issue ; we tried using a handcrafted String in the app and
        // it crashed just as well.

        Server server = new Server();
        server.setUsername("Gizko");
        server.setPassword("Gizko");
        server.setUrl("http://g2p.give2peer.org");

        RestService rs = new RestService(server);

        String json = null;
        try {
            json = rs.getJson("/profile");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Nope : " + e.getMessage());
        }

        Gson gson = new Gson();
        PrivateProfile pp = gson.fromJson(json, PrivateProfile.class);
        assertTrue(pp.user.getUsername().equals("gizko"));
        assertEquals(pp.user.getKarma(), 3);
        assertTrue(pp.items.isEmpty());
    }

}