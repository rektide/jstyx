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

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import simple.xml.Attribute;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.load.PersistenceException;
import simple.xml.load.Validate;

/**
 * Configuration object that describes a particular Grid Service (i.e. an
 * executable that is exposed for remote execution) from the point of view of the
 * server.  Note that much of this information is also useful to the client, and
 * so these bits of information are contained in GridServiceConfigForClient,
 * which this extends.
 * 
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="gridservice")
public class GridServiceConfigForServer extends GridServiceConfigForClient
{
    
    @Attribute(name="type", required=false) // TODO: make an enumeration?
    private String type = "local"; // Type of this service ("local", "condor" etc)
    
    @Attribute(name="command")
    private String command; // Command that will be run when this service is executed
    
    // Note that we will disallow setting both allowed-users and allowed-groups
    
    @Attribute(name="allowed-users", required=false)
    private String allowedUsersStr = ""; // Set this if we want to restrict access to
                                         // this service to certain (comma-separated) users
    private List<String> allowedUsers = null; // will be set on validate()
    
    @Attribute(name="allowed-groups", required=false)
    private String allowedGroupsStr = ""; // Set this if we want to restrict access to
                                         // this service to certain (comma-separated) groups
    private List<String> allowedGroups = null; // will be set on validate
    
    /**
     * The options that are used to provide more information about how the
     * service is to be run (e.g. extra properties for a Condor submit file)
     */
    @ElementList(name="options", type=Option.class, required=false)
    private ArrayList<Option> options = new ArrayList<Option>();
    
    // The directory that contains all the working directories of instances
    // of this service.  This is set by GRexConfig.validate()
    private File workingDirectory;

    public String getCommand()
    {
        return command;
    }

    public String getType()
    {
        return type;
    }

    public List<Option> getOptions()
    {
        return options;
    }
    
    /**
     * Checks that all the names are unique and that only zero or one Parameters
     * are marked greedy.  Also check that the security settings (allowed-users
     * and allowed-groups) are sane.
     */
    @Validate
    public void validate() throws PersistenceException
    {
        // Check that the parameters have been set correctly
        super.validate();
        
        // Check that permissions have been set correctly (can't have both 
        // allowed-users and allowed-groups)
        if (!this.allowedUsersStr.trim().equals(""))
        {
            // We're restricting access by user name
            this.allowedUsers = new ArrayList<String>();
            for (String allowedUser : this.allowedUsersStr.split(","))
            {
                this.allowedUsers.add(allowedUser.trim());
            }
        }
        
        if (!this.allowedGroupsStr.trim().equals(""))
        {
            if (this.allowedUsers != null)
            {
                throw new PersistenceException("Cannot set both allowed-users " +
                    "and allowed-groups");
            }
            // We're restricting access by group name
            this.allowedGroups = new ArrayList<String>();
            for (String allowedGroup : this.allowedGroupsStr.split(","))
            {
                this.allowedGroups.add(allowedGroup.trim());
            }
        }
    }

    /**
     * 
     * 
     * @return true if the given user is permitted access to thisGridServiceConfigForServere
     * (i.e. can view its details and create new instances)
     */
    public boolean canBeAccessedBy(User user)
    {
        if (user.isAdmin())
        {
            return true;
        }
        else if (this.allowedUsers != null)
        {
            // we are restricting by username
            return this.allowedUsers.contains(user.getUsername());
        }
        else if (this.allowedGroups != null)
        {
            // we are restricting by group
            for (Group group : user.getAuthorities())
            {
                if (this.allowedGroups.contains(group.getName()))
                {
                    return true;
                }
            }
            // User doesn't belong to any of the required groups
            return false;
        }
        else
        {
            // We are not restricting access to this GridServiceConfigForServer
            return true;
        }
    }
    
    /**
     * @return the usernames that are allowed to access this service,
     * or null if there is no restriction on usernames
     */
    List<String> getAllowedUsers()
    {
        return this.allowedUsers;
    }
    
    /**
     * @return the group names that are allowed to access this service,
     * or null if there is no restriction on groups
     */
    List<String> getAllowedGroups()
    {
        return this.allowedGroups;
    }

    /**
     * @return the directory that contains all the working directories
     * for the instances of this service
     */
    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    /**
     * Sets the directory that contains all the working directories
     * for the instances of this service.  This is called by
     * GRexConfig.validate() and should not be called otherwise.
     */
    public void setWorkingDirectory(File workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }
    
}
