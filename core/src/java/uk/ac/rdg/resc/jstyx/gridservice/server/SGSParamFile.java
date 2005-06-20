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

import uk.ac.rdg.resc.jstyx.server.InMemoryFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * A StyxFile interface to a parameter that is passed to an SGS instance as 
 * part of the command line of the underlying executable.
 * @todo Perhaps this should extend AsyncStyxFile?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
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
    
    private SGSParam param; // The logical representation of the parameter
    
    public SGSParamFile(SGSParam param) throws StyxException
    {
        // The file is named after the parameter name
        super(param.getName());
        this.param = param;
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
        int count, ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException
    {
        if (offset != 0)
        {
            throw new StyxException("Must write to the parameter file at offset zero.");
        }
        if (!truncate)
        {
            throw new StyxException("Must write to the parameter file with truncation");
        }
        // Set the limit of the input data buffer correctly
        data.limit(data.position() + count);
        String newValue = StyxUtils.dataToString(data);
        // Check that the new value is within range
        // TODO: make this work properly
        /*try
        {
            // TODO: need to check range, possible values etc
            this.param.checkValidValue(newValue);
        }
        catch(SGSConfigException sce)
        {
            throw new StyxException(sce.getMessage());
        }*/
        // If we've got this far the value must have been OK.
        super.write(client, offset, count, data, user, truncate, tag);
    }
    
    /**
     * @return the parameter as it will appear on the command line, including
     * the switch, if present (e.g. "-p 12")
     */
    public String getCommandLineFragment()
    {
        if (this.param.getSwitch() == null)
        {
            return this.getContents();
        }
        else
        {
            return this.param.getSwitch() + this.getContents();
        }
    }
    
    /**
     * @return the value of this parameter as a string. Simply calls
     * super.getContents(); this is only here for consistency of naming with
     * the other getValue...() methods
     */
    public String getValueAsString()
    {
        return this.getContents();
    }
    
    /**
     * @return the value of this parameter as a boolean (only valid if this
     * parameter is of boolean type)
     * @throws SGSConfigException if the value is not a valid boolean ("true"
     * or "false")
     */
    public boolean getValueAsBoolean() throws SGSConfigException
    {
        String val = this.getContents();
        if (val.trim().equalsIgnoreCase("true"))
        {
            return true;
        }
        else if (val.trim().equalsIgnoreCase("false"))
        {
            return false;
        }
        else
        {
            // TODO: should this be a StyxException?
            throw new SGSConfigException("\"" + val + "\" is not a valid boolean");
        }
    }
    
    /**
     * @return the value of this parameter as a long integer (only valid if this
     * parameter is of "int" type)
     * @throws SGSConfigException if the value is not a valid long (8-byte)
     * signed integer
     */
    public long getValueAsLong() throws SGSConfigException
    {
        String val = this.getContents();
        try
        {
            return Long.parseLong(val);
        }
        catch(NumberFormatException nfe)
        {
            throw new SGSConfigException("\"" + val +
                "\" is not a valid 8-byte signed integer");
        }
    }
    
    /**
     * @return the value of this parameter as a double precision floating-point
     * number (only valid if this parameter is of "float" type)
     */
    public double getValueAsDouble() throws SGSConfigException
    {
        String val = this.getContents();
        try
        {
            return Double.parseDouble(val);
        }
        catch(NumberFormatException nfe)
        {
            throw new SGSConfigException("\"" + val +
                "\" is not a valid double-precision floating-point number");
        }
    }
}
