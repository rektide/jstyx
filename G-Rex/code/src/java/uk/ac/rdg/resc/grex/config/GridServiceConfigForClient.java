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

package uk.ac.rdg.resc.grex.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import simple.xml.Attribute;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.load.PersistenceException;
import simple.xml.load.Validate;

/**
 * Contains the configuration for a grid service that is needed by a client.
 * This is a subset of GridServiceConfigForServer because the client does not
 * need all the information that the server does.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="gridservice")
public class GridServiceConfigForClient
{
    @Attribute(name="name")
    protected String name; // Unique name for this GridService
    
    @Attribute(name="description", required=false)
    protected String description = ""; // Human-readable description for this Grid Service
    
    /**
     * The input files and streams that are used by this service
     */
    @ElementList(name="inputs", type=Input.class, required=false)
    protected Vector<Input> inputs = new Vector<Input>(); 
    
    /**
     * The files and streams that are output by this service
     */
    @ElementList(name="outputs", type=Output.class, required=false)
    protected Vector<Output> outputs = new Vector<Output>();
    
    /**
     * The command-line parameters that are taken by this service
     */
    @ElementList(name="params", type=Parameter.class, required=false)
    protected Vector<Parameter> params = new Vector<Parameter>();
    
    /**
     * Creates a new instance of GridServiceConfigForClient
     */
    public GridServiceConfigForClient()
    {
    }
    
    @Validate
    public void validate() throws PersistenceException
    {
        // Check that all parameter names are unique and that the greedy tag
        // is set correctly
        boolean foundGreedy = false;
        Map<String, Parameter> paramsMap = new HashMap<String, Parameter>();
        for (Parameter param : this.params)
        {
            if (paramsMap.containsKey(param.getName()))
            {
                throw new PersistenceException("Duplicate parameter name %s", param.getName());
            }
            paramsMap.put(param.getName(), param);
            // We have already checked (in Parameter) that we only apply the
            // Greedy tag to unflaggedOptions
            if (param.isGreedy())
            {
                if (foundGreedy)
                {
                    throw new PersistenceException("Only one parameter can be marked greedy");
                }
                foundGreedy = true;
            }
        }
        // Check that input and output files that are linked to parameters
        // are correctly set
        for (Input input : this.inputs)
        {
            if (input.getLinkedParameterName() != null)
            {
                Parameter p = paramsMap.get(input.getLinkedParameterName());
                if (p == null)
                {
                    throw new PersistenceException("Unrecognized parameter name %s for input %s",
                        input.getLinkedParameterName(), input.getName());
                }
                else
                {
                    p.setLinkedInput(input);
                }
            }
        }
        for (Output output : this.outputs)
        {
            if (output.getLinkedParameterName() != null)
            {
                Parameter p = paramsMap.get(output.getLinkedParameterName());
                if (p == null)
                {
                    throw new PersistenceException("Unrecognized parameter name %s for output %s",
                        output.getLinkedParameterName(), output.getName());
                }
                else
                {
                    p.setLinkedOutput(output);
                }
            }
        }
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Vector<Input> getInputs()
    {
        return inputs;
    }

    public Vector<Output> getOutputs()
    {
        return outputs;
    }

    public Vector<Parameter> getParams()
    {
        return params;
    }
    
}
