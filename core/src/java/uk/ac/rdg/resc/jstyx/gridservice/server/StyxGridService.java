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
import javax.net.ssl.SSLContext;

import org.apache.mina.common.ByteBuffer;
import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.AsyncStyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;
import uk.ac.rdg.resc.jstyx.server.StyxServer;
import uk.ac.rdg.resc.jstyx.server.FileOnDisk;
import uk.ac.rdg.resc.jstyx.server.InMemoryFile;

import uk.ac.rdg.resc.jstyx.ssl.JonSSLContextFactory;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Class representing a Styx Grid Service
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.9  2005/05/11 13:45:19  jonblower
 * Converted SGS config code to use dom4j and Jaxen for XML parsing
 *
 * Revision 1.8  2005/05/09 07:13:51  jonblower
 * Changed getFileOnDisk() to getFileOrDirectoryOnDisk()
 *
 * Revision 1.7  2005/04/28 08:11:14  jonblower
 * Modified permissions handling in documentation directory of SGS
 *
 * Revision 1.6  2005/04/27 16:11:43  jonblower
 * Added capability to add documentation files to SGS namespace
 *
 * Revision 1.5  2005/03/24 14:47:47  jonblower
 * Provided default read() and write() methods for StyxFile so it is no longer abstract
 *
 * Revision 1.4  2005/03/24 09:48:31  jonblower
 * Changed 'count' from long to int throughout for reading and writing
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
 * Revision 1.3  2005/03/16 17:59:35  jonblower
 * Changed following changes to core JStyx library (replacement of java.nio.ByteBuffers with MINA's ByteBuffers)
 *
 * Revision 1.2  2005/02/21 18:13:23  jonblower
 * Following changes to core JStyx library
 *
 * Revision 1.1  2005/02/16 19:22:32  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public class StyxGridService
{
    
    private static final Logger log = Logger.getLogger(StyxGridService.class);
    
    private Vector instances; // The ID numbers of the current Grid Service instances
    private StyxDirectory root; // The root of the Grid Service
    private String command; // The command to run when a Grid Service instance is started 
                            // (this is passed to System.exec)
    private String workDir; // The directory in the local filesystem where all the 
                            // cache files etc associated with this service will be kept
    private Vector params;  // The parameters that each SGS instance uses
    
    /**
     * Creates a new StyxGridService.
     * @param sgsConfig Object containing the configuration of the SGS
     */
    public StyxGridService(SGSConfig sgsConfig) throws StyxException
    {
        log.debug("Creating StyxGridService called " + sgsConfig.getName());
        this.instances = new Vector();
        this.root = new StyxDirectory(sgsConfig.getName());
        this.root.addChild(new CloneFile());
        // The ".contents" file is an asynchronous interface to the contents of
        // the root directory of the SGS.  The first time this file is read by
        // a client it will return the contents of the root directory (just like
        // reading the root directory).  The second time it is read by the same
        // client the reply will only arrive when the contents have changed. 
        // This allows GUIs to automatically update when new SGS instances are
        // created or destroyed.
        this.root.addChild(new AsyncStyxFile(this.root, ".contents"));
        
        StyxDirectory docDir = new StyxDirectory("doc", 0555);
        //docDir.setReadOnly(); TODO Why doesn't this line work?
        this.root.addChild(docDir);
        // Add the "description" file as a read-only InMemoryFile
        InMemoryFile descFile = new InMemoryFile("description", 0444);
        descFile.setContents(sgsConfig.getDescription());
        docDir.addChild(descFile);
        // Add all the documentation files specified in the SGSConfig object
        Vector docFiles = sgsConfig.getDocFiles();
        for (int i = 0; i < docFiles.size(); i++)
        {
            DocFile docFile = (DocFile)docFiles.get(i);
            // Create a FileOnDisk that wraps the documentation file or directory
            // the .getFileOnDisk() method gets a DirectoryOnDisk if the docFile
            // is a directory, or a FileOnDisk otherwise
            StyxFile sf = FileOnDisk.getFileOrDirectoryOnDisk(docFile.getLocation());
            // Set the name of the file or directory
            sf.setName(docFile.getName());
            // Add the file to the doc directory
            docDir.addChild(sf);
        }
        
        this.command = sgsConfig.getCommand();
        this.workDir = sgsConfig.getWorkingDirectory();
        this.params = sgsConfig.getParams();
    }
    
    public StyxDirectory getRoot()
    {
        return this.root;
    }
    
    /**
     * Returns the next available instance ID
     */
    private int getNextInstanceID()
    {
        synchronized(this.instances)
        {
            for (int i = 0; i < Integer.MAX_VALUE; i++)
            {
                if(!this.instances.contains(new Integer(i)))
                {
                    return i;
                }
            }
        }
        return -1; // This should never happen unless we have over 2 billion instances!
    }
    
    /**
     * Returns the given id to the pool of valid IDs
     */
    void returnInstanceID(int id)
    {
        synchronized(this.instances)
        {
            this.instances.remove(new Integer(id));
        }
    }
    
    /**
     * Creates a new StyxGridServiceInstance and returns its ID number
     */
    private int newInstance(int id) throws StyxException
    {
        synchronized (this.instances)
        {
            this.instances.add(new Integer(id));
            this.root.addChild(new StyxGridServiceInstance(this, id, this.command,
                this.workDir, this.params));
            return id;
        }        
    }
    
    // The clone file - reading this file creates a new instance of the Grid Service
    private class CloneFile extends StyxFile
    {
        public CloneFile() throws StyxException
        {
            super("clone", 0444);
        }
        
        /**
         * Reading the clone file causes a new instance of the Grid Service to
         * be created, and returns the ID of the service. The client must request
         * enough bytes to return the service ID, otherwise a StyxException will
         * be thrown.
         * @todo Could also create the SGS when this file is opened, and leave
         * the file handle open on the ctl file of the new Service
         */
        public void read(StyxFileClient client, long offset, int count, int tag)
            throws StyxException
        {
            if (offset == 0)
            {
                // Create a new StyxGridServiceInstance and return its id
                int id = getNextInstanceID();
                // Construct the reply message
                byte[] msgBytes = StyxUtils.strToUTF8("" + id);
                // Check that the client has requested enough bytes for the reply
                if (count < msgBytes.length)
                {
                    returnInstanceID(id);
                    throw new StyxException("must request at least " + msgBytes.length + " bytes.");
                }
                newInstance(id);
                replyRead(client, msgBytes, tag);
            }
            else
            {
                replyRead(client, new byte[0], tag);
            }
        }        
    }
    
    public static void main (String[] args)
    {
        System.setProperty("java.protocol.handler.pkgs", "uk.ac.rdg.resc.jstyx.client.protocol");
        if (args.length != 1)
        {
            System.err.println("Usage: StyxGridService <config file>");
            return;
        }
        // Create the server configuration from the given XML config file
        try
        {
            SGSServerConfig config = new SGSServerConfig(args[0]);
            // Create the root directory
            StyxDirectory root = new StyxDirectory("/");
            // Add the SGSs to this directory
            Iterator it = config.getSGSConfigInfo();
            while(it.hasNext())
            {
                SGSConfig conf = (SGSConfig)it.next();
                root.addChild(new StyxGridService(conf).getRoot());
            }
            // Start the server
            int port = config.getPort();
            boolean useSSL = config.getUseSSL();
            SSLContext sslContext = null;
            if (useSSL)
            {
                sslContext = JonSSLContextFactory.getInstance(true,
                    config.getKeystoreLocation());
            }
            new StyxServer(port, root, sslContext).start();
            System.out.println("Started StyxGridServices, listening on port " + port);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return;
        }
    }
    
}
