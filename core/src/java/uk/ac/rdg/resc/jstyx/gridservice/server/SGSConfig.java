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

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.FlaggedOption;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Class containing configuration info for a single Styx Grid Service
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.16  2005/10/28 14:48:53  jonblower
 * Implementing JSAP-enabled parameter parsing
 *
 * Revision 1.15  2005/10/21 08:11:01  jonblower
 * Improving parameter/input file handling
 *
 * Revision 1.14  2005/10/18 14:08:14  jonblower
 * Removed inputfiles from namespace
 *
 * Revision 1.12  2005/08/02 08:05:18  jonblower
 * Continuing to implement steering
 *
 * Revision 1.11  2005/08/01 17:01:08  jonblower
 * Started to implement steering
 *
 * Revision 1.10  2005/08/01 16:38:05  jonblower
 * Implemented simple parameter handling
 *
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
    
    private Vector inputs;      // The inputs (files and streams) expected by this service
    private Vector outputs;     // The outputs (files and streams) made available by this service
    private Vector docFiles;    // The documentation files
    private Vector params;      // The parameters for this SGS
    private Vector steerables;  // The steerable parameters for this SGS
    private Vector serviceData; // The service data elements for this SGS
    //private Vector inputFiles;  // The input files needed by the executable
    //private boolean allowOtherInputFiles; // If this is true, we shall allow 
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

        // Create the parameters
        this.params = new Vector();
        Iterator paramListIter = gridService.selectNodes("params/param").iterator();
        while(paramListIter.hasNext())
        {
            Node paramEl = (Node)paramListIter.next();
            this.params.add(new SGSParam(paramEl));
        }
        
        // Look for input files and streams
        this.inputs = new Vector();
        Iterator inputListIter = gridService.selectNodes("io/input").iterator();
        while(inputListIter.hasNext())
        {
            Node input = (Node)inputListIter.next();
            this.inputs.add(new SGSInput(input));
        }
        // Now the output files and streams
        this.outputs = new Vector();
        Iterator outputListIter = gridService.selectNodes("io/output").iterator();
        while(outputListIter.hasNext())
        {
            Node input = (Node)outputListIter.next();
            this.outputs.add(new SGSOutput(input));
        }
        
        // Now the steerable parameters
        this.steerables = new Vector();
        Iterator steerableListIter = gridService.selectNodes("steering/steerable").iterator();
        while(steerableListIter.hasNext())
        {
            Node steerableEl = (Node)steerableListIter.next();
            this.steerables.add(new Steerable(steerableEl));
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
        /*Node inputFilesNode = gridService.selectSingleNode("inputfiles");
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
        }*/
        
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
     * @return a Parameter object (from the JSAP library) for the specification
     * at the given node in the XML file
     * @throws SGSConfigException if the parameter could not be created
     */
    private static Parameter getParameter(Node paramNode) throws SGSConfigException
    {
        String name = paramNode.valueOf("@name").trim();
        String paramType = paramNode.valueOf("@paramType");
        String defaultValue = paramNode.valueOf("@defaultValue");
        if (defaultValue == null)
        {
            defaultValue = JSAP.NO_DEFAULT;
        }
        String description = paramNode.valueOf("@description");
        boolean required = paramNode.valueOf("@required").equalsIgnoreCase("true")
            ? JSAP.REQUIRED : JSAP.NOT_REQUIRED;
        
        // Get the flags
        String shortFlagStr = paramNode.valueOf("@flag").trim();
        char shortFlag;
        if (shortFlagStr == null || shortFlagStr.equals(""))
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
        String longFlagStr = paramNode.valueOf("@longFlag").trim();
        String longFlag;
        if (longFlagStr == null || longFlagStr.equals(""))
        {
            longFlag = JSAP.NO_LONGFLAG;
        }
        else
        {
            longFlag = longFlagStr;
        }
        
        if (paramType.equals("switch"))
        {
            Switch param = new Switch(name, shortFlag, longFlag, description);
            if (defaultValue == null || defaultValue.trim().equalsIgnoreCase("false"))
            {
                param.setDefault("false");
            }
            else if (defaultValue.trim().equalsIgnoreCase("true"))
            {
                param.setDefault("true");
            }
            else
            {
                throw new SGSConfigException("Default value for a switch must be empty, \"true\" or \"false\"");
            }
            return param;
        }
        else if (paramType.equals("flaggedOption"))
        {
            
        }
        return null;
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
     * @return Vector of SGSInput objects containing details of all the 
     * input files and streams expected by the service
     */
    public Vector getInputs()
    {
        return this.inputs;
    }
    
    /**
     * @return Vector of SGSOutput objects containing details of all the 
     * output files and streams exposed by the service
     */
    public Vector getOutputs()
    {
        return this.outputs;
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
     * @return Vector of Steerable objects containing details of the parameters
     * that can be adjusted as the executable is running
     */
    public Vector getSteerables()
    {
        return this.steerables;
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
    /*public Vector getInputFiles()
    {
        return this.inputFiles;
    }*/
    
    /**
     * @return true if we shall allow input files other than those specified
     * to be uploaded to the SGS instance
     */
    /*public boolean getAllowOtherInputFiles()
    {
        return this.allowOtherInputFiles;
    }*/
}

/**
 * Class containing information about an input that is expected by
 * the SGS.
 */
class SGSInput
{
    private String name; // Name of the file or stream (e.g. "stdin" or "input.dat")
    
    public SGSInput(Node inputNode)
    {
        this.name = inputNode.valueOf("@name").trim();
    }
    
    /**
     * @return the name of the stream
     */
    public String getName()
    {
        return this.name;
    }
}

/**
 * Class containing information about an output file or stream that is exposed by
 * the SGS.
 */
class SGSOutput
{
    private String name; // Name of the file or stream (e.g. "stdout" or "output.dat")
    
    public SGSOutput(Node outputNode)
    {
        this.name = outputNode.valueOf("@name").trim();
    }
    
    /**
     * @return the name of the stream
     */
    public String getName()
    {
        return this.name;
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
    // Type of the parameter: TODO: replace with type-safe enumeration
    public static int SWITCH = 0;
    public static int FLAGGED_OPTION = 1;
    public static int UNFLAGGED_OPTION = 2;
    
    private String name; // Name for the parameter
    private int paramType; // Type of the parameter (switch, flagged option or unflagged option)
    private String shortFlag; // Optional command-line switch that precedes this parameter (e.g. -v)
    private String longFlag; // Optional command-line switch that precedes this parameter (e.g. --verbose)
    private String defaultValue; // Optional default value for the parameter
    private String valueType; // Type of the parameter's value (string, inputfile or outputfile)
    private String description; // Description of the parameter

    /**
     * Creates a parameter object for a SGS.
     * @param paramNode The XML element in the config file representing the parameter
     */
    SGSParam(Node paramNode) throws SGSConfigException
    {
        this.name = paramNode.valueOf("@name").trim();
        String paramTypeStr = paramNode.valueOf("@paramType").trim();
        if (paramTypeStr.equals("switch"))
        {
            this.paramType = SWITCH;
        }
        else if (paramTypeStr.equals("flaggedOption"))
        {
            this.paramType = FLAGGED_OPTION;
        }
        else if (paramTypeStr.equals("unflaggedOption"))
        {
            this.paramType = UNFLAGGED_OPTION;
        }
        if (this.paramType == SWITCH || this.paramType == FLAGGED_OPTION)
        {
            // We don't trim the short flag because the user might want to 
            // force a space between the switch and the parameter value
            this.shortFlag = paramNode.valueOf("@flag");
            this.longFlag = paramNode.valueOf("@longFlag").trim();
            if (this.shortFlag.trim().equals("") && this.longFlag.equals(""))
            {
                throw new SGSConfigException("Must set a flag for a switch or a flaggedOption");
            }
        }
        this.defaultValue = paramNode.valueOf("@default");
        if (this.paramType == SWITCH)
        {
            // Default value can only be "true" or "false".  If absent, set to false
            if (this.defaultValue == null)
            {
                this.defaultValue = "false";
            }
            else if (!this.defaultValue.equalsIgnoreCase("true") &&
                     !this.defaultValue.equalsIgnoreCase("false"))
            {
                throw new SGSConfigException("Default value for a switch can only be \"true\" or \"false\"");
            }
        }
        this.valueType = paramNode.valueOf("@valueType");
        this.description = paramNode.valueOf("@description");
    }

    public String getName()
    {
        return this.name;
    }
    
    /**
     * @return the type of this parameter (SWITCH, FLAGGED_OPTION or
     * UNFLAGGED_OPTION)
     */
    public int getParamType()
    {
        return this.paramType;
    }

    /**
     * @return the command-line flag that precedes this parameter (e.g. "-v")
     */
    public String getFlag()
    {
        return this.shortFlag;
    }

    /**
     * @return the long version of the command-line flag that precedes this parameter (e.g. "--verbose")
     */
    public String getLongFlag()
    {
        return this.longFlag;
    }
    
    public String getDefaultValue()
    {
        return this.defaultValue;
    }
}

/**
 * Simple class representing a steerable parameter, i.e. a parameter whose value
 * can be changed whilst the executable is running
 */
class Steerable
{
    private String name;
    private String initialValue;
    private String filePath;
    
    Steerable(Node steerableNode) throws SGSConfigException
    {
        this.name = steerableNode.valueOf("@name").trim();
        this.initialValue = steerableNode.valueOf("@initialvalue");
        this.filePath = steerableNode.valueOf("@file").trim();
        if (this.filePath.equals(""))
        {
            this.filePath = this.name;
        }
    }
    
    public String getName()
    {
        return this.name;
    }
    
    public String getInitialValue()
    {
        return this.initialValue;
    }
    
    /**
     * @return the path of the backing file for the steerable parameter, relative
     * to the working directory of the SGS instance. If no filename is explicitly
     * given in the config file, the filename will be the same as this.getName().
     */
    public String getFilePath()
    {
        return this.filePath;
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