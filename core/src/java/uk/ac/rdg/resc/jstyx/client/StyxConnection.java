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

package uk.ac.rdg.resc.jstyx.client;

import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.io.IOException;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.gleamynode.netty2.Session;
import net.gleamynode.netty2.SessionLog;
import net.gleamynode.netty2.SessionListener;
import net.gleamynode.netty2.Message;
import net.gleamynode.netty2.OrderedEventDispatcher;
import net.gleamynode.netty2.ThreadPooledEventDispatcher;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.TversionMessage;
import uk.ac.rdg.resc.jstyx.messages.RversionMessage;
import uk.ac.rdg.resc.jstyx.messages.TattachMessage;
import uk.ac.rdg.resc.jstyx.messages.RattachMessage;
import uk.ac.rdg.resc.jstyx.messages.TclunkMessage;
import uk.ac.rdg.resc.jstyx.messages.RclunkMessage;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxMessageRecognizer;

/**
 * Object representing a client connection to a Styx server.
 * @todo Queue up messages that are requested to be sent before the handshaking is complete
 * @todo Handle state (open, closed) carefully
 * @todo have a waitconnected() method?
 * @todo fire events (and have a StyxConnectionListener)?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:19  jonblower
 * Initial revision
 *
 */
public class StyxConnection extends Session implements SessionListener
{
    
    private String host; // The host and port to which this is connected
    private int port;
    private String user;
    
    private boolean ready;    // True if this session is ready for messages to be sent
    private Vector unsentMessages; // Messages that are waiting for the connection to be
                                   // established before they are sent
    
    private Vector tagsInUse;     // These vectors keep track of the fids
    private Vector fidsInUse;     // and tags that are in use
    
    private Hashtable msgQueue;   // Container for messages that are awaiting reply
    private Hashtable tClunksPending; // Contains fids of Tclunk messages that are awaiting reply
    
    private long rootFid;         // The fid associated with the root of the server
    private CStyxFile rootDirectory; // The root directory of the server as a CStyxFile
    private int maxMessageSize;   // The maximum size of message that can be sent on this connection
    
    private Vector listeners;     // The StyxConnectionListeners that will be informed of events
    
    private ThreadPooledEventDispatcher eventDispatcher;
    
    private static final Log log = LogFactory.getLog(TestClient.class);
    
    private static StyxMessageRecognizer recognizer = 
        new StyxMessageRecognizer(StyxMessageRecognizer.CLIENT_MODE);
    private static Integer numSessions = new Integer(0); // The number of sessions that have been opened
                                                         // This is an Integer object so we can use it as a lock
    private static final int CONNECT_TIMEOUT = 30; // seconds
    
    // We're creating one eventDispatcher per connection so we can set a pool 
    // size of 1
    private static final int DISPATCHER_THREAD_POOL_SIZE = 1;
    
    /**
     * Creates a new instance of StyxConnection. This will set in motion the
     * necessary handshaking, but will not wait until this handshaking is
     * complete. When the handshaking is complete, the connectionReady() event
     * on all registered StyxConnectionListeners will be fired, or the
     * connectError() event will be fired if there was a problem connecting.
     */
    public StyxConnection(String host, int port, String user)
        throws StyxException
    {
        this.host = host;
        this.port = port;
        this.user = user;
        this.ready = false;
        this.unsentMessages = new Vector();
        this.setSocketAddress(new InetSocketAddress(host, port));
        this.setMessageRecognizer(recognizer);
        // The IoProcessor and EventDispatcher objects are shared between all
        // instances of StyxConnection.
        try
        {
            this.setIoProcessor(StyxUtils.getIoProcessor());
        }
        catch(IOException ioe)
        {
            throw new StyxException("Internal error: could not start IoProcessor");
        }
        // Create a new eventDispatcher for each connection
        this.eventDispatcher = new OrderedEventDispatcher();
        this.eventDispatcher.setThreadPoolSize(DISPATCHER_THREAD_POOL_SIZE);
        this.eventDispatcher.start();
        this.setEventDispatcher(this.eventDispatcher);
        
        this.getConfig().setConnectTimeout(CONNECT_TIMEOUT);
        this.addSessionListener(this);
        
        this.rootFid = 0;
        this.tagsInUse = new Vector();
        this.fidsInUse = new Vector();
        this.msgQueue = new Hashtable();
        this.tClunksPending = new Hashtable();
        this.listeners = new Vector();
        // The synchronization ensures that the numSessions static variable
        // can only be altered by one thread at once
        synchronized(numSessions)
        {
            numSessions = new Integer(numSessions.intValue() + 1);
        }
        this.start();
    }
    
