package org.give2peer.karma.entity;


import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.lang3.text.WordUtils;

/**
 * Users are the humanoids holding their phones with their gorilla fingers.
 *
 * This class should closely mirror the user model returned by the API.
 * We use GSON to populate instances of this, usually.
 */
public class User implements Parcelable
{
    /**
     * This username can hold pretty much any UTF-8 character.
     */
    String username;

    /**
     * The level of a User limits what the user may or may not do in the app, as well as its quotas.
     * It is not stored, but dynamically derived from the karma points.
     */
    int level;

    /**
     * The karma of a User defines its level.
     */
    int karma;

    /**
     * Acceleration of Experience cost per level.
     * Used as a constant in the formulas for levelling up.
     */
    public static final int ACC_EXP_COST = 15;

    /**
     * Required Experience to be level 1.
     * Used as a constant in the formulas for levelling up.
     */
    public static final int EXP_LVL_1    = 10;


    //// PARCELABLE ////////////////////////////////////////////////////////////////////////////////

    protected User(Parcel in) {
        username = in.readString();
        level = in.readInt();
        karma = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(username);
        parcel.writeInt(level);
        parcel.writeInt(karma);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };


    //// HUMANIZATION //////////////////////////////////////////////////////////////////////////////

    /**
     * @return a pretty-formatted username
     */
    public String getPrettyUsername()
    {
        String s = getUsername().replaceAll("[_\\s\\d]+$", "");
        s = s.replaceAll("[_]+", " ");
        return WordUtils.capitalizeFully(s);
    }


    //// LEVEL & EXPERIENCE ////////////////////////////////////////////////////////////////////////

    /**
     * This is the total of karma points minus the karma points
     * required to attain the current level of the user.
     *
     * @return the karma points acquired towards next level.
     */
    public int getKarmaProgress()
    {
        return getKarma() - karmaOf(getLevel());
    }

    /**
     * @return the amount of karma points missing to gain next level.
     */
    public int getKarmaMissing()
    {
        return karmaOf(getLevel() + 1) - getKarma();
    }

    /**
     * This ignores the current karma points, it's xp(N+1) minus xp(N).
     *
     * @return the relative amount of karma points needed to gain next level.
     */
    public int getKarmaRequired()
    {
        return karmaOf(getLevel()+1) - karmaOf(getLevel());
    }


    //// STATIC METHODS TO COMPUTE LEVEL-BASED STUFF ///////////////////////////////////////////////

    /**
     * Thanks Aurel Page for the formula ♥
     *
     * @param karma points to compute the level of.
     * @return the level at which we are when we have `karma` points.
     */
    static int levelOf(int karma)
    {
        int a = ACC_EXP_COST;
        int b = EXP_LVL_1;
        int n = (int) Math.floor(
                (3 * a - 2 * b + Math.sqrt(Math.pow(2 * b - a, 2) + 8 * a * karma))
                /
                (2 * a)
        );

        return Math.max(n, 1) - 1;
    }

    /**
     * Thanks Aurel Page for the formula ♥
     *
     * @param level to compute the needed karma of.
     * @return the karma required to be `level`.
     */
    static int karmaOf(int level)
    {
        int a = ACC_EXP_COST;
        int b = EXP_LVL_1;
        int n = level + 1;

        return (b - a) * (n - 1) + a * (n * n - n) / 2;
    }


    //// BORING STUFF //////////////////////////////////////////////////////////////////////////////

    public String getUsername()               { return username;                                   }
    public void setUsername(String username)  { this.username = username;                          }
    public int getLevel()                     { return level;                                      }
    public void setLevel(int level)           { this.level = level;                                }
    public int getKarma()                     { return karma;                                      }
    public void setKarma(int karma)           { this.karma = karma;                                }

}
