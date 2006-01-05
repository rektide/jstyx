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

import java.util.Vector;
import java.util.Iterator;
import java.io.StringReader;

import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.UnflaggedOption;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.gridservice.server.*;

/**
 * Class containing configuration info for a single Styx Grid Service
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.6  2006/01/05 12:09:15  jonblower
 * Restructured configuration to give default values for server settings
 *
 * Revision 1.5  2005/12/01 08:29:47  jonblower
 * Refactored XML config handling to simplify clients
 *
 * Revision 1.4  2005/11/10 19:50:43  jonblower
 * Added code to handle output files
 *
 * Revision 1.3  2005/11/10 08:57:21  jonblower
 * Added code to handle output files and streams
 *
 * Revision 1.2  2005/11/09 17:45:00  jonblower
 * Changes to storing of XML config information
 *
 * Revision 1.1  2005/11/07 20:59:34  jonblower
 * Refactored SGS config classes to new package
 *
 * Revision 1.20  2005/11/04 19:28:20  jonblower
 * Changed structure of input files in config file and Styx namespace
 *
 * Revision 1.19  2005/11/03 07:42:47  jonblower
 * Implemented JSAP-based parameter parsing
 *
 * Revision 1.14  2005/10/18 14:08:14  jonblower
 * Removed inputfiles from namespace
 *
 * Revision 1.12  2005/08/02 08:05:18  jonblower
 * Continuing to implement steering
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
public class SGSConfig
{
    private SGSServerConfig serverConfig; // Configuration of the server (port, SSL details etc)
    private Node rootNode;      // The Node in the XML config file that is at the
                                // root of this Styx Grid Service
    private String name;        // The name of this SGS
    private String command;     // The command that is run by this SGS
    private String workDir;     // The working directory of this SGS
    private String description; // Short description of this SGS
    private String configXMLForClient;   // XML snippet used to create this config in a form suitable for clients
    
    private Vector inputs;      // The inputs (files and streams) expected by this service
                                // Instance of SGSInputFile
    private Vector outputs;     // The outputs (files and streams) made available by this service
    private Vector docFiles;    // The documentation files
    private Vector params;      // The parameters for this service
    private JSAP paramParser;   // Object that parses the parameters for this service
    private Vector steerables;  // The steerable parameters for this SGS
    private Vector serviceData; // The service data elements for this SGS

    /**
     * This is called by the SGS server program to generate a configuration
     * object from the XML config file.
     * @param gridService The Node in the XML config file that is at the
     * root of this Styx Grid Service
     * @param sgsRootDir The root of the working directory of this SGS server
     * @param serverConfig Configuration of the server
     * @throws IllegalArgumentException if the name of the SGS contains
     * a space.
     */
    public SGSConfig(Node gridService, SGSServerConfig serverConfig)
        throws SGSConfigException
    {
        this.init(gridService);
        this.serverConfig = serverConfig;
        this.rootNode = gridService;
        this.workDir = this.serverConfig.getCacheLocation() +
            StyxUtils.SYSTEM_FILE_SEPARATOR + name;
        this.setConfigXMLForClient();
        
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
     * This constructor is called by client programs to create an SGSConfig
     * object from an XML snippet that has been read over the Styx interface.
     * This does not populate any of the server-specific fields of this class
     * (e.g. the working directory).  After calling this constructor, clients 
     * will have access to all the fields that relate to <b>instances</b> of 
     * this SGS.
     * @param configXML XML snippet representing the Styx Grid Service
     */
    public SGSConfig(String configXML) throws SGSConfigException
    {
        // Parse the xml document using dom4j (without validation, since we
        // don't have the DTD.  This is OK because the server should have validated
        // the XML anyway)
        SAXReader reader = new SAXReader(false);
        try
        {
            Document doc = reader.read(new StringReader(configXML));
            this.init(doc.getRootElement());
        }
        catch(DocumentException de)
        {
            // TODO: log full stack trace
            throw new SGSConfigException("Error parsing config XML: " + de.getMessage());
        }
    }
    
    /**
     * Initializes class variables from the given Node in an XML document
     */
    private void init(Node gridService) throws SGSConfigException
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
        boolean usingStdin = false;

        // Create the parameters
        this.params = new Vector();
        this.paramParser = new JSAP();
        Iterator paramListIter = gridService.selectNodes("params/param").iterator();
        while(paramListIter.hasNext())
        {
            Node paramEl = (Node)paramListIter.next();
            SGSParam param = new SGSParam(paramEl);
            this.params.add(param);
            try
            {
                this.paramParser.registerParameter(param.getParameter());
            }
            catch (JSAPException jsape)
            {
                throw new SGSConfigException("Error parsing parameters: " + jsape.getMessage());
            }
        }
        
        // Look for input files and streams
        this.inputs = new Vector();
        Iterator inputListIter = gridService.selectNodes("inputs/input").iterator();
        while(inputListIter.hasNext())
        {
            Node input = (Node)inputListIter.next();
            SGSInput sgsIn = new SGSInput(input.valueOf("@type"), input.valueOf("@name"));
            if (sgsIn.getType() == SGSInput.STREAM)
            {
                usingStdin = true;
            }
            else if (sgsIn.getType() == SGSInput.FILE_FROM_PARAM)
            {
                // This input file is linked to a parameter.  We need to find
                // the parameter in question
                boolean found = false;
                for (int i = 0; i < this.params.size() && !found; i++)
                {
                    SGSParam param = (SGSParam)this.params.get(i);
                    if (param.getName().equals(sgsIn.getName()))
                    {
                        found = true;
                        param.setInputFile(sgsIn);
                    }
                }
                if (!found)
                {
                    throw new SGSConfigException("Error setting input files:" +
                        " parameter " + sgsIn.getName() + " does not exist");
                }
            }
            this.inputs.add(sgsIn);
        }
        
        // Now the output files and streams
        this.outputs = new Vector();
        Iterator outputListIter = gridService.selectNodes("outputs/output").iterator();
        while(outputListIter.hasNext())
        {
            Node output = (Node)outputListIter.next();
            SGSOutput sgsOut = new SGSOutput(output.valueOf("@type"), output.valueOf("@name"));
            if (sgsOut.getType() == SGSOutput.FILE_FROM_PARAM)
            {
                // This output file is linked to a parameter.  We need to find
                // the parameter in question
                boolean found = false;
                for (int i = 0; i < this.params.size() && !found; i++)
                {
                    SGSParam param = (SGSParam)this.params.get(i);
                    if (param.getName().equals(sgsOut.getName()))
                    {
                        found = true;
                        // Output files can't be set by a greedy parameter: the
                        // parameter must only consume one command-line argument
                        if (param.getParameter() instanceof UnflaggedOption)
                        {
                            UnflaggedOption uo = (UnflaggedOption)param.getParameter();
                            if (uo.isGreedy())
                            {
                                throw new SGSConfigException("Cannot link an" +
                                    " output file to a greedy parameter");
                            }
                        }
                        param.setOutputFile(sgsOut);
                    }
                }
                if (!found)
                {
                    throw new SGSConfigException("Error setting output files:" +
                        " parameter " + sgsOut.getName() + " does not exist");
                }
            }
            this.outputs.add(sgsOut);
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
            SDEConfig sdeConf = new SDEConfig(sdEl);
            // If we've asked for bytesConsumed to be available we must be
            // reading from stdin
            if (sdeConf.getName().equals("bytesConsumed") && !usingStdin)
            {
                throw new SGSConfigException("The bytesConsumed service data" +
                    " element is only available when stdin is also available.");
            }
            this.serviceData.add(sdeConf);
        }
    }
    
    /**
     * @return the configuration (port, SSL details etc) of the server
     */
    public SGSServerConfig getServerConfig()
    {
        return this.serverConfig;
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
     * is the string that is passed to Runtime.exec().  This
     * is only meaningful for server-side code (if a client calls this method it
     * will return null).
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
     * will use a subdirectory of this directory as its working directory.  This
     * is only meaningful for server-side code (if a client calls this method it
     * will return null).
     */
    public String getWorkingDirectory()
    {
        return this.workDir;
    }
    
    /**
     * Sets the XML that was used to create this config object in a form
     * suitable for clients to read.  This is the same as the XML that was used
     * to create the config object except that the command attribute of the root
     * element and the documentation section are missing.
     */
    private void setConfigXMLForClient()
    {
        // We will have to construct some of the XML by hand
        StringBuffer buf = new StringBuffer("<gridservice name=\"");
        buf.append(this.name);
        buf.append("\" description=\"");
        buf.append(this.description);
        buf.append("\">");
        
        // Loop over all the child elements and add to the XML unless the child
        // element is the documentation element
        Iterator it = this.rootNode.selectNodes("*").iterator();
        while (it.hasNext())
        {
            Node node = (Node)it.next();
            if (!node.getName().equals("docs"))
            {
                buf.append(node.asXML());
            }
        }
            
        buf.append("</gridservice>");
        this.configXMLForClient = buf.toString();
    }
    
    /**
     * @return the XML that was used to create this config object in a form
     * suitable for clients to read.  This is the same as the XML that was used
     * to create the config object except that the command attribute of the root
     * element and the documentation section are missing.
     */
    public String getConfigXMLForClient()
    {
        return this.configXMLForClient;
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
     * @return JSAP object that is used to parse the command-line parameters
     */
    public JSAP getParamParser()
    {
        return this.paramParser;
    }

    /** 
     * @return Vector containing details of all the parameters (as SGSParam objects)
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
}