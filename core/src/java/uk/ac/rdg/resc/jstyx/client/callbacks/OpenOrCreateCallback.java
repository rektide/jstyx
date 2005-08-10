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
import uk.ac.rdg.resc.jstyx.messages.RwalkMessage;
import uk.ac.rdg.resc.jstyx.messages.RstatMessage;
import uk.ac.rdg.resc.jstyx.messages.RopenMessage;
import uk.ac.rdg.resc.jstyx.messages.RcreateMessage;

/**
 * Callback used by CStyxFile.openOrCreateAsync()
 *
 * @author jdb
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/08/10 18:33:48  jonblower
 * Bug fixes
 *
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */

public class OpenOrCreateCallback extends MessageCallback
{
    private boolean isDirectory;
    private int mode;
    private MessageCallback callback;
    private CStyxFile file;
    private StyxConnection conn;

    public OpenOrCreateCallback(CStyxFile file, boolean isDirectory, int mode,
        MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.isDirectory = isDirectory;
        this.mode = mode;
        this.callback = callback;
    }

    public void nextStage()
    {
        // We must first find out if this is a file or directory (will also
        // test to see if it exists)
        if (this.file.hasFid())
        {
            // See if we have a Qid, so that we can work out whether this is 
            // a file or directory
            if (this.file.getQid() == null)
            {
                this.file.refreshAsync(this);
            }
            else
            {
                // Check to see if it is the right type of file (i.e. file or
                // directory)
                if (this.isDirectory == (this.file.getQid().getType() == 128))
                {
                    // Now we can open the file
                    this.file.openAsync(this.mode, this);
                }
                else
                {
                    String errMsg = this.file.getPath() + " already exists as a " +
                        ((this.file.getQid().getType() == 128) ? "directory" : "file");
                    this.error(errMsg, null);
                }
            }
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
            // We've just successfully walked to the file so we have a fid
            // and it must exist.  Move to the next stage (checking for existence
            // of a qid)
            this.nextStage();
        }
        else if (rMessage instanceof RstatMessage)
        {
            // We've just successfully got the stat of the file, so we have
            // the qid. Move to the next stage (i.e. checking
            // the file type)
            this.nextStage();
        }
        else if (rMessage instanceof RopenMessage)
        {
            // We've just opened the file.
            if (this.callback != null)
            {
                this.callback.replyArrived(rMessage, tMessage);
            }
            else
            {
                this.file.fireOpen();
            }
        }
        else if (rMessage instanceof RcreateMessage)
        {
            // Must be an RcreateMessage
            if (this.callback != null)
            {
                this.callback.replyArrived(rMessage, tMessage);
            }
            else
            {
                this.file.fireCreated();
            }
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        if (!this.file.hasFid())
        {
            // The file does not exist
            int perm = this.isDirectory ? 0777 : 0666;
            this.file.createAsync(this.isDirectory, perm, this.mode, this);
        }
        else if (this.callback != null)
        {
            this.callback.error(message, tMessage);
        }
        else
        {
            this.file.fireError(message);
        }
    }
}
