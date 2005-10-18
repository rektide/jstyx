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

package uk.ac.rdg.resc.jstyx.gridservice.client;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileOutputStream;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeAdapter;

import uk.ac.rdg.resc.jstyx.messages.TreadMessage;

import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Simple program that logs on to an SGS server, creates a new service instance,
 * runs it and redirects the output streams to the console and local files
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/10/18 14:41:32  jonblower
 * Closed PrintStreams properly
 *
 * Revision 1.2  2005/10/16 22:05:28  jonblower
 * Improved handling of output streams (mapped CStyxFiles to PrintStreams)
 *
 * Revision 1.1  2005/10/14 17:57:18  jonblower
 * Added SGSRun and associated shell scripts
 *
 */
public class SGSRun extends CStyxFileChangeAdapter
{
    
    private SGSInstanceClient instanceClient;
    
    private CStyxFile[] osFiles;
    private Hashtable/*<CStyxFile, PrintStream>*/ outputStreams; // The streams from which we can read output data
    
    private CStyxFile[] inputStreams; // The streams to which we can write data
    private CStyxFileOutputStream stdin; // The standard input to the SGS instance
    private int openStreams; // Keeps a count of the number of open streams
    
    /**
     * Creates a new SGSRun object
     * @param hostname The name (or IP address) of the SGS server
     * @param port The port of the SGS server
     * @param serviceName The name of the SGS to invoke
     * @throws StyxException if there was an error connecting to the server
     * or creating a new service instance
     */
    public SGSRun(String hostname, int port, String serviceName)
        throws StyxException
    {
        // Connect to the server
        StyxConnection conn = new StyxConnection(hostname, port);
        conn.connect();
        
        // Get a client for this server
        SGSServerClient serverClient = new SGSServerClient(conn.getRootDirectory());
        
        // Get a handle to the required Styx Grid Service
        SGSClient sgsClient = serverClient.getSGSClient(serviceName);
        
        // Create a new service instance
        String id = sgsClient.createNewInstance();
        this.instanceClient = sgsClient.getClientForInstance(id);
        
        this.openStreams = 0;
        
        // Get handles to the output streams and register this class as a listener
        this.osFiles = this.instanceClient.getOutputStreams();
        this.outputStreams = new Hashtable()/*<CStyxFile, PrintStream>*/;
        for (int i = 0; i < osFiles.length; i++)
        {
            this.osFiles[i].addChangeListener(this);
            if (this.osFiles[i].getName().equals("stdout"))
            {
                this.outputStreams.put(osFiles[i], System.out);
            }
            else if (this.osFiles[i].getName().equals("stderr"))
            {
                this.outputStreams.put(osFiles[i], System.err);
            }
            else
            {
                // TODO: Associate the file with a PrintWriter that represents a local file
            }
            this.openStreams++;
        }
        
        // Get handles to the input streams
        this.inputStreams = this.instanceClient.getInputStreams();
        // Look for the standard input
        for (int i = 0; i < this.inputStreams.length; i++)
        {
            if (this.inputStreams[i].getName().equals("stdin"))
            {
                this.stdin = new CStyxFileOutputStream(this.inputStreams[i]);
                this.openStreams++;
            }
            else
            {
                // TODO: at the moment we're ignoring all other possible input streams
            }
        }
    }
    
    /**
     * Starts the service and begins reading from the output streams
     * @throws StyxException if there was an error starting the service
     */
    public void start() throws StyxException
    {
        // Start the service
        this.instanceClient.startService();
        if (this.stdin != null)
        {
            // Start writing to the input stream
            new StdinReader().start();
        }
        // Read from the output streams
        for (int i = 0; i < this.osFiles.length; i++)
        {
            System.err.println("Started reading from " + this.osFiles[i].getName());
            this.osFiles[i].readAsync(0);
        }
    }
    
    /**
     * Reads from standard input and redirects to the SGS
     */
    private class StdinReader extends Thread
    {
        public void run()
        {
            try
            {
                byte[] b = new byte[1024]; // Read 1KB at a time
                int n = 0;
                do
                {
                    n = System.in.read(b);
                    if (n >= 0)
                    {
                        stdin.write(b, 0, n);
                    }
                } while (n >= 0);
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
            finally
            {
                try
                {
                    stdin.close();
                }
                catch (IOException ex)
                {
                    // Ignore errors here
                }
                streamClosed();
            }
        }
    }
    
    /**
     * This method is called when data arrive from one of the output streams
     */
    public void dataArrived(CStyxFile file, TreadMessage tReadMsg, ByteBuffer data)
    {
        // Get the stream associated with this file
        PrintStream stream = (PrintStream)this.outputStreams.get(file);
        if (stream == null)
        {
            // TODO: log error message (should never happen anyway)
            System.err.println("stream is null");
            return;
        }
        
        if (data.hasRemaining())
        {
            // Calculate the offset from which we need to read the next data chunk
            long newOffset = tReadMsg.getOffset().asLong() + data.remaining();
            
            // Get the data out of the buffer
            byte[] buf = new byte[data.remaining()];
            data.get(buf);
            
            // Write the data to the stream
            stream.write(buf, 0, buf.length);
            
            // Read the next chunk of data from the file
            file.readAsync(newOffset);
        }
        else
        {
            stream.close();
            file.close();
            this.streamClosed();
        }
    }
    
    /**
     * Called when an error occurs reading from one of the streams.  Could
     * happen if the stream in question does not exist
     */
    public void error(CStyxFile file, String message)
    {
        // TODO: log the error message somewhere.
        file.close();
        this.streamClosed();
    }
    
    /**
     * Called when we close a stream. If this is the last stream to be closed, the 
     * connection is also closed.
     */
    private void streamClosed()
    {
        this.openStreams--;
        if (this.openStreams == 0)
        {
            // No more open streams, so we can close the connection
            this.instanceClient.close();
        }
    }
    
    
    public static void main(String[] args)
    {
        if (args.length != 3)
        {
            System.err.println("Usage: SGSRun <hostname> <port> <servicename>");
            System.exit(1); // TODO: what is the best exit code here?
        }
        
        try
        {
            int port = Integer.parseInt(args[1]);
            new SGSRun(args[0], port, args[2]).start();
        }
        catch(NumberFormatException nfe)
        {
            System.err.println("Invalid port number");
            System.exit(1);
        }
        catch(StyxException se)
        {
            System.err.println("Error running Styx Grid Service: " + se.getMessage());
            System.exit(1);
        }
        
        // Note that the program will carry on running until all the streams
        // are closed
    }
    
}
