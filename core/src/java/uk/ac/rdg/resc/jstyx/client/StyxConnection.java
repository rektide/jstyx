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
import java.security.GeneralSecurityException;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Iterator;

import org.apache.mina.common.IoFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFuture.Callback;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.SSLFilter;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.TversionMessage;
import uk.ac.rdg.resc.jstyx.messages.RversionMessage;
import uk.ac.rdg.resc.jstyx.messages.TauthMessage;
import uk.ac.rdg.resc.jstyx.messages.RauthMessage;
import uk.ac.rdg.resc.jstyx.messages.TattachMessage;
import uk.ac.rdg.resc.jstyx.messages.RattachMessage;
import uk.ac.rdg.resc.jstyx.messages.TclunkMessage;
import uk.ac.rdg.resc.jstyx.messages.RclunkMessage;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.messages.StyxCodecFactory;
import uk.ac.rdg.resc.jstyx.ssl.StyxSSLContextFactory;

/**
 * Object representing a client connection to a Styx server.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.28  2006/03/21 14:58:41  jonblower
 * Implemented clear-text password-based authentication and did some simple tests
 *
 * Revision 1.27  2006/03/21 09:06:14  jonblower
 * Still implementing authentication
 *
 * Revision 1.24  2005/08/08 09:35:19  jonblower
 * Commented out thread pool filters
 *
 * Revision 1.23  2005/07/08 16:01:27  jonblower
 * Reinstated ProtocolThreadPoolFilter
 *
 * Revision 1.22  2005/07/08 15:22:54  jonblower
 * Upgraded MINA library to 0.7.3-SNAPSHOT
 *
 * Revision 1.21  2005/06/27 17:17:15  jonblower
 * Changed MessageCallback to pass Tmessage as parameter, rather than storing in the instance
 *
 * Revision 1.18  2005/05/25 15:37:55  jonblower
 * Removed cache of CStyxFiles, dealt differently with root fid
 *
 * Revision 1.17  2005/05/23 16:48:17  jonblower
 * Overhauled CStyxFile (esp. asynchronous methods) and StyxConnection (added cache of CStyxFiles)
 *
 * Revision 1.16  2005/05/16 16:16:52  jonblower
 * Implemented getRemoteHost() and getRemotePort()
 *
 * Revision 1.15  2005/05/16 13:09:54  jonblower
 * Added StyxConnection object as first argument in all StyxConnectionListener methods
 *
 * Revision 1.14  2005/05/16 12:57:39  jonblower
 * Constructors no longer throw StyxException
 *
 * Revision 1.13  2005/05/09 13:35:48  jonblower
 * Now throws an exception if try to send a message before connecting
 *
 * Revision 1.12  2005/05/05 16:57:31  jonblower
 * Updated MINA library to revision 168337 and changed code accordingly
 *
 * Revision 1.11  2005/03/22 17:42:24  jonblower
 * Changed default message size to 8192 for efficiency in MINA
 *
 * Revision 1.10  2005/03/22 10:19:52  jonblower
 * Fixed problem with ByteBuffer leak in StyxMessageDecoder and StyxFileInputStream
 *
 * Revision 1.9  2005/03/17 07:30:05  jonblower
 * Improved error logging code
 *
 * Revision 1.8  2005/03/16 17:55:52  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.7  2005/03/15 15:51:37  jonblower
 * Removed hard limit on maximum message size
 *
 * Revision 1.6  2005/03/11 13:58:25  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.5.2.2  2005/03/11 08:29:52  jonblower
 * Moved to log4j logging system (from apache commons logging)
 *
 * Revision 1.5.2.1  2005/03/10 11:48:30  jonblower
 * Updated to fit in with MINA framework
 *
 * Revision 1.4  2005/02/21 18:07:23  jonblower
 * Separated constructor and connect methods
 *
 * Revision 1.3  2005/02/18 17:56:31  jonblower
 * Set root directory in constructor; doesn't need to wait until connection is made
 *
 * Revision 1.1.1.1  2005/02/16 18:58:19  jonblower
 * Initial import
 *
 */
public class StyxConnection implements IoHandler
{
    protected static final Logger log = Logger.getLogger(StyxConnection.class);
    
