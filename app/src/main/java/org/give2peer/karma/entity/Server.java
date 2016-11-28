package org.give2peer.karma.entity;


/**
 * This is a persistable entity.
 *
 * Important note : 10.0.2.2 is always the IP of the computer running the emulator.
 *                  Pretty useful for development, as the server is easily run locally.
 *
 * Possible names :
 * - ServerConfiguration (bit long)
 */
public class Server extends BaseEntity
{
//    public static String DEFAULT_URL = "http://10.0.2.2:7676/v1";
    public static String DEFAULT_URL = "http://g2p.give2peer.org/v1";
    public static String DEFAULT_NAME = "Give2Peer";
    public static String DEFAULT_USERNAME = ""; // the emptiness is detected in the app onCreate
    public static String DEFAULT_PASSWORD = ""; // and we ask the server for credentials

    String url      = DEFAULT_URL;
    String name     = DEFAULT_NAME;
    String username = "";
    String password = "";

    /**
     * We use this in the registratiogouttonio@gmail.comn activity to either try to register a new account (if this is
     * true) or try to edit an existing account (if this is false) because we only pre-registered.
     */
    boolean isEditedByUser = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public Server() {}

    /**
     * This loads the default configuration that is automatically added to the app on first launch.
     *
     * Note: putting these in a class extending Server is a bad idea: the ORM chokes.
     */
    public Server loadDefaults()
    {
        url      = DEFAULT_URL;
        name     = DEFAULT_NAME;
        username = DEFAULT_USERNAME;
        password = DEFAULT_PASSWORD;

        return this;
    }

    /**
     * When we create a new server configuration, we fill it with this.
     * This is because we hacked the Settings to get our Server CRUD. It's BAD.
     */
    public Server loadDummy()
    {
        url      = "http://";
        name     = "Unnamed";
        username = "";
        password = "";

        return this;
    }

    /**
     * Password and username MUST both be filled for this to be true.
     *
     * @return whether or not this server configuration is complete.
     */
    public boolean isComplete()
    {
        return ( ! getUsername().isEmpty()) && ( ! getPassword().isEmpty());
    }

    /**
     * This is false by default.
     *
     *
     * @return whether or not this server configuration as been edited by the hand of the user.
     */
    public boolean isEditedByUser() { return isEditedByUser; }

    /**
     * Set this to true when the user edits "by hand" the configuration (ie: registers manually).
     */
    public void setEditedByUser() { this.isEditedByUser = true; }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public String getUrl()                   { return url;                                         }

    public void setUrl(String url)           { this.url = url;                                     }

    public String getName()                  { return name;                                        }

    public void setName(String name)         { this.name = name;                                   }

    public String getUsername()              { return username;                                    }

    public void setUsername(String username) { this.username = username;                           }

    public String getPassword()              { return password;                                    }

    public void setPassword(String password) { this.password = password;                           }

}