    /**
     * Creates a new instance of StyxConnection, connecting as an anonymous user
     */
    public StyxConnection(String host, int port)
        throws StyxException
    {
        this(host, port, "");
    }
    
    /**
     * Overrides the close() method in Session.  Clunks all fids before
     * closing the connection.  Does nothing if the connection is already closed.
     * The session is only definitively closed when the connectionClosed() event
     * is fired on the registered StyxConnectionListeners.
     */
    public synchronized void close()
    {
        if (this.ready)
        {
            // Start off the chain of clunking fids by clunking the last fid 
            // in the list. When the reply arrives, the next fid will be clunked,
            // and so forth until there are no more fids to clunk.
            // TODO: The problem with this approach is that if any of the Rclunks
            // do not arrive the connection will not close properly.  Should we
            // implement a timeout or something?
            this.clunkNextFid
            (
                new MessageCallback()
                {
                    public void replyArrived(StyxMessage msg)
                    {
                        clunkNextFid(this);
                    }
                    public void error(String message)
                    {
                        // TODO: log error properly
                        System.err.println("Error clunking fid: " + message);
                        clunkNextFid(this);
                    }
                }
            );
        }
        else
        {
            // Just close the connection
            super.close();
        }
    }
    
    /**
     * Clunks the last fid in the fidsInUse vector. If there are no more fids
     * left to clunk, the connection is closed.
     * @todo Should we actually be closing CStyxFiles rather than clunking the fids?
     */
    private void clunkNextFid(MessageCallback callback)
    {
        int openFids = fidsInUse.size();
        boolean tClunkSent = false;
        int i = 1;
        while(!tClunkSent && (openFids - i) >= 0)
        {            
            // Get the last fid in the list
            Long fid = (Long)fidsInUse.get(openFids - i);
            if (this.tClunksPending.containsValue(fid))
            {
                // We have already sent a Tclunk for this fid. Look for the next fid.
                i++;
            }
            else
            {
                // We don't have a Tclunk outstanding for this fid
                this.sendAsync(new TclunkMessage(fid.longValue()), callback);
                tClunkSent = true;
            }
        }
        if(!tClunkSent)
        {
            // There were no more fids left to clunk. Close the connection completely
            super.close();
        }
    }
    
    /**
     * Gets the CStyxFile representing the root directory of the remote Styx
     * server
     */
    public CStyxFile getRootDirectory()
    {
        return this.rootDirectory;
    }
    
