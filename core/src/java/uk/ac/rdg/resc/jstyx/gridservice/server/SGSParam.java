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
    private String defaultValue; // Optional default value for the parameter
    private String minValue; // Optional minimum value for the parameter (only valid for int and float)
    private String maxValue; // Optional maximum value for the parameter (only valid for int and float)
    private String[] values = null; // Array of possible values that the parameter can take
    private String description; // Optional description for the parameter

    /**
     * Creates a parameter object for a SGS.
     * @param paramNode The XML element in the config file representing the parameter
     */
    public SGSParam(Element paramNode) throws SGSConfigException
    {
        this.name = paramNode.getAttribute("name").trim();
        this.type = ParamType.getInstance(paramNode.getAttribute("type").trim());
        
        // The following fields are optional; if they don't exist in the config
        // file they will have the value ""
        
        this.minValue = paramNode.getAttribute("minValue").trim();
        this.maxValue = paramNode.getAttribute("maxValue").trim();
        String vals = paramNode.getAttribute("values").trim();
        
        if (! (this.minValue.equals("") && this.maxValue.equals("")) )
        {
            if (!vals.equals(""))
            {
                throw new SGSConfigException("Cannot specify both a min/max value and a"
                    + " list of possible values for parameter " + name);
            }
            if (this.type == ParamType.BOOLEAN || this.type == ParamType.STRING)
            {
                throw new SGSConfigException("Boolean and string parameters cannot have "
                    + "a minimum or maximum value");
            }
        }
        
        // Check that the minimum and maximum values are valid
        this.checkValue(this.minValue);
        this.checkValue(this.maxValue);
        
        // Get the list of possible parameter values
        if (!vals.equals(""))
        {
            this.values = vals.split(",");
            // Now check that all the values are valid
            for (int i = 0; i < this.values.length; i++)
            {
                this.checkValue(this.values[i]);
            }
        }
        
        // Get the default value
        this.defaultValue = paramNode.getAttribute("default").trim();
        if (!this.defaultValue.equals(""))
        {
            // Check that the default value is valid
            this.checkValue(this.defaultValue);
        }
        
        this.description = paramNode.getAttribute("description");
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }
    
    /**
     * Checks that the proposed new value for the parameter is valid,
     * throwing an Exception if it isn't.  To be valid, it must be parseable as
     * a value of its type, be within the min/max range if specified, and be
     * a member of the list of possible values, if specified
     */
    public void checkValue(String newValue) throws SGSConfigException
    {
        if (this.type == ParamType.BOOLEAN)
        {
            
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
    
    private ParamType()
    {
    }
}
