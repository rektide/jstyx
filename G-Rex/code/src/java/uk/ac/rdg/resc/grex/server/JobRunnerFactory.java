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
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
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
    
    /**
     * Contains all the JobRunners that have been created, keyed by
     * the unique ID of the GRexServiceInstance to which it belongs
     */
    private final Map<Integer, JobRunner> jobRunners = new Hashtable<Integer, JobRunner>();
    
    /**
     * Maps job types ("local", "condor" etc) to constructors for JobRunners.
     * This will be populated by setRunnerClasses(), which is called by the
     * Spring framework.
     */
    private Map<String, Constructor<JobRunner>> runnerConstructors;
    
    /**
     * Called by the Spring framework to set the Map of job types ("local", 
     * "condor", etc) as set in the configuration file to Classes of
     * JobRunner.
     * @throws InvalidJobRunnerException if one of the Classes is not a valid
     * JobRunner, or if a class does not provide a valid no-argument constructor
     */
    public void setRunnerClasses(Map<String, Class> runnerClasses)
        throws InvalidJobRunnerException
    {
        this.runnerConstructors = new Hashtable<String, Constructor<JobRunner>>();
        // Check that the Classes are of the correct type
        for (String jobType : runnerClasses.keySet())
        {
            Class clazz = runnerClasses.get(jobType);
            if (!JobRunner.class.isAssignableFrom(clazz))
            {
                throw new InvalidJobRunnerException(clazz + " is not a valid JobRunner");
            }
            // Get the no-argument constructor for this class
            try
            {
                Constructor<JobRunner> constructor = clazz.getConstructor();
                this.runnerConstructors.put(jobType, constructor);
            }
            catch(Exception e)
            {
                throw new InvalidJobRunnerException(clazz + " does not provide"
                    + " an accessible no-argument constructor");
            }
        }
    }
    
    /**
     * Gets a JobRunner of the given type ("local", "condor", etc) for the given
     * GRexServiceInstance.  If a JobRunner already exists for this instance,
     * it will be returned, otherwise a new one will be created.
     * @throws JobTypeNotSupportedException if the given job type is not supported
     */
    public synchronized final JobRunner getRunnerForInstance(GRexServiceInstance instance,
        String jobType) throws JobTypeNotSupportedException
    {
        JobRunner runner = this.jobRunners.get(instance.getId());
        if (runner == null)
        {
            Constructor<JobRunner> constructor = this.runnerConstructors.get(jobType);
            if (constructor == null)
            {
                throw new JobTypeNotSupportedException(jobType);
            }
            try
            {
                runner = constructor.newInstance();
                // Now add the new job runner to the store
                this.jobRunners.put(instance.getId(), runner);
            }
            catch(Exception e)
            {
                // Shouldn't happen: we've checked in setRunnerClasses() that
                // the constructor is accessible.
                throw new AssertionError("Could not instantiate JobRunner of type "
                    + constructor.getDeclaringClass() + " due to a "
                    + e.getClass());
            }
        }
        return runner;
    }
    
    /**
     * @return true if the given job type ("local", "condor" etc) is supported.
     */
    public boolean supportsJobType(String jobType)
    {
        return this.runnerConstructors.containsKey(jobType);
    }
    
    // TODO: need a method to remove a jobrunner from the store
    
}
