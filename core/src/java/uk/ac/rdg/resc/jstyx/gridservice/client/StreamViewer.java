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

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.CStyxFile;

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
 * Revision 1.7  2005/06/13 13:55:42  jonblower
 * Adapted LB viewer for JStyx framework
 *
 * Revision 1.6  2005/06/10 07:54:49  jonblower
 * Added code to convert event-based StreamViewer to InputStream-based one
 *
 * Revision 1.5  2005/05/27 21:22:39  jonblower
 * Further development of caching stream readers
 *
 * Revision 1.4  2005/05/27 17:05:07  jonblower
 * Changes to incorporate GeneralCachingStreamReader
 *
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
public abstract class StreamViewer extends JFrame
{
    protected CachedStreamReader reader;
    protected long offset;
    protected boolean started;
    
    protected InputStream is;
    
    public StreamViewer()
    {
        this.offset = 0;
        this.started = false;
        this.is = null;
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
    public void setStreamReader(CachedStreamReader reader)
    {
        this.reader = reader;
        this.setTitle(reader.getName());
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
            this.reader.read(this, this.offset, 8192);
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
    public abstract void newDataArrived(byte[] data, int size);
    
    /**
     * Called by CachedStreamReader when a chunk of data is read from stream
     */
    public final void newData(byte[] data, int size)
    {
        if (size > 0)
        {
            this.offset += size;
            this.newDataArrived(data, size);
            if (this.is != null)
            {
                synchronized (this.is)
                {
                    this.is.notifyAll();
                }
            }
            this.reader.read(this, this.offset, 8192);
        }
        else
        {
            System.err.println("Reached EOF");
        }
    }
    
    public void readError(Exception e)
    {
        e.printStackTrace();
    }
    
    /**
     * @return An InputStream that can be used to read from this stream.  This
     * InputStream <b>must</b> be consumed in a separate thread to avoid blocking.
     */
    public InputStream getInputStream()
    {
        if (this.is == null)
        {
            this.is = new StyxStream();
        }
        return this.is;
    }
    
    /**
     * An InputStream for reading from the Styx stream. This InputStream <b>must</b>
     * be consumed in a separate thread to avoid blocking
     */
    private class StyxStream extends InputStream
    {
        private long pos = 0;
        private FileInputStream in;
        
        public StyxStream()
        {
            try
            {
                this.in = new FileInputStream(reader.getCacheFile());
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        
        public synchronized int read() throws IOException
        {
            try
            {
                int b;
                do
                {
                    // Try reading from the stream
                    b = this.in.read();
                    if (b < 0)
                    {
                        // Check for EOF in the cache
                        if (reader.isEOF())
                        {
                            System.err.println("EOF in cache file");
                            return -1;
                        }
                        // We're not ready to read from this position yet.
                        // Wait until we are notified that more data have arrived
                        System.err.println("Data not available yet");
                        this.wait();
                        System.err.println("Data available");
                    }
                } while (b < 0);
                return b;
            }
            catch(InterruptedException ie)
            {
                ie.printStackTrace();
                return -1;
            }
        }
        
        public synchronized void close() throws IOException
        {
            if (this.in != null)
            {
                this.in.close();
            }
        }
    }
    
}
