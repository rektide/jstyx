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

import com.sleepycat.persist.model.Persistent;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a sub-job of a service instance, e.g. one element of a parameter
 * sweep or Monte Carlo simulation.  Sub-jobs have parameters and input files
 * that override those of the service instance.  Parameters and input files that
 * are common to all sub-jobs are stored in GRexServiceInstance.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Persistent
public class SubJob
{
    // The id is unique within the GRexServiceInstance
    private int id;
    
    // Contains the names and values of all parameters that are set on this sub-job
    private Map<String, String> params = new HashMap<String, String>();

    // The state of this particular sub-job
    private GRexServiceInstance.State state = GRexServiceInstance.State.CREATED;
    
    // Directory in which all files relating to this sub-job will be kept
    // Note that the database engine doesn't know how to persist java.io.File
    private String workingDirectory;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
    
    /**
     * @return a Map of the parameters that have been set on this service instance
     */
    public Map<String, String> getParameters()
    {
        return this.params;
    }
    
    /**
     * @return the value of the parameter with the given name, or null if a
     * value has not been set
     */
    public String getParamValue(String name)
    {
        return this.params.get(name);
    }
    
    /**
     * Sets a value of a parameter on this service instance
     */
    public void setParameter(String name, String value)
    {
        this.params.put(name, value);
    }

    public GRexServiceInstance.State getState()
    {
        return state;
    }

    public void setState(GRexServiceInstance.State state)
    {
        this.state = state;
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