    /**
     * The default maximum message size that this connection will request. This
     * is not necessarily the same as the maximum message size that will be used;
     * this is up to the remote server.
     *
     * This is set to 8192 because it is most efficient if this is a power of two;
     * MINA allocates its reusable ByteBuffers in sizes that are powers of two. If 
     * this were, say, 8216 bytes then MINA would allocate ByteBuffers of capacity
     * 16384 bytes to accommodate a single message.
     */
    private static final int DEFAULT_MAX_MESSAGE_SIZE_REQUEST = 8192;
    
    protected String host; // The host and port to which this is connected
    private int port;
    private String username;
    private String password;
    
    protected boolean connecting;    // True if connect() or connectAsync() has been
                                   // called, and the connection has not been closed
    private boolean connected;     // True if this is connected to the remote
                                   // server (handshaking might not have been done)
    private String errMsg;         // Non-null if an error occurred
    private Vector unsentMessages; // Messages that are waiting for the connection to be
                                   // established before they are sent
    
    private Vector tagsInUse;     // These vectors keep track of the fids
    private Vector fidsInUse;     // and tags that are in use
    
    private Hashtable msgQueue;   // Container for messages that are awaiting reply
    private Hashtable tClunksPending; // Contains fids of Tclunk messages that are awaiting reply
    
    private long rootFid;         // The fid associated with the root of the server
    private CStyxFile rootDirectory; // The root directory of the server as a CStyxFile
    private int maxMessageSizeRequest;  // The requested maximum size of message that can be sent on this connection
    private int maxMessageSize;   // The actual maximum size of message that can be sent on this connection
    private boolean useSSL;       // True if we are going to make an SSL connection
    
    protected IoSession session;
    
    private Vector listeners;     // The StyxConnectionListeners that will be informed of events
    
    /**
     * Creates a new instance of StyxConnection. This does not actually make the
     * connection; call connectAsync() or connect() to do this.
     */
    public StyxConnection(String host, int port, String username, String password,
        int maxMessageSizeRequest)
    {
        this.host = host;
        this.port = port;
        this.username = username.trim();
        this.password = password.trim();
        this.connecting = false;
        this.connected = false;
        this.errMsg = null;
        this.unsentMessages = new Vector();
        this.rootFid = -1;
        this.tagsInUse = new Vector();
        this.fidsInUse = new Vector();
        this.msgQueue = new Hashtable();
        this.tClunksPending = new Hashtable();
        this.listeners = new Vector();
        this.maxMessageSizeRequest = maxMessageSizeRequest;
        this.rootDirectory = this.getFile("/");
    }
    
    /**
     * Uses DEFAULT_MAX_MESSAGE_SIZE_REQUEST
     */
    public StyxConnection(String host, int port, String username, String password)
    {
        this(host, port, username, password, DEFAULT_MAX_MESSAGE_SIZE_REQUEST);
    }
    
    /**
     * Creates a new instance of StyxConnection, connecting as an anonymous user
     */
    public StyxConnection(String host, int port, int maxMessageSizeRequest)
    {
        this(host, port, "", "", maxMessageSizeRequest);
    }
    
    /**
     * Creates a new instance of StyxConnection, connecting as an anonymous user
     */
    public StyxConnection(String host, int port)
    {
        this(host, port, "", "");
    }
    
    /**
     * Creates a new instance of StyxConnection, connecting as an anonymous user
     */
    public StyxConnection(String host, int port, boolean useSSL)
    {
        this(host, port, "", "");
        this.useSSL = true;
    }
    
    /**
     * @return the name (or IP address) of the remote host
     */
    public String getRemoteHost()
    {
        return this.host;
    }
    
    /**
     * @return the port of the remote host
     */
    public int getRemotePort()
    {
        return this.port;
    }
    
