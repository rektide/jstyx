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
 * Revision 1.6  2005/08/31 08:15:57  jonblower
 * Corrections to comments
 *
 * Revision 1.5  2005/06/27 17:17:15  jonblower
 * Changed MessageCallback to pass Tmessage as parameter, rather than storing in the instance
 *
 * Revision 1.4  2005/05/25 15:39:02  jonblower
 * Bug fixes
 *
 * Revision 1.3  2005/05/23 16:48:17  jonblower
 * Overhauled CStyxFile (esp. asynchronous methods) and StyxConnection (added cache of CStyxFiles)
 *
 * Revision 1.1.1.1  2005/02/16 18:58:18  jonblower
 * Initial import
 *
 */
public abstract class MessageCallback
{
    
    /**
     * Called by StyxConnection when a reply arrives to the Tmessage. If the
     * reply is an Rerror message, the error() method of this class will be
     * called with the error string. If the type of the reply is not as
     * expected, the error() method will be called. Otherwise, the replyArrived()
     * method will be called. Subclasses may not override this method.
     */
    final void gotReply(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (rMessage instanceof RerrorMessage)
        {
            this.error(((RerrorMessage)rMessage).getMessage(), tMessage);
        }
        else
        {
            if (rMessage.getType() != tMessage.getType() + 1)
            {
                this.error("Unexpected type of reply (" + rMessage.getType() +
                    ") to message of type " + tMessage.getType(),
                    tMessage);
            }
            else
            {
                this.replyArrived(rMessage, tMessage);
            }
        }
    }
    
    /**
     * Called when the reply arrives.  If the reply is an RreadMessage, the
     * message's buffer should be released when it is no longer needed, 
     * using <code>rMessage.getData().release()</code>.
     * @param rMessage the reply
     * @param tMessage the original T-message
     */
    public abstract void replyArrived(StyxMessage rMessage, StyxMessage tMessage);
    
    /**
     * Called when an error occurs (this may occur if the server returned an
     * Rerror message, or if the reply was not of the expected type, or for 
     * other reasons).
     * @param message The error string
     * @param tMessage The T-message that caused this error to happen
     */
    public abstract void error(String message, StyxMessage tMessage);
    
}
