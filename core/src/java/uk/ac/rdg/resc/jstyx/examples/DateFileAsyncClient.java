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

import java.nio.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeAdapter;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * 
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/02 22:18:29  jonblower
 * Initial import
 *
 */
public class DateFileAsyncClient extends CStyxFileChangeAdapter
{
    private StyxConnection conn;
    private int offset;
    
    public DateFileAsyncClient(StyxConnection conn)
    {
        this.conn = conn;
        this.offset = 0;
    }
    
    public static void main(String[] args) throws Exception
    {
        StyxConnection conn = new StyxConnection("localhost", 9876);
        conn.connectAsync();
        // Create a CStyxFile but don't open it yet
        CStyxFile dateFile = new CStyxFile(conn, "date");
        // Add a changelistener to this file
        dateFile.addChangeListener(new DateFileAsyncClient(conn));
        // Start reading from the start of this file
        dateFile.readAsync(0);
    }
    
    public void dataArrived(CStyxFile file, TreadMessage tReadMsg, ByteBuffer data)
    {
        if (data.hasRemaining())
        {
            this.offset += data.remaining();
            System.out.print(StyxUtils.dataToString(data));
            file.readAsync(this.offset);
        }
        else
        {
            try
            {
                file.close();
                this.conn.close();
            }
            catch(StyxException se)
            {
                // TODO: does close() really have to throw an Exception?
                se.printStackTrace();
            }
        }
    }
    
}
