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
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;
import uk.ac.rdg.resc.jstyx.messages.RreadMessage;
import uk.ac.rdg.resc.jstyx.messages.RopenMessage;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Callback used when reading from a file (see CStyxFile.readAsync())
 *
 * @author jdb
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */

public class ReadCallback extends MessageCallback
{
    private long offset;
    private int bytesRequired;
    private MessageCallback callback;
    private CStyxFile file;
    private StyxConnection conn;

    public ReadCallback(CStyxFile file, long offset, int bytesRequired,
        MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.offset = offset;
        this.bytesRequired = bytesRequired;
        this.callback = callback;
    }

    public void nextStage()
    {
        if (this.file.isOpen())
        {
            // The file is open. Check the mode
            int rwx = this.file.getMode() & 3; // mask off last two bits to get OREAD, OWRITE, 
                                               // ORDWR or OEXEC (i.e. ignore OTRUNC/ORCLOSE)
            if (rwx == StyxUtils.OREAD || rwx == StyxUtils.ORDWR)
            {
                // Try to read the given number of bytes from the file
                TreadMessage tReadMsg = new TreadMessage(this.file.getFid(),
                    new ULong(this.offset), this.bytesRequired < 0 ?
                        this.file.getIoUnit() : this.bytesRequired);
                conn.sendAsync(tReadMsg, this);
            }
            else
            {
                this.error("File " + this.file.getPath() + " is not open for reading", null);
            }
        }
        else
        {
            // We need to open the file
            this.file.openAsync(StyxUtils.OREAD, this);
        }
    }

    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (rMessage instanceof RopenMessage)
        {
            // This is the reponse from openAsync()
            // We've just got the open fid for the file. Go to the next stage
            // (i.e. read from the file)
            this.nextStage();
        }
        else
        {
            // Must be an RreadMessage
            if (this.callback != null)
            {
                this.callback.replyArrived(rMessage, tMessage);
            }
            else
            {
                this.file.fireDataArrived((TreadMessage)tMessage, (RreadMessage)rMessage);
            }
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        String errMsg = "Error reading from " + this.file.getPath() + ": " + message;
        if (this.callback != null)
        {
            this.callback.error(message, tMessage);
        }
        else
        {
            this.file.fireError(errMsg);
        }
    }
}
