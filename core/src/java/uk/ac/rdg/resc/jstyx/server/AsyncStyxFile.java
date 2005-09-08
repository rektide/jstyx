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

package uk.ac.rdg.resc.jstyx.server;

import org.apache.mina.common.ByteBuffer;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.server.StyxSessionState;
import uk.ac.rdg.resc.jstyx.messages.RerrorMessage;

/**
 * A wrapper for a StyxFile that implements asynchronous behaviour: the first
 * read from a given client returns the file's data as normal. If the file is not
 * closed, subsequent reads from the same client block until the file content is
 * changed.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.10  2005/09/08 07:08:59  jonblower
 * Removed "String user" from list of parameters to StyxFile.write()
 *
 * Revision 1.9  2005/08/10 18:35:28  jonblower
 * Added simple test main() function
 *
 * Revision 1.8  2005/06/20 07:17:35  jonblower
 * Wrapped SGSParamFile as AsyncStyxFile
 *
 * Revision 1.7  2005/05/11 10:33:50  jonblower
 * Implemented MonitoredFileOnDisk.java
 *
 * Revision 1.6  2005/05/10 08:02:18  jonblower
 * Changes related to implementing MonitoredFileOnDisk
 *
 * Revision 1.5  2005/05/09 07:10:37  jonblower
 * Minor changes
 *
 * Revision 1.4  2005/03/24 09:48:31  jonblower
 * Changed 'count' from long to int throughout for reading and writing
 *
 * Revision 1.3  2005/03/16 17:56:23  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.2  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.1.1.1.2.1  2005/03/09 19:44:18  jonblower
 * Changes concerned with migration to MINA
 *
 * Revision 1.1.1.1  2005/02/16 18:58:31  jonblower
 * Initial import
 *
 */
public class AsyncStyxFile extends StyxFile implements StyxFileChangeListener
{
    
    private StyxFile baseFile;       // The file to wrap
    private Hashtable knownClients;  // Clients that have connected to this file before
    private Vector clientQueue;      // Clients that are awaiting a reply
    private long minReplyInterval;   // The minimum amount of time in ms between replies to the same client
                                     // (prevents loading of server in case of rapidly-changing
                                     // data)
    
    /**
     * Creates a new AsyncStyxFile with the same name as the underlying file.
     * (Note: this must be placed in a different directory from that of the
     * underlying file to prevent name conflicts.
     */
    public AsyncStyxFile(StyxFile file) throws StyxException
    {
        this(file, file.getName());
    }
    
    public AsyncStyxFile(StyxFile file, String name) throws StyxException
    {
        this(file, name, 0666);
    }
    
    public AsyncStyxFile(StyxFile file, String name, int permissions) throws StyxException
    {
        this(file, name, permissions, false, false);
    }
    
    public AsyncStyxFile(StyxFile file, String name, int permissions,
        boolean isAppendOnly, boolean isExclusive)
        throws StyxException
    {
        super(name, permissions, isAppendOnly, isExclusive);
        this.baseFile = file;
        this.knownClients = new Hashtable();
        this.clientQueue = new Vector();
        this.minReplyInterval = 0;
        // Register our interest in changes to the underlying StyxFile
        this.baseFile.addChangeListener(this);
    }
    
    /**
     * Sets a minimum reply interval, i.e. the minimum amount of time that must
     * elapse between successive replies to the same client. This is most useful
     * when using this AsyncStyxFile to provide notification of a very
     * rapidly-changing quantity as it can save a lot of unnecessary messages
     * being sent. (This is set to zero when the AsyncStyxFile is first created)
     */
    public void setMinReplyInterval(float seconds)
    {
        this.minReplyInterval = new Float(seconds * 1000).longValue();
    }
    
    public synchronized void read(StyxFileClient client, long offset,
        int count, int tag) throws StyxException
    {
        // Check to see if this client has connected before
        ClientInfo cinfo = (ClientInfo)this.knownClients.get(client);
        if (cinfo == null)
        {
            // this client hasn't connected before. Reply immediately.
            this.replyClient(cinfo, client, offset, count, tag);
        }
        else
        {
            long now = System.currentTimeMillis();
            // This client has read from this file before.  If the client is 
            // requesting data from an offset greater than when it last read
            // this file, return the data and update the offset
            if (offset > cinfo.offset)
            {
                this.replyClient(cinfo, client, offset, count, tag);
            }
            else
            {
                // The client is requesting data that is has received already.
                // Check to see if the file has changed since the last read and
                // if the requisite amount of time has elapsed since the last read
                long timeSinceLastReply = now - cinfo.timeLastReply;
                if (cinfo.versionLastRead != this.getVersion() 
                    && timeSinceLastReply >= this.minReplyInterval)
                {
                    this.replyClient(cinfo, client, offset, count, tag);
                }
                else
                {
                    // File has not changed since the last read, or the required
                    // amount of time has not passed since the last read. Add the client to
                    // the queue of clients awaiting reply, making sure that the
                    // tag is set correctly
                    cinfo.offset = offset;
                    cinfo.count = count;
                    cinfo.tag = tag;
                    this.clientQueue.add(cinfo);
                }
            }
        }
    }
    
