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

package uk.ac.rdg.resc.jstyx.tutorial;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Accompanies the ReadingAndWriting section of the tutorial.  Contains examples
 * of various ways of reading from and writing to files on a Styx system.  Before
 * running this class, run the SimpleServer.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/09/01 07:48:41  jonblower
 * Writing "ReadingAndWriting" section of tutorial
 *
 */
public class ReadingAndWriting
{
    
    public static void setAndGetContents(CStyxFile file) throws StyxException
    {
        System.out.println(file.getContents());
        //file.setContents("hello JStyx world");
    }
    
    public static void main (String[] args)
    {
        // Create a connection to the server.  You may need to change the server
        // address and port to suit your system.
        StyxConnection conn = new StyxConnection("localhost", 9876);
        try
        {
            // Connect to the server and perform handshaking
            conn.connect();
            // Get a handle to the "readme" file on the server
            CStyxFile readmeFile = conn.getFile("readme");
            
            // Use the simple set- and getContents() methods
            setAndGetContents(readmeFile);
        }
        catch (StyxException se)
        {
            se.printStackTrace();
        }
        finally
        {
            // Close the connection.  If this isn't done some threads will not
            // be stopped.
            conn.close();
        }
    }
    
}
