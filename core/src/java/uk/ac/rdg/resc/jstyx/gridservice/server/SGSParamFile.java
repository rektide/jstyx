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

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.mina.common.ByteBuffer;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.Option;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.UnflaggedOption;

import uk.ac.rdg.resc.jstyx.gridservice.config.SGSParam;

import uk.ac.rdg.resc.jstyx.server.AsyncStyxFile;
import uk.ac.rdg.resc.jstyx.server.InMemoryFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * A StyxFile interface to a parameter that is passed to an SGS instance as 
 * part of the command line of the underlying executable.  This is an AsyncStyxFile
 * so clients can be automatically notified of changes to the value of the
 * parameter.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.21  2006/02/17 09:24:55  jonblower
 * Change to comments
 *
 * Revision 1.20  2005/12/07 17:47:58  jonblower
 * Changed "commandline" file to "args" - now just contains arguments, not program name
 *
 * Revision 1.19  2005/11/14 21:31:54  jonblower
 * Got SGSRun working for SC2005 demo
 *
 * Revision 1.18  2005/11/11 21:57:21  jonblower
 * Implemented passing of URLs to input files
 *
 * Revision 1.17  2005/11/07 21:06:42  jonblower
 * Now allows setting of empty values for non-required parameters
 *
 * Revision 1.16  2005/11/04 19:31:16  jonblower
 * Added code to disallow parameter setting while service is running
 *
 * Revision 1.15  2005/11/04 09:11:23  jonblower
 * Made SGSParamFile inherit from AsyncStyxFile instead of InMemoryFile
 *
 * Revision 1.14  2005/11/03 07:42:47  jonblower
 * Implemented JSAP-based parameter parsing
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
public class SGSParamFile extends AsyncStyxFile
{
    private static final String URL_PREFIX = "readfrom:";
    
    private SGSParam param; // The logical representation of the parameter
    private StyxGridServiceInstance instance;
    private boolean valueSet; // True if a value has been set for this parameter
    
    public SGSParamFile(SGSParam param, StyxGridServiceInstance instance) throws StyxException
    {
        // The file is named after the parameter name
        super(new InMemoryFile(param.getName()));
        this.param = param;
        this.instance = instance;
        if (this.getJSAPParameter().getDefault() != null)
        {
            // TODO: We are only allowing a single default value
            this.setParameterValue(this.getJSAPParameter().getDefault()[0]);
            this.valueSet = true;
        }
    }
    
    /**
     * @return the JSAP Parameter object that is associated with this file
     */
    public Parameter getJSAPParameter()
    {
        return this.param.getParameter();
    }
    
    /**
     * The new value for the parameter must come in a single message (i.e.
     * the offset must be zero and the incoming ByteBuffer must contain the
     * entire parameter value).  Must also write with truncation.
     */
    public synchronized void write(StyxFileClient client, long offset,
        int count, ByteBuffer data, boolean truncate, int tag)
        throws StyxException
    {
        if (instance.getStatus() == StatusCode.RUNNING)
        {
            throw new StyxException("Cannot set new parameter values while service is running");
        }
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
        
        this.setParameterValue(newValue);
        
        // If we've got this far the value must have been OK.
        super.write(client, offset, count, data, truncate, tag);
        
        this.instance.argumentsChanged();
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
        if (this.getJSAPParameter() instanceof Switch)
        {
            Switch sw = (Switch)this.getJSAPParameter();
            if (this.getParameterValue().equalsIgnoreCase("true"))
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
        else if (this.getJSAPParameter() instanceof FlaggedOption)
        {
            FlaggedOption fo = (FlaggedOption)this.getJSAPParameter();
            // Use the long flag if present, if not the short one
            if (fo.getLongFlag() == JSAP.NO_LONGFLAG)
            {
                return "-" + fo.getShortFlag() + " " + this.getParameterValue();
            }
            else
            {
                return "--" + fo.getLongFlag() + "=" + this.getParameterValue();
            }
        }
        else if (this.getJSAPParameter() instanceof UnflaggedOption)
        {
            return this.getParameterValue();
        }
        else
        {
            // Should never get here unless we add more param types in future
            return "";
        }
    }
    
    /**
     * @return the current value of this parameter
     */
    public String getParameterValue()
    {
        return ((InMemoryFile)this.baseFile).getContents();
    }
    
    /**
     * Sets the parameter value, checking that the value is OK.
     * @throws StyxException if the parameter is not valid
     */
    public void setParameterValue(String newValue) throws StyxException
    {
        // Check that the new value is valid
        // Switches must be "true" or "false"
        if (this.getJSAPParameter() instanceof Switch)
        {
            if (!newValue.equalsIgnoreCase("true") &&
                !newValue.equalsIgnoreCase("false"))
            {
                throw new StyxException("Parameter " + this.getName() +
                    " can only be \"true\" or \"false\"");
            }
        }
        else
        {
            Option op = (Option)this.getJSAPParameter();
            // Check for empty values
            // TODO: also check type of argument (integer, float etc)
            if (newValue.trim().equals(""))
            {
                if (op.required())
                {
                    throw new StyxException("Parameter " + this.getName() +
                        " must have a non-empty value");
                }
                else
                {
                    // Parameter is not required, so unset the parameter and return
                    ((InMemoryFile)this.baseFile).setContents("");
                    this.valueSet = false;
                    return;
                }
            }
            else if (this.param.getInputFile() != null)
            {
                // This parameter represents an input file.
                // For each value in this parameter, see if it is a "readfrom:<url>"
                // If so, do nothing: if not, add an InputFile to allow clients
                // to upload data to this file
                String[] files = newValue.split(" ");
                // First we must remove all previous input files that were set by
                // this parameter
                this.instance.removeInputFiles(files);
                for (int i = 0; i < files.length; i++)
                {
                    if (!files[i].startsWith(URL_PREFIX))
                    {
                        // This is not a URL.
                        this.instance.addInputFile(files[i]);
                    }
                }
            }
            // Parameters representing output files don't show up in the namespace
        }
        // TODO: only set contents if the value has changed
        ((InMemoryFile)this.baseFile).setContents(newValue);
        this.valueSet = true;
    }
    
    /**
     * Checks to see if the contents of this file are valid.  At the moment, this
     * just checks to see if a value has been set for a required parameter.
     * @throws StyxException if the file contents are not valid for some reason.
     */
    public void checkValid() throws StyxException
    {
        if (this.getJSAPParameter() instanceof Option)
        {
            Option op = (Option)this.getJSAPParameter();
            if (this.valueSet)
            {
                if (this.param.getInputFile() != null)
                {
                    // This parameter represents an input file.  See if this is a URL
                    // and if so, download it
                    // TODO: if this is not a URL, check that it exists
                    String str = this.getParameterValue();
                    if (str.startsWith(URL_PREFIX))
                    {
                        // This could be a URL
                        String urlStr = str.substring(URL_PREFIX.length());
                        try
                        {
                            URL url = new URL(urlStr);
                            File urlPath = new File(url.getPath());
                            // TODO: be cleverer about file names, particularly
                            // watching out for name clashes
                            String name = urlPath.getName().equals("") ? "random.dat" : urlPath.getName();
                            this.instance.downloadFrom(url, name);
                            // Now set the contents of this file to the new file name
                            this.setParameterValue(name);
                        }
                        catch(MalformedURLException mue)
                        {
                            throw new StyxException(urlStr + " is not a valid URL");
                        }
                    }
                }
            }
            else
            {
                // A value hasn't been set
                if (op.required())
                {
                    throw new StyxException(this.name + " is a required parameter:" +
                        " a value must be set");
                }
            }
        }
    }
    
}