    /**
     * Connects to the remote server and handshakes. This method returns 
     * immediately; when the connection and handshaking are complete, the
     * connectionReady() event will be fired on all registered 
     * StyxConnectionListeners. If an error occurred when connecting or
     * handshaking, the connectionError() event will be fired on the listeners.
     * This method will do nothing if we have already connected or are in the
     * process of connecting.
     */
    public synchronized void connectAsync()
    {
        if (this.connecting)
        {
            log.info("Already connecting");
        }
        else
        {
            this.connecting = true;
            IoConnector connector = new SocketConnector();
            ConnectFuture future = connector.connect(
                new InetSocketAddress( this.host, this.port ), this );
            // Add a callback to test if the connection was successful.
            future.setCallback(new IoFuture.Callback()
            {
                public void operationComplete( IoFuture f )
                {
                    ConnectFuture future = ( ConnectFuture ) f;
                    try
                    {
                        log.debug("Connection complete");
                        session = future.getSession();
                    }
                    catch( IOException e )
                    {
                        // Connect failed
                        fireStyxConnectionError(new IOException("Could not connect to server"));
                    }
                }
            });
        }
    }
    
    /**
     * Connects to the remote server and handshakes. This method blocks until
     * the connection is made and the handshaking is complete, throwing a
     * StyxException if an error occurred when connecting. If the connection is
     * already made, this method does nothing. The connectionReady()
     * and connectionError() events will be fired on any registered listeners
     * when the connection is ready or if an error occurs.
     * @throws StyxException if the IOProcessor could not be started or if 
     * an error occurred during connection or handshaking
     */
    public void connect() throws StyxException
    {
        this.connectAsync(); // This does nothing if already started
        while (this.rootFid < 0 && this.errMsg == null)
        {
            synchronized(this)
            {
                try
                {
                    this.wait();
                }
                catch(InterruptedException ie)
                {
                    // do nothing
                }
            }
        }
        if (this.errMsg != null)
        {
            // An error occurred when connecting
            throw new StyxException(this.errMsg);
        }
    }
    
    /**
     * @return true if a connection has been made to the server
     */
    public boolean isConnected()
    {
        return this.connected;
    }
    
