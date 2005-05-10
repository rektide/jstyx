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
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/05/10 08:02:18  jonblower
 * Changes related to implementing MonitoredFileOnDisk
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
    private long lastModified;    // last modified time
    private long length;          // length of file in bytes
    
    private boolean monitoring;   // Set this false to stop the monitoring
    private Thread monitor;       // The thread that does the monitoring
    
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
        super(new FileOnDisk(file));
        this.file = file;
        this.monitorInterval = monitorInterval;
        this.fileExists = file.exists();
        if (fileExists)
        {
            this.lastModified = file.lastModified();
            this.length = file.length();
        }
        // Prepare the thread but don't start monitoring the file yet
        this.monitor = new Thread(this);
    }
    
    /**
     * Creates a new instance of MonitoredFileOnDisk that monitors the underlying
     * file every 2 seconds.
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
                        // The existence of the file hasn't changed. Let's look at
                        // the last modified time
                        long newLastMod = this.file.lastModified();
                        if (newLastMod != this.lastModified)
                        {
                            this.lastModified = newLastMod;
                            // notify that the contents have changed.
                            this.contentsChanged();
                        }
                        // Last mod time hasn't changed.  Check the length of the file
                        // TODO: is this necessary?  Will this ever change if lastModified
                        // hasn't changed?
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
                        this.contentsChanged();
                    }
                }
                else
                {
                    // The file doesn't exist
                    if (this.fileExists)
                    {
                        // The file previously existed, now it has vanished.
                        // TODO: write EOF to all clients?
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
     * This is called when an error occurs when reading the underlying file
     * (see the corresponding method in AsyncStyxFile). This method simply logs
     * the error and takes no further action.
     */
    protected void handleReadError(String message, StyxFileClient client, int tag)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Error reading from " + this.file.getPath() + ": " + message);
        }
    }
    
    /**
     * Modified version of FileOnDisk that does not require the underlying file
     * to actually exist. If the file does not exist, any attempts to read from
     * the file result in EOF.  Attempts to write to a non-existent file will
     * result in a StyxException being thrown.
     */
    private class ModFileOnDisk extends FileOnDisk
    {
        
    }
    
}
