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

import org.apache.mina.common.ByteBuffer;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.Option;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.UnflaggedOption;

import uk.ac.rdg.resc.jstyx.server.InMemoryFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * A StyxFile interface to a parameter that is passed to an SGS instance as 
 * part of the command line of the underlying executable.
 * @todo Perhaps this should extend AsyncStyxFile?
 * @todo We should allow parameters to be backed by a file (i.e. steerable
 * rather than on the command line)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.13  2005/11/02 09:01:54  jonblower
 * Continuing to implement JSAP-based parameter parsing
 *
 * Revision 1.12  2005/11/01 16:27:34  jonblower
 * Continuing to implement JSAP-enabled parameter parsing
 *
 * Revision 1.10  2005/09/08 07:08:59  jonblower
 * Removed "String user" from list of parameters to StyxFile.write()
 *
 * Revision 1.9  2005/08/01 16:38:05  jonblower
 * Implemented simple parameter handling
 *
 * Revision 1.8  2005/07/29 16:56:07  jonblower
 * Implementing reading command line asynchronously
 *
 * Revision 1.7  2005/06/20 07:17:34  jonblower
 * Wrapped SGSParamFile as AsyncStyxFile
 *
 * Revision 1.6  2005/04/27 16:11:43  jonblower
 * Added capability to add documentation files to SGS namespace
 *
 * Revision 1.5  2005/04/26 07:46:11  jonblower
 * Continuing to improve setting of parameters in Styx Grid Services
 *
 * Revision 1.2  2005/03/26 14:30:17  jonblower
 * Modified to use SGSConfigException
 *
 * Revision 1.1  2005/03/24 17:34:58  jonblower
 * Initial import
 *
 */
public class SGSParamFile extends InMemoryFile
{
    
    private Parameter param; // The logical representation of the parameter
    private StyxGridServiceInstance instance;
    private boolean valueSet; // True if a value has been set for this parameter
    
    public SGSParamFile(Parameter param, StyxGridServiceInstance instance) throws StyxException
    {
        // The file is named after the parameter name
        super(param.getID());
        this.param = param;
        this.instance = instance;
        if (this.param.getDefault() != null)
        {
            // TODO: We are only allowing a single default value
            this.setContents(this.param.getDefault()[0]);
            this.valueSet = true;
        }
    }
    
    /**
     * @return the JSAP Parameter object that is associated with this file
     */
    public Parameter getParameter()
    {
        return this.param;
    }
    
    /**
     * When we write to a SGSParamFile, we first check that the new value
     * is within range for the parameter.
     *
     * The new value for the parameter must come in a single message (i.e.
     * the offset must be zero and the incoming ByteBuffer must contain the
     * entire parameter value).  Must also be writing with truncation.
     */
    public synchronized void write(StyxFileClient client, long offset,
        int count, ByteBuffer data, boolean truncate, int tag)
        throws StyxException
    {
        if (offset != 0)
        {
            throw new StyxException("Must write to the start of the parameter file");
        }
        if (!truncate)
        {
            throw new StyxException("Must write to the parameter file with truncation");
        }
        // Set the limit of the input data buffer correctly
        data.limit(data.position() + count);
        String newValue = StyxUtils.dataToString(data);
        
        // Check that the new value is valid
        // Switches must be "true" or "false"
        if (this.param instanceof Switch)
        {
            if (!newValue.equalsIgnoreCase("true") &&
                !newValue.equalsIgnoreCase("false"))
            {
                throw new StyxException("Parameter " + this.getName() +
                    " can only be \"true\" or \"false\"");
            }
        }
        // Options must have some content - can't be just whitespace
        // TODO: also check type of argument (integer, float etc)
        else
        {
            // TODO: should be allowed to set empty value if parameter is not
            // required
            if (newValue.trim().equals(""))
            {
                throw new StyxException("Parameter " + this.getName() +
                    " must have a non-empty value");
            }
        }
        
        // If we've got this far the value must have been OK.
        super.write(client, offset, count, data, truncate, tag);
        // Note that we have set a value
        this.valueSet = true;
        // Notify that the command line has changed
        this.instance.commandLineChanged();
    }
    
    /**
     * @return the parameter as it will appear on the command line, including
     * the flag, if present (e.g. "-p 12"). Returns an empty string if no 
     * value has yet been set
     */
    public synchronized String getCommandLineFragment()
    {
        if (!this.valueSet)
        {
            return "";
        }
        if (this.param instanceof Switch)
        {
            Switch sw = (Switch)this.param;
            if (this.getContents().equalsIgnoreCase("true"))
            {
                if (sw.getLongFlag() == JSAP.NO_LONGFLAG)
                {
                    return "-" + sw.getShortFlag();
                }
                else
                {
                    return "--" + sw.getLongFlag();
                }
            }
            else
            {
                return "";
            }
        }
        else if (this.param instanceof FlaggedOption)
        {
            FlaggedOption fo = (FlaggedOption)this.param;
            // Use the short flag if present, if not the long one
            if (fo.getLongFlag() == JSAP.NO_LONGFLAG)
            {
                return "-" + fo.getShortFlag() + " " + this.getContents();
            }
            else
            {
                return "--" + fo.getLongFlag() + "=" + this.getContents();
            }
        }
        else if (this.param instanceof UnflaggedOption)
        {
            return this.getContents();
        }
        else
        {
            // Should never get here unless we add more param types in future
            return "";
        }
    }
    
    /**
     * Checks to see if the contents of this file are valid.  At the moment, this
     * just checks to see if a value has been set for a required parameter.
     * @throws StyxException if the file contents are not valid for some reason.
     */
    public void checkValid() throws StyxException
    {
        if (this.param instanceof Option)
        {
            Option op = (Option)this.param;
            if (op.required() && !this.valueSet)
            {
                throw new StyxException(this.name + " is a required parameter:" +
                    " a value must be set");
            }
        }
    }
    
}
