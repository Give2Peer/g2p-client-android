package org.give2peer.karma.response;

import org.give2peer.karma.entity.Item;
import org.give2peer.karma.entity.User;

import java.util.List;

/**
 * GSON recipient for the response of `GET` `/profile` request.
 */
public class PrivateProfileResponse
{

    public User user;

    public List<Item> items;

    public PrivateProfileResponse() {} // maybe needed, maybe not

    public User getUser() { return user; }

    public List<Item> getItems() { return items; }
}
