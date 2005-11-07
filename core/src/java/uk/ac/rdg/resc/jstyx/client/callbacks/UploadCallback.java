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

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.MessageCallback;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.RwriteMessage;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Callback that is used when uploading a file to the server. Contains 
 * state that needs to persist between message exchanges with the server.  See
 * CStyxFile.uploadAsync()
 *
 * @author jdb
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/11/07 22:01:35  jonblower
 * Added code to close stream when UploadCallback has been created from a File object
 *
 * Revision 1.3  2005/11/04 19:25:14  jonblower
 * Added code to write a zero-byte message to represent EOF
 *
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */

public class UploadCallback extends MessageCallback
{
    private InputStream in;
    private byte[] bytes;
    private long offset;
    private MessageCallback callback;
    private CStyxFile file;
    private StyxConnection conn;
    private boolean closeStreamWhenComplete;

    public UploadCallback(CStyxFile file, File localFile, MessageCallback callback)
    {
        this.init(file, null, callback);
        this.closeStreamWhenComplete = true;
        try
        {
            this.in = new FileInputStream(localFile);
        }
        catch(FileNotFoundException fnfe)
        {
            this.error("file does not exist", null);
        }
    }

    public UploadCallback(CStyxFile file, InputStream in, MessageCallback callback)
    {
        this.init(file, in, callback);
    }

    private void init(CStyxFile file, InputStream in, MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.in = in;
        this.bytes = null;
        this.offset = 0;
        this.callback = callback;
        this.closeStreamWhenComplete = false;
    }

    public void nextStage(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (this.file.isOpen())
        {
            // Now we can write to the file
            try
            {
                if (this.bytes == null)
                {
                    this.bytes = new byte[this.file.getIoUnit()];
                }
                // Read from the source file
                int n = in.read(this.bytes);
                if (n > 0)
                {
                    // Write to the server
                    this.file.writeAsync(this.bytes, 0, n, this.offset, true, this);
                }
                else
                {
                    // We've reached EOF. Write zero bytes
                    // to the server to signify EOF
                    this.file.writeAsync(new byte[0], this.offset, true, this);
                }
            }
            catch(IOException ioe)
            {
                this.error("IOException occurred: " + ioe.getMessage(), null);
            }
        }
        else
        {
            // If we haven't opened this file, open or create it
            // The "false" means create a file, not a directory
            this.file.openOrCreateAsync(false, StyxUtils.OWRITE | StyxUtils.OTRUNC, this);
        }
    }

    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (rMessage instanceof RwriteMessage)
        {
            TwriteMessage tWriteMsg = (TwriteMessage)tMessage;
            RwriteMessage rWriteMsg = (RwriteMessage)rMessage;
            if (tWriteMsg.getCount() == rWriteMsg.getNumBytesWritten())
            {
                if (rWriteMsg.getNumBytesWritten() > 0)
                {
                    this.offset += rWriteMsg.getNumBytesWritten();
                    this.nextStage(rMessage, tMessage);
                }
                else
                {
                    // We've reached EOF. Close the file and notify that
                    // upload is complete.
                    this.file.close();
                    if (this.closeStreamWhenComplete)
                    {
                        try
                        {
                            this.in.close();
                        }
                        catch(IOException ioe)
                        {
                            // Ignore this exception
                        }
                    }
                    if (this.callback == null)
                    {
                        this.file.fireUploadComplete();
                    }
                    else
                    {
                        this.callback.replyArrived(rMessage, tMessage);
                    }
                }
            }
            else
            {
                this.error("Error writing data: tried to write " + tWriteMsg.getCount()
                    + " bytes, actually wrote " + rWriteMsg.getNumBytesWritten()
                    + " bytes.", tMessage);
            }
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        if (this.in != null)
        {
            try
            {
                this.in.close();
            }
            catch(IOException ioe)
            {
                CStyxFile.getLogger().debug("IOException when closing input stream: "
                    + ioe.getMessage());
            }
        }
        String errMsg = "Error uploading: " + message;
        if (this.callback == null)
        {
            this.file.fireError(errMsg);
        }
        else
        {
            this.callback.error(errMsg, tMessage);
        }
    }
}