    /**
     * This simply calls write() in the contained StyxFile object. This will
     * cause the contentsChanged() method to be called, notifying any waiting
     * clients of the change to the file
     */
    public synchronized void write(StyxFileClient client, long offset,
        int count, ByteBuffer data, boolean truncate, int tag)
        throws StyxException
    {
        this.baseFile.write(client, offset, count, data, truncate, tag);
    }
    
    /**
     * Called to notify that the underlying data have changed.
     * @param force If this is true, clients will be notified of changes to 
     * the underlying data, irrespective of how long they have waited.
     */
    public void contentsChanged(boolean force)
    {
        this.incrementVersion();
        // Reply to all waiting clients with the file's new data
        synchronized (this.clientQueue)
        {
            Iterator it = this.clientQueue.iterator();
            while(it.hasNext())
            {
                ClientInfo cinfo = (ClientInfo)it.next();
                // Only reply to the client if the requisite amount of time has
                // elapsed since the last reply
                long now = System.currentTimeMillis();
                long timeSinceLastReply = now - cinfo.timeLastReply;
                if (force || timeSinceLastReply > this.minReplyInterval)
                {
                    try
                    {
                        this.replyClient(cinfo, cinfo.client, cinfo.offset, cinfo.count, cinfo.tag);
                    }
                    catch(StyxException se)
                    {
                        // This exception is thrown if there was an error reading
                        // the underlying StyxFile.
                        StyxServerProtocolHandler.reply(cinfo.client.getSession(),
                            new RerrorMessage(se.getMessage()), cinfo.tag);
                    }
                    it.remove();
                }
            }
        }
        // Now the clientQueue is empty        
    }
    
    /**
     * Gets the StyxFile that is wrapped by this AsyncStyxFile
     */
    public StyxFile getBaseFile()
    {
        return this.baseFile;
    }
    
    /**
     * This is called when the contents of the underlying StyxFile are changed
     * (required by StyxFileChangeListener interface).
     */
    public void contentsChanged()
    {
        this.contentsChanged(false);
    }
    
    /**
     * Free all resources associated with this file
     */
    public synchronized void delete()
    {
        super.delete();
        this.knownClients.clear();
        this.clientQueue.clear();
    }
    
    /**
     * Called when a client disconnects from the file. We remove all references
     * to this client
     */
    protected synchronized void clientDisconnected(StyxFileClient client)
    {
        this.knownClients.remove(client);
        synchronized (this.clientQueue)
        {
            Enumeration en = this.clientQueue.elements();
            while(en.hasMoreElements())
            {
                ClientInfo cinfo = (ClientInfo)en.nextElement();
                // We're checking for equality of object references - this is OK
                // because we're looking for two references to the same object
                if (client == cinfo.client)
                {
                    this.knownClients.remove(cinfo);
                }
            }
        }
    }
    
    /**
     * Replies to a client and sets the fields of the ClientInfo
     * @throws StyxException if there was an error reading the underlying StyxFile
     */
    private void replyClient(ClientInfo cinfo, StyxFileClient client, long offset,
        int count, int tag) throws StyxException
    {
        long now = System.currentTimeMillis();
        if (cinfo == null)
        {
            cinfo = new ClientInfo(client, tag, offset, count, this.getVersion(), now);
            this.knownClients.put(client, cinfo);
        }
        cinfo.offset = offset;
        cinfo.count = count;
        cinfo.versionLastRead = this.getVersion();
        cinfo.timeLastReply = now;
        cinfo.tag = tag;
        this.baseFile.read(client, offset, count, tag);
    }
    
    /**
     * Class representing a client that is waiting for a reply from an
     * asynchronous read/write
     */
    private class ClientInfo
    {
        private StyxFileClient client;
        private int tag;
        private long offset; // The offset requested by the client
        private int count; // The number of bytes requested by the client
        private long versionLastRead; // the version of the file on the last read
        private long timeLastReply; // the time when data was last sent to this client
        
        private ClientInfo(StyxFileClient client, int tag, long offset, int count,
            long versionLastRead, long timeLastReply)
        {
            this.client = client;
            this.tag = tag;
            this.offset = offset;
            this.count = count;
            this.versionLastRead = versionLastRead;
            this.timeLastReply = timeLastReply;
        }
    }
    
    /**
     * Simple test function
     */
    public static void main(String[] args) throws Exception
    {
        AsyncStyxFile asf = new AsyncStyxFile(new InMemoryFile("test"));
        StyxDirectory root = new StyxDirectory("/");
        root.addChild(asf);
        new StyxServer(2911, root).start();
    }
    
}
