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

package uk.ac.rdg.resc.jstyx.gridservice.client;

import javax.swing.JPanel;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeListener;

/**
 * Class representing a viewer for an output stream from a Styx Grid Service
 * instance.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/05/26 16:47:43  jonblower
 * Initial import
 *
 */
public class StreamViewer extends JPanel implements CStyxFileChangeListener
{
    private CStyxFile stream;
    private long offset;
    private boolean started;
    
    public StreamViewer(CStyxFile stream)
    {
        this.stream = stream;
        this.stream.addChangeListener(this);
        this.offset = 0;
        this.started = false;
    }
    
    /**
     * Sends a message to start reading from the stream.  If we have already
     * started reading from the stream, this does nothing
     */
    public void start()
    {
        if (!this.started)
        {
            this.started = true;
            this.stream.readAsync(this.offset);
        }
    }
    
    /**
     * Sends a message to stop reading from this stream. The stream position will
     * not be reset: use reset() for this. After calling this, we can continue
     * reading from the stream by calling start(). Note that one more chunk of 
     * data might arrive after calling this (if there was a read message outstanding
     * at the time of calling this).
     */
    public void stop()
    {
        if (this.started)
        {
            this.started = false;
        }
    }
    
    /**
     * Sets the position of the stream to the given offset (i.e. number of bytes
     * after the beginning of the file).
     */
    public void setPosition(long pos)
    {
        this.offset = pos;
    }
    
    /**
     * Gets the current position of the stream (i.e. the position of the next
     * byte that will arrive)
     */
    public long getPosition()
    {
        return this.offset;
    }
    
    /**
     * Resets the stream to the start, so we can start reading from the beginning
     * again. Exactly equivalent to <code>setPosition(0)</code>
     */
    public void reset()
    {
        this.setPosition(0);
    }
    
    /**
     * Called when new data arrive from the server
     */
    public void newDataArrived(ByteBuffer data)
    {
        System.err.println(StyxUtils.dataToString(data));
    }
    
    /**
     * Called when we reach the end of the stream.  The total length of the
     * stream data can now be read by calling <code>getPosition()</code>.  To
     * start reading from the start of the stream, call <code>reset()</code> then
     * <code>start()</code>.
     */
    public void eof()
    {
        System.err.println("end of stream reached on " + this.stream.getName());
    }
    
    /**
     * Called when an error occurs
     */
    public void streamError(String message)
    {
        System.err.println("Error with stream " + this.stream.getName() + ": "
            + message);
    }
    
    public void dataArrived(CStyxFile file, TreadMessage tReadMsg, ByteBuffer data)
    {
        int dataSize = data.remaining();
        if (dataSize > 0)
        {
            this.newDataArrived(data);
            this.offset += dataSize;
            this.stream.readAsync(this.offset);
        }
        else
        {
            this.started = false;
            this.eof();
        }
    }
    
    public void error(CStyxFile file, String message)
    {
        this.streamError(message);
    }
    
    // Empty methods required by the CStyxFileChangeListener interface
    public void fileOpen(CStyxFile file, int mode){}
    public void fileCreated(CStyxFile file, int mode){}
    public void dataWritten(CStyxFile file, TwriteMessage tWriteMsg){}
    public void statChanged(CStyxFile file, DirEntry newDirEntry){}
    public void childrenFound(CStyxFile file, CStyxFile[] children){}
    public void uploadComplete(CStyxFile targetFile) {}
    
}
