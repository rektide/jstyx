/*
 * TestServer.java
 *
 * Created on 05 November 2004, 08:38
 */

package uk.ac.rdg.resc.jstyx.server;

import java.net.InetSocketAddress;

/**
 * Simple main class to start a Styx server
 * @author  Jon
 */
public class TestServer
{
    
    private static final int DEFAULT_PORT = 8080;
    
    public static void main(String[] args) throws Throwable
    {        
        if (args.length > 1)
        {
            System.err.println("Usage: TestServer [port]");
            return;
        }
        int port = DEFAULT_PORT;
        if (args.length == 1)
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
        
        // Get the user's home directory
        String home = System.getProperty("user.home");
        
        // Set up the file tree
        StyxDirectory root = new DirectoryOnDisk("C:\\Inferno");
        
        // Set up the server and start it
        StyxServer server = new StyxServer(root, port);
        server.start();
        
    }
}