package org.give2peer.karma.response;

import org.give2peer.karma.entity.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GSON recipient for the response of `GET` `/items/around/{lat}/{lon}` request.
 */
public class FindItemsResponse
{
    int    karma = 0;
    Item[] items;

    /** @return the item that was added, as it is now in the server's database. */
    public Item[] getItems() { return items; }
    /** @return the amount of karma gained by the action of finding these items. */
    public int getKarma()    { return karma; }
}
