package org.give2peer.karma.test;

import org.give2peer.karma.EventAggregator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;

import android.support.test.runner.AndroidJUnit4;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


//
// /!\
// Configure this test to use, as instrumentation runner, the following class :
// android.support.test.runner.AndroidJUnitRunner
//

// These three events are fired in any order, and we want to react only when they all fired.
class SubEventA {}
class SubEventB {}
class SubEventC {}

// We want that event to be fired when all three events above were fired, and not before.
class RealEvent {}

// A mock class to listen to events posted by the aggregator
class ListenerOfRealEvents {
    public int thaum = 0;

    public boolean wasTriggered() { return thaum > 0; }

    @Subscribe
    public void makeMagic(RealEvent re) { thaum += 1; }
}

@RunWith(AndroidJUnit4.class)
public class EventAggregatorTest
{
    public EventAggregatorTest() {}

    @Test
    public void eventAggregation() {

        List<Class<?>> subEvents = new ArrayList<Class<?>>();
        subEvents.add(SubEventA.class);
        subEvents.add(SubEventB.class);
        subEvents.add(SubEventC.class);

        List<Class<?>> tryMe = Arrays.asList(SubEventA.class, SubEventB.class, SubEventC.class);

        EventBus bus = EventBus.getDefault();

        EventAggregator ea = new EventAggregator(bus, subEvents, new RealEvent());
        bus.register(ea);

        ListenerOfRealEvents lore = new ListenerOfRealEvents();
        bus.register(lore);

        assertFalse("Sanity check : the listener of real events should not have already " +
                    "triggered upon instantiation.", lore.wasTriggered());

        // Let's post the first event !
        bus.post(new SubEventA());

        assertFalse("The aggregated event should not have been posted after only one " +
                    "event.", lore.wasTriggered());

        // Let's post the second event, but let's try event C and not B, as order should not matter.
        bus.post(new SubEventC());

        assertFalse("The aggregated event should not have been posted after only two " +
                    "events.", lore.wasTriggered());

        // Okay, let's post the third and last event, and now Realevent should have been fired too.
        bus.post(new SubEventB());

        assertTrue("The aggregated event should have been posted after the third event.",
                   lore.wasTriggered());

        // Cleanup
        bus.unregister(lore);
        bus.unregister(ea);
    }

}