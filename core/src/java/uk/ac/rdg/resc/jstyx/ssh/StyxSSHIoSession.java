/*
 * Copyright (c) 2006 The University of Reading
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

package uk.ac.rdg.resc.jstyx.ssh;

import java.net.SocketAddress;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.log4j.Logger;

import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.server.StyxSessionState;
import uk.ac.rdg.resc.jstyx.messages.StyxMessage;

/**
 * IoSession object for the StyxSSHServer and StyxSSHConnection. Messages are
 * written to a specified PrintStream.  Most methods is this class are dummies
 * as they are not needed (we do not use the full MINA framework in this case).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class StyxSSHIoSession extends BaseIoSession
{
    protected static final Logger log = Logger.getLogger(StyxSSHIoSession.class);
    
    private StyxSessionState sessionState;
    private IoHandler handler;
    private PrintStream[] streams;
    
    /**
     * Creates a new instance of StyxStreamIoSession.  This constructor is 
     * typically used for debugging, to ensure that Styx messages will be sent
     * to a log file as well as to the required output stream.
     * @param streams The Styx messages will be printed to all of these streams.
     */
    public StyxSSHIoSession(IoHandler handler, PrintStream[] streams)
    {
        this.sessionState = new StyxSessionState(this);
        this.handler = handler;
        this.streams = streams;
    }
    
    /** Creates a new instance of StyxStreamIoSession */
    public StyxSSHIoSession(IoHandler handler, PrintStream stream)
    {
        this(handler, new PrintStream[]{stream});
    }
    
    /**
     * Writes the message to the output PrintStreams immediately
     */
    public synchronized WriteFuture write( Object message )
    {
        if (!(message instanceof StyxMessage))
        {
            throw new IllegalArgumentException("message is not a StyxMessage");
        }
        StyxMessage styxMsg = (StyxMessage)message;
        
        // Encode the message into bytes
        ByteBuffer buf = styxMsg.encode();
        byte[] b = new byte[buf.remaining()];
        buf.get(b);
        
        // Write the message to the output stream
        try
        {
            for (int i = 0; i < this.streams.length; i++)
            {
                this.streams[i].write(b);
            }
        }
        catch(IOException ioe)
        {
            // Shouldn't happen
            ioe.printStackTrace();
        }
        
        // Notify the handler that the message has been sent
        try
        {
            this.handler.messageSent(this, message);
        }
        catch(Exception e)
        {
            // Ignore: shouldn't happen anyway
        }
        
        WriteFuture future = new WriteFuture();
        future.setWritten(true);
        return future;
    }
    
    /**
     * Gets the StyxSessionState that is attached to this session.  Each 
     * StyxStreamIoSession has an associated state object: these state objects
     * are not shared between session instances.
     */
    public Object getAttachment()
    {
        return this.sessionState;
    }
    
    public IoHandler getHandler()
    {
        return this.handler;
    }
    
    public CloseFuture close()
    {
        for (int i = 0; i < this.streams.length; i++)
        {
            this.streams[i].close();
        }
        try
        {
            this.handler.sessionClosed(this);
        }
        catch (Exception e)
        {
            log.error("Exception thrown when closing IoHandler", e);
        }
        CloseFuture cf = new CloseFuture();
        cf.setClosed();
        return cf;
    }
    
    public void updateTrafficMask() {}
    public int getScheduledWriteBytes() { return 0; }
    public int getScheduledWriteRequests() { return 0; }
    public SocketAddress getServiceAddress() { return null; }
    public SocketAddress getLocalAddress() { return null; }
    public SocketAddress getRemoteAddress() { return null; }
    public TransportType getTransportType() { return null; }
    public IoFilterChain getFilterChain() { return null; }
    public IoSessionConfig getConfig() { return null; }
    public IoService getService() { return null; }
    
}
