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

import java.util.Hashtable;
import java.util.Map;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;

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
     * Maps job types ("local", "condor" etc) to concrete implementations of
     * JobRunner.  Will be populated by the Spring framework
     */
    private Map<String, Class> runnerClasses;
    
    /**
     * Called by the Spring framework to set the Map of job types ("local", 
     * "condor", etc) as set in the configuration file to Classes of
     * JobRunner.
     */
    public void setRunnerClasses(Map<String, Class> runnerClasses)
    {
        // Check that the Classes are of the correct type
        for (Class clazz : runnerClasses.values())
        {
            if (!JobRunner.class.isAssignableFrom(clazz))
            {
                // TODO: throw an Exception
            }
        }
        this.runnerClasses = runnerClasses;
    }
    
    /**
     * Gets a JobRunner of the given type ("local", "condor", etc) for the given
     * GRexServiceInstance.  If a JobRunner already exists for this instance,
     * it will be returned, otherwise a new one will be created.
     */
    public synchronized final JobRunner getRunnerForInstance(GRexServiceInstance instance,
        String jobType)
    {
        JobRunner runner = this.jobRunners.get(instance.getId());
        if (runner == null)
        {
            Class runnerClass = this.runnerClasses.get(jobType);
            if (runnerClass == null)
            {
                // TODO: throw an Exception
            }
            Object runnerObj = runnerClass.newInstance();
            // We know that the cast in the line below is safe because we have
            // checked in setJobRunners.
            runner = (JobRunner)runnerObj;
            // Now add the new job runner to the store
            this.jobRunners.put(instance.getId(), runner);
        }
        return runner;
    }
    
}
