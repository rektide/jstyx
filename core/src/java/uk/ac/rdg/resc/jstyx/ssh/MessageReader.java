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

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.IOException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoHandler;

import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.messages.StyxMessageDecoder;

/**
 * Thread that reads Styx messages from an InputStream, passing them to a protocol
 * handler when each message arrives.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
class MessageReader extends Thread implements ProtocolDecoderOutput
{    
    private static final Logger log = Logger.getLogger(MessageReader.class);
    
    private InputStream in;
    private IoHandler handler;
    private IoSession session;

    public MessageReader(InputStream in, IoHandler handler, IoSession session)
    {
        this.in = in;
        this.handler = handler;
        this.session = session;
        this.setName("Message reader");
    }

    public void run()
    {
        log.debug("Started MessageReader");
        StyxMessageDecoder decoder = new StyxMessageDecoder();
        //ByteBuffer buf = ByteBuffer.allocate(8192);
        //buf.setAutoExpand(true);
        try
        {
            byte[] b = new byte[8192];
            int n;
            do
            {
                n = this.in.read(b);
                if (n > 0)
                {
                    ByteBuffer buf = ByteBuffer.allocate(n);
                    buf.put(b, 0, n);
                    buf.flip();
                    // We don't need a real IoSession in this case
                    decoder.decode(null, buf, this);
                    //buf.compact();
                    buf.release();
                }
            } while (n >= 0);
        }
        catch (Exception e)
        {
            this.throwHandlerException(e);
        }
        finally
        {
            log.debug("MessageReader finished");
            //buf.release();
            // Release resources associated with the StyxMessageDecoder.
            // This doesn't need a real IoSession
            decoder.dispose(null);
            
            if (this.handler instanceof StyxConnection &&
                ((StyxConnection)this.handler).isConnected())
            {
                // We are still connected to the server but we're not going to
                // receive any more messages - something is wrong
                this.throwHandlerException(
                    new IOException("connection to Styx server broken unexpectedly"));
            }
        }
    }
    
    /**
     * Convenience method to save having to try-catch the exception that
     * exceptionCaught() throws
     */
    private void throwHandlerException(Exception e)
    {
        try
        {
            this.handler.exceptionCaught(this.session, e);
        }
        catch(Exception ex)
        {
            // Not sure why exceptionCaught() can throw an Exception...
            log.error("Exception thrown in exceptionCaught()", ex);
        }
    }

    /**
     * This is called when the StyxMessageDecoder finds a complete message.
     * Specified by ProtocolDecoderOutput interface
     */
    public void write(Object message)
    {
        // Send the message to the StyxServerProtocolHandler.  This may
        // result in a reply being written to the standard output via
        // the session object
        try
        {
            this.handler.messageReceived(this.session, message);
        }
        catch(Exception e)
        {
            // Shouldn't happen
            e.printStackTrace();
        }
    }
}
