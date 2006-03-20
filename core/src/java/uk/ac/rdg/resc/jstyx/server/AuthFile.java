/*
 * Copyright (c) 2006 The University of Reading
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

package uk.ac.rdg.resc.jstyx.server;

import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * StyxFile that is used to exchange authentication information.  Clients must
 * write their password into this file.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2006/03/20 17:51:50  jonblower
 * Adding authentication to base JStyx system
 *
 */
public class AuthFile extends InMemoryFile
{
    private StyxSecurityContext securityContext;
    private User user;
    
    /**
     * Creates a new instance of AuthFile
     * @param securityContext The security context for this connection
     * @param username The name of the user (as claimed by the user)
     * @throws StyxSecurityException if the user could not be identified or 
     * if there was an error reading from the security context information
     */
    public AuthFile(StyxSecurityContext securityContext, String username)
        throws StyxException
    {
        super("auth", "nobody", "nobody", 0666, false, false);
        this.auth = true;
        this.securityContext = securityContext;
        this.user = this.securityContext.getUser(username);
    }
    
    /**
     * @return true if the user with the given name has successfully authenticated
     */
    public boolean isAuthenticated(String username)
    {
        if (this.user.getUsername().trim().equals(username) &&
            this.user.passwordMatches(this.getContents()))
        {
            return true;
        }
        return false;
    }
    
    /**
     * @return the User that this AuthFile belongs to
     */
    public User getUser()
    {
        return this.user;
    }
    
}
