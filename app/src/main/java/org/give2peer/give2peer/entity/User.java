package org.give2peer.give2peer.entity;


/**
 * We use GSON to populate instances of this, usually.
 */
public class User
{
    String username;
    int level;
    int experience;

    public String getUsername()               { return username;              }
    public void setUsername(String username)  { this.username = username;     }
    public int getLevel()                     { return level;                 }
    public void setLevel(int level)           { this.level = level;           }
    public int getExperience()                { return experience;            }
    public void setExperience(int experience) { this.experience = experience; }
}
