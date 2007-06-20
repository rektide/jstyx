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
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
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

/**
 * Stores and accesses service instances that are held in a Berkeley database.
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
    private static final String STORE_NAME = "instances";
    
    private GRexConfig config; // We need this to find the home directory of the G-Rex server
    
    private Environment env;
    private EntityStore store; // This is where we keep TrexServiceInstance objects
    
    private PrimaryIndex<Integer, GRexServiceInstance> instancesById;
    private SecondaryIndex<String, Integer, GRexServiceInstance> instancesByServiceName;
    
    
    /**
     * This is called by the Spring framework to initialize this object
     * @throws Exception if there was an error initializing the database
     */
    public void init() throws Exception
    {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        // TODO: should we set this to be transactional?
        
        File dbPath = new File(this.config.getHomeDirectory(), "instancesdb");
        if (!dbPath.exists() && !dbPath.mkdir())
        {
            throw new Exception("Instances database could not be created at "
                + dbPath.getPath());
        }
        this.env = new Environment(dbPath, envConfig);
        this.store = new EntityStore(this.env, STORE_NAME, storeConfig);
        
        // Set up the indices we will use to access instances
        this.instancesById = this.store.getPrimaryIndex(Integer.class,
            GRexServiceInstance.class);
        this.instancesByServiceName = this.store.getSecondaryIndex(this.instancesById,
            String.class, "serviceName");
        
        log.debug("Database created in " + dbPath.getPath());
    }
    
    /**
     * Adds the given instance to the database.  NOTE: this will overwrite
     * any previous Instance with the same ID.
     * 
     * @param instance the GRexServiceInstance to add to the database
     * @return the unique ID of the instance that has been created
     * @throws DatabaseException if there was an error adding the instance
     */
    public int addServiceInstance(GRexServiceInstance instance)
        throws DatabaseException
    {
        // The ID will be created automatically from a sequence
        GRexServiceInstance prevInst = this.instancesById.put(instance);
        return instance.getId();
    }
    
    /**
     * Gets a TrexServiceInstance object representing the instance with the 
     * given id.  This method should only be used for reading the instance
     * object.  If you want to update the instance, use (TODO) in order to
     * guarantee thread safety.
     * @param instanceID ID of the instance to be retrieved
     * @return the TrexServiceInstance object, or null if there is no 
     * object with the given ID
     * @throws DatabaseException if there was an error retrieving the object
     */
    public GRexServiceInstance getServiceInstanceById(int instanceID)
        throws DatabaseException
    {
        return this.instancesById.get(instanceID);
    }
    
    /**
     * Gets all the service instances that belong to the given service. 
     * @param serviceName Name of the service to which the instances belong. 
     * @return a List of instances that belong to the service.
     * @throws DatabaseException if there was an error retrieving the data
     */
    public synchronized List<GRexServiceInstance> getServiceInstancesByServiceName(String serviceName)
        throws DatabaseException
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
        finally
        {
            if (cursor != null) cursor.close();
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
        if (this.env != null) this.env.close();
        log.debug("Instances database closed");
    }
    
    /**
     * This will be used by the Spring framework to inject the config object
     * before handleRequestInternal is called
     */
    public void setGrexConfig(GRexConfig config)
    {
        this.config = config;
    }
    
}
