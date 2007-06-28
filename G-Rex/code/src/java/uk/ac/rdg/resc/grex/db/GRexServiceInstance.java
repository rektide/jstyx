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

package uk.ac.rdg.resc.grex.db;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import com.sleepycat.persist.model.Relationship;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.oro.io.GlobFilenameFilter;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.config.Output;
import uk.ac.rdg.resc.grex.config.User;
import uk.ac.rdg.resc.grex.server.OutputFile;

/**
 * Java Bean that represents the state of a particular service instance.  These
 * objects are stored in the Berkeley DB, indexable by instance ID and service ID.
 * Note that instance IDs are unique to all services, i.e. two instances will never
 * share the same ID, even if they belong to different services.
 *
 * @todo When this class changes, need to assign a higher version number - how
 * do we do this?
 * @todo Make this class immutable for thread safety
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Entity
public class GRexServiceInstance
{
    @PrimaryKey(sequence="ID") // Auto-generate the ID
    private int id; // Unique ID for this instance
    
    @SecondaryKey(relate=Relationship.MANY_TO_ONE) // A service may contain many Instances
    // WARNING! If you change the name of this field, you also need to change
    // the constructor for the secondary key in InstancesStoreBerkeley!
    private String serviceName; // Unique ID of the service to which this instance belongs
    
    private String description = null; // Free-text description provided by the creator of the instance
    
    private String baseUrl = ""; // Base of the URL to this instance, e.g.
                                 // "http://myserver.com/G-Rex/helloworld/instances/"
    
    // Directory in which all files relating to this instance will be kept
    // Note that the database doesn't know how to persist java.io.File
    private String workingDirectory;
    
    // Contains the names and values of all parameters that are set on this instance
    private Map<String, String> params = new HashMap<String, String>();
    
    private String owner = ""; // Name of the user that owns this instance
    private String group = ""; // Name of the Group to which the user belongs
    
    // The state of this service instance
    // This describes the overall (coarse-grained) state of the instance.
    public enum State{CREATED, RUNNING, FINISHED, ABORTED, ERROR};
    private State state;
    
    private Integer exitCode = null; // Will be set when the instance has finished
    
    // Permissions for this service instance
    public enum Permissions{NONE, READONLY, FULL};
    // Default permissions for owner, group members and all others
    private Permissions ownerPermissions = Permissions.FULL;
    private Permissions groupPermissions = Permissions.READONLY;
    private Permissions otherPermissions = Permissions.NONE;
    
    /**
     * The configuration information for the service to which this instance
     * belongs.  This is not persisted to the database.
     */
    private transient GridServiceConfigForServer gsConfig;
    
    /**
     * Creates a new instance of GRexServiceInstance
     */
    public GRexServiceInstance()
    {
        this.id = 0; // This is the signal for the database to create a new 
                     // primary key automatically from a sequence.  See the
                     // Javadoc for PrimaryKey.
        this.setState(State.CREATED);
    }

    public int getId()
    {
        return id;
    }
    
    /**
     * This method should not be called directly: the database system assigns
     * primary keys automatically.
     */
    public void setId(int id)
    {
        this.id = id;
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public void setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }
    
    /**
     * @return the description of this instance, or null if none has been set
     */
    public String getDescription()
    {
        return this.description;
    }
    
    public void setDescription(String desc)
    {
        this.description = desc;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup(String group)
    {
        this.group = group;
    }

    public Permissions getOwnerPermissions()
    {
        return ownerPermissions;
    }

    public void setOwnerPermissions(Permissions ownerPermissions)
    {
        this.ownerPermissions = ownerPermissions;
    }

    public Permissions getGroupPermissions()
    {
        return groupPermissions;
    }

    public void setGroupPermissions(Permissions groupPermissions)
    {
        this.groupPermissions = groupPermissions;
    }

    public Permissions getOtherPermissions()
    {
        return otherPermissions;
    }

    public void setOtherPermissions(Permissions otherPermissions)
    {
        this.otherPermissions = otherPermissions;
    }
    
    /**
     * Sets the base URL for this instance, e.g. "http://myserver.com/G-Rex/helloworld/instances/".
     * Should not be called directly (this is called by PostOperationsController
     * when creating a new service instance)
     */
    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }
    
    /**
     * @return the full URL to this service instance
     */
    public String getUrl()
    {
        return this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + this.id;
    }
    
    /**
     * @return true if the given user cen read information from this instance
     */
    public boolean canBeReadBy(User user)
    {
        if (user.isAdmin())
        {
            return true;
        }
        else if (this.owner.equals(user.getUsername()) &&
            this.ownerPermissions != Permissions.NONE)
        {
            return true;
        }
        else if (user.isMemberOf(this.group) &&
            this.groupPermissions != Permissions.NONE)
        {
            return true;
        }
        else if (this.otherPermissions != Permissions.NONE)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * @return true if the given user can modify this service (e.g. change
     * permissions, start and stop)
     */
    public boolean canBeModifiedBy(User user)
    {
        if (user.isAdmin())
        {
            return true;
        }
        else if (this.owner.equals(user.getUsername()) &&
            this.ownerPermissions == Permissions.FULL)
        {
            return true;
        }
        else if (user.isMemberOf(this.group) &&
            this.groupPermissions == Permissions.FULL)
        {
            return true;
        }
        else if (this.otherPermissions == Permissions.FULL)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public State getState()
    {
        return state;
    }

    public void setState(State state)
    {
        this.state = state;
    }
    
    /**
     * @return a Map of the parameters that have been set on this service instance
     */
    public Map<String, String> getParameters()
    {
        return this.params;
    }
    
    /**
     * @return the value of the parameter with the given name, or null if a
     * value has not been set
     */
    public String getParamValue(String name)
    {
        return this.params.get(name);
    }
    
    /**
     * Sets a value of a parameter on this service instance
     */
    public void setParameter(String name, String value)
    {
        this.params.put(name, value);
    }

    /**
     * Gets the exit code as an Integer, or null if the exit code has not yet
     * been set
     */
    public Integer getExitCode()
    {
        return this.exitCode;
    }

    public void setExitCode(Integer exitCode)
    {
        this.exitCode = exitCode;
    }
    
    /**
     * @return true if this instance has finished running (i.e. if state is
     * FINISHED, ERROR or ABORTED
     */
    public boolean isFinished()
    {
        return this.state == State.FINISHED || this.state == State.ABORTED
            || this.state == State.ERROR;
    }

    public void setGridServiceConfig(GridServiceConfigForServer gsConfig)
    {
        this.gsConfig = gsConfig;
    }

    public GridServiceConfigForServer getGridServiceConfig()
    {
        return this.gsConfig;
    }
    
    /**
     * @return a List of OutputFiles in the working directory of this
     * instance that are available for downloading now, or will be available for
     * downloading when the job has finished.  This method is called by the JSPs
     * that provide XML and HTML output to the web.
     * @todo What happens with directories?
     */
    public List<OutputFile> getCurrentOutputFiles()
    {
        List<OutputFile> files = new ArrayList<OutputFile>();
        File wd = new File(this.workingDirectory);
        
        // Search through all output patterns specified in this configuration and
        // see which files in the working directory match
        for (Output op : this.gsConfig.getOutputs())
        {
            String name = op.getName();
            if (op.getLinkedParameterName() != null)
            {
                // The name of the file comes from the value of this parameter
                name = this.getParamValue(op.getLinkedParameterName());
            }
            // Treat the name of the output as a glob pattern
            FilenameFilter filter = new GlobFilenameFilter(name);
            // Loop over all the files that match the glob pattern
            // TODO does this iterate over subdirectories?
            for (File f : wd.listFiles(filter))
            {
                // Note that this will overwrite previous entries if a file
                // with the same path has already been recorded.  Therefore
                // entries lower in the list of outputs in the config file
                // have priority.  TODO: is this intuitive?
                
                // Note: this is an O(N2) algorithm, but hopefully this isn't
                // important in the grand scheme of things.
                OutputFile opf = new OutputFile(f, this, op.isAppendOnly());
                for (OutputFile existingFile : files)
                {
                    if (opf.getFile().equals(existingFile.getFile()))
                    {
                        // remove the previous file before adding the new one
                        files.remove(existingFile);
                        break;
                    }
                }
                files.add(opf);
            }
        }
        
        return files;
    }
}
