package test;

import com.google.gson.Gson;

import org.give2peer.karma.entity.PrivateProfile;
import org.testng.annotations.Test;

import static org.junit.Assert.assertTrue;

public class GsonParsingUnitTest {

    @Test
    public void parsingPrivateProfileWithNoItems()
    {
        String json = "{\"user\":{\"id\":3,\"username\":\"gizko\",\"email\":\"gizko@give2peer.org\",\"created_at\":{\"date\":\"2016-04-07 22:11:48.000000\",\"timezone_type\":3,\"timezone\":\"Europe\\/Paris\"},\"karma\":3,\"level\":0},\"items\":[]}";
        Gson gson = new Gson();
        PrivateProfile pp = gson.fromJson(json, PrivateProfile.class);
        assertTrue(pp.user.getUsername().equals("gizko"));
    }

}