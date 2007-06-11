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

import java.util.Vector;
import simple.xml.Attribute;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.load.PersistenceException;
import simple.xml.load.Validate;

/**
 * Configuration object that describes a particular GridService (i.e. an
 * executable that is exposed for remote execution)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="gridservice")
public class GridService
{
    @Attribute(name="name")
    private String name; // Unique name for this GridService
    @Attribute(name="type", required=false) // TODO: make an enumeration?
    private String type = "local"; // Type of this service ("local", "condor" etc)
    @Attribute(name="command")
    private String command; // Command that will be run when this service is executed
    @Attribute(name="description", required=false)
    private String description = ""; // Human-readable description for this GridService
    
    /**
     * The input files and streams that are used by this service
     */
    @ElementList(name="inputs", type=Input.class, required=false)
    private Vector<Input> inputs = new Vector<Input>(); 
    
    /**
     * The files and streams that are output by this service
     */
    @ElementList(name="outputs", type=Output.class, required=false)
    private Vector<Output> outputs = new Vector<Output>();
    
    /**
     * The command-line parameters that are taken by this service
     */
    @ElementList(name="params", type=Parameter.class, required=false)
    private Vector<Parameter> params = new Vector<Parameter>();
    
    /**
     * The options that are used to provide more information about how the
     * service is to be run
     */
    @ElementList(name="options", type=Option.class, required=false)
    private Vector<Option> options = new Vector<Option>();

    public String getName()
    {
        return name;
    }

    public String getCommand()
    {
        return command;
    }

    public String getDescription()
    {
        return description;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setCommand(String command)
    {
        this.command = command;
    }

    public void setDescription(String description)
    {
        this.description = description;
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

    public String getType()
    {
        return type;
    }

    public Vector<Option> getOptions()
    {
        return options;
    }
    
    /**
     * Checks that all the names are unique and that only zero or one Parameters
     * are marked greedy
     */
    @Validate
    public void validate() throws PersistenceException
    {
        // TODO: check that all names are unique
        boolean foundGreedy = false;
        for (Parameter param : this.params)
        {
            // We have already checked (in Parameter) that we only apply the
            // Greedy tag to unflaggedOptions
            if (param.isGreedy())
            {
                if (foundGreedy)
                {
                    throw new PersistenceException("Only one parameter can be marked Greedy");
                }
                foundGreedy = true;
            }
        }
    }
    
}
