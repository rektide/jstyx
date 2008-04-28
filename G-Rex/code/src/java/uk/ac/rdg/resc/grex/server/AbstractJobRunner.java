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

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
import uk.ac.rdg.resc.grex.db.GRexServiceInstancesStore;
import uk.ac.rdg.resc.grex.exceptions.InstancesStoreException;

/**
 * Convenience abstract class that implements the common methods of the JobRunner
 * interface and some other convenience methods.  It is suggested that implementations
 * of JobRunner should extend this class instead of implementing the JobRunner
 * interface directly.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class AbstractJobRunner implements JobRunner
{
    private static final Log log = LogFactory.getLog(AbstractJobRunner.class);
     
    // Maps SubJob Ids to lists of output files.  Stored in memory and not 
    // serialized to disk (hence not part of the ServiceInstance or Job objects)
    //private Map<Integer, SortedSet<OutputFile>> outputFiles;         
    // Maps SubJob Ids to lists of finished output files.
    // private Map<Integer, SortedSet<OutputFile>> outputFinished;    
    
    /**
     * The set of output files in the working directory.  The sort key is
     * last modified time, as defined by the compareTo method in the 
     * OutputFile class.  This set is cleared and re-created at regular intervals
     * by the CheckOutputFiles thread launched by the job runner.
     *
     * Note: This set is not used at the moment.  May not be necessary.
     */    
    private SortedSet<OutputFile> outputFiles = new TreeSet<OutputFile>();
    
    /* This set contains output files that have finished, i.e. those that the
     * running program has finshed writing to.  It is maintained by the
     * CheckOutputFiles thread launched by the job runner.
     *
     * Note: This set is not used at the moment.  May not be necessary.
     */
    private SortedSet<OutputFile> outputFinished = new TreeSet<OutputFile>();
        
    /**
     * The name of the file in the working directory that will represent the
     * standard output stream
     */
    public static final String STDOUT = "stdout";
    /**
     * The name of the file in the working directory that will represent the
     * standard error stream
     */
    public static final String STDERR = "stderr";
    /**
     * The name of the file in the working directory that will represent the
     * standard input stream
     */
    public static final String STDIN = "stdin";
    
    protected GRexServiceInstance instance;
    protected GridServiceConfigForServer gsConfig;
    protected GRexServiceInstancesStore instancesStore;
    
    protected AbstractJobRunner()
    {
    }

    public void setServiceInstance(GRexServiceInstance instance)
    {
        this.instance = instance;
    }

    
    
    public GRexServiceInstance getServiceInstance()
    {
        return this.instance;
    }
    
    
    /*
     * Methods giving access to the sets of output files
     */
    public SortedSet<OutputFile> getOutputFiles()
    {
        return outputFiles;
    }    
    public SortedSet<OutputFile> getOutputFinished()
    {
        return outputFinished;
    }
    
    /**
     * Methods for maintaining the set of finished files in the job runner
     * object
     */
    public boolean addFinishedFile(OutputFile opFile){
        return outputFinished.add(opFile);        
    }
    public boolean removeFinishedFile(OutputFile opFile) {
        return outputFiles.add(opFile);        
    }
    

    public void setGridServiceConfig(GridServiceConfigForServer gsConfig)
    {
        this.gsConfig = gsConfig;
    }

    public GridServiceConfigForServer getGridServiceConfig()
    {
        return this.gsConfig;
    }

    public GRexServiceInstancesStore getInstancesStore()
    {
        return instancesStore;
    }

    public void setInstancesStore(GRexServiceInstancesStore instancesStore)
    {
        this.instancesStore = instancesStore;
    }
    
    /**
     * Stores any changes to the instance in the persistent store.
     */
    protected void saveInstance()
    {
        try
        {
            this.instancesStore.updateServiceInstance(this.instance);
        }
        catch (InstancesStoreException ise)
        {
            // TODO: what should we do here?  This error is very unlikely to 
            // happen but if it does it will mean that the status of the instance
            // is not recorded correctly in the database, and hence the user 
            // might get inconsistent information.  However, if there is a problem
            // with the database almost every call to this server will fail.
            // For now, we swallow the error and log it.
            log.error("Can't persist instance " + this.instance.getId() +
                " to store", ise);
        }
    }

    /* Decides whether or not output to a file has finished.
      The method for doing this depends on the type of job runner,
     so this version simply returns false. */
    //public boolean isOutputFinished(OutputFile opFile) {
    //    return false;
    //}
    
    
}
