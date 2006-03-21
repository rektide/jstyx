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
import uk.ac.rdg.resc.jstyx.messages.TopenMessage;
import uk.ac.rdg.resc.jstyx.messages.RopenMessage;
import uk.ac.rdg.resc.jstyx.messages.RwalkMessage;

/**
 * Callback used when opening a file (see CStyxFile.openAsync())
 *
 * @author jdb
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2006/03/21 14:58:42  jonblower
 * Implemented clear-text password-based authentication and did some simple tests
 *
 * Revision 1.2  2005/08/08 09:36:19  jonblower
 * Minor changes
 *
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */

public class OpenCallback extends MessageCallback
{
    private CStyxFile file;
    private StyxConnection conn;
    private int theMode;
    private MessageCallback callback;

    public OpenCallback(CStyxFile file, int mode, MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.theMode = mode;
        this.callback = callback;
    }

    public void nextStage()
    {
        if (!this.file.hasFid())
        {
            this.file.walkFidAsync(this);
        }
        else if (!this.file.isOpen())
        {
            // We have a fid that we can open. Open the file
            TopenMessage tOpenMsg = new TopenMessage(this.file.getFid(), this.theMode);
            this.conn.sendAsync(tOpenMsg, this, this.file.isAuth());
        }
        else
        {
            // The file is already open. Check that the mode is correct
            if (this.file.getMode() == this.theMode)
            {
                if (this.callback != null)
                {
                    // Notify callback with dummy Ropen message
                    // TODO will this null cause problems further down the line?
                    this.callback.replyArrived(new RopenMessage(null, -1), null);                        
                }
                else
                {
                    this.file.fireOpen();
                }
            }
            else
            {
                this.error("File already open under a different mode", null);
            }
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
            // Must be an RopenMessage
            RopenMessage rOpenMsg = (RopenMessage)rMessage;
            this.file.setMode(this.theMode);
            this.file.setIoUnit((int)rOpenMsg.getIoUnit());
            if (this.callback != null)
            {
                this.callback.replyArrived(rMessage, tMessage);
            }
            else
            {
                this.file.fireOpen();
            }
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        String errMsg = "Error opening " + this.file.getPath() + ": " + message;
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
