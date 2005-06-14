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

import java.util.Vector;
import java.util.Iterator;
import java.io.File;

import org.dom4j.Node;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Class containing configuration info for a single Styx Grid Service
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.9  2005/06/14 07:45:16  jonblower
 * Implemented setting of params and async notification of parameter changes
 *
 * Revision 1.8  2005/05/19 18:42:07  jonblower
 * Implementing specification of input files required by SGS
 *
 * Revision 1.7  2005/05/16 11:00:53  jonblower
 * Changed SGS config XML file structure: separated input and output streams and changed some tag names
 *
 * Revision 1.6  2005/05/13 16:49:34  jonblower
 * Coded dynamic detection and display of service data, also included streams in config file
 *
 * Revision 1.5  2005/05/11 15:14:30  jonblower
 * Implemented more flexible definition of service data elements
 *
 * Revision 1.4  2005/05/11 13:45:19  jonblower
 * Converted SGS config code to use dom4j and Jaxen for XML parsing
 *
 * Revision 1.3  2005/04/27 16:11:35  jonblower
 * Added capability to add documentation files to SGS namespace
 *
 * Revision 1.2  2005/03/26 14:27:53  jonblower
 * Modified to use SGSConfigException
 *
 * Revision 1.1  2005/03/24 17:34:58  jonblower
 * Initial import
 *
 */
class SGSConfig
{
    private String name;        // The name of this SGS
    private String command;     // The command that is run by this SGS
    private String workDir;     // The working directory of this SGS
    private String description; // Short description of this SGS
    
    private Vector streams;     // The streams exposed by this service
    private Vector docFiles;    // The documentation files
    private Vector params;      // The parameters for this SGS
    private Vector serviceData; // The service data elements for this SGS
    private Vector inputFiles;  // The input files needed by the executable
    private boolean allowOtherInputFiles; // If this is true, we shall allow 
                                          // input files other than those specified
                                          // to be uploaded to the SGS instance

    /**
     * @param gridService The Node in the XML config file that is at the
     * root of this Styx Grid Service
     * @param sgsRootDir The root of the working directory of this SGS
     * @throws IllegalArgumentException if the name of the SGS contains
     * a space.
     */
    public SGSConfig(Node gridService, String sgsRootDir) throws SGSConfigException
    {
        this.name = gridService.valueOf("@name");
        // Check that the name is valid
        if (this.name.indexOf(" ") != -1)
        {
            // TODO: check for other whitespace characters
            throw new IllegalArgumentException("The name of an SGS cannot contain a space");
        }
        this.command = gridService.valueOf("@command");
        this.description = gridService.valueOf("@description");
        this.workDir = sgsRootDir + StyxUtils.SYSTEM_FILE_SEPARATOR + name;
        
        // Create the streams
        this.streams = new Vector();
        // Do the input streams first
        Iterator streamListIter = gridService.selectNodes("streams/instream").iterator();
        while(streamListIter.hasNext())
        {
            Node stream = (Node)streamListIter.next();
            this.streams.add(new SGSStream(stream, SGSStream.INPUT));
        }
        // Now the output streams
        streamListIter = gridService.selectNodes("streams/outstream").iterator();
        while(streamListIter.hasNext())
        {
            Node stream = (Node)streamListIter.next();
            this.streams.add(new SGSStream(stream, SGSStream.OUTPUT));
        }

        // Now create the parameters
        this.params = new Vector();
        Iterator paramListIter = gridService.selectNodes("params/param").iterator();
        while(paramListIter.hasNext())
        {
            Node paramEl = (Node)paramListIter.next();
            this.params.add(new SGSParam(paramEl));
        }
        
        // Create the service data elements
        this.serviceData = new Vector();
        Iterator serviceDataIter =
            gridService.selectNodes("serviceData/serviceDataElement").iterator();
        while(serviceDataIter.hasNext())
        {
            Node sdEl = (Node)serviceDataIter.next();
            this.serviceData.add(new SDEConfig(sdEl));
        }
        
        // Create the input files: just a Vector of Files to indicate the path
        // of the input file relative to the working directory of the service
        // instance.
        Node inputFilesNode = gridService.selectSingleNode("inputfiles");
        this.inputFiles = new Vector();
        if (inputFilesNode != null)
        {
            this.allowOtherInputFiles = inputFilesNode.valueOf("@allowOthers").equals("yes");
            Iterator inputFilesIter = inputFilesNode.selectNodes("inputfile").iterator();
            while(inputFilesIter.hasNext())
            {
                Node fileEl = (Node)inputFilesIter.next();
                File inputFilePath = new File(fileEl.valueOf("@path"));
                if (inputFilePath.isAbsolute())
                {
                    // Path must be relative so that the input file can be placed
                    // in the working directory of the SGS instance
                    throw new SGSConfigException("Input files locations cannot be absolute paths");
                }
                this.inputFiles.add(inputFilePath);
            }
        }
        
        // Create the documentation files
        this.docFiles = new Vector();
        Iterator docListIter = gridService.selectNodes("docs/doc").iterator();
        while(docListIter.hasNext())
        {
            Node docEl = (Node)docListIter.next();
            String name = docEl.valueOf("@name");
            String location = docEl.valueOf("@location");
            this.docFiles.add(new DocFile(name, location));
        }
    }

