package org.give2peer.karma;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

/**
 * Posts (but only once !) an event to the event bus when all subscribed events have been posted.
 *
 * Well... this was a failure... It seems it can't be done... :(
 *
 * Other possible names :
 * - MetaEvent ; but this is not an event per se, as it needs to be registered like a subscriber.
 */
public class EventAggregator
{
    EventBus       bus;
    List<Class<?>> subscribed;
    Object         posted;

    public EventAggregator(EventBus bus, List<Class<?>> subscribed, Object posted)
    {
        this.bus = bus;
        this.subscribed = subscribed;
        this.posted = posted;

    }

    // AAAAAH IT CAN'T BE DONE WITH THE EVENTBUS AS IT IS, IT SEEMS...
    // Unless maybe if we use some reflection to procedurally create the methods of this class with
    // an @Subscribe annotation... Can it be done ?

}
