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
import uk.ac.rdg.resc.jstyx.messages.TstatMessage;
import uk.ac.rdg.resc.jstyx.messages.RstatMessage;
import uk.ac.rdg.resc.jstyx.messages.RwalkMessage;

/**
 * Description of RefreshCallback goes here.
 *
 * @author jdb
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */

public class RefreshCallback extends MessageCallback
{
    private MessageCallback callback;
    private CStyxFile file;
    private StyxConnection conn;

    public RefreshCallback(CStyxFile file, MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.callback = callback;
    }

    public void nextStage()
    {
        if (this.file.hasFid())
        {
            // We have a fid, so get the stat of the file
            TstatMessage tStatMsg = new TstatMessage(this.file.getFid());
            this.conn.sendAsync(tStatMsg, this);
        }
        else
        {
            // We need to get a fid for this file
            this.file.walkFidAsync(this);
        }
    }

    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (rMessage instanceof RwalkMessage)
        {
            // This is the reply from getFidAsync().  Now proceed to the
            // next stage (get the stat of the file)
            this.nextStage();
        }
        else
        {
            // This must be an RstatMessage
            RstatMessage rStatMsg = (RstatMessage)rMessage;
            // Setting the DirEntry will also set the Qid of the file
            this.file.setDirEntry(rStatMsg.getDirEntry());
            if (this.callback != null)
            {
                this.callback.replyArrived(rMessage, tMessage);
            }
            else
            {
                this.file.fireStatChanged(rStatMsg);
            }
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        String errMsg = "Error getting stat of " + this.file.getPath() + ": " + message;
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
