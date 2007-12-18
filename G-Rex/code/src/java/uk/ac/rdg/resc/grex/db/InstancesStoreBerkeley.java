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

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DeadlockException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.rdg.resc.grex.config.GRexConfig;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.exceptions.InstancesStoreException;

/**
 * Stores and accesses service instances that are held in a Berkeley database.
 * This is not a relational database: it is essentially like an on-disk Hashtable,
 * where the keys are instance IDs and the values are GRexServiceInstance objects.
 *
 * @todo Sort out thread safety very carefully!
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */

public class InstancesStoreBerkeley implements GRexServiceInstancesStore
{
    private static final Log log = LogFactory.getLog(InstancesStoreBerkeley.class);
    
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String STORE_NAME = "instances";
    private static final int MAX_DEADLOCK_RETRIES = 10;
    private static final String MASTER_WORKING_DIR_NAME = "master";
    
    private GRexConfig config; // We need this to find the home directory of the G-Rex server
    
    private Environment env;
    private EntityStore store; // This is where we keep GRexServiceInstance objects
    
    private PrimaryIndex<Integer, GRexServiceInstance> instancesById;
    private SecondaryIndex<String, Integer, GRexServiceInstance> instancesByServiceName;
    
    /**
     * This is called by the Spring framework to initialize this object
     * @throws Exception if there was an error initializing the database
     */
    public void init() throws Exception
    {
        // Set up the database environment
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        //envConfig.setLockTimeout(1000000);
        
        // Create the database environment
        File dbPath = this.config.getInstancesStoreDirectory();
        this.env = new Environment(dbPath, envConfig);
        this.store = new EntityStore(this.env, STORE_NAME, storeConfig);
        
        // Set up the indices that we will use to access service instances
        this.instancesById = this.store.getPrimaryIndex(Integer.class,
            GRexServiceInstance.class);
        // The string "serviceName" matches the name of the field in
        // GRexServiceInstance
        this.instancesByServiceName = this.store.getSecondaryIndex(this.instancesById,
            String.class, "serviceName");
        
        log.debug("Database created in " + dbPath.getPath());
    }
    
    /**
     * Adds the given instance to the database, creates a unique ID for this instance
     * and creates the working directory for the instance.  The creation of the
     * working directory must take place in this method because the id of the
     * service is not known until the instance has been added to the store
     * (TODO: is there a better structure for this?)
     * 
     * @param instance the GRexServiceInstance to add to the database
     * @param parentWorkingDirectory The directory in which the working directory
     * will be created
     * @return the unique ID of the instance that has been created
     * @throws InstancesStoreException if there was an error adding the instance
     * (e.g. an instance with the same id already exists)
     */
    public int addServiceInstance(GRexServiceInstance instance,
        File parentWorkingDirectory) throws InstancesStoreException
    {
        
        // Create the new instance, get the id, then create the working
        // directory for the instance in a transaction (so that if one of these
        // operations fails, the whole transaction will be aborted).
        File instanceWd, oldInstanceWd, oldInstanceWdNewName;
        Transaction txn = null;
        try
        {
            // Check to see if this instance already exists
            if (this.instancesById.contains(instance.getId()))
            {
                throw new InstancesStoreException("Instance with id " +
                    instance.getId() + " already exists");
            }
            
            txn = this.env.beginTransaction(null, null);
            // Add the new instance to the store
            // The ID will be created automatically from a sequence
            GRexServiceInstance prevInst = this.instancesById.put(txn, instance);
            int id = instance.getId();
            // Create the working directory for this instance
            log.debug("Persistent master working directory directory = " + instance.getPersistentDirName());
            
            // Determinte path of working directory for this instance and setup working directory File object
            if (instance.getPersistentDirName()!="") {
                instanceWd = new File(instance.getPersistentDirName(), this.MASTER_WORKING_DIR_NAME);
                log.debug("Persistent directory name = " + instanceWd.getPath());
            }
            else instanceWd = new File(parentWorkingDirectory, id + 
                FILE_SEPARATOR + this.MASTER_WORKING_DIR_NAME);
            
            /*instanceWd = new File(parentWorkingDirectory, id + 
                FILE_SEPARATOR + this.MASTER_WORKING_DIR_NAME);
             */

            /* Set value of working directory name string in the instance */
            instance.setWorkingDirectory(instanceWd.getPath());
            
            // If the directory already exists and we are not using a persistent
            // working directory, rename the directory to be deleted later.
            oldInstanceWdNewName = new File(instanceWd.getPath() + "-old");
            if (instanceWd.exists()) {
                if (instance.getPersistentDirName()=="") {
                    oldInstanceWd = new File(instanceWd.getPath());
                    oldInstanceWd.renameTo(oldInstanceWdNewName);
                }
            }
            
            // Create the working directory
            boolean success = instanceWd.mkdirs();
            if (!success)
            {
                throw new DatabaseException("Error creating working directory "
                    + instanceWd.getPath() + " for instance " + id);
            }
            
            // Update the instance with the new working directory
            this.instancesById.put(txn, instance);
            
            // If we've got this far the working directory has been created and
            // we can commit the whole transaction
            txn.commit();
            
            // Delete the old working directory if one was found
            if (oldInstanceWdNewName.exists()) {
                deleteDir(oldInstanceWdNewName);
            }
            
            return id;
        }
        catch (DatabaseException dbe)
        {
            if (txn != null)
            {
                try
                {
                    txn.abort();
                }
                catch(DatabaseException dbe2)
                {
                    throw new InstancesStoreException(dbe2);
                }
            }
            log.error("Aborted creation of new service instance", dbe);
            throw new InstancesStoreException(dbe);
        }
    }
    
