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

/**
 * Callback class that is associated with an outgoing Tmessage. When the reply
 * arrives, the gotReply() method is called by StyxConnection.replyArrived().
 * If the reply is of the correct type, the replyArrived() method of this class
 * is called; otherwise the error() method is called.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/21 18:09:42  jonblower
 * *** empty log message ***
 *
 * Revision 1.1.1.1  2005/02/16 18:58:18  jonblower
 * Initial import
 *
 */
public abstract class MessageCallback
{
    
    protected StyxMessage tMessage; // Allows the outgoing message to be attached to this callback
    
    public MessageCallback()
    {
        this.tMessage = null;
    }
    
    /**
     * Sets the TMessage that was sent; this will be called from
     * StyxConnection.sendAsync().
     */
    void setTMessage(StyxMessage message)
    {
        this.tMessage = message;
    }
    
    /**
     * Called by StyxConnection when a reply arrives to the Tmessage. If the
     * reply is an Rerror message, the error() method of this class will be
     * called with the error string. If the type of the reply is not as
     * expected, the error() method will be called. Otherwise, the replyArrived()
     * method will be called. Subclasses may not override this method.
     */
    final void gotReply(StyxMessage message)
    {
        if (message instanceof RerrorMessage)
        {
            this.error(((RerrorMessage)message).getMessage(), message.getTag());
        }
        else
        {
            if (message.getType() != this.tMessage.getType() + 1)
            {
                this.error("Unexpected type of reply (" + message.getType() +
                    ") to message of type " + this.tMessage.getType(), message.getTag());
            }
            else
            {
                this.replyArrived(message);
            }
        }
    }
    
    /**
     * Called when the reply arrives.
     * @param message the StyxMessage that has just arrived
     */
    public abstract void replyArrived(StyxMessage message);
    
    /**
     * Called when an error occurs (this may occur if the server returned an
     * Rerror message, or if the reply was not of the expected type, or for 
     * other reasons).
     * @param message The error string
     * @param tag The tag of the message that caused the error. If this is -1
     * then no message was sent but an error was still generated
     */
    public abstract void error(String message, int tag);
    
}
