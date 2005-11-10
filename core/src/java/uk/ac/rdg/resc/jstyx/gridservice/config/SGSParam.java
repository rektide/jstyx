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

package uk.ac.rdg.resc.jstyx.gridservice.config;

import org.dom4j.Node;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.Option;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.UnflaggedOption;

/**
 * Class containing information about the command-line parameters expected by
 * the Styx Grid Service
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/11/10 08:56:41  jonblower
 * Added link to output file
 *
 * Revision 1.2  2005/11/09 17:46:30  jonblower
 * setInputFile() now only works for Options
 *
 * Revision 1.1  2005/11/07 20:59:34  jonblower
 * Refactored SGS config classes to new package
 *
 */

public class SGSParam
{
    
    private Parameter param;  // The JSAP parameter object
    private SGSInput inputFile;   // The input file (if any) linked to this parameter
    private SGSOutput outputFile; // The output file (if any) linked to this parameter
    
    SGSParam(Node paramNode) throws SGSConfigException
    {
        this.param = createParameter(paramNode);
        this.inputFile = null;
        this.outputFile = null;
    }
    
    /**
     * @return the JSAP parameter object associated with this parameter
     */
    public Parameter getParameter()
    {
        return this.param;
    }
    
    /**
     * @return the name of this parameter
     */
    public String getName()
    {
        return this.param.getID();
    }
    
    /**
     * Sets the input file that is linked to this parameter
     * @throws SGSConfigException if this parameter is not an Option
     */
    public void setInputFile(SGSInput inputFile) throws SGSConfigException
    {
        if (this.outputFile != null)
        {
            throw new SGSConfigException("Paramter " + this.getName() + 
                " is already linked to an output file");
        }
        if (this.param instanceof Option)
        {
            this.inputFile = inputFile;
        }
        else
        {
            throw new SGSConfigException("Can only set an input file for an Option");
        }
    }
    
    /**
     * Sets the output file that is linked to this parameter
     * @throws SGSConfigException if this parameter is not an Option
     */
    public void setOutputFile(SGSOutput outputFile) throws SGSConfigException
    {
        if (this.inputFile != null)
        {
            throw new SGSConfigException("Paramter " + this.getName() + 
                " is already linked to an input file");
        }
        if (this.param instanceof Option)
        {
            this.outputFile = outputFile;
        }
        else
        {
            throw new SGSConfigException("Can only set an output file for an Option");
        }
    }
    
    /**
     * @return the input file that is linked to this parameter, or null if 
     * this parameter is not linked to an input file
     */
    public SGSInput getInputFile()
    {
        return this.inputFile;
    }
    
    /**
     * @return a Parameter object (from the JSAP library) for the specification
     * at the given node in the XML file
     * @throws SGSConfigException if the parameter could not be created
     */
    private static Parameter createParameter(Node paramNode) throws SGSConfigException
    {
        // Name and paramType are compulsory attributes so must exist
        String name = paramNode.valueOf("@name").trim();
        String paramType = paramNode.valueOf("@paramType");
        
        // We must check to see if a default value has been set
        String defaultValue;
        if (paramNode.selectSingleNode("@defaultValue") == null)
        {
            // Note: JSAP.NO_DEFAULT == null (at least in current JSAP version)
            defaultValue = JSAP.NO_DEFAULT;
        }
        else
        {
            defaultValue = paramNode.valueOf("@defaultValue");
        }
        
        // All other strings other than "yes" are interpreted as "no"
        boolean required = paramNode.valueOf("@required").equalsIgnoreCase("yes")
            ? JSAP.REQUIRED : JSAP.NOT_REQUIRED;
        
        // The description is not compulsory: if it has not been set then 
        // description will be the empty string
        String description = paramNode.valueOf("@description");
        
        // Get the flags
        char shortFlag;
        if (paramNode.selectSingleNode("@flag") == null)
        {
            shortFlag = JSAP.NO_SHORTFLAG;
        }
        else
        {
            String shortFlagStr = paramNode.valueOf("@flag").trim();
            if (shortFlagStr.equals(""))
            {
                shortFlag = JSAP.NO_SHORTFLAG;
            }
            else if (shortFlagStr.length() == 1)
            {
                shortFlag = shortFlagStr.charAt(0);
            }
            else
            {
                throw new SGSConfigException("Short flag can only be 1 character long");
            }
        }
        
        String longFlag;
        if (paramNode.selectSingleNode("@longFlag") == null)
        {
            longFlag = JSAP.NO_LONGFLAG;
        }
        else
        {
            longFlag = paramNode.valueOf("@longFlag").trim();
            if (longFlag.equals(""))
            {
                longFlag = JSAP.NO_LONGFLAG;
            }
        }
        
        if (paramType.equals("switch"))
        {
            Switch param = new Switch(name, shortFlag, longFlag, description);
            if (defaultValue == JSAP.NO_DEFAULT
                || defaultValue.trim().equalsIgnoreCase("false"))
            {
                param.setDefault("false");
            }
            else if (defaultValue.trim().equalsIgnoreCase("true"))
            {
                param.setDefault("true");
            }
            else
            {
                throw new SGSConfigException("Default value for " + name +
                    " must be empty, \"true\" or \"false\"");
            }
            return param;
        }
        else
        {
            if (defaultValue != JSAP.NO_DEFAULT && defaultValue.trim().equals(""))
            {
                throw new SGSConfigException("Default value for " + name
                    + " cannot be empty");
            }
            else if (paramType.equals("flaggedOption"))
            {
                return new FlaggedOption(name, JSAP.STRING_PARSER, 
                    defaultValue, required, shortFlag, longFlag, description);
            }
            else if (paramType.equals("unflaggedOption"))
            {
                boolean greedy = paramNode.valueOf("@greedy").equals("yes");
                return new UnflaggedOption(name, JSAP.STRING_PARSER, defaultValue, 
                    required, greedy, description);
            }
            else
            {
                throw new SGSConfigException("Illegal parameter type: " + paramType);
            }
        }
    }
    
}