    /**
     * @return the name of the service
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return the command string that is run when the SGS is started.  This
     * is the string that is passed to Runtime.exec().
     */ 
    public String getCommand()
    {
        return this.command;
    }

    /**
     * @return a String that briefly describes this SGS.
     */ 
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @return the working directory of this SGS. Each instance of the SGS
     * will use a subdirectory of this directory as its working directory.
     */
    public String getWorkingDirectory()
    {
        return this.workDir;
    }
    
    /**
     * @return Vector of SGSStream objects containing details of all the 
     * streams exposed by this service
     */
    public Vector getStreams()
    {
        return this.streams;
    }

    /** 
     * @return Vector of SGSParam objects containing details of all the 
     * parameters
     */
    public Vector getParams()
    {
        return this.params;
    }

    /** 
     * @return Vector of documentation file objects
     */
    public Vector getDocFiles()
    {
        return this.docFiles;
    }

    /** 
     * @return Vector of service data objects
     */
    public Vector getServiceData()
    {
        return this.serviceData;
    }
    
    /**
     * @return Vector of java.io.Files indicating the path of the input file
     * relative to the working directory of the service instance.
     */
    public Vector getInputFiles()
    {
        return this.inputFiles;
    }
    
    /**
     * @return true if we shall allow input files other than those specified
     * to be uploaded to the SGS instance
     */
    public boolean getAllowOtherInputFiles()
    {
        return this.allowOtherInputFiles;
    }
}

/**
 * Class containing information about a stream that can be accessed through
 * the SGS.  Currently only the standard streams (stdin, stdout, stderr)
 * are supported.
 */
class SGSStream
{
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    
    private String name; // Name of the stream ("stdin", "stdout" or "stderr")
    private int type; // Type of the stream (INPUT or OUTPUT)
    
    public SGSStream(Node streamNode, int type)
    {
        this.name = streamNode.valueOf("@name").trim();
        String typeStr = streamNode.valueOf("@type").trim();
        if (type != INPUT && type != OUTPUT)
        {
            throw new IllegalArgumentException("Internal error: type must be" +
                " INPUT or OUTPUT");
        }
        this.type = type;
    }
    
    /**
     * @return the name of the stream
     */
    public String getName()
    {
        return this.name;
    }
    
    /**
     * @return the type of the stream (INPUT or OUTPUT)
     */
    public int getType()
    {
        return this.type;
    }
}

