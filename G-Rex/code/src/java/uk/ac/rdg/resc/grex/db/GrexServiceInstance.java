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

/**
 * Java Bean that represents the state of a particular service instance.  These
 * objects are stored in the Berkeley DB, indexable by instance ID and service ID.
 * Note that instance IDs are unique to all services, i.e. two instances will never
 * share the same ID, even if they belong to different services.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Entity
public class GrexServiceInstance
{
    @PrimaryKey(sequence="ID") // Auto-generate the ID
    private int id; // Unique ID for this instance
    
    @SecondaryKey(relate=Relationship.MANY_TO_ONE) // A service may contain many Instances
    // WARNING! If you change the name of this field, you also need to change
    // the constructor for the secondary key in InstanceDatabase!
    private String serviceName; // Unique ID of the service to which this instance belongs
    
    private String workingDirectory; // Directory in which all files relating to this instance will be kept
    
    // TODO: owner and group information
    // TODO: permissions 
    
    
    /** Creates a new instance of TrexServiceInstance */
    public GrexServiceInstance()
    {
        // TODO: initialize the state 
        this.id = 0; // This is the signal for the database to create a new 
                     // primary key automatically from a sequence.  See the
                     // Javadoc for PrimaryKey.
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
    
}
