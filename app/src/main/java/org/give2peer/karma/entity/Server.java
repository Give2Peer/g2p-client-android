package org.give2peer.karma.entity;


/**
 * This is a persistable entity.
 * Possible names :
 * - ServerConfiguration (bit long)
 */
public class Server extends BaseEntity
{
    String url      = "";
    String name     = "";
    String username = "";
    String password = "";

    boolean isEditedByUser = false;

    public static String DEFAULT_URL = "http://g2p.give2peer.org/v1";
    public static String DEFAULT_NAME = "Give2Peer \uD835\uDEC2";
    public static String DEFAULT_USERNAME = ""; // the emptiness is detected in the app onCreate
    public static String DEFAULT_PASSWORD = ""; // and we ask the server for credentials

    public Server() {}

    /**
     * This loads the default configuration that is automatically added to the app on first launch.
     *
     * Note: putting these in a class extending Server is a bad idea: the ORM chokes.
     *
     * TODO@beta: either automatic registration somewhere, or a proper Anon account.
     */
    public Server loadDefaults()
    {
        url      = DEFAULT_URL;
        name     = DEFAULT_NAME;
        username = DEFAULT_USERNAME;
        password = DEFAULT_PASSWORD;

        return this;
    }

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
     * @return whether or not this server configuration as been edited by the hand of the user.
     */
    public boolean isEditedByUser() { return isEditedByUser; }

    /**
     * Set this to true when the user edits "by hand" the configuration (ie: registers manually).
     */
    public void setEditedByUser() { this.isEditedByUser = true; }

    /**********************************************************************************************/

    public String getUrl()                   { return url;                                         }

    public void setUrl(String url)           { this.url = url;                                     }

    public String getName()                  { return name;                                        }

    public void setName(String name)         { this.name = name;                                   }

    public String getUsername()              { return username;                                    }

    public void setUsername(String username) { this.username = username;                           }

    public String getPassword()              { return password;                                    }

    public void setPassword(String password) { this.password = password;                           }

}
