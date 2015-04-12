package org.give2peer.give2peer;


public class Application extends android.app.Application
{
    private static Application singleton;

    String serverUrl = "http://g2p.give2peer.org";
    String username = "Goutte";
    String password = "Goutte";

    protected ItemRepository itemRepository;

    @Override
    public void onCreate()
    {
        super.onCreate();
        singleton = this;

        itemRepository = new ItemRepository(serverUrl, username, password);
    }

    public Application getInstance() { return singleton; }

    public ItemRepository getItemRepository() { return itemRepository; }
}
