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

import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.server.DirectoryOnDisk;
import uk.ac.rdg.resc.jstyx.server.StyxServer;

/**
 * Simple class to start a Styx server that serves up a filesystem that
 * mirrors the underlying host filesystem, starting at a given root directory
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/03/14 16:40:01  jonblower
 * Modifications for using SSL
 *
 * Revision 1.2  2005/03/11 14:00:29  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.1.2.2  2005/03/11 08:31:50  jonblower
 * Change to default port number
 *
 * Revision 1.1.2.1  2005/03/10 11:53:54  jonblower
 * Modified for MINA framework
 *
 * Revision 1.1  2005/02/28 16:17:35  jonblower
 * Initial import of TestServer
 *
 */
public class TestServer
{
    
    private static final int DEFAULT_PORT = 8080;
    
    /**
     * The TestServer takes two arguments, both optional.  The first is the
     * port number under which the server will listen (defaults to 8080 if
     * not set). The second is the directory in the host filesystem which
     * will be at the root of the Styx server. This defaults to the user's
     * home directory (i.e. the output of System.getProperty("user.home"))
     * if not set.
     */
    public static void main(String[] args) throws Throwable
    {
        // Set the default port and root directory of the server
        int port = DEFAULT_PORT;
        // Default root directory is the user's home directory
        String home = System.getProperty("user.home");
        // Use SSL if there is a third command-line argument
        boolean useSSL = false;
        
        if (args.length > 0)
        {
            try
            {
                port = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException nfe)
            {
                System.err.println(args[0] + " is not a valid port number");
                return;
            }
        }
        if (args.length > 1)
        {
            home = args[1];
        }
        if (args.length > 2)
        {
            // TODO: perhaps this third argument should be something specific...
            useSSL = true;
        }
        if (args.length > 3)
        {
            System.err.println("Usage: TestServer [port] [root directory] [use SSL]");
            return;
        }
        
        // Set up the file tree
        StyxDirectory root = new DirectoryOnDisk(home);
        
        // Set up the server and start it
        StyxServer server = new StyxServer(port, root, useSSL);
        server.start();
        
    }
}