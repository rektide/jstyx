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

import javax.net.ssl.SSLContext;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Describes security information for a Styx server.  This is used for finding
 * valid users and groups and whether or not anonymous logins are allowed.
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
public class StyxSecurityContext
{
    private static final Logger log = Logger.getLogger(StyxSecurityContext.class);
    
    private String securityFile;
    private boolean allowAnonymousLogin;
    private boolean supportAuthentication;
    private SSLContext sslContext;
    
    /**
     * Creates a security context in which anonymous logins are allowed, 
     * SSL is not used and authentication is not supported
     */
    public StyxSecurityContext()
    {
        this.allowAnonymousLogin = true;
        this.supportAuthentication = false;
        this.sslContext = null;
    }
    
    /**
     * Creates a new StyxSecurityContext from a configuration file
     * @param securityFile XML File from which security information is to be read.
     * If this is null, access to the server will be anonymous and unsecured.
     * @throws StyxSecurityException if the security config file could not be read
     */
    public StyxSecurityContext(String securityFile) throws StyxSecurityException
    {
        try
        {
            this.securityFile = securityFile;
            if (this.securityFile == null)
            {
                throw new IllegalArgumentException("securityFile cannot be null");
            }
            // Validate the input file with the DTD
            SAXReader reader = new SAXReader(true);
            Document doc = reader.read(securityFile);
            Node serverNode = doc.selectSingleNode("security/server");
            this.allowAnonymousLogin =
                serverNode.valueOf("@allowAnonymousLogin").equalsIgnoreCase("true");
            this.supportAuthentication = true;
            if (serverNode.valueOf("useSSL").equalsIgnoreCase("true"))
            {
                // TODO Set up SSL
            }
        }
        catch (DocumentException de)
        {
            throw new StyxSecurityException("Error reading security config file "
                + securityFile + ": " + de.getMessage());
        }
    }
    
    /**
     * @return true if this security context allows users to login anonymously
     */
    public boolean allowsAnonymousLogin()
    {
        return this.allowAnonymousLogin;
    }
    
    /**
     * @return true if this server supports authentication
     */
    public boolean supportsAuthentication()
    {
        return this.supportAuthentication;
    }
    
    /**
     * @return the SSLContext for this server, or null if we are not using SSL
     */
    public SSLContext getSSLContext()
    {
        return this.sslContext;
    }
    
    /**
     * @return a User object for the anonymous user
     */
    public User getAnonymousUser()
    {
        return new User(this, StyxUtils.ANONYMOUS_USER, "", "Anonymous User");
    }
    
    /**
     * @return an object describing the user with the given username
     * @throws StyxSecurityException if a user with the given name does not
     * exist in this security context or if this server does not support authentication
     * or if there was an error finding the user's details
     */
    public User getUser(String username) throws StyxSecurityException
    {
        if (this.supportAuthentication)
        {
            try
            {
                SAXReader reader = new SAXReader(true);
                Document doc = reader.read(securityFile);
                Node userNode =
                    doc.selectSingleNode("security/users/user[@name='" +
                    username + "']");
                if (userNode == null)
                {
                    throw new StyxSecurityException("User " + username + " not found");
                }
                String password = userNode.selectSingleNode("password").getText();
                String fullName = userNode.selectSingleNode("fullName").getText();
                return new User(this, username, password, fullName);
            }
            catch(DocumentException de)
            {
                throw new StyxSecurityException("Error reading security config file "
                    + securityFile + ": " + de.getMessage());
            }
        }
        else
        {
            throw new StyxSecurityException("Server does not support authentication");
        }
    }
    
    /**
     * @return true if the given user is a member of the given group.  If the
     * group is the default group (StyxUtils.DEFAULT_GROUP) this will return true
     * as all users are members of this group.  If the user is the anonymous user
     * (StyxUtils.ANONYMOUS_USER) this will return false unless the group is the
     * default group.
     */
    public boolean isMember(String username, String group)
    {
        log.debug("Checking to see if " + username + " is a member of " + group);
        if (group.equals(StyxUtils.DEFAULT_GROUP))
        {
            // All users belong to the default group
            return true;
        }
        else if (username.equals(StyxUtils.ANONYMOUS_USER))
        {
            // User is not authenticated
            return false;
        }
        else
        {
            try
            {
                // Look inside the config file
                SAXReader reader = new SAXReader(true);
                Document doc = reader.read(this.securityFile);
                Node groupNode =
                    doc.selectSingleNode("security/groups/group[@name='" +
                    group + "']");
                if (groupNode == null)
                {
                    log.debug("Group " + group + " not found");
                    return false;
                }
                else
                {
                    Iterator usernames = groupNode.selectNodes("username").iterator();
                    while (usernames.hasNext())
                    {
                        Node usernameNode = (Node)usernames.next();
                        if (usernameNode.getText().trim().equals(username))
                        {
                            // This user is a member of the group
                            return true;
                        }
                    }
                    // We haven't found the user's name in this group
                    return false;
                }
            }
            catch(DocumentException de)
            {
                // We can't check the user's group credentials
                log.error("Error occurred getting details for group " + group
                    + " from " + this.securityFile);
                return false;
            }
        }
    }
    
    /**
     * Simple test routine
     */
    public static void main(String[] args) throws Exception
    {
        StyxSecurityContext context = new StyxSecurityContext("E:\\Jon's Documents" +
            "\\work\\java\\JStyx\\core\\conf\\styxSecurity.xml");
        User jdb = context.getUser("jdb");
    }
}
