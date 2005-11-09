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
import java.io.StringReader;
import java.io.File;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileOutputStream;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeAdapter;

import uk.ac.rdg.resc.jstyx.messages.TreadMessage;

import uk.ac.rdg.resc.jstyx.StyxException;

import uk.ac.rdg.resc.jstyx.gridservice.config.*;

/**
 * Simple program that logs on to an SGS server, creates a new service instance,
 * runs it and redirects the output streams to the console and local files
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/11/09 18:00:24  jonblower
 * Implemented automatic uploading of input files
 *
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
    private StyxConnection conn;
    private SGSClient sgsClient;
    private SGSInstanceClient instanceClient;
    
    private JSAPResult result; // Result of parsing command-line parameters
    
    // The files we're going to upload to the service
    private Vector/*<File>*/ filesToUpload; 
    
    private CStyxFile[] osFiles;
    private Hashtable/*<CStyxFile, PrintStream>*/ outputStreams; // The streams from which we can read output data
    
    private SGSConfig config;  // Config info for this SGS, read from the server
    
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
        this.conn = new StyxConnection(hostname, port);
        this.conn.connect();
        
        // Get a client for this server
        SGSServerClient serverClient = new SGSServerClient(this.conn.getRootDirectory());
        
        // Get a handle to the required Styx Grid Service
        this.sgsClient = serverClient.getSGSClient(serviceName);
        
        // Get the configuration of this SGS
        this.getConfig();
    }
    
    /**
     * Reads the configuration file from the server so that we know how to parse
     * parameters, deal with input files etc.  This information cannot be gleaned
     * simply from interpreting the namespace itself.
     */
    private void getConfig() throws StyxException
    {
        // Parse the xml document using dom4j (without validation, since we
        // don't have the DTD.  This is OK because the server should have validated
        // the XML anyway)
        SAXReader reader = new SAXReader(false);
        try
        {
            Document doc = reader.read(new StringReader(this.sgsClient.getConfigXML()));
            this.config = new SGSConfig(doc.getRootElement());
        }
        catch(DocumentException de)
        {
            // TODO: log full stack trace
            throw new StyxException("Error parsing config XML: " + de.getMessage());
        }
        catch(SGSConfigException sce)
        {
            // TODO: log full stack trace
            throw new StyxException("Error creating SGSConfig object: " + sce.getMessage());
        }
    }
    
    /**
     * Checks the command-line arguments: makes sure they can be parsed and checks
     * for the existence of all input files
     * @throws StyxException if the arguments are not valid
     */
    public void checkArguments(String[] args) throws StyxException
    {
        JSAP parser = this.config.getParamParser();
        this.result = parser.parse(args);
        if (this.result.success())
        {
            // Parsing was successful.  Now we can check that the input files
            // exist
            this.filesToUpload = new Vector();
            Vector inputs = this.config.getInputs();
            for (int i = 0; i < inputs.size(); i++)
            {
                SGSInput input = (SGSInput)inputs.get(i);
                if (input.getType() == SGSInput.FILE)
                {
                    File f = new File(input.getName());
                    if (f.exists())
                    {
                        this.filesToUpload.add(f);
                    }
                    else
                    {
                        // For now, we assume that all fixed-name files are required
                        throw new StyxException("File " + input.getName() +
                            " does not exist");
                    }
                }
                else if (input.getType() == SGSInput.FILE_FROM_PARAM)
                {
                    // TODO: deal with this
                }
            }
        }
        else
        {
            // Couldn't parse the command line
            System.err.println("Usage: " + this.sgsClient.getName() + " " +
                parser.getUsage());
            Iterator errIt = this.result.getErrorMessageIterator();
            String errMsg = "Error occurred parsing command line: ";
            if (errIt.hasNext())
            {
                errMsg += (String)errIt.next();
            }
            else
            {
                errMsg += "no details";
            }
            throw new StyxException(errMsg);
        }
    }
    
    /**
     * Creates a new service instance
     */
    public void createNewServiceInstance() throws StyxException
    {
        String id = this.sgsClient.createNewInstance();
        this.instanceClient = this.sgsClient.getClientForInstance(id);
    }
    
    /**
     * Sets the values of all the parameters
     */
    public void setParameters() throws StyxException
    {
        // Set the parameters one by one.  We do it this way because in the
        // case of a parameter that specifies an input file, we do not
        // send the full path of the input file, just its name.
        CStyxFile[] paramFiles = this.instanceClient.getParameterFiles();
        for (int i = 0; i < paramFiles.length; i++)
        {
            // Search for this parameter in the configuration
            boolean found = false;
            Vector params = this.config.getParams();
            for (int j = 0; j < params.size() && !found; j++)
            {
                SGSParam param = (SGSParam)params.get(j);
                if (param.getName().equals(paramFiles[i].getName()))
                {
                    found = true;
                    String paramValue = this.getParameterValue(param);
                    if (!paramValue.equals(""))
                    {
                        paramFiles[i].setContents(paramValue);
                    }
                }
            }
            if (!found)
            {
                // This should never be reached.
                throw new StyxException("Internal error: could not find" +
                    " parameter " + paramFiles[i].getName() + " in the " +
                    "configuration.");
            }
        }
    }
    
    /**
     * Gets the value for the given parameter from the command line, taking into
     * account whether or not it represents an input file
     * @param param The SGSParam object representing this parameter
     * @param the value as read from the command line, ready to write to the
     * remote parameter file on the Styx server
     * @return the value for the parameter or the empty string if the parameter
     * value has not been set
     * @throws StyxException if it is an input file and the file does not exist
     */
    private String getParameterValue(SGSParam param) throws StyxException
    {
        // The following is very similar to code in
        // StyxGridServiceInstance.CommandLineFile.write()
        if (param.getParameter() instanceof Switch)
        {
            boolean switchSet = this.result.getBoolean(param.getName());
            return switchSet ? "true" : "false";
        }
        else
        {
            // This is an Option
            // See if this parameter represents an input file
            boolean paramIsInputFile = (param.getInputFile() != null);                            
            String[] arr = this.result.getStringArray(param.getName());
            if (arr != null && arr.length > 0)
            {
                StringBuffer str = new StringBuffer();
                for (int j = 0; j < arr.length; j++)
                {
                    String val = arr[j];
                    if (paramIsInputFile)
                    {
                        File file = new File(val);
                        if (file.exists())
                        {
                            // Schedule the file for upload
                            this.filesToUpload.add(file);
                        }
                        else
                        {
                            throw new StyxException(val + " does not exist");
                        }
                        val = file.getName();
                    }
                    str.append(val);
                    if (j < arr.length - 1)
                    {
                        str.append(" ");
                    }
                }
                return str.toString();
            }
            else
            {
                return "";
            }
        }
    }
    
    /**
     * Uploads the necessary input files to the server
     */
    public void uploadInputFiles() throws StyxException
    {
        // Get handle to the inputs directory
        CStyxFile inputsDir = this.instanceClient.getInputStreamsDir();
        for (int i = 0; i < this.filesToUpload.size(); i++)
        {
            File file = (File)this.filesToUpload.get(i);
            CStyxFile targetFile = inputsDir.getFile(file.getName());
            System.out.print("Uploading " + file.getName() + " to "
                + targetFile.getPath() + "...");
            targetFile.upload(file);
            System.out.println(" complete");
        }
    }
    
    /**
     * Uploads the necessary input files to the server
     */
    /*public void uploadInputFiles2() throws StyxException
    {
        // Once we have got to this stage, the inputs/ directory of the SGS
        // instance's namespace will contain all the files that the SGS is 
        // expecting.  First we retrieve these
        CStyxFile[] inputStreams = this.instanceClient.getInputStreams();
        
        for (int i = 0; i < inputStreams.length; i++)
        {
            // Look for the standard input: we treat this as a special case
            if (inputStreams[i].getName().equals("stdin"))
            {
                System.out.println("Found stdin");
                this.stdin = new CStyxFileOutputStream(inputStreams[i]);
                this.openStreams++;
            }
            else
            {
                // We need to upload the input file before we can go further
                Object fileObj = this.filesToUpload.get(inputStreams[i].getName());
                if (fileObj != null)
                {
                    // We've got a file to upload
                    File f = (File)fileObj;
                    System.out.print("Uploading " + f.getName() + "...");
                    inputStreams[i].upload(f);
                    System.out.println(" complete");
                }
                else
                {
                    // We haven't got a file to upload to this input.  We have
                    // already checked to see if this is required (in the
                    // checkArguments() method) so we just assume that this
                    // input file is not required and ignore it.
                }
            }
        }
    }*/
    
    public void run() throws StyxException
    {
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
     * Disconnects cleanly from the server
     */
    public void disconnect()
    {
        if (this.conn != null)
        {
            this.conn.close(); // TODO this doesn't seem to work properly
        }
    }
    
    /**
     * Reads from standard input and redirects to the SGS
     * @todo recreates code in StyxGridServiceInstance.RedirectStream: refactor
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
        if (args.length < 3)
        {
            System.err.println("Usage: SGSRun <hostname> <port> <servicename>");
            System.exit(1); // TODO: what is the best exit code here?
        }
        
        SGSRun runner = null;
        try
        {
            int port = Integer.parseInt(args[1]);
            
            // Create an SGSRun object: this connects to the server and verifies
            // that the given Styx Grid Service exists
            runner = new SGSRun(args[0], port, args[2]);
            
            // Get the arguments to be passed to the Styx Grid Service
            String[] sgsArgs = new String[args.length - 3];
            System.arraycopy(args, 3, sgsArgs, 0, sgsArgs.length);
            
            // Check the command-line arguments
            runner.checkArguments(sgsArgs);
            
            // Create a new service instance
            runner.createNewServiceInstance();
            
            // Set the parameters of the service instance
            runner.setParameters();
            
            // Upload the input files to the server
            runner.uploadInputFiles();
            
            
            runner.disconnect();
            
        }
        catch(NumberFormatException nfe)
        {
            System.err.println("Invalid port number");
            runner.disconnect();
            System.exit(1);
        }
        catch(StyxException se)
        {
            System.err.println("Error running Styx Grid Service: " + se.getMessage());
            runner.disconnect();
            System.exit(1);
        }
        finally
        {
            System.out.println("In finally clause");
        }
        // Note that the program will carry on running until all the streams
        // are closed
    }
    
}
