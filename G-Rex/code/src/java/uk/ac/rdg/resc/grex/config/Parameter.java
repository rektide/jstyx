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

import simple.xml.Attribute;
import simple.xml.Root;
import simple.xml.load.PersistenceException;
import simple.xml.load.Validate;

/**
 * A command-line parameter that is used by a service instance
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="param")
public class Parameter
{
    /**
     * The possible types of a Parameter.  Note that we can't simply do
     * "public enum Type { switch, flaggedOption, unflaggedOption }" because
     * switch is a reserved word in Java.  We want the values to be in lower
     * case in the XML config information so we jump through a couple of hoops.
     */
    public enum Type
    {
        SWITCH("switch"),
        FLAGGED_OPTION("flaggedOption"),
        UNFLAGGED_OPTION("unflaggedOption");
        private final String name;
        Type(String name)
        {
            this.name = name;
        }
        public String toString()
        {
            return this.name;
        }
    };
    
    @Attribute(name="name")
    private String name; // A unique name for this parameter
    
    @Attribute(name="flag", required=false)
    private String flag; // The command-line flag that is associated with this parameter
    
    @Attribute(name="longFlag", required=false)
    private String longFlag; // The long command-line flag
    
    @Attribute(name="defaultValue", required=false)
    private String defaultValue; // A default value for this parameter
    
    @Attribute(name="description", required=false)
    private String description; // Human-readable description for this parameter
    
    @Attribute(name="required", required=false)
    private boolean required = false; // True if this parameter must be set before a service is run
    
    @Attribute(name="greedy", required=false)
    private boolean greedy = false; // True if this is an UnflaggedOption that consumes the
                                    // rest of the command line
    
    @Attribute(name="type") // The type of this parameter ("switch", "flaggedOption" or "unflaggedoption")
    private String typeStr; // The Type represented as a String
    private Type type;      // The typeStr will be converted to a Type on validation
                            // Note that Simple XML supports enums but in our case we want
                            // to use "switch" as an attribute, which is a reserved word in
                            // Java.  Hence we use a string then convert in validate()
    
    /** Creates a new instance of Parameter */
    public Parameter()
    {
    }

    public String getName()
    {
        return name;
    }

    public String getFlag()
    {
        return flag;
    }

    public String getLongFlag()
    {
        return longFlag;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isRequired()
    {
        return required;
    }

    public boolean isGreedy()
    {
        return greedy;
    }
    
    public Type getType()
    {
        return this.type;
    }
    
    /**
     * Validates this Parameter and sets the Type
     */
    @Validate
    public void validate() throws PersistenceException
    {
        if (this.typeStr.trim().equals("switch"))
        {
            this.type = Type.SWITCH;
        }
        else if (this.typeStr.trim().equals("flaggedOption"))
        {
            this.type = Type.FLAGGED_OPTION;
        }
        else if (this.typeStr.trim().equals("unflaggedOption"))
        {
            this.type = Type.UNFLAGGED_OPTION;
        }
        else
        {
            throw new PersistenceException("Invalid paramType \"" + this.typeStr + "\"");
        }
        // Check that the "greedy" option is set correctly
        if (this.greedy && this.type != Type.UNFLAGGED_OPTION)
        {
            throw new PersistenceException("Only unflaggedOptions can be greedy");
        }
        // TODO: create a JSAP Parameter object here?
    }
    
}
