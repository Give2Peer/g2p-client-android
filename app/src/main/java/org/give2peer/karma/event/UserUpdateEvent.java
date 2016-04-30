package org.give2peer.karma.event;

import org.give2peer.karma.entity.User;


public class UserUpdateEvent
{
    User user;

    public UserUpdateEvent(User user)
    {
        this.user = user;
    }

    /**
     * @return the User that was fetched from the server.
     */
    public User getUser() { return user; }
}
