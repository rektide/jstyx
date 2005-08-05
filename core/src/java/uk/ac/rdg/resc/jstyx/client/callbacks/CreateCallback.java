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
import uk.ac.rdg.resc.jstyx.messages.TcreateMessage;
import uk.ac.rdg.resc.jstyx.messages.RcreateMessage;

/**
 * Callback used when creating a file (see CStyxFile.createAsync())
 *
 * @author jdb
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */

public class CreateCallback extends MessageCallback
{
    private boolean isDirectory;
    private int permissions;
    private int mode;
    private MessageCallback callback;
    private long parentFid;
    private CStyxFile file;
    private StyxConnection conn;

    public CreateCallback(CStyxFile file, boolean isDirectory, int permissions,
        int mode, MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.isDirectory = isDirectory;
        this.permissions = permissions;
        this.mode = mode;
        this.callback = callback;
        this.parentFid = -1;
    }

    public void nextStage()
    {
        if (this.file.hasFid())
        {
            // We already have a fid for this file
            this.error("File already exists", null);
        }
        else if (this.parentFid < 0)
        {
            // We need to get a fid for the parent directory
            TwalkMessage tWalkMsg = new TwalkMessage(this.conn.getRootFid(), 
                this.conn.getFreeFid(), this.file.getParentPath());
            conn.sendAsync(tWalkMsg, this);
        }
        else
        {
            // We're ready to create the file
            TcreateMessage tCreateMsg = new TcreateMessage(this.parentFid,
                this.file.getName(), this.permissions, this.isDirectory, this.mode);
            conn.sendAsync(tCreateMsg, this);
        }
    }

    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (rMessage instanceof RwalkMessage)
        {
            // Walk to the parent directory was at least partially successful
            RwalkMessage rWalkMsg = (RwalkMessage)rMessage;
            TwalkMessage tWalkMsg = (TwalkMessage)tMessage;
            if (rWalkMsg.getNumSuccessfulWalks() == tWalkMsg.getNumPathElements())
            {
                // We've got the fid of the parent directory
                this.parentFid = tWalkMsg.getNewFid();
                this.nextStage();
            }
            else
            {
                String errMsg = "'" + 
                tWalkMsg.getPathElements()[rWalkMsg.getNumSuccessfulWalks()]
                    + "' does not exist.";
                this.error(errMsg, tWalkMsg);
            }
        }
        else
        {
            // This must be an RcreateMessage
            RcreateMessage rCreateMsg = (RcreateMessage)rMessage;
            TcreateMessage tCreateMsg = (TcreateMessage)tMessage;
            this.file.setFid(tCreateMsg.getFid());
            this.file.setMode(tCreateMsg.getMode());
            if (this.callback != null)
            {
                callback.replyArrived(rMessage, tMessage);
            }
            else
            {
                this.file.fireCreated();
            }
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        if (tMessage instanceof TwalkMessage)
        {
            // return the fid to the pool
            conn.returnFid(((TwalkMessage)tMessage).getNewFid());
        }
        if (this.callback != null)
        {
            this.callback.error(message, tMessage);
        }
        else
        {
            this.file.fireError(message);
        }
    }
}
