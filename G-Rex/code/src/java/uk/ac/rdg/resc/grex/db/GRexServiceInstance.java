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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
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
    private int id = 0; // Unique ID for this instance.  Setting id = 0
                     // is the signal for the database to create a new 
                     // primary key automatically from a sequence.  See the
                     // Javadoc for PrimaryKey.
    
    @SecondaryKey(relate=Relationship.MANY_TO_ONE) // A service may contain many Instances
    // WARNING! If you change the name of this field, you also need to change
    // the constructor for the secondary key in InstancesStoreBerkeley!
    private String serviceName; // Unique ID of the service to which this instance belongs
    
    private String description = null; // Free-text description provided by the creator of the instance
    
    private String baseUrl = ""; // Base of the URL to this instance, e.g.
                                 // "http://myserver.com/G-Rex/helloworld/instances/"
    
    private String owner = ""; // Name of the user that owns this instance
    private String group = ""; // Name of the Group that owns this instance
    
    private boolean interactive = false; // TODO: in future, support interactive jobs
    
    // Permissions for this service instance
    public enum Permissions{NONE, READONLY, FULL};
    // Default permissions for owner, group members and all others
    private Permissions ownerPermissions = Permissions.FULL;
    private Permissions groupPermissions = Permissions.READONLY;
    private Permissions otherPermissions = Permissions.NONE;
    
    /**
     * The master Job for this instance.  In many cases this will be the only
     * Job that exists, but for composite jobs the master job contains the files
     * and parameters that are common to all sub-jobs.
     */
    private Job masterJob = new Job();
    
    /**
     * The sub-jobs (if any) that belong to this instance
     */
    private List<Job> subJobs = new ArrayList<Job>();
    
    /**
     * The configuration information for the service to which this instance
     * belongs.  This is not persisted to the database.
     */
    private transient GridServiceConfigForServer gsConfig;

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
        return this.masterJob.getWorkingDirectory();
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        this.masterJob.setWorkingDirectory(workingDirectory);
    }
    
    /**
     * @return a java.io.File representation of the working directory of this
     * job.  A new File object is created with each invocation.
     */
    public File getWorkingDirectoryFile()
    {
        return this.masterJob.getWorkingDirectoryFile();
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

    /**
     * @return the overall state of this service instance.
     */
    public Job.State getState()
    {
        return this.masterJob.getState();
    }

    public void setState(Job.State state)
    {
        this.masterJob.setState(state);
    }
    
    /**
     * @return a Map of the parameters that have been set on this service instance
     */
    public Map<String, String> getParameters()
    {
        return this.masterJob.getParameters();
    }
    
    /**
     * @return the value of the parameter with the given name, or null if a
     * value has not been set
     */
    public String getParamValue(String name)
    {
        return this.masterJob.getParamValue(name);
    }
    
    /**
     * Sets a value of a parameter on this service instance
     */
    public void setParameter(String name, String value)
    {
        this.getParameters().put(name, value);
    }

    /**
     * Gets the exit code as an Integer, or null if the exit code has not yet
     * been set
     */
    public Integer getExitCode()
    {
        return this.masterJob.getExitCode();
    }

    public void setExitCode(Integer exitCode)
    {
        this.masterJob.setExitCode(exitCode);
    }
    
    /**
     * @return true if this instance has finished running (delegates to 
     * this.masterJob.isFinished())
     */
    public boolean isFinished()
    {
        return this.masterJob.isFinished();
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
     * @return true if this is an interactive job.  This does nothing currently:
     * interactive jobs are not yet supported.
     */
    public boolean isInteractive()
    {
        return interactive;
    }

    /** 
     * Use this method to declare the job to be interactive, i.e. will allow the
     * user to enter data on the standard input during job execution.  (Does
     * nothing currently: interactive jobs are not yet supported.)
     */
    public void setInteractive(boolean interactive)
    {
        this.interactive = interactive;
    }

    public List<Job> getSubJobs()
    {
        return subJobs;
    }

    /**
     * @return the sub-job with the given id
     * @throws ArrayIndexOutOfBoundsException if there is no sub-job with the 
     * given id
     */
    public Job getSubJob(int subJobId)
    {
        return this.subJobs.get(subJobId);
    }

    public int getNumSubJobs()
    {
        return this.subJobs.size();
    }
}
