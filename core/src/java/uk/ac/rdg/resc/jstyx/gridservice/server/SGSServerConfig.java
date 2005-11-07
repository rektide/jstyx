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
import java.util.List;

import org.apache.log4j.Logger;

import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;

import uk.ac.rdg.resc.jstyx.gridservice.config.SGSConfig;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSConfigException;

/**
 * Configuration of a Styx Grid Service server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.8  2005/11/07 21:04:48  jonblower
 * Moved SGS config classes to new package
 *
 * Revision 1.7  2005/05/16 11:00:53  jonblower
 * Changed SGS config XML file structure: separated input and output streams and changed some tag names
 *
 * Revision 1.6  2005/05/11 13:45:19  jonblower
 * Converted SGS config code to use dom4j and Jaxen for XML parsing
 *
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
    
    private Document doc;
    
    /**
     * Creates a new server configuration from the given XML document
     * @throws Exception i
     */
    public SGSServerConfig(String xmlFilename) throws SGSConfigException
    {
        this.gridServices = new Vector();
        
        try
        {
            // Parse the xml document using dom4j (with validation)
            SAXReader reader = new SAXReader(true);
            this.doc = reader.read(xmlFilename);
            // Get the base node of the server configuration
            Node serverNode = this.doc.selectSingleNode("/sgs/server");
            // Get the port number
            this.port = this.findPort(serverNode);
            // Get the SSL parameters
            this.getSSLConfig(serverNode);
            // Get the configuration for all Styx Grid Services
            this.getSGSConfig();
        }
        catch(DocumentException de)
        {
            if (log.isDebugEnabled())
            {
                de.printStackTrace();
            }
            throw new SGSConfigException(de.getMessage());
        }
    }
    
    /**
     * Use XPath to find the port number on which the server will listen
     */
    private int findPort(Node serverNode) throws SGSConfigException
    {
        String portStr = serverNode.valueOf("@port");
        try
        {
            return Integer.parseInt(portStr);
        }
        catch(NumberFormatException nfe)
        {
            throw new SGSConfigException("Invalid port number: " + portStr);
        }
    }
    
    /**
     * Gets the SSL-related parameters
     */
    private void getSSLConfig(Node serverNode) throws SGSConfigException
    {
        Node sslNode = serverNode.selectSingleNode("ssl");
        if (sslNode.valueOf("@activated").equalsIgnoreCase("yes"))
        {
            this.useSSL = true;
        }
        else
        {
            this.useSSL = false;
        }
        Node keystoreNode = sslNode.selectSingleNode("keystore");
        this.keystore = keystoreNode.valueOf("@location");
        log.debug("SSL keystore location: " + this.keystore);
    }
    
    /**
     * Get the configuration of each Styx Grid Service
     */
    private void getSGSConfig() throws SGSConfigException
    {
        Node gridServicesNode = this.doc.selectSingleNode("/sgs/gridservices");
        String sgsRoot = gridServicesNode.valueOf("@root");
        
        List gridServicesList = gridServicesNode.selectNodes("gridservice");
        Iterator it = gridServicesList.iterator();
        while(it.hasNext())
        {
            this.gridServices.add(new SGSConfig((Node)it.next(), sgsRoot));
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
