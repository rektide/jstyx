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
import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.MessageCallback;

import uk.ac.rdg.resc.jstyx.messages.StyxMessage;
import uk.ac.rdg.resc.jstyx.messages.RopenMessage;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;
import uk.ac.rdg.resc.jstyx.messages.RreadMessage;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Callback that is used when downloading a file from the server. Contains 
 * state that needs to persist between message exchanges with the server. See
 * CStyxFile.downloadAsync()
 *
 * @author jdb
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/11/03 07:39:45  jonblower
 * Bug fixes
 *
 * Revision 1.1  2005/08/05 13:46:40  jonblower
 * Factored out all callback objects from CStyxFile into separate classes
 *
 */

public class DownloadCallback extends MessageCallback
{
    private MessageCallback callback;
    private File localFile;
    private int numRequests;
    private FileChannel fout;
    private long offset;
    private boolean eof;
    private int numOutstandingMessages;
    private long bytesDownloaded;
    private CStyxFile file;
    private StyxConnection conn;

    public DownloadCallback(CStyxFile file, File localFile, int numRequests,
        MessageCallback callback)
    {
        this.file = file;
        this.conn = this.file.getConnection();
        this.callback = callback;
        this.localFile = localFile;
        this.numRequests = numRequests;
        this.fout = null;
        this.offset = 0;
        this.eof = false;
        this.numOutstandingMessages = 0;
        this.bytesDownloaded = 0;
    }

    public void nextStage()
    {
        if (this.file.isOpen())
        {
            try
            {
                // The file is open. Open the output stream for the local file
                if (this.fout == null)
                {
                    if (this.localFile != null)
                    {
                        this.fout = new RandomAccessFile(this.localFile, "rw").getChannel();
                        // This truncation is not necessary if we have just
                        // created the file
                        this.fout.truncate(0);
                    }
                }
                // Now send a bunch of Tread messages to start the ball rolling
                for (int i = 0; i < this.numRequests; i++)
                {
                    this.readNextChunk();
                }
            }
            catch (FileNotFoundException fnfe)
            {
                this.error("cannot open " + this.localFile + " for writing", null);
            }
            catch (IOException ioe)
            {
                this.error("cannot truncate " + this.localFile + ": " +
                    ioe.getMessage(), null);
            }
        }
        else
        {
            // We must open the file
            this.file.openAsync(StyxUtils.OREAD, this);
        }
    }

    private synchronized void readNextChunk()
    {
        if (!this.eof)
        {
            // Read the maximum number of bytes allowed in a single message
            // (i.e. ioUnit)
            this.file.readAsync(this.offset, this);
            this.offset += this.file.getIoUnit();
            this.numOutstandingMessages++;
        }
    }

    public void replyArrived(StyxMessage rMessage, StyxMessage tMessage)
    {
        if (rMessage instanceof RopenMessage)
        {
            // We have just opened the file.
            this.nextStage();
        }
        else if (rMessage instanceof RreadMessage)
        {
            this.numOutstandingMessages--;
            RreadMessage rReadMsg = (RreadMessage)rMessage;
            TreadMessage tReadMsg = (TreadMessage)tMessage;
            if (rReadMsg.getCount() != 0)
            {
                this.bytesDownloaded += rReadMsg.getCount();
                try
                {
                    if (rReadMsg.getCount() == tReadMsg.getCount())
                    {
                        // We have got all the bytes we asked for so read the
                        // next chunk
                        this.readNextChunk();
                    }
                    else
                    {
                        // We didn't get all the bytes we asked for so we'll
                        // have to make another request for the remaining
                        // bytes.  Hopefully this won't happen very often
                        long pos = tReadMsg.getOffset().asLong() + rReadMsg.getCount();
                        int bytesRequired = tReadMsg.getCount() - rReadMsg.getCount();
                        synchronized (this)
                        {
                            this.file.readAsync(pos, bytesRequired, this);
                            this.numOutstandingMessages++;
                        }
                    }
                    if (this.fout != null)
                    {
                        // Write the data to the output file at the right file position
                        fout.write(rReadMsg.getData().buf(), tReadMsg.getOffset().asLong());
                    }
                }
                catch(IOException ioe)
                {
                    this.error("error writing to " + this.file + ": " +
                        ioe.getMessage(), null);
                }
            }
            else
            {
                //System.err.println("Got Rread message with zero bytes");
                // We have reached EOF
                this.eof = true;
            }
            if (this.eof && this.numOutstandingMessages == 0)
            {
                //System.err.println("Bytes downloaded: " + this.bytesDownloaded);
                // There are no more outstanding messages
                this.closeFile();
                if (this.callback == null)
                {
                    this.file.fireDownloadComplete();
                }
                else
                {
                    callback.replyArrived(rMessage, tMessage);
                }
            }
        }
    }

    private void closeFile()
    {
        if (this.fout != null)
        {
            try
            {
                this.fout.close();
            }
            catch(IOException ioe)
            {
                CStyxFile.getLogger().debug("IOException when closing " +
                    this.file.getPath() + ": " + ioe.getMessage());
            }
        }
    }

    public void error(String message, StyxMessage tMessage)
    {
        // TODO: must stop error() being called multiple times
        this.closeFile();
        String errMsg = "Error downloading from " + this.file.getPath() + ": " + message;
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
