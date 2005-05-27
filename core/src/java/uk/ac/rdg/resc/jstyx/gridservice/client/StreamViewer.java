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

import javax.swing.JFrame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeListener;

/**
 * Class representing a viewer for an output stream from a Styx Grid Service
 * instance.  Subclasses only need to implement the <code>newDataArrived()</code>
 * method, which is called when new data are read from the stream.  Subclasses
 * <i>may</i> override <code>eof</code> (which is called when end-of-stream is
 * reached) and <code>streamError</code> (which is called if an error occurs).
 * See <code>TextStreamViewer</code> for an example of a very simple StreamViewer.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/05/27 07:44:07  jonblower
 * Continuing to implement Stream viewers
 *
 * Revision 1.2  2005/05/26 21:33:40  jonblower
 * Added method for viewing streams in a window
 *
 * Revision 1.1  2005/05/26 16:47:43  jonblower
 * Initial import
 *
 */
public abstract class StreamViewer extends JFrame implements CStyxFileChangeListener
{
    protected CStyxFile stream;
    protected long offset;
    protected boolean started;
    
    public StreamViewer()
    {
        this.offset = 0;
        this.started = false;
        this.addWindowListener(new WindowAdapter()
        {
            // Stop reading from the stream if the window is closed
            public void windowClosing(WindowEvent we)
            {
                stop();
            }
        });
    }
    
    /**
     * Sets the CStyxFile that represents the stream
     */
    public void setStream(CStyxFile stream)
    {
        this.stream = stream;
        this.stream.addChangeListener(this);
        this.setTitle(stream.getName());
    }
    
    /**
     * Sends a message to start reading from the stream and makes the GUI visible.
     * If we have already started reading from the stream, this does nothing.
     */
    public void start()
    {
        this.setVisible(true);
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
    public abstract void newDataArrived(ByteBuffer data);
    
    /**
     * Called when we reach the end of the stream.  The total length of the
     * stream data can now be read by calling <code>getPosition()</code>.  To
     * start reading from the start of the stream, call <code>reset()</code> then
     * <code>start()</code>.  This default implementation does nothing: subclasses
     * should override this if they want to do something when end-of-stream is
     * reached.
     */
    public void eof()
    {
    }
    
    /**
     * Called when an error occurs. This default implementation does nothign:
     * Subclasses should override this to handle errors.
     */
    public void streamError(String message)
    {
    }
    
    public void dataArrived(CStyxFile file, TreadMessage tReadMsg, ByteBuffer data)
    {
        int dataSize = data.remaining();
        if (dataSize > 0)
        {
            this.newDataArrived(data);
            this.offset += dataSize;
            if (this.started)
            {
                this.stream.readAsync(this.offset);
            }
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
