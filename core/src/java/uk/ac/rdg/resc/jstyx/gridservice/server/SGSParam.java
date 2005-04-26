/*
 * Copyright (c) 2005 The University of Reading
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

package uk.ac.rdg.resc.jstyx.gridservice.server;

import org.w3c.dom.Element;

/**
 * Class containing information about a single SGS parameter.  Note that this
 * does not contain the value of the parameter at a given instant.  Rather, it
 * defines the type and the possible values that the parameter can take.  The
 * SGSParamFile class contains the actual value of the parameter.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/04/26 07:46:11  jonblower
 * Continuing to improve setting of parameters in Styx Grid Services
 *
 *
 * Revision 1.2  2005/03/26 14:27:53  jonblower
 * Modified to use SGSConfigException
 *
 * Revision 1.1  2005/03/24 17:34:58  jonblower
 * Initial import
 *
 */
class SGSParam
{
    private String name; // Name for the parameter
    private ParamType type; // Type of the parameter (boolean, int, float, string)
    private boolean required; // True if this parameter must contain a value,
                              // false if it is allowed to be blank
    private String defaultValue; // Optional default value for the parameter
    private String minValue; // Optional minimum value for the parameter (only valid for int and float)
    private String maxValue; // Optional maximum value for the parameter (only valid for int and float)
    private String[] values = null; // Array of possible values that the parameter can take
    private String strSwitch; // Optional command-line switch that precedes this parameter
    private String description; // Optional description for the parameter

    /**
     * Creates a parameter object for a SGS.
     * @param paramNode The XML element in the config file representing the parameter
     */
    public SGSParam(Element paramNode) throws SGSConfigException
    {
        this.name = paramNode.getAttribute("name").trim();
        this.type = ParamType.getInstance(paramNode.getAttribute("type").trim());
        this.required = paramNode.getAttribute(name).trim().equalsIgnoreCase("yes");
        
        // The following fields are optional; if they don't exist in the config
        // file they will have the value ""        
        this.defaultValue = paramNode.getAttribute("default");
        this.minValue = paramNode.getAttribute("minValue").trim();
        this.maxValue = paramNode.getAttribute("maxValue").trim();
        String vals = paramNode.getAttribute("values").trim();
        // We don't trim the switch parameter because the user might want to 
        // force a space between the switch and the parameter value
        this.strSwitch = paramNode.getAttribute("switch");
        this.description = paramNode.getAttribute("description").trim();
        
        if (! (this.minValue.equals("") && this.maxValue.equals("")) )
        {
            if (!vals.equals(""))
            {
                throw new SGSConfigException("Cannot specify both a min/max value and a"
                    + " list of possible values for parameter " + name);
            }
            if (this.type == ParamType.BOOLEAN || this.type == ParamType.STRING)
            {
                throw new SGSConfigException("Boolean and string parameters"
                    + " cannot have a minimum or maximum value");
            }
        }
        
        // Check that the minimum and maximum values are valid
        this.checkValidValue(this.minValue);
        this.checkValidValue(this.maxValue);
        
        // Get the list of possible parameter values
        if (!vals.equals(""))
        {
            this.values = vals.split(",");
            // Now check that all the values are valid
            for (int i = 0; i < this.values.length; i++)
            {
                this.checkValidValue(this.values[i]);
            }
        }
        
        if (!this.defaultValue.equals(""))
        {
            // Check that the default value is valid
            this.checkValidValue(this.defaultValue);
        }
        
    }

    public String getName()
    {
        return this.name;
    }

    /**
     * @return the command-line switch that precedes this parameter (e.g. "-p")
     */
    public String getSwitch()
    {
        return this.strSwitch;
    }
    
    public String getDescription()
    {
        return this.description;
    }
    
    /**
     * Checks that the proposed new value for the parameter is valid,
     * throwing a SGSConfigException if it isn't. To be valid, it must be
     * parseable as a value of its type (boolean, int or float). If the type
     * of the parameter is String, this method does nothing.
     * @param value the value to check
     */
    public void checkValidValue(String value) throws SGSConfigException
    {
        // TODO: allow blank values?
        if (this.type == ParamType.BOOLEAN)
        {
            if (! (value.trim().equalsIgnoreCase("true") || 
                   value.trim().equalsIgnoreCase("false")) )
            {
                // TODO: allow "yes" and "no"?
                throw new SGSConfigException("Boolean parameter " + this.name
                    + " must be either \"true\" or \"false\"");
            }
        }
        else if (this.type == ParamType.INT)
        {
            try
            {
                long val = Long.parseLong(value);
            }
            catch(NumberFormatException nfe)
            {
                throw new SGSConfigException("Value for " + this.name +
                    " must be a valid 8-byte signed integer");
            }
        }
        else if (this.type == ParamType.FLOAT)
        {
            try
            {
                double val = Double.parseDouble(value);
            }
            catch(NumberFormatException nfe)
            {
                throw new SGSConfigException("Value for " + this.name +
                    " must be a valid double-precision floating-point number");
            }
        }
    }
}

/**
 * Type-safe enumeration representing the possible types
 */
class ParamType
{
    public static final ParamType BOOLEAN = new ParamType();
    public static final ParamType INT = new ParamType();
    public static final ParamType FLOAT = new ParamType();
    public static final ParamType STRING = new ParamType();
    
    public static ParamType getInstance(String type) throws SGSConfigException
    {
        if (type.equalsIgnoreCase("boolean"))
        {
            return BOOLEAN;
        }
        else if (type.equalsIgnoreCase("int"))
        {
            return INT;
        }
        else if (type.equalsIgnoreCase("float"))
        {
            return FLOAT;
        }
        else if (type.equalsIgnoreCase("string"))
        {
            return STRING;
        }
        else
        {
            throw new SGSConfigException("Unknown parameter type: " + type);
        }
    }
    
    /**
     * Constructor is made private to prevent other classes making instances
     */
    private ParamType()
    {
    }
}