    /**
     * Overrides the close() method in Session.  Clunks all fids before
     * closing the connection.  Does nothing if the connection has not been made.
     * If the connection has been made, but the handshaking has not been done,
     * this will enqueue the close request so that, when handshaking is
     * complete, the fids will be clunked.
     * Note that there is a possibility that if close() is called immediately
     * after connectAsync(), the call to close() will return (having done nothing)
     * before the connection is open. TODO: what can we do about this?
     * The session is only definitively closed when the connectionClosed() event
     * is fired on the registered StyxConnectionListeners.
     * @todo rename this closeAsync() and implement a blocking close() method?
     */
    public void close()
    {
        log.debug("Called close() on StyxConnection");
        if (this.connected)
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
                    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
                    {
                        clunkNextFid(this);
                    }
                    public void error(String message, StyxMessage tMessage)
                    {
                        log.debug("Error clunking fid: " + message);
                        clunkNextFid(this);
                    }
                }
            );
        }
        else
        {
            try
            {
                this.sessionClosed(this.session);
            }
            catch(Exception e)
            {
                log.error("Exception thrown when closing StyxConnection", e);
            }
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
            // No more fids left to clunk
            if (this.session != null)
            {
                // The Session object can be null in the case of an SSH
                // connection if we have connected but not authenticated or
                // started the StyxSSHServer on the remote machine
                this.session.close();
            }
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
     * Simple struct to contain a T-message plus its associated callback. Note 
     * that we cannot store the T-message in the callback itself because a callback
     * may be associated with several T-messages
     */
    private class MessagePlusCallback
    {
        private StyxMessage tMessage;
        private MessageCallback callback;
        public MessagePlusCallback(StyxMessage tMessage, MessageCallback callback)
        {
            this.tMessage = tMessage;
            this.callback = callback;
        }
    }
    
    /**
     * Sends a message and returns its tag.  Note that this method will return
     * immediately.  When the reply arrives, the replyArrived() method of the
     * callback object will be called. (The callback can be null.).  If the 
     * connection hasn't been made yet, the message will be put in a queue and
     * will be sent when the connection is ready (TODO this is convenient as it
     * saves waiting for the connectionReady() event, but is this the best thing
     * to do?)
     *
     * If this is called before connect() or connectAsync(), the callback's error
     * function will be called.
     *
     * @param tMessage the message to be sent
     * @param callback the MessageCallback to be called when the reply arrives
     * @return the tag of the outgoing message
     */
    public int sendAsync(StyxMessage tMessage, MessageCallback callback)
    {
        return this.sendAsync(tMessage, callback, false);
    }
    
    /**
     * Sends a message and returns its tag.  Note that this method will return
     * immediately.  When the reply arrives, the replyArrived() method of the
     * callback object will be called. (The callback can be null.).  If the 
     * connection hasn't been made yet, the message will be put in a queue and
     * will be sent when the connection is ready (TODO this is convenient as it
     * saves waiting for the connectionReady() event, but is this the best thing
     * to do?)
     *
     * If this is called before connect() or connectAsync(), the callback's error
     * function will be called unless isHandshake==true;
     *
     * @param tMessage the message to be sent
     * @param callback the MessageCallback to be called when the reply arrives
     * @param isHandshake if true, this message is part of the connection process
     * itself (e.g. authentication)
     * @return the tag of the outgoing message
     */
    public int sendAsync(StyxMessage tMessage, MessageCallback callback, boolean isHandshake)
    {
        if (!this.connecting)
        {
            if (callback != null)
            {
                callback.error("Must connect before sending a message", tMessage);
            }
            else
            {
                log.error("Attempt to send a message before connecting");
            }
        }
        // Set the tag for the message: this is also the key for the message
        // in the Hashtable of outstanding messages
        int tag;
        if (tMessage instanceof TversionMessage)
        {            
            // if this is a TversionMessage, we can use NOTAG
            tag = StyxUtils.NOTAG;
        }
        else
        {
            tag = this.getFreeTag();
        }
        tMessage.setTag(tag);

        if (callback != null)
        {
            // add to queue of waiting messages
            this.msgQueue.put(new Integer(tag), new MessagePlusCallback(tMessage, callback));
        }
        
        if (tMessage instanceof TclunkMessage)
        {
            // Store the fids of outstanding Tclunks so we don't attempt to
            // send another clunk for this fid when we close the connection
            TclunkMessage tClunkMsg = (TclunkMessage)tMessage;
            this.tClunksPending.put(new Integer(tag), new Long(tClunkMsg.getFid()));
        }
        
        synchronized(this)
        {
            // If the connection is ready (signalled by rootFid being set), or
            // if this is part of a handshake, send the message, otherwise, enqueue it
            if (this.rootFid >= 0 || tMessage instanceof TclunkMessage || isHandshake)
            {
                // Send the message
                this.session.write(tMessage);
            }
            else
            {
                // We just store the message because the callback has already been stored
                this.unsentMessages.add(tMessage);
            }
        }
        
        // return the message's tag
        return tag;
    }
    
    /**
     * Sends a message and blocks until the corresponding reply arrives
     * @throws StyxException if the message type is not as expected, or if the 
     * connection has not been made.
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
    public void messageReceived( IoSession session, Object message )
    {
        if (log.isDebugEnabled())
        {
            log.debug("RCVD: " + message);
        }
        StyxMessage rMessage = (StyxMessage)message;
        
        // Get the tag of the reply
        int tag = rMessage.getTag();
        
        // Find the callback object in the list of outstanding messages
        MessagePlusCallback mpc = (MessagePlusCallback)this.msgQueue.remove(new Integer(tag));
        
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
        
        if (mpc != null && mpc.callback != null)
        {
            mpc.callback.gotReply(rMessage, mpc.tMessage);
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
            for (long i = 0; i < StyxUtils.MAXUINT; i++)
            {
                Long lngFid = new Long(i);
                // Check to see if this tag is already in use
                if (!this.fidsInUse.contains(lngFid))
                {
                    // If not in use, add to the Vector, then return it
                    this.fidsInUse.add(lngFid);
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
     * Invoked when the session is created.  Initialize default socket
     * parameters and user-defined attributes here.
     */
    public void sessionCreated( IoSession session )
    {
        this.session = session;
        ProtocolCodecFactory codec = StyxCodecFactory.getInstance();
        session.getFilterChain().addLast(
                "protocolFilter", new ProtocolCodecFilter( codec ) );
        log.debug("Connection created");
    }
    
    /**
     * Called when the socket connection to the remote server has been established
     */
    public void sessionOpened( IoSession session )
    {
        this.connected = true;
        log.debug("Connection opened.");
        // Also could use Styx with SSL ("9P2000s")
        TversionMessage tVerMsg = new TversionMessage(this.maxMessageSizeRequest,
            "9P2000");
        this.sendAsync(tVerMsg, new TversionCallback(), true);
    }
    
    private class TversionCallback extends MessageCallback
    {
        public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
        {
            RversionMessage rVerMsg = (RversionMessage)rMessage;
            if (rVerMsg.getVersion().equals("9P2000s"))
            {
                // Server supports SSL, so we must add an SSL filter.  This must
                // be first in the chain so that messages are encoded into bytes
                // (by the ProtocolCodecFilter) before being encrypted
                try
                {
                    SSLFilter filter = new SSLFilter(
                        StyxSSLContextFactory.getClientSSLContext());
                    filter.setUseClientMode(true);
                    session.getFilterChain().addFirst("ssl", filter);
                    log.info("Using SSL");
                }
                catch(GeneralSecurityException gse)
                {
                    fireStyxConnectionError(gse);
                }
            }
            else if (rVerMsg.getVersion().equals("9P2000"))
            {
                log.info("Server does not support SSL");
                if (!username.equalsIgnoreCase(""))
                {
                    // TODO: warn the user that password will be transmitted in
                    // the clear
                }
            }
            else
            {
                this.error("Unsupported protocol version " + rVerMsg.getVersion(),
                    tMessage);
            }
            maxMessageSize = (int)rVerMsg.getMaxMessageSize();
            if (username.equalsIgnoreCase(""))
            {
                log.debug("Unauthenticated connection");
                TattachMessage tAttMsg = new TattachMessage(getFreeFid(), username);
                sendAsync(tAttMsg, new TattachCallback(), true);
            }
            else
            {
                log.debug("Authenticated connection");
                TauthMessage tAuthMsg = new TauthMessage(getFreeFid(), username, "");
                sendAsync(tAuthMsg, new TauthCallback(), true);
            }
        }
        public void error(String message, StyxMessage tMessage)
        {
            fireStyxConnectionError(new Throwable(message));
        }
    }
    
    private class TauthCallback extends MessageCallback
    {
        public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
        {
            TauthMessage tAuthMsg = (TauthMessage)tMessage;
            RauthMessage rAuthMsg = (RauthMessage)rMessage;
            // We now have a fid to an authentication file.  We write the
            // password into this file
            CStyxFile authFile = getFile("/auth");
            authFile.setFid(tAuthMsg.getAfid());
            authFile.setQid(rAuthMsg.getAQid());
            authFile.writeAsync(password, 0, new AuthFileWriteCallback(tAuthMsg.getAfid()));
        }
        public void error(String message, StyxMessage tMessage)
        {
            TauthMessage tAuthMsg = (TauthMessage)tMessage;
            returnFid(tAuthMsg.getAfid());
            fireStyxConnectionError(new Throwable(message));
        }
    }
    
    /**
     * This callback is called when we have written the password to the
     * auth file
     */
    private class AuthFileWriteCallback extends MessageCallback
    {
        private long afid;
        public AuthFileWriteCallback(long afid)
        {
            this.afid = afid;
        }
        public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
        {
            // The write was successful.  Now we can attach
            TattachMessage tAttMsg =
                new TattachMessage(getFreeFid(), this.afid, username, "");
            sendAsync(tAttMsg, new TattachCallback(), true);
        }
        public void error(String message, StyxMessage tMessage)
        {
            fireStyxConnectionError(new Throwable(message));
        }
    }
    
    private class TattachCallback extends MessageCallback
    {
        public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
        {
            TattachMessage tAttMsg = (TattachMessage)tMessage;
            rootFid = tAttMsg.getFid();
            fireStyxConnectionReady();
        }
        public void error(String message, StyxMessage tMessage)
        {
            TattachMessage tAttMsg = (TattachMessage)tMessage;
            returnFid(tAttMsg.getFid());
            fireStyxConnectionError(new Throwable(message));
        }
    }
    
    /**
     * Called when the connection is closed
     * @throws Exception if an error occurs (actualy this will never happen
     * from this method unless overridden in a subclass)
     */
    public void sessionClosed( IoSession session ) throws Exception
    {
        if (this.connected)
        {
            log.debug("Connection closed.");
        }
        this.connected = false;
        this.connecting = false;
        this.rootFid = -1;
        
        this.fireStyxConnectionClosed();
    }
    
    /**
     * Called when an exception is caught by MINA; fires the connectError()
     * event on all registered listeners and closes the connection.
     */
    public void exceptionCaught( IoSession session, Throwable cause )
    {
        this.fireStyxConnectionError(cause);
    }
    
    /**
     * Gets a CStyxFile with the given path.  Note that each call to this method
     * will return a new object, even if the path is identical.
     * This does not open, create or check the existence of the file: no
     * messages are sent to the server in this method so this will never block.
     * @throws InvalidPathException if the given path is not valid and absolute
     * (only catch this runtime exception if it is likely that the path could be
     * invalid, e.g. when the path is being input by a user)
     */
    public CStyxFile getFile(String path)
    {
        if (!path.startsWith("/"))
        {
            path = "/" + path;
        }
        return new CStyxFile(this, path);
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
        CStyxFile file = this.getFile(path);
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
     */
    public String getContents(String path) throws StyxException
    {
        return this.getFile(path).getContents();        
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
        log.info("***** CONNECTION OPENED TO " + host + ":" + port
            + " *****");
        synchronized(this)
        {
            // Notify any waiting threads (e.g. if using connect())
            this.notifyAll();
        }
        synchronized(this.listeners)
        {
            for (int i = 0; i < this.listeners.size(); i++)
            {
                StyxConnectionListener listener =
                    (StyxConnectionListener)this.listeners.get(i);
                listener.connectionReady(this);
            }
        }
        // Now we can send the messages that are awaiting connection
        synchronized(this.unsentMessages)
        {
            Iterator it = this.unsentMessages.iterator();
            while(it.hasNext())
            {
                StyxMessage message = (StyxMessage)it.next();
                log.debug("Sending message now connection is ready");
                this.session.write(message);
                it.remove();
            }
        }
    }
    
    /**
     * Fired when the connection is closed.
     */
    private void fireStyxConnectionClosed()
    {
        log.info("***** CONNECTION TO " + this.host + ":" + this.port
            + " CLOSED *****");
        synchronized(this.listeners)
        {
            for (int i = 0; i < this.listeners.size(); i++)
            {
                StyxConnectionListener listener =
                    (StyxConnectionListener)this.listeners.get(i);
                listener.connectionClosed(this);
            }
        }
    }
    
    /**
     * Fired when an error occurs (i.e. an exception is caught by MINA)
     */
    protected void fireStyxConnectionError(Throwable cause)
    {
        this.errMsg = cause.getMessage();
        if (log.isDebugEnabled())
        {
            cause.printStackTrace();
        }
        log.error("***** ERROR OCCURRED ON CONNECTION TO " + this.host +
            ":" + this.port + " (" + cause.getClass().getName() + ":" +
            this.errMsg + ") *****");
        synchronized(this)
        {
            this.notifyAll();
        }
        synchronized(this.listeners)
        {
            for (int i = 0; i < this.listeners.size(); i++)
            {
                StyxConnectionListener listener =
                    (StyxConnectionListener)this.listeners.get(i);
                listener.connectionError(this, this.errMsg);
            }
        }
        // call error() on all waiting callbacks of outstanding messages
        synchronized(this.unsentMessages)
        {
            Iterator it = this.unsentMessages.iterator();
            while(it.hasNext())
            {
                StyxMessage tMessage = (StyxMessage)it.next();
                int tag = tMessage.getTag();
                MessageCallback callback =
                    (MessageCallback)this.msgQueue.remove(new Integer(tag));
                callback.error("Could not send message: " + tMessage, tMessage);
                it.remove();
            }
        }
        // Make sure the connection is closed
        this.close();
    }
    
    /**
     * Called by MINA when a message has been sent
     */
    public void messageSent( IoSession session, Object message )
    {
        if (log.isDebugEnabled())
        {
            log.debug("SENT: " + message);
        }
    }
    
    /**
     * Required by the ProtocolHandler interface. Does nothing in this class.
     */
    public void sessionIdle( IoSession session, IdleStatus status )
    {
        return;
    }
}
