package org.give2peer.give2peer.entity;


import com.orm.SugarRecord;

/**
 * This is a persistable entity.
 *
 */
public class ServerConfiguration extends SugarRecord<ServerConfiguration>
{
    String url      = "";
    String name     = "";
    String username = "";
    String password = "";

    /**
     * This loads the default configuration that is automatically added to the app on first launch.
     *
     * Note: putting these in a class extending ServerConfiguration is a bad idea: the ORM chokes.
     *
     * TODO: either automatic registration somewhere, or a proper Anon account
     */
    public ServerConfiguration loadDefaults()
    {
        url      = "http://g2p.give2peer.org";
        name     = "Give2Peer Demo";
        username = "Goutte";
        password = "Goutte";

        return this;
    }

    public String getUrl()                   { return url; }

    public void setUrl(String url)           { this.url = url; }

    public String getName()                  { return name; }

    public void setName(String name)         { this.name = name; }

    public String getUsername()              { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPassword()              { return password; }

    public void setPassword(String password) { this.password = password; }
}
