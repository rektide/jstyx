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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
public class GRexConfig implements ApplicationContextAware
{    
    private static final Log log = LogFactory.getLog(GRexConfig.class);
    private static final String DEFAULT_HOME_DIRECTORY =
        System.getProperty("user.home") + System.getProperty("file.separator") + ".grex";
    private static final String DEFAULT_CONFIG_FILENAME = "GRexConfig.xml";
    
    /**
     * The name of the directory that will contain configuration information
     */
    private static final String CONFIG_DIRECTORY_NAME = "conf";
    /**
     * The name of the directory that will contain the GRexServiceInstancesStore
     */
    private static final String INSTANCES_STORE_DIRECTORY_NAME = "db";
    /**
     * The name of the directory that will contain the working directory of
     * the service instances
     */
    private static final String WORKING_DIRECTORY_NAME = "wd";
    
    /**
     * The home directory for this G-Rex server.  This will contain the 
     * database of service instances and all the working directories of 
     * the instances.  Defaults to $HOME/.grex
     */
    @Element(name="homeDirectory", required=false)
    private String homeDirectoryStr = DEFAULT_HOME_DIRECTORY;
    private File homeDirectory; // Home directory will be made into a File in validate()
    private File instancesStoreDirectory;
    private File masterWorkingDirectory;
    
    /**
     * The users that can access this G-Rex server
     */
    @ElementList(name="users", type=User.class)
    private ArrayList<User> users = new ArrayList<User>();
    
    /**
     * The groups (roles) of users that are used to control access
     */
    @ElementList(name="groups", type=Group.class)
    private ArrayList<Group> groups = new ArrayList<Group>();
    
    /**
     * The services that are exposed by this G-Rex server
     */
    @ElementList(name="gridservices", type=GridServiceConfigForServer.class)
    private ArrayList<GridServiceConfigForServer> gridServices = new ArrayList<GridServiceConfigForServer>();
    
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
     * Reads the configuration information, looking first in $HOME/.grex/conf, 
     * then in the CLASSPATH for a file called "GRexConfig.xml".
     */
    public static GRexConfig readConfig() throws Exception
    {
        // Look for the config file in the home directory
        File confDir = new File(DEFAULT_HOME_DIRECTORY, CONFIG_DIRECTORY_NAME);
        File confFile = new File(confDir, DEFAULT_CONFIG_FILENAME);
        if (!confFile.exists())
        {
            // We have to create this file and its parent directories
            confDir.mkdirs();
            // Look for the default config file in the classpath
            InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(DEFAULT_CONFIG_FILENAME);
            if (in == null)
            {
                throw new FileNotFoundException("Cannot find " + DEFAULT_CONFIG_FILENAME
                    + " in the CLASSPATH");
            }
            OutputStream out = new FileOutputStream(confFile);
            try
            {
                log.debug("Copying config information from classpath to " +
                    confFile.getPath());
                int len;
                byte[] buf = new byte[8192];
                while ((len = in.read(buf)) >= 0)
                {
                    out.write(buf, 0, len);
                }
            }
            finally
            {
                in.close();
                out.close();
            }
        }
        GRexConfig config = new Persister().read(GRexConfig.class, confFile);
        log.debug("Loaded configuration from " + confFile.getPath());
        return config;
    }
    
    /**
     * Private constructor to prevent direct instantiation
     */
    private GRexConfig()
    {
    }

    public List<GridServiceConfigForServer> getGridServices()
    {
        return this.gridServices;
    }
    
