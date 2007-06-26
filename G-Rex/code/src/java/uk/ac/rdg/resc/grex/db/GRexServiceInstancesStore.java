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

import java.io.File;
import java.util.List;
import uk.ac.rdg.resc.grex.exceptions.InstancesStoreException;

/**
 * Defines the methods that must be implemented by a store of G-Rex service
 * instances
 * @todo "throws Exception" is not very neat.  Can we use a narrower exception class?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public interface GRexServiceInstancesStore
{
    /**
     * Finds a particular service instance by its unique id
     * @param id The id of the instance
     * @return The TrexServiceInstance with this id
     */
    public GRexServiceInstance getServiceInstanceById(int id) throws InstancesStoreException;
    
    /**
     * Finds all the instances of the service with the given name
     * @param serviceName The name of the service as specified in the config file
     * @return a List of all the instances of this service
     */
    public List<GRexServiceInstance> getServiceInstancesByServiceName(String serviceName)
        throws InstancesStoreException;
    
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
     * @throws DatabaseException if there was an error adding the instance
     * (e.g. an instance with the same id already exists)
     */
    public int addServiceInstance(GRexServiceInstance instance,
        File parentWorkingDirectory) throws InstancesStoreException;
    
    /**
     * Updates the copy of the given instance object in the database
     * (identified by its id).
     * 
     * @param instance the GRexServiceInstance to update
     * @throws DatabaseException if there was an error updating the instance
     * (e.g. there is no existing instance with the same id)
     */
    public void updateServiceInstance(GRexServiceInstance instance)
        throws InstancesStoreException;
    
    /**
     * Finds all the service instances (of any service) that belong to the 
     * user with the given username
     * @param userName The name of the user that owns the service instances
     * @return a List of all the instances that belong to this user
     */
    //public List<GRexServiceInstance> getServiceInstancesBelongingTo(String userName)
    //    throws Exception;
}
