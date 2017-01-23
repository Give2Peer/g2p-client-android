package org.give2peer.karma.response;

import org.give2peer.karma.entity.ItemPicture;

/**
 * GSON recipient for the response of `POST` `/item/picture` request.
 */
public class PictureItemBeforehandResponse
{
    ItemPicture picture;

    public ItemPicture getPicture() {
        return picture;
    }

    public void setPicture(ItemPicture picture) {
        this.picture = picture;
    }
}