    /**
     * Sends a message and returns its tag.  Note that this method will return
     * immediately.  When the reply arrives, the replyArrived() method of the
     * callback object will be called. (The callback can be null.)
     * @return the tag of the outgoing message
     */
    public int sendAsync(StyxMessage message, MessageCallback callback)
    {
        // Set the tag for the message: this is also the key for the message
        // in the Hashtable of outstanding messages
        int tag;
        if (message instanceof TversionMessage)
        {            
            // if this is a TversionMessage, we can use NOTAG
            tag = StyxUtils.NOTAG;
        }
        else
        {
            tag = this.getFreeTag();
        }
        message.setTag(tag);

        // Store the outgoing message in the callback
        if (callback != null)
        {
            callback.setTMessage(message);
            // add to queue of waiting messages
            this.msgQueue.put(new Integer(tag), callback);
        }
        
        if (message instanceof TclunkMessage)
        {
            // Store the fids of outstanding Tclunks so we don't attempt to
            // send another clunk for this fid when we close the connection
            TclunkMessage tClunkMsg = (TclunkMessage)message;
            this.tClunksPending.put(new Integer(tag), new Long(tClunkMsg.getFid()));
        }
        
        synchronized(this)
        {
            // If the connection is open, send the message, otherwise, enqueue it
            if (this.ready || message instanceof TversionMessage ||
                message instanceof TattachMessage)
            {
                // Send the message TODO: check return value 
                super.write(message);
            }
            else
            {
                // We just store the message because the callback has already been stored
                this.unsentMessages.add(message);
            }
        }
        
        // return the message's tag
        return tag;
    }
    
    /**
     * Sends a message and blocks until the corresponding reply arrives
     * @throws StyxException if the message type is not as expected
     */
    public StyxMessage send(StyxMessage message) throws StyxException
    {
        // Create a callback object
        StyxReplyCallback callback = new StyxReplyCallback();
        this.sendAsync(message, callback);
        // callback.getReply() waits until the reply arrives, then returns the reply
        StyxMessage reply = callback.getReply();
        return reply;
    }
    
    /**
     * Called when a reply has arrived from a Styx server
     */
    public void messageReceived(Session session, Message message)
    {
        SessionLog.info(log, session, "RCVD: " + message);
        StyxMessage rMessage = (StyxMessage)message;
        
        // Get the tag of the reply
        int tag = rMessage.getTag();
        
        // Find the callback object in the list of outstanding messages
        MessageCallback callback = (MessageCallback)this.msgQueue.remove(new Integer(tag));
        
        // Return the tag to the pool if it isn't a RversionMessage
        if (!(message instanceof RversionMessage))
        {
            this.returnTag(tag);
        }
        
        if (message instanceof RclunkMessage)
        {
            Long lngFid = (Long)this.tClunksPending.remove(new Integer(tag));
            // Return the fid to the pool
            this.returnFid(lngFid.longValue());
        }
        
        if (callback != null)
        {
            callback.gotReply(rMessage);
        }
    }
    
    /**
     * @return the next unused fid, or -1 if there are none left (this is
     * extremely unlikely and would require StyxUtils.MAXUINT tags to be in
     * use - this would probably only happen due to a bug)
     */
    public long getFreeFid()
    {
        synchronized(this.fidsInUse)
        {
            // Fid zero is the root fid, so we start at 1
            for (int i = 1; i < StyxUtils.MAXUINT; i++)
            {
                // Check to see if this tag is already in use
                if (!this.fidsInUse.contains(new Long(i)))
                {
                    // If not in use, add to the Vector, then return it
                    this.fidsInUse.add(new Long(i));
                    return i;
                }
            }
            return -1;
        }
    }
    
    /**
     * @return the next unused tag, or -1 if there are none left (this is
     * extremely unlikely and would require 65535 tags to be in simultaneous
     * use - this would probably only happen due to a bug)
     * @todo: this works by searching a Vector of used tags. Would this be
     * quicker if it were done some other way?
     */
    private int getFreeTag()
    {
        synchronized(this.tagsInUse)
        {
            for (int i = 0; i < StyxUtils.MAXUSHORT; i++)
            {
                // Check to see if this tag is already in use
                if (!this.tagsInUse.contains(new Integer(i)))
                {
                    // If not in use, add to the Vector, then return it
                    this.tagsInUse.add(new Integer(i));
                    return i;
                }
            }
            return -1;
        }
    }
    
    /**
     * Returns the given fid back to the pool
     */
    public void returnFid(long fid)
    {
        // The remove() method is synchronized so this is safe
        this.fidsInUse.remove(new Long(fid));
    }
    
