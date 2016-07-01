package test;

import com.google.gson.Gson;

import org.give2peer.karma.entity.Item;
import org.give2peer.karma.entity.Server;
import org.give2peer.karma.response.PrivateProfileResponse;
import org.give2peer.karma.service.RestService;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ItemUnitTest
{

    @Test
    public void generatingDisplayedItemTitle()
    {
        Item item = new Item();

        item.setTitle("\t\tyellow\n    \tglove ");


        // annnnnd where am I gonna find a context here ?
        //assertEquals(item.getHumanTitle(context), "something else");
    }

}