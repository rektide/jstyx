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

import org.apache.log4j.Logger;

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
 * Revision 1.5  2005/03/26 14:27:53  jonblower
 * Modified to use SGSConfigException
 *
 * Revision 1.4  2005/03/24 17:33:51  jonblower
 * Improved reading of service parameters from config file
 *
 * Revision 1.3  2005/03/24 07:57:41  jonblower
 * Improved code for reading SSL info from SGSconfig file and included parameter information for the Grid Services in the config file
 *
 * Revision 1.2  2005/03/22 17:45:25  jonblower
 * Now reads SSL switch from config file
 *
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.1  2005/02/16 19:22:32  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public class SGSServerConfig
{
    
    private static final Logger log = Logger.getLogger(SGSServerConfig.class);
    
    protected int port; // The port on which the server will listen
    protected boolean useSSL; // True if the server is to be secured with SSL
    protected String keystore; // The location of the keystore
    protected Vector gridServices; // Information about all the SGSs
    private Element root; // The root of the XML document
    private Element serverConfigNode; // The root of the serverConfig section
    private Element gridservicesNode; // The root of the gridservices section    
    
    /**
     * Creates a new server configuration from the given XML document
     * @throws Exception i
     */
    public SGSServerConfig(String xmlFilename) throws SGSConfigException
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
            // Get the SSL parameters
            this.getSSLConfig();
            this.getSGSConfig();
        }
        catch(Exception e)
        {
            if (log.isDebugEnabled())
            {
                e.printStackTrace();
            }
            throw new SGSConfigException(e.getMessage());
        }
    }
    
    /**
     * Finds the port number on which the server will listen
     */
    private int findPort() throws SGSConfigException
    {
        NodeList list = this.root.getElementsByTagName("serverConfig");
        if (list.getLength() != 1)
        {
            throw new SGSConfigException("There cannot be more than one serverConfig element");
        }
        this.serverConfigNode = (Element)list.item(0);
        NodeList serverConfigList = this.serverConfigNode.getElementsByTagName("port");
        if (serverConfigList.getLength() != 1)
        {
            throw new SGSConfigException("There cannot be more than one port definition");
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
        throw new SGSConfigException("Could not find port number in config file");
    }
    
    /**
     * Gets the SSL-related parameters
     */
    private void getSSLConfig() throws SGSConfigException
    {
        NodeList list = this.root.getElementsByTagName("serverConfig");
        if (list.getLength() != 1)
        {
            throw new SGSConfigException("There cannot be more than one serverConfig element");
        }
        this.serverConfigNode = (Element)list.item(0);
        NodeList serverConfigList = this.serverConfigNode.getElementsByTagName("ssl");
        if (serverConfigList.getLength() != 1)
        {
            throw new SGSConfigException("There cannot be more than one ssl tag");
        }
        Element sslNode = (Element)serverConfigList.item(0);
        if (sslNode == null)
        {
            throw new SGSConfigException("Can't find ssl tag");
        }
        String activated = sslNode.getAttribute("activated");
        if (activated.equalsIgnoreCase("yes"))
        {
            this.useSSL = true;
        }
        else if (activated.equalsIgnoreCase("no"))
        {
            this.useSSL = false;
        }
        else
        {
            throw new SGSConfigException("\"activated\" attribute must be either \"on\" or \"off\"");
        }
        // Now get the location of the keystore
        NodeList keystoreList = sslNode.getElementsByTagName("keystore");
        if (keystoreList.getLength() != 1)
        {
            throw new SGSConfigException("There must be one and only one keystore element");
        }
        Element keystoreNode = (Element)keystoreList.item(0);
        this.keystore = keystoreNode.getAttribute("location");
    }
    
    /**
     * Get the configuration of each Styx Grid Service
     */
    private void getSGSConfig() throws SGSConfigException
    {
        NodeList list = this.root.getElementsByTagName("gridservices");
        if (list.getLength() != 1)
        {
            throw new SGSConfigException("There cannot be more than one gridservices element");
        }
        Element gridServicesNode = (Element)list.item(0);
        String sgsRoot = gridServicesNode.getAttribute("root");
        NodeList gridServicesList = gridServicesNode.getElementsByTagName("gridservice");
        for (int i = 0; i < gridServicesList.getLength(); i++)
        {
            Element gridService = (Element)gridServicesList.item(i);
            this.gridServices.add(new SGSConfig(gridService, sgsRoot));
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
     * @return true if the server is to use SSL, false otherwise
     */
    public boolean getUseSSL()
    {
        return this.useSSL;
    }
    
    /**
     * @return the location of the keystore file
     */
    public String getKeystoreLocation()
    {
        return this.keystore;
    }
    
    /**
     * @return iterator of all the SGSConfig objects (one per SGS)
     */
    public Iterator getSGSConfigInfo()
    {
        return this.gridServices.iterator();
    }
}
