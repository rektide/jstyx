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

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.RerrorMessage;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Class that implements a method that waits until a reply arrives, then
 * returns the reply.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/21 18:09:43  jonblower
 * *** empty log message ***
 *
 * Revision 1.1.1.1  2005/02/16 18:58:20  jonblower
 * Initial import
 *
 */
class StyxReplyCallback extends MessageCallback
{
    private StyxMessage reply; // The reply from a Styx server (i.e. an RMessage)
    private String errorMsg;   // Non-null if an error has occurred
    
    /**
     * Creates a new instance of StyxReplyCallback
     */
    public StyxReplyCallback()
    {
        this.reply = null;
        this.errorMsg = null;
    }
    
    /**
     * Waits for the reply to arrive, then returns it
     * @return the RMessage corresponding to the TMessage that was sent
     * @throws StyxException if an error is returned from the server
     */
    public synchronized StyxMessage getReply() throws StyxException
    {
        while (reply == null && errorMsg == null)
        {
            try
            {
                // wait for Producer to put value
                wait();
            }
            catch (InterruptedException e)
            {
            }
        }
        if (errorMsg != null)
        {
            throw new StyxException(errorMsg);
        }
        return reply;
    }
    
    /**
     * Called when a valid reply (i.e. not an Rerror message) arrives from
     * the server.
     */
    public synchronized void replyArrived(StyxMessage msg)
    {
        reply = msg;
        notifyAll();
    }
    
    /**
     * Called when an error occurs (either an Rerror message was returned by
     * the server, the reply was of an unexpected type, or something else - 
     * see MessageCallback).
     */
    public synchronized void error(String message, int tag)
    {
        errorMsg = message;
        notifyAll();
    }
    
}
