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
import java.io.IOException;

import java.util.Vector;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

/**
 * Configuration of a Styx Grid Service server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 19:22:32  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public class SGSServerConfig
{
    
    protected int port; // The port on which the server will listen
    protected Vector gridServices; // Information about all the SGSs
    private Element root; // The root of the XML document
    private Element serverConfigNode; // The root of the serverConfig section
    private Element gridservicesNode; // The root of the gridservices section    
    
    /**
     * Creates a new server configuration from the given XML document
     * @throws Exception i
     */
    public SGSServerConfig(String xmlFilename) throws Exception
    {
        this.gridServices = new Vector();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            // TODO: set the error handler?
            Document doc = builder.parse(new File(xmlFilename));
            this.root = doc.getDocumentElement();
            this.port = this.findPort();
            this.getSGSConfig();
        }
        catch(ParserConfigurationException pce)
        {
            pce.printStackTrace();
        }
        catch(SAXException se)
        {
            se.printStackTrace();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Finds the port number on which the server will listen
     */
    private int findPort() throws Exception
    {
        NodeList list = this.root.getElementsByTagName("serverConfig");
        if (list.getLength() != 1)
        {
            throw new Exception("There cannot be more than one serverConfig element");
        }
        this.serverConfigNode = (Element)list.item(0);
        NodeList serverConfigList = this.serverConfigNode.getElementsByTagName("port");
        if (serverConfigList.getLength() != 1)
        {
            throw new Exception("There cannot be more than one port definition");
        }
        Element portNode = (Element)serverConfigList.item(0);
        NodeList portList = portNode.getChildNodes();
        for (int i = 0; i < portList.getLength(); i++)
        {
            Node node = portList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE)
            {
                return Integer.parseInt(node.getNodeValue());
            }
        }
        throw new Exception("Could not find port number in config file");
    }
    
    /**
     * Get the configuration of each Styx Grid Service
     */
    private void getSGSConfig() throws Exception
    {
        NodeList list = this.root.getElementsByTagName("gridservices");
        if (list.getLength() != 1)
        {
            throw new Exception("There cannot be more than one gridservices element");
        }
        Element gridServicesNode = (Element)list.item(0);
        String sgsRoot = gridServicesNode.getAttribute("root");
        NodeList gridServicesList = gridServicesNode.getElementsByTagName("gridservice");
        for (int i = 0; i < gridServicesList.getLength(); i++)
        {
            Element gridService = (Element)gridServicesList.item(i);
            // TODO: could check that these attributes exist, but this should
            // be handled by the parser validating against the DTD
            this.gridServices.add(new SGSConfig(gridService.getAttribute("name"),
                gridService.getAttribute("command"), sgsRoot));
        }        
    }
    
    /**
     * @return The number of the port on which the server will listen
     */
    public int getPort()
    {
        return this.port;
    }
    
    /**
     * @return iterator of all the SGSConfig objects (one per SGS)
     */
    public Iterator getSGSConfigInfo()
    {
        return this.gridServices.iterator();
    }
    
    /**
     * Class containing configuration info for a single Styx Grid Service
     */
    public class SGSConfig
    {
        private String name;    // The name of this SGS
        private String command; // The command that is run by this SGS
        private String workDir; // The working directory of this SGS
        
        /**
         * @param name The name of the SGS
         * @param command The command that will be run when the SGS is started
         * @param sgsRoot The directory in the local filesystem where all the
         * temporary data and cache files for all the SGS instances will be kept.
         * (E.g. if this is "C:\\SGS", instance 4 of the "md5sum" SGS will have
         * its working directory in "C:\\SGS\\md5sum\\4".) 
         * @throws IllegalArgumentException if the name of the SGS contains
         * a space.
         */
        public SGSConfig(String name, String command, String sgsRoot)
        {
            // Check that the name is valid
            if (name.indexOf(" ") != -1)
            {
                // TODO: check for other whitespace characters
                throw new IllegalArgumentException("The name of an SGS cannot contain a space");
            }
            this.name = name;
            this.command = command;
            this.workDir = sgsRoot + "\\" + name;
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
         * @return the working directory of this SGS. Each instance of the SGS
         * will use a subdirectory of this directory as its working directory.
         */
        public String getWorkingDirectory()
        {
            return this.workDir;
        }
    }
    
}