    /**
     * Returns the given tag back to the pool
     */
    private void returnTag(int tag)
    {
        // The remove() method is synchronized so this is safe
        this.tagsInUse.remove(new Integer(tag));
    }
    
    /**
     * @return the fid associated with the root of the remote server
     */
    public long getRootFid()
    {
        return this.rootFid;
    }
    
    /**
     * @return the maximum size of message that can be sent on this connection
     */
    public int getMaxMessageSize()
    {
        return this.maxMessageSize;
    }
    
    /**
     * Sets the maximum size of message that can be sent on this connection
     */
    public void setMaxMessageSize(int maxMessageSize)
    {
        this.maxMessageSize = maxMessageSize;
    }
    
    /**
     * Called when the socket connection to the remote server has been established
     */
    public void connectionEstablished(Session session)
    {
        SessionLog.info(log, this, "Connection established.");
        TversionMessage tVerMsg = new TversionMessage(8192);
        this.sendAsync(tVerMsg, new TversionCallback());
    }
    
    private class TversionCallback extends MessageCallback
    {
        public void replyArrived(StyxMessage message)
        {
            RversionMessage rVerMsg = (RversionMessage)message;
            maxMessageSize = (int)rVerMsg.getMaxMessageSize();
            TattachMessage tAttMsg = new TattachMessage(rootFid, user);
            sendAsync(tAttMsg, new TattachCallback());
        }
        public void error(String message)
        {
            fireStyxConnectionError(message);
        }
    }
    
    private class TattachCallback extends MessageCallback
    {
        public void replyArrived(StyxMessage message)
        {
            setRootDirectory();
            ready = true;
            fireStyxConnectionReady();
        }
        public void error(String message)
        {
            fireStyxConnectionError(message);
        }
    }
    
    /**
     * Once the root fid has been set and the Tattach message has arrived, 
     * this sets the root directory.
     */
    private void setRootDirectory()
    {
        this.rootDirectory = new CStyxFile(this, "/", this.rootFid);
    }
    
    /**
     * Called when the connection is closed
     */
    public void connectionClosed(Session session)
    {
        SessionLog.info(log, this, "Connection closed.");
        this.ready = false;
        // Stop the event dispatcher
        this.eventDispatcher.stop();
        // The synchronization ensures that the numSessions static variable
        // can only be altered by one thread at once
        synchronized(numSessions)
        {
            numSessions = new Integer(numSessions.intValue() - 1);
            if (numSessions.equals(new Integer(0)));
            {
                // If this is the last session that has been closed, request that
                // the IoProcessor thread stopped.
                StyxUtils.stopIoProcessor();
            }
        }
        this.fireStyxConnectionClosed();
    }
    
    /**
     * Called when an exception is caught by Netty; fires the connectError()
     * event on all registered listeners and closes the connection.
     */
    public void exceptionCaught(Session session, Throwable cause)
    {
        this.fireStyxConnectionError(cause.getClass().getName() + ":" +
            cause.getMessage());
    }
    
    /**
     * Opens a file on the server, throwing a StyxException if the file can't 
     * be found or opened in the given mode. Blocks until the file is open.
     * @param path The path of the file relative to the server root.
     * @param mode Integer representing the mode - see the constants in StyxUtils.
     * For example, to open a file for reading, use StyxUtils.OREAD. To open a
     * file for writing with truncation use StyxUtils.OWRITE | StyxUtils.OTRUNC.
     * @return The file that has just been opened
     */
    public CStyxFile openFile(String path, int mode) throws StyxException
    {
        CStyxFile file = new CStyxFile(this, path);
        file.open(mode);
        return file;
    }
    
    /**
     * Gets the entire contents of a file as a single string. Opens the file
     * for reading, reads the contents and closes the file.
     * This should only be used for relatively short files that can sensibly
     * fit into a String - do not use for large files as not only will this 
     * method block until the file is read, you may run into memory problems.
     * @param path The path of the file relative to the server root.
     * @see CStyxFile.readAll()
     */
    public String getContents(String path) throws StyxException
    {
        return new CStyxFile(this, path).getContents();        
    }
    
