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

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Part of JStyx tutorial: CStyxFile-based client for a DateFile
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/03/16 17:55:53  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.1  2005/03/02 09:19:30  jonblower
 * Initial import of DateFileClient code
 *
 */
public class DateFileClient2
{
    
    public static void main(String[] args) throws Exception
    {
        // Create and open a connection to the remote server
        StyxConnection conn = new StyxConnection("localhost", 9876);
        conn.connect();
        // Get a CStyxFile representing the DateFile at the remote end,
        // opening it for reading
        CStyxFile dateFile = conn.openFile("date", StyxUtils.OREAD);
        
        ByteBuffer buf = null;
        do
        {
            // Read a chunk of data from the file
            buf = dateFile.read();
            // If we have reached EOF, there will be no bytes in the buffer
            if (buf.hasRemaining())
            {
                // Convert the data in the buffer to a string and print it
                String s = StyxUtils.dataToString(buf);
                System.out.print(s);
            }
        } while (buf.hasRemaining());
    
        // Close the file and the connection
        dateFile.close();
        conn.close();
    }
    
}
