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

import java.util.Vector;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.MessageCallback;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.RstatMessage;
import uk.ac.rdg.resc.jstyx.messages.RreadMessage;
import uk.ac.rdg.resc.jstyx.messages.StyxBuffer;

import uk.ac.rdg.resc.jstyx.types.DirEntry;

/**
 * Callback that is used when reading the children of a directory.  See
 * CStyxFile.getChildrenAsync()
 *
 * @author jdb
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/08/31 17:07:59  jonblower
 * Fixed bug with clunking fids and released ByteBuffer correctly
 *
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */

public class GetChildrenCallback extends MessageCallback
{
    private Vector dirEntries;
    private long offset;
    private boolean wasOpen;
    private boolean firstTime;
    private MessageCallback callback;
    private CStyxFile file;
    private StyxConnection conn;

    public GetChildrenCallback(CStyxFile file, MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.dirEntries = new Vector();
        this.offset = 0;
        this.callback = callback;
        this.firstTime = true;
    }

    public void nextStage()
    {
        // First check that this is a directory
        if (this.file.getDirEntry() == null)
        {
            // We need to get the stat for this file
            this.file.refreshAsync(this);
        }
        else if (this.file.getQid().getType() == 128)
        {
            // this is a directory (or we don't care if this is a directory
            // or not).
            if (this.firstTime)
            {
                // First check to see if we already have it open
                this.wasOpen = this.file.isOpen();
                this.firstTime = false; 
            }
            // Now find the children
            this.file.readAsync(this.offset, this);
        }
        else
        {
            // This is not a directory. raise an error
            this.error("not a directory", null);
        }
    }

    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (rMessage instanceof RstatMessage)
        {
            // We've just got the stat of the file. Proceed to the next
            // stage (i.e. start reading data)
            this.nextStage();
        }
        else
        {
            // this must be an RreadMessage
            RreadMessage rReadMsg = (RreadMessage)rMessage;
            ByteBuffer data = rReadMsg.getData();
            this.offset += data.remaining();
            if (data.remaining() > 0)
            {
                // Wrap data as a StyxBuffer
                StyxBuffer styxBuf = new StyxBuffer(data);
                // Get all the DirEntries from this buffer
                while(data.hasRemaining())
                {
                    DirEntry dirEntry = styxBuf.getDirEntry();
                    CStyxFile newFile = this.conn.getFile(this.file.getPath()
                        + "/" + dirEntry.getFileName());
                    newFile.setDirEntry(dirEntry);
                    this.dirEntries.add(newFile);
                }
                // Read from this file again
                this.nextStage();
            }
            else
            {
                // We've read all the data from the file
                if (!this.wasOpen)
                {
                    // If this file wasn't open before we started reading
                    // the children, close it
                    this.file.close();
                }
                this.file.setChildren((CStyxFile[])this.dirEntries.toArray(new CStyxFile[0]));
                if (this.callback != null)
                {
                    this.callback.replyArrived(rMessage, tMessage);
                }
                else
                {
                    this.file.fireChildrenFound();
                }
            }
            // We need to release the buffer
            data.release();
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        String errMsg = "Error getting directory contents from " + this.file.getPath()
            + ": " + message;
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
