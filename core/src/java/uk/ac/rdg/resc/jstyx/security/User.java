/*
 * User.java
 *
 * Created on 22 November 2004, 17:30
 */

package uk.ac.rdg.resc.jstyx.security;

/**
 * Class representing a user
 * @author  jdb
 */
public class User
{
    
    private String shortName; // Name of the user as it will appear in stat messages (can't contain spaces)
    
    /** Creates a new instance of User */
    public User(String shortName)
    {
        this.shortName = shortName;
    }
    
    /**
     * @return the short name of the user, as it will appear in directory entries
     */
    public String getShortName()
    {
        return this.shortName;
    }
    
}
