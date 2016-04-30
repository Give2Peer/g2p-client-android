package org.give2peer.karma.event;

import org.give2peer.karma.entity.Item;
import org.give2peer.karma.entity.User;

import java.util.List;


public class AuthoredItemsUpdateEvent
{
    List<Item> items;

    public AuthoredItemsUpdateEvent(List<Item> items)
    {
        this.items = items;
    }

    /**
     * @return the items that were recently fetched from the server.
     */
    public List<Item> getItems() { return items; }
}
