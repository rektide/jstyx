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

import java.net.URL;
import java.io.*;
import uk.ac.rdg.resc.jstyx.client.StyxFileInputStreamReader;

/**
 * Part of JStyx tutorial: URL-based client for a DateFile
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/02 09:19:30  jonblower
 * Initial import of DateFileClient code
 *
 */
public class DateFileURLClient
{
    public static void main(String[] args) throws Exception
    {
        URL url = new URL("styx://localhost:9876/date");
        InputStream in = url.openStream();
        
        // Use of StyxFileInputStreamReader ensures that UTF-8 is used to
        // convert the bytes from the stream into characters
        InputStreamReader reader = new StyxFileInputStreamReader(in);
        BufferedReader bufRead = new BufferedReader(reader);
        
        // Read the input data a line at a time and print to standard output
        String line = null;
        do
        {
            line = bufRead.readLine();
            if (line != null)
            {
                System.out.println(line);
            }
        } while (line != null);
        
        // Close the stream
        bufRead.close();
    }
}
