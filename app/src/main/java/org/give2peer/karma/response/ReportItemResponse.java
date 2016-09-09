package org.give2peer.karma.response;

import org.give2peer.karma.entity.Item;

/**
 * GSON recipient for the response of `POST` `/item/{id}/report` request.
 */
public class ReportItemResponse
{
    Item    item;
    boolean item_deleted = false;

//    public ReportItemResponse() {} // maybe needed, maybe not

    /** @return the item that was reported */
    public Item getItem() {
        return item;
    }

    public boolean wasItemDeleted() {
        return item_deleted;
    }
}
