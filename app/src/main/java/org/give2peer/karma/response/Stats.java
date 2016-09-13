package org.give2peer.karma.response;

public class Stats {

    int users_count; // number of registered users.
    int items_count; // number of published items right now.
    int items_total; // number of published items since the beginning.

    public int getUsersCount() {
        return users_count;
    }

    public int getItemsCount() {
        return items_count;
    }

    public int getItemsTotal() {
        return items_total;
    }

}