/**
 * Class containing information about a single SGS parameter.  Note that this
 * does not contain the value of the parameter at a given instant.  Rather, it
 * defines the type and the possible values that the parameter can take.  The
 * SGSParamFile class contains the actual value of the parameter.
 *
 * @todo what about command-line switches that take no argument?
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
    SGSParam(Node paramNode) throws SGSConfigException
    {
        this.name = paramNode.valueOf("@name").trim();
        // TODO Uncomment this when we have full parameter functionality
        /*this.type = ParamType.getInstance(paramNode.valueOf("@type").trim());
        // TODO this.required = paramNode.getAttribute(name).trim().equalsIgnoreCase("yes");
        
        // The following fields are optional; if they don't exist in the config
        // file they will have the value ""        
        this.defaultValue = paramNode.valueOf("@default");
        this.minValue = paramNode.valueOf("@minValue").trim();
        this.maxValue = paramNode.valueOf("@maxValue").trim();
        String vals = paramNode.valueOf("@values").trim();
        // We don't trim the switch parameter because the user might want to 
        // force a space between the switch and the parameter value
        this.strSwitch = paramNode.valueOf("@switch");
        this.description = paramNode.valueOf("@description").trim();
        
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
        //this.checkValidValue(this.minValue);
        //this.checkValidValue(this.maxValue);
        
        // Get the list of possible parameter values
        if (!vals.equals(""))
        {
            this.values = vals.split(",");
            // Now check that all the values are valid
            for (int i = 0; i < this.values.length; i++)
            {
                //this.checkValidValue(this.values[i]);
            }
        }
        
        if (!this.defaultValue.equals(""))
        {
            // Check that the default value is valid
            //this.checkValidValue(this.defaultValue);
        }*/
        
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

/**
 * Simple class containing the name and file location of a piece of documentation
 */
class DocFile
{
    private String name;  // The name of the file as it will appear in the namespace
    private File location; // The location of the file in the host filesystem
    
    DocFile(String name, String location)
    {
        this.location = new File(location);
        this.name = name.trim();
        // if the name is blank, set the name to the last part of the location
        if (this.name.equals(""))
        {
            this.name = this.location.getName();
        }
    }
    
    /**
     * @return the name of the documentation file as it will appear in 
     * the namespace of the SGS
     */
    String getName()
    {
        return this.name;
    }
    
    /**
     * @return the location of the file in the host filesystem
     */
    File getLocation()
    {
        return this.location;
    }
}

/**
 * Simple class containing details of a service data element
 */
class SDEConfig
{
    private String name; // The name of the SDE
    private float minUpdateInterval; // The minimum update interval
    private String file; // The path to the file that backs this SDE (not always present)
    
    /**
     * Creates an SDEConfig object from the given Node in the XML document.
     * @throws SGSConfigException if there was an error parsing the information
     */
    public SDEConfig(Node sdeNode) throws SGSConfigException
    {
        this.name = sdeNode.valueOf("@name").trim();
        try
        {
            String updateStr = sdeNode.valueOf("minUpdateInterval");
            if (updateStr.trim().equals(""))
            {
                // Default of 2 seconds.
                // TODO: how can we get this from the DTD?
                this.minUpdateInterval = 2.0f;
            }
            else
            {
                this.minUpdateInterval = Float.parseFloat(updateStr);
            }
        }
        catch(NumberFormatException nfe)
        {
            throw new SGSConfigException("Invalid value (" +
                sdeNode.valueOf("minUpdateInterval") + 
                ") for minimum update interval");
        }
        this.file = sdeNode.valueOf("@file").trim();
    }
    
    public String getName()
    {
        return this.name;
    }
    
    public float getMinUpdateInterval()
    {
        return this.minUpdateInterval;
    }
    
    /**
     * @return the path to the file that backs this SDE, relative to the working
     * directory of the SGS
     */
    public String getFilePath()
    {
        return this.file;
    }
}