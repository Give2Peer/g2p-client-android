package org.give2peer.give2peer.entity;


import org.apache.commons.lang3.text.WordUtils;

/**
 * We use GSON to populate instances of this, usually.
 */
public class User
{
    String username;
    int level;
    int experience;

    /**
     * Acceleration of Experience cost per level.
     * Used as a constant in the formulas for levelling up.
     */
    public static final int ACC_EXP_COST = 15;

    /**
     * Required Experience to be level 2.
     * Used as a constant in the formulas for levelling up.
     */
    public static final int EXP_LVL_2    = 10;


    //// HUMANIZATION //////////////////////////////////////////////////////////////////////////////

    public String getPrettyUsername()
    {
        return WordUtils.capitalizeFully(getUsername());
    }


    //// LEVEL & EXPERIENCE ////////////////////////////////////////////////////////////////////////

    /**
     * This is the total of experience points minus the experience points
     * required to attain the current level of the user.
     *
     * @return the experience points acquired towards next level.
     */
    public int getExperienceProgress()
    {
        return getExperience() - experienceOf(getLevel());
    }

    /**
     * @return the amount of experience points missing to gain next level.
     */
    public int getExperienceMissing()
    {
        return experienceOf(getLevel() + 1) - getExperience();
    }

    /**
     * This ignores the current experience points, it's xp(N+1) minus xp(N).
     *
     * @return the relative amount of experience points needed to gain next level.
     */
    public int getExperienceRequired()
    {
        return experienceOf(getLevel()+1) - experienceOf(getLevel());
    }


    //// STATIC METHODS TO COMPUTE LEVEL-BASED STUFF ///////////////////////////////////////////////

    /**
     * Thanks Aurel Page for the formula ♥
     *
     * @param experience points to compute the level of.
     * @return the level at which we are when we have `experience` points.
     */
    static int levelOf(int experience)
    {
        int a = ACC_EXP_COST;
        int b = EXP_LVL_2;
        int n = (int) Math.floor(
                (3 * a - 2 * b + Math.sqrt(Math.pow(2 * b - a, 2) + 8 * a * experience))
                /
                (2 * a)
        );

        return Math.max(n, 1);
    }

    /**
     * Thanks Aurel Page for the formula ♥
     *
     * @param level to compute the needed experience of.
     * @return the experience required to be `level`.
     */
    static int experienceOf(int level)
    {
        int a = ACC_EXP_COST;
        int b = EXP_LVL_2;
        int n = level;

        return (b - a) * (n - 1) + a * (n * n - n) / 2;
    }


    //// BORING STUFF //////////////////////////////////////////////////////////////////////////////

    public String getUsername()               { return username;              }
    public void setUsername(String username)  { this.username = username;     }
    public int getLevel()                     { return level;                 }
    public void setLevel(int level)           { this.level = level;           }
    public int getExperience()                { return experience;            }
    public void setExperience(int experience) { this.experience = experience; }
}