    /**
     * Recursive method for deleting a directory and its contents.  (The argument
     * can also represent a file)
     * @return true if the deletion was successful, false otherwise
     */
    private static boolean deleteDir(File dir)
    {
        log.debug("Deleting contents of " + dir.getPath());
        if (dir.isDirectory())
        {
            for (String child : dir.list())
            {
                File theDir = new File(dir, child);
                boolean success = deleteDir(theDir);
                if (!success)
                {
                    log.debug("Error deleting " + theDir.getAbsolutePath());
                    return false;
                }
            }
        }
        return dir.delete();
    }
    
    /**
     * Updates the copy of the given instance object in the database
     * (identified by its id).
     * 
     * @param instance the GRexServiceInstance to update
     * @throws InstancesStoreException if there was an error updating the instance
     * (e.g. there is no existing instance with the same id)
     */
    public void updateServiceInstance(GRexServiceInstance instance)
        throws InstancesStoreException
    {
        Transaction txn = null;
        int retry_count = 0;
        boolean success = false;
        while (retry_count < MAX_DEADLOCK_RETRIES && !success) {
            try
            {
                if (retry_count > 0) {
                    log.debug("Attempt No. " + (retry_count+1) + " to execute update transaction");
                }
                txn = this.env.beginTransaction(null, null);
                // Check to see if this instance exists
                if (!this.instancesById.contains(txn, instance.getId(), null))
                {
                    throw new DatabaseException("There is no instance with id "
                        + instance.getId() + " to update");
                }
                // The ID will be created automatically from a sequence
                this.instancesById.put(txn, instance);
                txn.commit();
                success = true;
            }
            catch(DeadlockException de) {
                try {
                    txn.abort();
                }
                catch(DatabaseException dbe)
                {
                    log.debug("Error aborting transaction");
                    throw new InstancesStoreException(dbe);
                }
                retry_count++;                
                if (retry_count >= MAX_DEADLOCK_RETRIES){
                    log.debug("Exceeded retry limit.  Giving up");
                    throw new InstancesStoreException(de);
                }
                else {
                    log.debug("Lock request timed out at attempt No.  " + retry_count + ". Trying again...");
                }
            }
            catch(DatabaseException dbe)
            {
                if (txn != null)
                {
                    try
                    {
                        txn.abort();
                    }
                    catch(DatabaseException dbe2)
                    {
                        log.debug("Error aborting transaction");
                        throw new InstancesStoreException(dbe2);
                    }
                }
                log.error("Aborted update of service instance due to exception:", dbe);
                throw new InstancesStoreException(dbe);
            }
        }
    }
    
