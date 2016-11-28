package org.give2peer.karma.response;

import org.give2peer.karma.entity.Item;

/**
 * GSON recipient for the response of `POST` `/item/{id}/delete` request.
 */
public class DeleteItemResponse
{
    Item item;

    /** @return the item that was deleted */
    public Item getItem() {
        return item;
    }
}
