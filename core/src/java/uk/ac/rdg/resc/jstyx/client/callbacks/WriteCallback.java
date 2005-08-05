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
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.RwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.RopenMessage;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Callback used when writing to a file (see CStyxFile.writeAsync())
 *
 * @author jdb
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */

public class WriteCallback extends MessageCallback
{
    private byte[] bytes;
    private int pos;
    private int count;
    private long offset;
    private boolean truncate;
    private MessageCallback callback;
    private CStyxFile file;
    private StyxConnection conn;

    public WriteCallback(CStyxFile file, byte[] bytes, int pos, int count,
        long offset, boolean truncate, MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.bytes = bytes;
        this.pos = pos;
        this.count = count;
        this.offset = offset;
        this.truncate = truncate;
        this.callback = callback;
    }

    public void nextStage()
    {
        if (this.file.isOpen())
        {
            // The file is already open.  Check the mode
            int rwx = this.file.getMode() & 3; // mask off last two bits to get OREAD, OWRITE, 
                                               // ORDWR or OEXEC (i.e. ignore OTRUNC/ORCLOSE)
            if (rwx != StyxUtils.OWRITE && rwx != StyxUtils.ORDWR)
            {
                this.error("File " + this.file.getPath() + " is not open for writing", null);
            }
            else
            {
                // Check the truncation flag
                boolean truncFlagPresent = ((this.file.getMode() & StyxUtils.OTRUNC) == StyxUtils.OTRUNC);
                if (truncFlagPresent == this.truncate)
                {
                    // Truncation flag is set correctly
                    TwriteMessage tWriteMsg = new TwriteMessage(this.file.getFid(),
                        new ULong(this.offset), this.bytes, this.pos, this.count);
                    conn.sendAsync(tWriteMsg, this);
                }
                else if (truncFlagPresent)
                {
                    // Truncation flag is present when it shouldn't be
                    this.error("File " + this.file.getPath() + " is open with truncation", null);
                }
                else
                {
                    // Truncation flag should be present but isn't
                    this.error("File " + this.file.getPath() + " is not open with truncation", null);
                }
            }
        }
        else
        {
            // We must open the file
            int theMode = StyxUtils.OWRITE;
            if (this.truncate)
            {
                theMode |= StyxUtils.OTRUNC;
            }
            this.file.openAsync(theMode, this);
        }
    }

    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (rMessage instanceof RopenMessage)
        {
            // This is the reply from openAsync, so the file is now open.
            // Proceed to the next stage (i.e. write to the file)
            this.nextStage();
        }
        else
        {
            // This must be an Rwrite message, i.e. we have just successfully
            // written to the file.
            // Check the number of bytes written
            RwriteMessage rWriteMsg = (RwriteMessage)rMessage;
            TwriteMessage tWriteMsg = (TwriteMessage)tMessage;
            if (tWriteMsg.getCount() != rWriteMsg.getNumBytesWritten())
            {
                // Should not happen
                this.error("all bytes not written", tMessage);
            }
            else if (this.callback != null)
            {
                this.callback.replyArrived(rMessage, tMessage);
            }
            else
            {
                this.file.fireDataWritten((TwriteMessage)tMessage);
            }
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        String errMsg = "Error writing to " + this.file.getPath() + ": " + message;
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
