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

import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
import uk.ac.rdg.resc.grex.db.GRexServiceInstancesStore;
import uk.ac.rdg.resc.grex.exceptions.InstanceNotReadyException;
import java.util.SortedSet;

/**
 * Interface describing the methods exposed by all JobRunners (classes that
 * execute a given GRexServiceInstance).  A JobRunner updates the state of the
 * service instance and any sub-jobs.
 * 
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public interface JobRunner
{

    /*
     * Methods giving access to the sets of output files
     */
    public SortedSet<OutputFile> getOutputFiles();
    public SortedSet<OutputFile> getOutputFinished();

    /**
     * Methods for maintaining the set of finished files in the job runner
     * object
     */
    public boolean addFinishedFile(OutputFile opFile);
    public boolean removeFinishedFile(OutputFile opFile);
    
        
    /**
     * Sets the service instance that this JobRunner will execute
     */
    public void setServiceInstance(GRexServiceInstance instance);
    
    /**
     * Gets the service instance that this JobRunner is executing
     */
    public GRexServiceInstance getServiceInstance();
    
    /**
     * Sets the configuration information for the service that this instance
     * belongs to.
     */
    public void setGridServiceConfig(GridServiceConfigForServer gsConfig);
    
    /**
     * Gets the configuration information for the service that this instance
     * belongs to.
     */
    public GridServiceConfigForServer getGridServiceConfig();

    /**
     * Sets the persistent store of service instances, used to persist changes
     * to state
     */
    public void setInstancesStore(GRexServiceInstancesStore instancesStore);

    /**
     * Gets the persistent store of service instances, used to persist changes
     * to state
     */
    public GRexServiceInstancesStore getInstancesStore();
    
    /**
     * Starts the JobRunner.  This method should return immediately in order to
     * provide the user with a timely response.  Subclasses should run lengthy
     * tasks in separate threads.
     *
     * The task of the start() method is to prepare the job, then kick it off, 
     * setting the state of the job to RUNNING or ERROR before returning.
     */
    public void start();
    
    /**
     * Forcibly stops the JobRunner (in response to an abort message from a user)
     */
    public void abort();
    
    /**
     * Cleans up the service, i.e. removes all files from the working directory.
     * Can only be called once the service has finished (or been aborted).
     */
    // TODO
}
