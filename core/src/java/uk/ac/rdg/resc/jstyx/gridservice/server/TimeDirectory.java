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

package uk.ac.rdg.resc.jstyx.gridservice.server;

import java.util.Date;
import java.text.ParseException;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Directory containing files that are pertinent to the lifecycle of the Styx
 * Grid Service instance.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2006/01/04 16:45:29  jonblower
 * Implemented automatic termination of SGS instances using Quartz scheduler
 *
 * Revision 1.1  2006/01/04 11:24:58  jonblower
 * Implemented time directory in the SGS instance namespace
 *
 */

public class TimeDirectory extends StyxDirectory
{
    
    private StyxGridServiceInstance instance;  // The instance to which this directory belongs
    
    /** Creates a new instance of TimeDirectory */
    public TimeDirectory(StyxGridServiceInstance instance) throws StyxException
    {
        super("time", 0555);
        this.instance = instance;
        this.addChild(new CurrentTimeFile());
        this.addChild(new CreationTimeFile());
        this.addChild(new TerminationTimeFile());
    }
    
    /**
     * File that can be read to give the current time according to the SGS server
     */
    private class CurrentTimeFile extends StyxFile
    {
        public CurrentTimeFile() throws StyxException
        {
            super("currentTime", 0444); // Read-only file
        }
        
        public void read(StyxFileClient client, long offset, int count, int tag)
            throws StyxException
        {
            String formattedDate = StyxUtils.formatAsXsdDateTime(new Date());
            this.processAndReplyRead(formattedDate, client, offset, count, tag);
        }
    }
    
    /**
     * File that can be read to give the time at which the SGS instance was created
     */
    private class CreationTimeFile extends StyxFile
    {
        public CreationTimeFile() throws StyxException
        {
            super("creationTime", 0444); // Read-only file
        }
        
        public void read(StyxFileClient client, long offset, int count, int tag)
            throws StyxException
        {
            String formattedDate = StyxUtils.formatAsXsdDateTime(instance.getCreationTime());
            this.processAndReplyRead(formattedDate, client, offset, count, tag);
        }
    }
    
    /**
     * File that can be read to give the time at which the SGS instance will be
     * terminated
     */
    private class TerminationTimeFile extends StyxFile
    {
        public TerminationTimeFile() throws StyxException
        {
            super("terminationTime", 0666); // File can be read and written
        }
        
        public void read(StyxFileClient client, long offset, int count, int tag)
            throws StyxException
        {
            Date termTime = instance.getTerminationTime();
            String formattedDate;
            if (termTime == null)
            {
                formattedDate = "";
            }
            else
            {
                formattedDate = StyxUtils.formatAsXsdDateTime(termTime);
            }
            this.processAndReplyRead(formattedDate, client, offset, count, tag);
        }
        
        /**
         * Sets the termination time.  The incoming data must contain a complete
         * time string formatted according to the xsd:dateTime format, or the
         * empty string.
         */
        public void write(StyxFileClient client, long offset, int count,
            ByteBuffer data, boolean truncate, int tag)
            throws StyxException
        {
            if (count == 0)
            {
                // This is an EOF message.  TODO we should check that it
                // is being written at the correct offset
                this.replyWrite(client, 0, tag);
            }
            else
            {
                if (offset != 0)
                {
                    throw new StyxException("Must write to the terminationTime file at offset zero");
                }
                String dateString = StyxUtils.dataToString(data);
                try
                {
                    Date termTime = null;
                    if (!dateString.trim().equals(""))
                    {
                        termTime = StyxUtils.parseXsdDateTime(dateString);
                    }
                    instance.setTerminationTime(termTime);
                    this.replyWrite(client, count, tag);
                }
                catch (ParseException pe)
                {
                    throw new StyxException("The given time (" + dateString +
                        ") is not a valid time in the xsd:dateTime format");
                }
            }
        }
    }
    
}
