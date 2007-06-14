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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import simple.xml.Element;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.load.PersistenceException;
import simple.xml.load.Persister;
import simple.xml.load.Validate;

/**
 * Class that configures a G-Rex server by describing all the services it
 * exposes.  The configuration information is read from an XML file using the
 * Simple XML library.
 *
 * This also acts as the interface for the Acegi framework to retrieve user
 * details, hence it implements UserDetailsService.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="grex")
public class GRexConfig implements UserDetailsService
{
    private static final Log log = LogFactory.getLog(GRexConfig.class);
    private static final String DEFAULT_CONFIG_FILENAME = "GRexConfig.xml";
    
    /**
     * The home directory for this G-Rex server.  This will contain the 
     * database of service instances and all the working directories of 
     * the instances.  Defaults to $HOME/.grex
     */
    @Element(name="homeDirectory", required=false)
    private String homeDirectoryStr = System.getProperty("user.home") +
        System.getProperty("file.separator") + ".grex";
    private File homeDirectory; // Home directory will be made into a File in validate()
    
    /**
     * The users that can access this G-Rex server
     */
    @ElementList(name="users", type=User.class)
    private Vector<User> users = new Vector<User>();
    
    /**
     * The groups (roles) of users that are used to control access
     */
    @ElementList(name="groups", type=Group.class)
    private Vector<Group> groups = new Vector<Group>();
    
    /**
     * The services that are exposed by this G-Rex server
     */
    @ElementList(name="gridservices", type=GridService.class)
    private Vector<GridService> gridServices = new Vector<GridService>();
    
    /**
     * Creates a new instance of GRexConfig by reading the config information
     * from the file with the given path
     */
    public static GRexConfig readConfig(String configFilePath) throws Exception
    {
        File configFile = new File(configFilePath);
        GRexConfig config = new Persister().read(GRexConfig.class, configFile);
        log.debug("Loaded configuration from " + configFile.getPath());
        return config;
    }
    
    /**
     * Reads the configuration information, looking in the CLASSPATH for 
     * a file called "GRexConfig.xml"
     */
    public static GRexConfig readConfig() throws Exception
    {
        InputStream in = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(DEFAULT_CONFIG_FILENAME);
        if (in == null)
        {
            throw new Exception("Cannot find " + DEFAULT_CONFIG_FILENAME
                + " in the CLASSPATH");
        }
        GRexConfig config = new Persister().read(GRexConfig.class, in);
        log.debug("Loaded configuration from " + DEFAULT_CONFIG_FILENAME
            + " (found in classpath)");
        return config;
    }
    
    /**
     * Private constructor to prevent direct instantiation
     */
    private GRexConfig()
    {
    }

    public Vector<GridService> getGridServices()
    {
        return gridServices;
    }
    
    /**
     * @return the GridService with the given name, or null if there is no
     * service with this name
     */
    public GridService getGridServiceByName(String serviceName)
    {
        for (GridService gs : this.gridServices)
        {
            if (gs.getName().equals(serviceName))
            {
                return gs;
            }
        }
        return null;
    }
    
    /**
     * @return the home directory for this G-Rex server.  This is guaranteed
     * to exist.
     */
    public File getHomeDirectory()
    {
        return this.homeDirectory;
    }
    
    /**
     * @return a Vector of users that have access to the system
     */
    public Vector<User> getUsers()
    {
        return this.users;
    }
    
    /**
     * Checks that the data we have read are valid.  Checks that there are no
     * duplicate names for GridServices and that the home directory can be
     * created.
     */
    @Validate
    public void validate() throws PersistenceException
    {
        // Checks that all Users have unique names
        List<String> usernames = new ArrayList<String>();
        for (User user : this.users)
        {
            String username = user.getUsername();
            if (usernames.contains(username))
            {
                throw new PersistenceException("Duplicate username %s", username);
            }
            usernames.add(username);
        }
        // TODO: check that the groups the users belong to are valid
        // and set the GrantedAuthorities[] property properly
        
        // Checks that all GridServices have unique names
        List<String> names = new ArrayList<String>();
        for (GridService gs : this.gridServices)
        {
            String name = gs.getName();
            if (names.contains(name))
            {
                throw new PersistenceException("Duplicate gridservice name %s", name);
            }
            names.add(name);
        }
        // Now create the home directory
        this.homeDirectory = new File(this.homeDirectoryStr);
        if (this.homeDirectory.exists())
        {
            if (!this.homeDirectory.isDirectory())
            {
                throw new PersistenceException("Home directory " +
                    this.homeDirectory.getPath() + " already exists as a file");
            }
        }
        else
        {
            boolean created = this.homeDirectory.mkdir();
            if (!created)
            {
                throw new PersistenceException("Home directory " +
                    this.homeDirectory.getPath() + " could not be created");
            }
        }
        log.debug("GRex config validated successfully.");
    }

    /**
     * Retrieves a user's details from his or her username
     * @throws UsernameNotFoundException if the user does not exist
     * @throws DataAccessException if there was an error accessing the user
     * details (will not happen here as the user details are in memory)
     */
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException, DataAccessException
    {
        for (User user : this.users)
        {
            if (user.getUsername().equals(username))
            {
                return user;
            }
        }
        // If we've got this far we haven't found the user
        throw new UsernameNotFoundException(username + " not found");
    }
}
