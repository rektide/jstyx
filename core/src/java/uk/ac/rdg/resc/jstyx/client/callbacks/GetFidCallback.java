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

package uk.ac.rdg.resc.jstyx.client.callbacks;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.MessageCallback;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.TwalkMessage;
import uk.ac.rdg.resc.jstyx.messages.RwalkMessage;

/**
 * A callback object used to get a fid for a CStyxFile.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */
    
public class GetFidCallback extends MessageCallback
{
    private MessageCallback callback;
    private CStyxFile file;
    private StyxConnection conn;

    public GetFidCallback(CStyxFile file, MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.callback = callback;
    }

    public void walkFid()
    {
        // Need to get a fid for this file
        TwalkMessage tWalkMsg = new TwalkMessage(this.conn.getRootFid(),
            this.conn.getFreeFid(), this.file.getPath());
        this.conn.sendAsync(tWalkMsg, this);
    }

    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
    {
        // Message can only be an RwalkMessage
        RwalkMessage rWalkMsg = (RwalkMessage)rMessage;
        TwalkMessage tWalkMsg = (TwalkMessage)tMessage;
        // Check that the walk was successful
        if (tWalkMsg.getNumPathElements() == rWalkMsg.getNumSuccessfulWalks())
        {
            this.file.setFid(tWalkMsg.getNewFid());
            if (rWalkMsg.getNumSuccessfulWalks() > 0)
            {
                // We can't get the qid from the Rwalk if this was a zero-
                // length walk
                this.file.setQid(rWalkMsg.getQid(rWalkMsg.getNumSuccessfulWalks() - 1));
            }
            if (this.callback != null)
            {
                this.callback.replyArrived(rWalkMsg, tMessage);
            }
        }
        else
        {
            // The walk failed at some point
            String errMsg = "'" + 
                tWalkMsg.getPathElements()[rWalkMsg.getNumSuccessfulWalks()]
                + "' does not exist.";
            this.error(errMsg, tWalkMsg);
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        // The walk was not successful.  Return the fid to the pool
        // and throw an error.
        if (tMessage != null)
        {
            TwalkMessage tWalkMsg = (TwalkMessage)tMessage;
            conn.returnFid(tWalkMsg.getNewFid());
        }
        String errMsg = "Error getting fid for " + this.file.getPath() + ": " + message;
        if (this.callback != null)
        {
            this.callback.error(errMsg, tMessage);
        }
        else
        {
            this.file.fireError(errMsg);
        }
    }
}