    /**
     * Sets the contents of the given file to the given string. Overwrites anything
     * else in the file.
     * @param path The path of the file relative to the server root
     * @param str The new file contents
     * @throws StyxException if there was an error opening or writing to the file
     */
    public void setContents(String path, String str) throws StyxException
    {
        new CStyxFile(this, path).setContents(str);
    }
    
    /**
     * Adds a StyxConnectionListener to this connection. The methods of the
     * listener will be called when events happen, such as the connection being
     * ready for messages, the connection being closed, and an error in connection.
     * If this listener is already registered, this method does nothing.
     */
    public void addListener(StyxConnectionListener listener)
    {
        synchronized(this.listeners)
        {
            if (!this.listeners.contains(listener))
            {
                this.listeners.add(listener);
            }
        }
    }
    
    /**
     * Removes the given listener; does nothing if the listener has not been
     * registered with this.addListener().
     */
    public void removeListener(StyxConnectionListener listener)
    {
        this.listeners.remove(listener);
    }
    
    /**
     * Fired when the connection is ready for traffic (i.e. when a Rattach
     * message arrives).
     */
    private void fireStyxConnectionReady()
    {
        // TODO: log this properly
        System.err.println("***** CONNECTION OPENED TO " + host + ":" + port
            + " *****");
        synchronized(this.listeners)
        {
            for (int i = 0; i < this.listeners.size(); i++)
            {
                StyxConnectionListener listener =
                    (StyxConnectionListener)this.listeners.get(i);
                listener.connectionReady();
            }
        }
        // Now we can send the messages that are awaiting connection
        synchronized(this.unsentMessages)
        {
            Iterator it = this.unsentMessages.iterator();
            while(it.hasNext())
            {
                StyxMessage message = (StyxMessage)it.next();
                System.err.println("Sending message now connection is ready");
                super.write(message);
                it.remove();
            }
        }
    }
    
    /**
     * Fired when the connection is closed.
     */
    private void fireStyxConnectionClosed()
    {
        // TODO: log this properly
        System.err.println("***** CONNECTION TO " + this.host + ":" + this.port
            + " CLOSED *****");
        synchronized(this.listeners)
        {
            for (int i = 0; i < this.listeners.size(); i++)
            {
                StyxConnectionListener listener =
                    (StyxConnectionListener)this.listeners.get(i);
                listener.connectionClosed();
            }
        }
    }
    
    /**
     * Fired when an error occurs when connecting to the remote server
     */
    private void fireStyxConnectionError(String message)
    {
        // TODO: log this properly
        System.err.println("***** ERROR OCCURRED ON CONNECTION TO " + this.host +
            ":" + this.port + " (" + message + ") *****");
        synchronized(this.listeners)
        {
            for (int i = 0; i < this.listeners.size(); i++)
            {
                StyxConnectionListener listener =
                    (StyxConnectionListener)this.listeners.get(i);
                listener.connectError(message);
            }
        }
        // call error() on all waiting callbacks of outstanding messages
        synchronized(this.unsentMessages)
        {
            Iterator it = this.unsentMessages.iterator();
            while(it.hasNext())
            {
                int tag = ((StyxMessage)it.next()).getTag();
                MessageCallback callback =
                    (MessageCallback)this.msgQueue.remove(new Integer(tag));
                callback.error("Could not send message: " + message);
                it.remove();
            }
        }
        // Make sure the connection is closed
        this.close();
    }
    
    /**
     * Called by Netty when a message has been sent
     */
    public void messageSent(Session session, Message message)
    {
        SessionLog.info(log, session, "SENT: " + message);
    }
    
    /**
     * Required by the SessionListener interface. Does nothing in this class.
     */
    public void sessionIdle(Session session)
    {
        return;
    }
}
