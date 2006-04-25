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

package uk.ac.rdg.resc.jstyx.client.callbacks;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.MessageCallback;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.TremoveMessage;
import uk.ac.rdg.resc.jstyx.messages.RwalkMessage;

/**
 * Callback used when removing a file (see CStyxFile.removeAsync())
 *
 * @author jdb
 * $Revision: 604 $
 * $Date: 2006-03-21 14:58:42 +0000 (Tue, 21 Mar 2006) $
 */

public class RemoveCallback extends MessageCallback
{
    private CStyxFile file;
    private MessageCallback callback;

    public RemoveCallback(CStyxFile file, MessageCallback callback)
    {
        this.file = file;
        this.callback = callback;
    }

    public void nextStage()
    {
        if (this.file.hasFid())
        {
            TremoveMessage tRemMsg = new TremoveMessage(this.file.getFid());
            this.file.getConnection().sendAsync(tRemMsg, this);
        }
        else
        {
            // We don't have a fid for the file yet
            this.file.walkFidAsync(this);
        }
    }

    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (rMessage instanceof RwalkMessage)
        {
            // This is the reponse from getFidAsync()
            // We've just got the open fid for the file. Go to the next stage
            // (i.e. actually open the file)
            this.nextStage();
        }
        else
        {
            // Must be an RremoveMessage
            if (this.callback != null)
            {
                this.callback.replyArrived(rMessage, tMessage);
            }
            else
            {
                this.file.fireRemoved();
            }
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        String errMsg = "Error removing " + this.file.getPath() + ": " + message;
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
