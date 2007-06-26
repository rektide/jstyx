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

package uk.ac.rdg.resc.grex.server;

import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.rdg.resc.grex.config.GRexConfig;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
import uk.ac.rdg.resc.grex.db.GRexServiceInstancesStore;
import uk.ac.rdg.resc.grex.exceptions.InvalidJobRunnerException;
import uk.ac.rdg.resc.grex.exceptions.JobTypeNotSupportedException;

/**
 * Bean that will be set up by the Spring framework and can be used to create
 * instances of JobRunner
 * 
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class JobRunnerFactory
{
    private static final Log log = LogFactory.getLog(JobRunnerFactory.class);
    
    /**
     * Contains all the JobRunners that have been created, keyed by
     * the unique ID of the GRexServiceInstance to which it belongs
     */
    private final Map<Integer, JobRunner> jobRunners
        = new Hashtable<Integer, JobRunner>();
    
    /**
     * Maps job types ("local", "condor" etc) to constructors for JobRunners.
     * This will be populated by setRunnerClasses(), which is called by the
     * Spring framework.
     */
    private Map<String, Constructor> runnerConstructors = null;
    
    /**
     * The configuration information for this server, injected by the Spring
     * framework.
     */
    private GRexConfig config;
    
    /**
     * The persistent store of service instances, injected by the Spring
     * framework.  (Needed for JobRunners to store changes to instance state)
     */
    private GRexServiceInstancesStore instancesStore;
    
    /**
     * Gets a JobRunner of the correct type for the given
     * GRexServiceInstance.  If a JobRunner already exists for this instance,
     * it will be returned, otherwise a new one will be created.
     */
    public synchronized final JobRunner getRunnerForInstance(GRexServiceInstance instance)
    {
        JobRunner runner = this.jobRunners.get(instance.getId());
        if (runner == null)
        {
            log.info("Creating new JobRunner object for instance " + instance.getId());
            // Find the definition of the grid service to which this instance
            // belongs.  This should never be null.
            GridServiceConfigForServer gs = this.config.getGridServiceByName(instance.getServiceName());
            if (gs == null)
            {
                throw new AssertionError("Internal error: gs is null in getRunnerForInstance()");
            }
            Constructor constructor = this.runnerConstructors.get(gs.getType());
            if (constructor == null)
            {
                // Shouldn't happen: we have checked that we can support all the
                // job types definied in the configuration object
                throw new AssertionError("Internal error: a JobRunner for job type "
                    + gs.getType() + " cannot be found");
            }
            try
            {
                // We know this cast is safe because we have checked in setRunnerClasses()
                runner = (JobRunner)constructor.newInstance();
            }
            catch(ClassCastException cce)
            {
                // Shouldn't happen: we have checked in setRunnerClasses()
                throw new AssertionError("Internal error: new object is not a JobRunner");
            }
            catch(Exception e)
            {
                // Shouldn't happen: we've checked in setRunnerClasses() that
                // the constructor is accessible.
                throw new AssertionError("Could not instantiate JobRunner of type "
                    + constructor.getDeclaringClass() + " due to a "
                    + e.getClass());
            }
            // Add the instance object to the JobRunner
            runner.setServiceInstance(instance);
            // Add the grid service configuration object
            runner.setGridServiceConfig(gs);
            // Add the persistence class so that JobRunners can save changes to state
            runner.setInstancesStore(this.instancesStore);
            // Now add the new job runner to the store
            this.jobRunners.put(instance.getId(), runner);
        }
        return runner;
    }
    
    // TODO: need a method to remove a jobrunner from the store after a job finishes
    
    /**
     * Called by the Spring framework to set the Map of job types ("local", 
     * "condor", etc) as set in the configuration file to Classes of
     * JobRunner.
     * @throws InvalidJobRunnerException if one of the Classes is not a valid
     * JobRunner, or if a class does not provide a valid no-argument constructor
     * @throws JobTypeNotSupportedException if the configuration object was set
     * before this method was called (via setGrexConfig()) and one of the gridservices
     * defined in the config has a jobType that has no corresponding JobRunner.
     */
    public void setRunnerClasses(Map<String, Class> runnerClasses)
        throws InvalidJobRunnerException, JobTypeNotSupportedException
    {
        this.runnerConstructors = new Hashtable<String, Constructor>();
        // Check that the Classes are of the correct type
        for (String jobType : runnerClasses.keySet())
        {
            Class clazz = runnerClasses.get(jobType);
            if (!JobRunner.class.isAssignableFrom(clazz))
            {
                throw new InvalidJobRunnerException(clazz + " is not a valid JobRunner");
            }
            // Get the no-argument constructor for this class.
            try
            {
                this.runnerConstructors.put(jobType, clazz.getConstructor());
            }
            catch(Exception e)
            {
                throw new InvalidJobRunnerException(clazz + " does not provide"
                    + " an accessible no-argument constructor");
            }
        }
        if (this.config != null)
        {
            // Spring must have injected the configuration object before this one,
            // so we should validate now
            this.validateJobTypes();
        }
    }
    
    /**
     * Called by the Spring framework to set the configuration information for
     * this server.  Checks that this JobRunnerFactory can handle all the jobTypes
     * in the configuration, throwing a JobTypeNotSupportedException if not.
     * Not that this means that Spring must call setRunnerClasses() before this
     * method.
     */
    public void setGrexConfig(GRexConfig config) throws JobTypeNotSupportedException
    {
        this.config = config;
        if (this.runnerConstructors != null)
        {
            // Spring must have injected the runnerClasses object before this one,
            // so we should validate now
            this.validateJobTypes();
        }
    }
    
    /**
     * When both the config and runnerConstructors have been set, this method is
     * called to validate that we can create a JobRunner for each jobType in the
     * config information
     * @throws JobTypeNotSupportedException if the configuration information
     * contains a jobType that has no corresponding JobRunner
     */
    private void validateJobTypes() throws JobTypeNotSupportedException
    {
        for (GridServiceConfigForServer gs : this.config.getGridServices())
        {
            if (!this.runnerConstructors.containsKey(gs.getType()))
            {
                throw new JobTypeNotSupportedException(gs.getType());
            }
        }
    }

    /**
     * Called by the Spring framework to inject a persistent store of service
     * instances
     */
    public void setInstancesStore(GRexServiceInstancesStore instancesStore)
    {
        this.instancesStore = instancesStore;
    }
}
