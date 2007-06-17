/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.grex.config;

import java.util.Vector;
import org.acegisecurity.userdetails.UserDetails;
import simple.xml.Attribute;
import simple.xml.Root;

/**
 * A user that has access to the G-Rex system, as expressed in the config file.
 * This implements the UserDetails interface so that this object can be used
 * directly in the Acegi framework.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="user")
public class User implements UserDetails
{
    @Attribute(name="username")
    private String username;
    
    @Attribute(name="password")
    private String password; // The password is stored in the clear: digest security needs this
    
    @Attribute(name="fullname")
    private String fullname; // Full name of the user: should this be optional?
    
    @Attribute(name="groups", required=false)  // Comma-separated list of names of groups to which the
    private String groupNamesStr = null; // user belongs.  Checked in GRexConfig.validate()
    
    @Attribute(name="emailAddress", required=false)
    private String emailAddress = ""; // Used to notify the user of updates to their jobs
    
    @Attribute(name="disabled", required = false)
    private boolean disabled = false; // Can be set true to disable a user without
                                      // removing it from the config file
    
    // The user's groups (i.e. roles) will be set in GRexConfig.validate()
    private Vector<Group> groups = new Vector<Group>();
    
    private boolean admin = false; // Will be set true if this user is added to the admin group
    
    /** Creates a new instance of User */
    public User()
    {
        // All users are added to the ALL_USERS group.  This is used
        // by Acegi security to make sure the user logs in before he or she
        // can use the system
        this.groups.add(Group.ALL_USERS);
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    /**
     * @return true if this user can be authenticated, false if the account has
     * been disabled
     */
    public boolean isEnabled()
    {
        return !this.disabled;
    }

    /**
     * Always returns true in this implementation: we don't allow credentials to expire
     */
    public boolean isCredentialsNonExpired()
    {
        return true;
    }

    /**
     * Always returns true in this implementation: we don't lock accounts (but we
     * can disable them, see isEnabled()
     */
    public boolean isAccountNonLocked()
    {
        return true;
    }

    /**
     * Always returns true in this implementation: we don't allow accounts to expire
     * (but we could do in future
     */
    public boolean isAccountNonExpired()
    {
        return true;
    }
    
    /**
     * Add this user to the given Group
     */
    void addGroup(Group group)
    {
        this.groups.add(group);
        if (group.isAdminGroup())
        {
            this.admin = true;
        }
    }
    
    /**
     * Gets the default group for this user.  This will be the first group that
     * appears in the user's group list in the config file, or the special group
     * ROLE_ALL_USERS if the user has not been explicitly added to a group.
     */
    public Group getDefaultGroup()
    {
        Group[] groups = this.getAuthorities();
        if (groups.length > 1)
        {
            return groups[1]; // This will return the first group in the config file,
                              // remembering that the first group is ROLE_ALL_USERS
        }
        else
        {
            return groups[0]; // Returns ROLE_ALL_USERS: this happens if the user
                              // hasn't been explicitly added to any groups in the
                              // config file
        }
    }

    /**
     * Required by the UserDetails interface: used to specify the {@link Group}s to
     * which the User belongs
     * @return Array of Group objects, one for each Group to which the user belongs
     */
    public Group[] getAuthorities()
    {
        return (Group[])this.groups.toArray(new Group[0]);
    }
    
    /**
     * @return true if this user is an administrator (i.e. belongs to the 
     * "admin" group)
     */
    public boolean isAdmin()
    {
        return this.admin;
    }
    
    /**
     * @return true if this user is a member of the given group
     */
    public boolean isMemberOf(String groupName)
    {
        for (Group group : this.groups)
        {
            if (groupName.equals(group.getName()))
            {
                return true;
            }
        }
        return false;
    }

    public String getEmailAddress()
    {
        return emailAddress;
    }

    /**
     * Gets an array of names of groups to which this user belongs,
     * as recorded in the config file.  This method should not normally be used
     * (use getAuthorities() instead).
     */
    String[] getGroupNames()
    {
        if (this.groupNamesStr == null)
        {
            return new String[0];
        }
        else
        {
            return this.groupNamesStr.split(",");
        }
    }
    
}
