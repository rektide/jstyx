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

/**
 * A MessageCallback that contains another MessageCallback. By default, this 
 * class simply passes any messages that arrive to the nested MessageCallback;
 * subclasses will want to override replyArrived() and error() to get the desired
 * behaviour, in the knowledge that they can access the nested callback as
 * this.nestedCallback.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/21 18:09:43  jonblower
 * *** empty log message ***
 *
 * Revision 1.1.1.1  2005/02/16 18:58:18  jonblower
 * Initial import
 *
 */
public class NestedMessageCallback extends MessageCallback
{
    
    protected MessageCallback nestedCallback;
    protected Object attachment;
    
    /**
     * @throws IllegalArgumentException if the given callback is null
     */
    public NestedMessageCallback(MessageCallback callback)
    {
        this(callback, null);
    }
    
    /**
     * @throws IllegalArgumentException if the given callback is null
     */
    public NestedMessageCallback(MessageCallback callback, Object attachment)
    {
        if (callback == null)
        {
            throw new IllegalArgumentException("Nested callback cannot be null");
        }
        this.nestedCallback = callback;
        this.attachment = attachment;
    }
    
    /**
     * This ensures that the Tmessage is also set in the nested callback
     */
    public void setTMessage(StyxMessage message)
    {
        super.setTMessage(message);
        this.nestedCallback.setTMessage(message);
    }
    
    /**
     * This default implementation simply passes the message to the nested
     * callback.
     */
    public void replyArrived(StyxMessage message)
    {
        this.nestedCallback.replyArrived(message);
    }
    
    /**
     * This default implementation simply passes the error message to the nested
     * callback.
     */
    public void error(String message, int tag)
    {
        this.nestedCallback.error(message, tag);
    }
}
