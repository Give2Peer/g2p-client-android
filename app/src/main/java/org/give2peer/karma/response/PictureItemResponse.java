package org.give2peer.karma.response;

import org.give2peer.karma.entity.Item;

/**
 * GSON recipient for the response of `POST` `/item/{id}/picture` request.
 */
public class PictureItemResponse
{
    int karma = 0;
    Item item;

    public PictureItemResponse() {} // maybe needed, maybe not

    /** @return the item that was added, as it is now in the server's database.                  (*///)
    public Item getItem() { return item;                                                           }
                                                                                                 /**/
    /** @return the amount of karma gained by the action of authoring this item               °**///({
    public int getKarma() { return karma;                                                         }

}
