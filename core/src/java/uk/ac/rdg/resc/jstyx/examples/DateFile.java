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

package uk.ac.rdg.resc.jstyx.examples;

import java.util.Date;
import java.nio.ByteBuffer;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.types.ULong;
import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.StyxServer;
import uk.ac.rdg.resc.jstyx.server.StyxFileClient;

/**
 * Part of JStyx tutorial: implements a file that, when read, returns the current
 * date and time.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/01 19:46:53  jonblower
 * Initial import of DateFile.java
 *
 */
public class DateFile extends StyxFile
{
    
    public DateFile() throws StyxException
    {
        super("date");
    }
    
    public void write(StyxFileClient client, long offset, long count,
        ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException
    {
        throw new StyxException("Cannot write to this file");
    }
    
    public void read(StyxFileClient client, long offset, long count, int tag)
        throws StyxException
    {
        // Get the current date and time in String format
        String dateStr = new Date().toString();
        // Convert the String to bytes in UTF-8 format
        byte[] dateBytes = StyxUtils.strToUTF8(dateStr);
        
        ByteBuffer buf;
        if ((int)offset >= dateBytes.length)
        {
            // If the client has requested bytes from an offset greater than the
            // length of the string, return zero bytes (signifies end-of-file)
            buf = ByteBuffer.allocate(0);
        }
        else
        {
            // Work out the number of bytes to return
            int bytesToReturn;
            if (count > dateBytes.length - offset)
            {
                bytesToReturn = (int)(dateBytes.length - offset);
            }
            else
            {
                bytesToReturn = (int)count;
            }
            // Wrap the appropriate portion of the array as a ByteBuffer
            buf = ByteBuffer.wrap(dateBytes, (int)offset, bytesToReturn);
        }
        // Send the data back to the user
        this.replyRead(client, buf, tag);
    }
    
    public ULong getLength()
    {
        return new ULong(28);
    }
    
    public static void main(String[] args) throws Exception
    {
        // Create the root directory of the Styx server
        StyxDirectory root = new StyxDirectory("/");
        // Add a DateFile to the root
        root.addChild(new DateFile());
        // Start a StyxServer, listening on port 9876
        new StyxServer(9876, root).start();
    }
    
}
