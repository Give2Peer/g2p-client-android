package org.give2peer.karma.entity;

import java.util.List;

/**
 * GSON recipient for the response of `GET` `/profile` request.
 */
public class PrivateProfile
{
    public User user;

    public List<Item> items;

    public PrivateProfile() {} // maybe needed, maybe not
}
