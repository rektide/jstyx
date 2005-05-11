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

package uk.ac.rdg.resc.jstyx.server;

import org.apache.log4j.Logger;

import java.io.File;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.types.ULong;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;

/**
 * A file on disk that is continuously monitored.  This is basically an
 * AsyncStyxFile that wraps a FileOnDisk, with a separate thread that monitors
 * the underlying file (checks for changes to the last modified time and length
 * of the file).  Note that the underlying file does not have to exist at the 
 * time the MonitoredFileOnDisk object is constructed or at the time the monitor
 * is started.
 *
 * @todo Start monitoring when the first client opens this file?  Stop monitoring
 * when the last client closes the file?
 *
 * @todo Link refresh() with notifications to clients?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/05/11 10:33:50  jonblower
 * Implemented MonitoredFileOnDisk.java
 *
 *
 * Revision 1.1  2005/05/09 13:22:43  jonblower
 * Initial import
 *
 */
public class MonitoredFileOnDisk extends AsyncStyxFile implements Runnable
{
    
    private static final Logger log = Logger.getLogger(MonitoredFileOnDisk.class);
    
    private File file;            // the file to monitor
    private long monitorInterval; // The time between polls to the filesystem,
                                  // in milliseconds
    private boolean fileExists;   // true if the file exists
    private long length;          // length of the file in bytes
    
    private boolean monitoring;   // Set this false to stop the monitoring
    private Thread monitor;       // The thread that does the monitoring
    
    /**
     * Creates a new instance of MonitoredFileOnDisk. Call startMonitoring() to
     * start monitoring the underlying file.
     * @param filename The name of the underlying file to monitor. Note that this
     * file does not need to exist yet
     * @param monitorInterval The time in milliseconds between each check to see
     * if the file has changed
     * @throws StyxException if the file name is illegal
     */
    public MonitoredFileOnDisk(String filename, long monitorInterval) throws StyxException
    {
        this(new File(filename), monitorInterval);
    }
    
    /**
     * Creates a new instance of MonitoredFileOnDisk that monitors the underlying
     * file every 2 seconds. Call startMonitoring() to start monitoring the
     * underlying file.
     * @param filename The name of the underlying file to monitor. Note that this
     * file does not need to exist yet
     * @throws StyxException if the file name is illegal
     */
    public MonitoredFileOnDisk(String filename) throws StyxException
    {
        this(new File(filename));
    }
    
    /**
     * Creates a new instance of MonitoredFileOnDisk. Call startMonitoring() to
     * start monitoring the underlying file.
     * @param file The underlying file to monitor. Note that this file does not
     * need to exist yet
     * @param monitorInterval The time in milliseconds between each check to see
     * if the file has changed
     * @throws StyxException if the file name is illegal
     */
    public MonitoredFileOnDisk(File file, long monitorInterval) throws StyxException
    {
        super(new FileOnDisk(file, false)); // The "false" means that the underlying
            // java.io.File does not have to exist: if it does not exist, it will
            // appear as an empty read-only file
        this.file = file;
        this.monitorInterval = monitorInterval;
        this.fileExists = file.exists();
        this.lastModifiedTime = file.lastModified() / 1000;
        this.length = file.length();
        // Prepare the thread but don't start monitoring the file yet
        this.monitor = new Thread(this);
    }
    
    /**
     * Creates a new instance of MonitoredFileOnDisk that monitors the underlying
     * file every 2 seconds. Call startMonitoring() to start monitoring the
     * underlying file.
     * @param file The underlying file to monitor. Note that this file does not
     * need to exist yet
     * @throws StyxException if the file name is illegal
     */
    public MonitoredFileOnDisk(File file) throws StyxException
    {
        this(file, 2000);
    }
    
    public void run()
    {
        try
        {
            while(this.monitoring)
            {
                
                long start = System.currentTimeMillis();
                if (this.file.exists())
                {
                    if (this.fileExists)
                    {
                        // We already knew that this file exists. Let's look at
                        // the last modified time.
                        long newLastMod = this.file.lastModified() / 1000;
                        if (newLastMod != this.lastModifiedTime)
                        {
                            this.lastModifiedTime = newLastMod;
                            // notify that the contents have changed.
                            this.contentsChanged();
                        }
                        // Last mod time hasn't changed.  Check the length of the file
                        // TODO: is this necessary?  Will this ever change if lastModified
                        // hasn't changed?  Just a double-check really.
                        else
                        {
                            long newLength = this.file.length();
                            if (newLength != this.length)
                            {
                                this.length = newLength;
                                this.contentsChanged();
                            }
                        }
                    }
                    else
                    {
                        // This is the first time that we've noticed that the file exists
                        this.fileExists = true;
                        this.length = this.file.length();
                        this.lastModifiedTime = this.file.lastModified() / 1000;
                        this.contentsChanged();
                    }
                }
                else
                {
                    // The file doesn't exist
                    if (this.fileExists)
                    {
                        // The file previously existed, now it has vanished.
                        this.length = 0L;
                        this.lastModifiedTime = 0L;
                        this.contentsChanged(); // This will write EOF to all waiting clients
                    }
                }
                
                // Work out how long we have to wait before the next poll, given
                // that the preceding code may have taken a little time
                long timeTaken = System.currentTimeMillis() - start;
                long timeToWait = this.monitorInterval - timeTaken;
                if (timeToWait > 0)
                {
                    Thread.sleep(timeToWait);
                }
                
            } // end while loop
        }
        catch (InterruptedException ie)
        {
            if (log.isDebugEnabled())
            {
                log.debug("MonitoredFileOnDisk " + this.name +
                    " interrupted while sleeping");
            }
        }
    }
    
    public ULong getLength()
    {
        return new ULong(this.length);
    }
    
    /**
     * Starts monitoring the file
     */
    public void startMonitoring()
    {
        log.debug("Starting to monitor " + this.file.getPath());
        this.monitoring = true;
        this.monitor.start();
    }
    
    /**
     * Stops monitoring the file
     */
    public void stopMonitoring()
    {
        log.debug("Stopping monitoring of " + this.file.getPath());
        this.monitoring = false;
        this.monitor.interrupt();
    }
    
    /**
     * Simple test program
     */
    public static void main (String[] args) throws Exception
    {
        MonitoredFileOnDisk monFile = new MonitoredFileOnDisk("C:\\monitorme.txt");
        monFile.startMonitoring();
        StyxDirectory root = new StyxDirectory("/").addChild(monFile);
        new StyxServer(9996, root).start();
    }
    
}
