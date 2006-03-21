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

/**
 * Class representing the user at the other end of a Styx connection.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2006/03/21 14:58:42  jonblower
 * Implemented clear-text password-based authentication and did some simple tests
 *
 * Revision 1.2  2006/03/21 09:06:15  jonblower
 * Still implementing authentication
 *
 * Revision 1.1  2006/03/20 17:51:50  jonblower
 * Adding authentication to base JStyx system
 *
 */
public class User
{
    private StyxSecurityContext securityContext;
    private String username;
    private String password;
    private String fullName;
    
    /**
     * Creates a new instance of User
     */
    public User(StyxSecurityContext securityContext, String username,
        String password, String fullName)
    {
        this.securityContext = securityContext;
        this.username = username.trim();
        this.password = password.trim();
        this.fullName = fullName.trim();
    }
    
    public String getUsername()
    {
        return this.username;
    }
    
    public String getFullName()
    {
        return this.fullName;
    }
    
    public boolean passwordMatches(String otherPassword)
    {
        return this.password.equals(otherPassword);
    }
    
    /**
     * @return true if this user is a member of the group with the given name.
     * See StyxSecurityContext.isMember()
     */
    public boolean isMemberOf(String groupName)
    {
        return this.securityContext.isMember(this.username, groupName);
    }
    
}