    /**
     * Gets a GRexServiceInstance object representing the instance with the 
     * given id.  This always returns a newly-created object (see the Javadoc
     * for com.sleepycat.persist.PrimaryIndex).
     * @param instanceID ID of the instance to be retrieved
     * @return the GRexServiceInstance object, or null if there is no 
     * object with the given ID
     * @throws InstancesStoreException if there was an error retrieving the object
     */
    public GRexServiceInstance getServiceInstanceById(int instanceID)
        throws InstancesStoreException
    {
        GRexServiceInstance inst=null;
        GridServiceConfigForServer gsConfig=null;
        
        int retry_count = 0;
        boolean success = false;
        while (retry_count < MAX_DEADLOCK_RETRIES && !success) {
            try
            {
                if (retry_count > 0) {
                    log.debug("Attempt No. " + (retry_count+1) + " to execute this.instancesById.get(instanceID)");
                }
                inst = this.instancesById.get(instanceID);        
                if (inst == null)
                {
                    log.error("There is no instance of " + gsConfig.getName()
                        + " with id " + instanceID);
                }
                else {
                    /*
                     * Set non-persistent properties of the instance.
                     */
                    // First set the service configuration.  Note that config should not be
                    // null because it is supposed to be injected by Spring before this
                    // object can be used. See this.setGrexConfig
                    String serviceName = inst.getServiceName();
                    gsConfig = config.getGridServiceByName(serviceName);
                    if (gsConfig == null)
                    {
                        log.error("There is no service called " + serviceName);
                    }                                                
                    inst.setGridServiceConfig(gsConfig);
                    
                    // Then set the handle to the instance in the jobs
                    inst.getMasterJob().setInstance(inst);
                    for (Job subJob : inst.getSubJobs())
                    {
                        subJob.setInstance(inst);
                    }                
                }
                success = true;
            }
            catch(DeadlockException de) {
                retry_count++;                
                if (retry_count >= MAX_DEADLOCK_RETRIES){
                    log.debug("Exceeded retry limit.  Giving up");
                    throw new InstancesStoreException(de);
                }
                else {
                    log.debug("Lock request timed out at attempt No.  " + retry_count + ". Trying again...");
                }
            }
            catch(DatabaseException dbe)
            {
                log.error("Caught exception at retry_count = " + retry_count + ": ", dbe);
                throw new InstancesStoreException(dbe);
            }
        }
        
        return inst;
    }
    
    /**
     * Gets a GRexServiceInstance object representing the instance with the 
     * given id and checks that it belongs to the service with the given name.
     * @param instanceID ID of the instance to be retrieved
     * @param serviceName Name of the service to which the instance should belong
     * @return the GRexServiceInstance object, or null if there is no 
     * object with the given ID that belongs to the given service
     * @throws InstancesStoreException if there was an error retrieving the object
     */
    public GRexServiceInstance getServiceInstanceById(int instanceID, String serviceName)
        throws InstancesStoreException
    {
        GRexServiceInstance instance = this.getServiceInstanceById(instanceID);
        /*if (instance == null) log.debug("instance is null!!!");
        log.debug("Setting instance for job " + instance.getMasterJob().getId());
        instance.getMasterJob().setInstance(instance);
        for (Job subJob : instance.getSubJobs())
        {
            log.debug("Setting instance for job " + instance.getMasterJob().getId());
            subJob.setInstance(instance);
        }*/
        if (instance != null && instance.getServiceName().equals(serviceName))
        {
            return instance;
        }
        return null;
    }
    
    /**
     * Gets all the service instances that belong to the given service. 
     * @param serviceName Name of the service to which the instances belong. 
     * @return a List of instances that belong to the service.
     * @throws InstancesStoreException if there was an error retrieving the data
     */
    public synchronized List<GRexServiceInstance> getServiceInstancesByServiceName(String serviceName)
        throws InstancesStoreException
    {
        // EntityCursors are not thread-safe so this method must be synchronized
        EntityCursor<GRexServiceInstance> cursor = null;
        ArrayList<GRexServiceInstance> instances = new ArrayList<GRexServiceInstance>();
        try
        {
            cursor = this.instancesByServiceName.subIndex(serviceName).entities();
            for (GRexServiceInstance instance : cursor)
            {
                instances.add(instance);
            }
            return instances;
        }
        catch (DatabaseException dbe)
        {
            throw new InstancesStoreException(dbe);
        }
        finally
        {
            if (cursor != null)
            {
                try
                {
                    cursor.close();
                }
                catch(DatabaseException dbe)
                {
                    throw new InstancesStoreException(dbe);
                }
            }
        }        
    }
    
    /**
     * Closes the database.  This will be called automatically by the Spring
     * framework.
     * @throws DatabaseException if an error occurred
     */
    public void close() throws DatabaseException
    {
        if (this.store != null) this.store.close();
        if (this.env != null)
        {
            this.env.cleanLog();
            this.env.close();
        }
        log.debug("Instances database closed");
    }
    
    /**
     * This will be used by the Spring framework to inject the config object
     * before this object can be used
     */
    public void setGrexConfig(GRexConfig config)
    {
        this.config = config;
    }
    
}
