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
import org.acegisecurity.GrantedAuthority;
import simple.xml.Attribute;
import simple.xml.Root;
import simple.xml.load.Commit;
import simple.xml.load.PersistenceException;
import simple.xml.load.Validate;

/**
 * A Group (i.e. a collection of users).  Used by the Acegi security framework
 * as a role for a user.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="group")
public class Group implements GrantedAuthority
{
    /**
     * The name of the admin group (users who can do anything)
     */
    private static final String ADMIN = "admin";
    /**
     * The name of the group that contains all users.  Note: if you change this
     * property you will also need to change the settings for the 
     * filterInvocationInterceptor in applicationContext.xml.
     * This name must start with "ROLE_"
     */
    private static final String ALL_USERS_NAME = "ROLE_ALL_USERS";
    /**
     * Group that contains all users
     */
    public static final Group ALL_USERS = new Group(ALL_USERS_NAME);
    
    @Attribute(name="name")
    private String name; // The unique name for the Group
    
    @Attribute(name="description", required=false)
    private String description = "";
    
    /** Creates a new instance of Group */
    public Group()
    {
    }
    
    /**
     * Constructor for internally-created Groups
     */
    private Group(String name)
    {
        this.name = name;
    }

    /**
     * @return the name of this Group.  This is used by the Acegi framework
     * to authenticate users.
     */
    public String getAuthority()
    {
        return this.name;
    }
    
    /**
     * @return the name of this Group
     */
    public String getName()
    {
        return this.name;
    }
    
    /**
     * @return true if this is the administrators group (i.e. the group of
     * users who can do anything)
     */
    public boolean isAdminGroup()
    {
        return this.name.equals(ADMIN);
    }
    
    /**
     * Checks that we haven't tried to create a group with a reserved name
     */
    @Validate
    public void validate() throws PersistenceException
    {
        if (this.name.equals(ALL_USERS_NAME))
        {
            throw new PersistenceException("Can't create a group with the name " +
                ALL_USERS_NAME);
        }
    }
    
}
