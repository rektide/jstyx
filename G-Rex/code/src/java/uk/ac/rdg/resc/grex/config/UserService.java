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

import java.util.List;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataAccessException;

/**
 * Provides access to the database of users that are allowed to access this
 * G-Rex server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class UserService implements UserDetailsService
{
    
    private List<User> users = null; // This will be set once the config information is read

    /**
     * Retrieves a user's details from his or her username.  Required by
     * Acegi security.
     * @throws UsernameNotFoundException if the user does not exist
     * @throws DataAccessException if there was an error accessing the user
     * details (will not happen here as the user details are in memory)
     */
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException, DataAccessException
    {
        if (this.users != null)
        {
            for (User user : this.users)
            {
                if (user.getUsername().equals(username))
                {
                    return user;
                }
            }
        }
        // If we've got this far we haven't found the user
        throw new UsernameNotFoundException(username + " not found");
    }

    /**
     * This is called by the GRexConfig object when it has been initialized
     * to populate the user database
     */
    public void setUsers(List<User> users)
    {
        this.users = users;
    }
    
}