    /**
     * @return the GridService with the given name, or null if there is no
     * service with this name
     */
    public GridServiceConfigForServer getGridServiceByName(String serviceName)
    {
        for (GridServiceConfigForServer gs : this.gridServices)
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
     * @return a List of users that have access to the system
     */
    public List<User> getUsers()
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
        // Now create the home directory
        this.homeDirectory = new File(this.homeDirectoryStr);
        mkdir(this.homeDirectory);
        
        // Create the directories for the database and the working directories
        this.instancesStoreDirectory = new File(this.homeDirectory,
            INSTANCES_STORE_DIRECTORY_NAME);
        mkdir(this.instancesStoreDirectory);
        this.masterWorkingDirectory = new File(this.homeDirectory,
            WORKING_DIRECTORY_NAME);
        mkdir(this.masterWorkingDirectory);
        
        // Check that the Group names are unique
        List<String> groupnames = new ArrayList<String>();
        for (Group group : this.groups)
        {
            if (groupnames.contains(group.getName()))
            {
                throw new PersistenceException("Duplicate group name %s", group.getName());
            }
            groupnames.add(group.getName());
        }
        
        // Checks that all Users have unique names and belong to valid groups
        List<String> usernames = new ArrayList<String>();
        for (User user : this.users)
        {
            String username = user.getUsername();
            if (usernames.contains(username))
            {
                throw new PersistenceException("Duplicate username %s", username);
            }
            usernames.add(username);
            
            // Check that the groups are valid
            for (String groupname : user.getGroupNames())
            {
                boolean found = false;
                for (Group group : this.groups)
                {
                    if (groupname.trim().equals(group.getName()))
                    {
                        user.addGroup(group);
                        found = true;
                    }
                }
                if (!found)
                {
                    throw new PersistenceException("Unknown group name %s", groupname);
                }
            }
        }
        
        // Checks that all GridServices have unique names
        List<String> names = new ArrayList<String>();
        for (GridServiceConfigForServer gs : this.gridServices)
        {
            String name = gs.getName();
            if (names.contains(name))
            {
                throw new PersistenceException("Duplicate gridservice name %s", name);
            }
            names.add(name);
            
            // Create a working directory for this service
            File wdForService;
            
            if (gs.getPersistentDirName()=="") {
                wdForService = new File(this.masterWorkingDirectory, name);
            }
            else wdForService = new File(gs.getPersistentDirName());
            log.debug("Directory for service = " + wdForService.getPath());
             
            //wdForService = new File(this.masterWorkingDirectory, name);
            gs.setWorkingDirectory(wdForService);
            mkdir(wdForService);
        }
        
        // Check that the security settings for GridServices are correctly formed
        // (with valid user and group names)
        for (GridServiceConfigForServer gs : this.gridServices)
        {
            if (gs.getAllowedUsers() != null)
            {
                for (String username : gs.getAllowedUsers())
                {
                    if (!usernames.contains(username))
                    {
                        throw new PersistenceException("Unknown username %s in " +
                            "allowed-users", username);
                    }
                }
            }            
            if (gs.getAllowedGroups() != null)
            {
                for (String groupname : gs.getAllowedGroups())
                {
                    if (!groupnames.contains(groupname))
                    {
                        throw new PersistenceException("Unknown group name %s in " +
                            "allowed-groups", groupname);
                    }
                }
            }
        }
        
        log.debug("GRex config validated successfully.");
    }
    
    /**
     * Creates a directory with the given path.  If the directory already
     * exists this does nothing.  If the directory cannot be created, or if a
     * file with the same name already exists, this throws a PersistenceException.
     */
    private static void mkdir(File dir) throws PersistenceException
    {
        if (dir.exists())
        {
            if (!dir.isDirectory())
            {
                throw new PersistenceException("A file with the name " +
                    dir.getPath() + " already exists: directory not created");
            }
        }
        else
        {
            boolean created = dir.mkdir();
            if (!created)
            {
                throw new PersistenceException("Directory " +
                    dir.getPath() + " could not be created");
            }
        }
    }

    /**
     * @return the directory that will contain the database of service instances
     * (the GRexServiceInstancesStore)
     */
    public File getInstancesStoreDirectory()
    {
        return this.instancesStoreDirectory;
    }

    /**
     * @return the directory that will contain all the working directories of
     * the service instances
     */
    public File getMasterWorkingDirectory()
    {
        return this.masterWorkingDirectory;
    }

    /**
     * Called by the Spring framework after this object has been initialized.
     * This method sets the list of users in the UserService
     */
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException
    {
        // Set the admin password in the Users bean, which we'll need to
        // get from the app context
        UserService userService = (UserService)applicationContext.getBean("userService");
        if (userService == null)
        {
            log.error("Could not retrieve UserService object from application context");
        }
        else
        {
            log.debug("Setting list of users in userService");
            userService.setUsers(this.users);
        }
    }
}
