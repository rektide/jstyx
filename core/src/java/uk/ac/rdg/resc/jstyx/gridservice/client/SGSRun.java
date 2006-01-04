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
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.File;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Enumeration;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.FlaggedOption;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileOutputStream;
import uk.ac.rdg.resc.jstyx.client.CStyxFileChangeAdapter;

import uk.ac.rdg.resc.jstyx.messages.TreadMessage;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

import uk.ac.rdg.resc.jstyx.gridservice.config.SGSConfig;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSParam;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSInput;

/**
 * Simple program that logs on to an SGS server, creates a new service instance,
 * runs it and redirects the output streams to the console and local files
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.14  2006/01/04 11:24:57  jonblower
 * Implemented time directory in the SGS instance namespace
 *
 * Revision 1.13  2005/12/09 18:41:56  jonblower
 * Continuing to simplify client interface to SGS instances
 *
 * Revision 1.12  2005/12/07 17:53:31  jonblower
 * Added type to SGSParam (STRING, INPUT_FILE and OUTPUT_FILE)
 *
 * Revision 1.11  2005/12/07 08:56:32  jonblower
 * Refactoring SGS client code
 *
 * Revision 1.10  2005/12/01 17:17:07  jonblower
 * Simplifying client interface to SGS instances
 *
 * Revision 1.9  2005/12/01 08:29:47  jonblower
 * Refactored XML config handling to simplify clients
 *
 * Revision 1.8  2005/11/28 17:20:18  jonblower
 * Fixed bug with not exiting cleanly when error occurs
 *
 * Revision 1.7  2005/11/14 21:31:54  jonblower
 * Got SGSRun working for SC2005 demo
 *
 * Revision 1.6  2005/11/11 21:57:21  jonblower
 * Implemented passing of URLs to input files
 *
 * Revision 1.5  2005/11/10 19:50:43  jonblower
 * Added code to handle output files
 *
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
    private static final String OUTPUT_REFS = "sgs-output-refs";
    private static final String HELP = "sgs-help";
    private static final String VERBOSE_HELP = "sgs-verbose-help";
    private static final String DEBUG = "sgs-debug";
    
    private StyxConnection conn;
    private SGSClient sgsClient;
    private SGSInstanceClient instanceClient;
    private CStyxFile exitCodeFile;
    private String exitCode;
    private boolean serviceStarted;
    
    private JSAPResult result; // Result of parsing command-line parameters
    
    private boolean debug;  // If set true we will print debug messages to stdout
    
    private CStyxFile[] osFiles;
    private Hashtable/*<CStyxFile, PrintStream>*/ outputStreams; // The streams from which we can read output data
    
    private SGSConfig config;  // Config info for this SGS, read from the server
    
    private int openStreams; // Keeps a count of the number of open streams
    private Hashtable/*<SGSInput, File>*/ filesToUpload; // The fixed files that we will upload
                                  // (not the ones that are set through parameters)
    
    /**
     * Creates a new SGSRun object
     * @param hostname The name (or IP address) of the SGS server
     * @param port The port of the SGS server
     * @param serviceName The name of the SGS to invoke
     * @throws StyxException if there was an error connecting to the server
     * or getting the configuration of the SGS 
     */
    public SGSRun(String hostname, int port, String serviceName) throws StyxException
    {
        this.debug = false;
        this.openStreams = 0;
        this.exitCode = null;
        this.serviceStarted = false;
        
        // Get a client for this server
        SGSServerClient serverClient = new SGSServerClient(hostname, port);
        
        // Get a handle to the required Styx Grid Service
        this.sgsClient = serverClient.getSGSClient(serviceName);
        
        // Get the configuration of this SGS
        this.config = this.sgsClient.getConfig();
        
        this.filesToUpload = new Hashtable();
    }
    
    /**
     * Gets the JSAP object from the config object and adds the extra parameters
     * we need to parse the command line
     */
    private JSAP getJSAP() throws StyxException
    {
        // Get a JSAP object that can parse the command line
        JSAP jsap = this.config.getParamParser();
        // Add extra parameters
        try
        {
            // Add a switch to allow the user to print out a help message for this SGS
            jsap.registerParameter(new Switch(HELP, JSAP.NO_SHORTFLAG, HELP,
                "Set this switch to print out a short help message"));
            // Add a switch to allow the user to print out a verbose help message for this SGS
            jsap.registerParameter(new Switch(VERBOSE_HELP, JSAP.NO_SHORTFLAG, VERBOSE_HELP,
                "Set this switch to print out a long help message"));
            // Add a switch to enable debugging messages to be printed to stdout
            jsap.registerParameter(new Switch(DEBUG, JSAP.NO_SHORTFLAG, DEBUG,
                "Set this switch in order to enable printing of debug messages"));
            // Add a switch to allow outputting of references to files instead of
            // the actual files themselves
            jsap.registerParameter(new Switch(OUTPUT_REFS, JSAP.NO_SHORTFLAG, OUTPUT_REFS,
                "Set this switch in order to get URLs to all output files rather than actual files"));
            
            // Add a parameter for each fixed input file so that the user can set the
            // URL with an argument like --input.txt-ref=
            Vector inputs = this.config.getInputs();
            for (int i = 0; i < inputs.size(); i++)
            {
                SGSInput input = (SGSInput)inputs.get(i);
                if (input.getType() == SGSInput.FILE)
                {
                    jsap.registerParameter(new FlaggedOption(input.getName() + "-ref",
                        JSAP.STRING_PARSER, null, false, JSAP.NO_SHORTFLAG,
                        input.getName() + "-ref",
                        "If set, will cause the input file " + input.getName() +
                        " to be uploaded from the given URL"));
                }
            }
        }
        catch (JSAPException jsape)
        {
            throw new StyxException(jsape.getMessage());
        }
        
        return jsap;
    }
    
    /**
     * Checks the command-line arguments: makes sure they can be parsed and checks
     * for the existence of all input files
     * @throws StyxException if the arguments are not valid
     */
    public void checkArguments(String[] args) throws StyxException
    {
        JSAP parser = this.getJSAP();
        this.result = parser.parse(args);
        
        // Check if the user wants help
        if (this.result.getBoolean(VERBOSE_HELP))
        {
            System.out.println("");
            System.out.println("Usage: " + this.sgsClient.getName() + " " +
                parser.getUsage());
            System.out.println("");
            System.out.println(parser.getHelp());
            System.exit(0);
        }
        else if (this.result.getBoolean(HELP))
        {
            System.out.println("");
            System.out.println("Usage: " + this.sgsClient.getName() + " " +
                parser.getUsage());
            System.exit(0);
        }
        
        if (this.result.success())
        {
            // Parsing was successful
            this.debug = this.result.getBoolean(DEBUG);
            // Now we can check that the required input files exist
            Vector inputs = this.config.getInputs();
            for (int i = 0; i < inputs.size(); i++)
            {
                SGSInput input = (SGSInput)inputs.get(i);
                // See if a URL has been set for this input
                String url = this.result.getString(input.getName() + "-url");
                if (url != null && !url.trim().equals(""))
                {
                    // We have a URL for this input (stdin or fixed file)
                    if (this.debug)
                    {
                        System.out.println("Set URL for " + input.getName() +
                            ": " + url);
                    }
                    if (url.startsWith("readfrom:"))
                    {
                        this.filesToUpload.put(input, url);
                    }
                    else
                    {
                        // We assume that this is just a URL
                        this.filesToUpload.put(input, "readfrom:" + url);
                    }
                }
                else
                {
                    // No URL has been set for this input file
                    if (input.getType() == SGSInput.FILE)
                    {
                        File f = new File(input.getName());
                        if (f.exists())
                        {
                            this.filesToUpload.put(input, f);
                        }
                        else
                        {
                            // For now, we assume that all fixed-name files are required
                            throw new StyxException("File " + input.getName() +
                                " does not exist");
                        }
                    }
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
            errMsg += errIt.hasNext() ? (String)errIt.next() : "no details";
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
        // Read the exit code: when the exit code has arrived, the
        // gotServiceDataValue() event
        // TODO make this class an SGSInstanceClientChangeListener
        this.instanceClient.addChangeListener(new SListener());
        this.instanceClient.readServiceDataValueAsync("exitCode");
    }
    
    /**
     * Listens for the exit code appearing (signals that service is complete)
     */
    private class SListener extends SGSInstanceClientChangeAdapter
    {
        public void gotServiceDataValue(String sdName, String newData)
        {
            if (sdName.equals("exitCode"))
            {
                exitCode = newData;
                checkEnd();
            }
        }
    }
    
    /**
     * Sets the values of all the parameters
     */
    public void setParameters() throws StyxException, FileNotFoundException
    {
        // Run through each of the parameter values in the configuration
        Vector params = this.config.getParams();
        for (Iterator it = params.iterator(); it.hasNext(); )
        {
            SGSParam param = (SGSParam)it.next();
            // Set the parameter value
            // TODO need to check that getStringArray() also works for 
            // switches (we hope it returns "true" or "false")
            this.instanceClient.setParameterValue(param,
                this.result.getStringArray(param.getName()));
        }
    }
    
    /**
     * Upload the input files to the server.
     */
    public void uploadInputFiles() throws StyxException
    {
        // First schedule the fixed input files (inc. stdin) for upload
        for (Enumeration en = this.filesToUpload.keys(); en.hasMoreElements(); )
        {
            SGSInput inputFile = (SGSInput)en.nextElement();
            Object dataSrc = this.filesToUpload.get(inputFile);
            try
            {
                if (dataSrc instanceof String)
                {
                    this.instanceClient.setInputSource(inputFile, (String)dataSrc);
                }
                else
                {
                    // We assume this must be a file
                    this.instanceClient.setInputSource(inputFile, (File)dataSrc);
                }
            }
            catch(FileNotFoundException fnfe)
            {
                // This should not happen as we have already checked that
                // the file exists in checkArguments()
                throw new StyxException("Internal error: " + fnfe.getMessage());
            }
        }
        // Now we actually upload the input files
        this.instanceClient.uploadInputFiles();
    }
    
    /**
     * Starts the service and begins reading from the output streams
     * @throws StyxException if there was an error starting the service
     */
    public void start() throws StyxException
    {
        // Start the service.
        this.instanceClient.startService();
        this.readOutputStreams();
        this.serviceStarted = true;
    }
    
    /**
     * Prepares the output streams and starts reading from them
     */
    private void readOutputStreams() throws StyxException
    {
        /*// Get handles to the output streams and register this class as a listener
        this.osFiles = this.instanceClient.getOutputs();
        this.outputStreams = new Hashtable();
        for (int i = 0; i < osFiles.length; i++)
        {
            this.osFiles[i].addChangeListener(this);
            PrintStream prtStr = null;
            if (this.osFiles[i].getName().equals("stdout"))
            {
                prtStr = System.out;
            }
            else if (this.osFiles[i].getName().equals("stderr"))
            {
                prtStr = System.err;
            }
            else
            {
                try
                {
                    FileOutputStream fout = new FileOutputStream(osFiles[i].getName());
                    prtStr = new PrintStream(fout);
                }
                catch(FileNotFoundException fnfe)
                {
                    // Called by FileOutputStream constructor
                    System.err.println("Couldn't create target file for "
                        + osFiles[i].getName() + ": " + fnfe.getMessage());
                }
            }
            if (this.result.getBoolean(OUTPUT_REFS))
            {
                // Just write the URL to the relevant PrintStream
                PrintWriter writer = new PrintWriter(prtStr);
                writer.write("readfrom:" + osFiles[i].getURL());
                writer.close();
            }
            else
            {
                this.openStreams++;
                this.outputStreams.put(osFiles[i], prtStr);
                if (this.debug)
                {
                    System.out.println("Started reading from " + this.osFiles[i].getName());
                }
                this.osFiles[i].readAsync(0);
            }
        }*/
    }
    
    /**
     * Disconnects cleanly from the server
     */
    public void disconnect()
    {
        if (this.conn != null)
        {
            this.conn.close();
        }
    }
    
    /**
     * This method is called when data arrive from one of the output streams
     */
    public void dataArrived(CStyxFile file, TreadMessage tReadMsg, ByteBuffer data)
    {
        if (file == this.exitCodeFile)
        {
            this.exitCode = StyxUtils.dataToString(data);
            this.checkEnd();
        }
        else
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
                if (stream != System.out && stream != System.err)
                {
                    // Don't close the standard streams, we may need them for other
                    // messages
                    stream.close();
                }
                file.close();
                this.openStreams--;
                this.checkEnd();
            }
        }
    }
    
    /**
     * Called when an error occurs reading from one of the streams.  Could
     * happen if the stream in question does not exist
     */
    public void error(CStyxFile file, String message)
    {
        // TODO: log the error message somewhere.
        System.err.println("Error running Styx Grid Service: " + message);
        file.close();
        if (file != this.exitCodeFile)
        {
            this.openStreams--;
        }
        this.checkEnd();
    }
    
    /**
     * Called when we think the program might have ended (when a stream is closed,
     * when the exit code is received or when the main() method ends).
     */
    public void checkEnd()
    {
        if (this.openStreams == 0 && this.exitCode != null && this.serviceStarted)
        {
            // No more open streams, so we can close the connection
            this.instanceClient.close();
            int ec = Integer.MIN_VALUE;
            try
            {
                ec = Integer.parseInt(this.exitCode);
            }
            catch(NumberFormatException nfe)
            {
                System.err.println("Error parsing exit code: " + this.exitCode
                    + " is not a valid integer");
            }
            if (this.debug)
            {
                System.out.println("Exiting with code " + ec);
            }
            System.exit(ec);
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
            
            // Get the arguments to be passed to the Styx Grid Service
            String[] sgsArgs = new String[args.length - 3];
            System.arraycopy(args, 3, sgsArgs, 0, sgsArgs.length);
            
            // Create an SGSRun object: this connects to the server and verifies
            // that the given Styx Grid Service exists
            runner = new SGSRun(args[0], port, args[2]);
            
            // Check the command-line arguments
            runner.checkArguments(sgsArgs);
            
            // Create a new service instance
            runner.createNewServiceInstance();
            
            // Set the parameters of the service instance
            runner.setParameters();
            
            // Upload the input files
            runner.uploadInputFiles();
            
            // Start the service
            runner.start();
            
            // Check to see if we can finish now
            runner.checkEnd();
            // Note that the program will carry on running until all the streams
            // are closed
        }
        catch(NumberFormatException nfe)
        {
            System.err.println("Invalid port number");
            // TODO: what is an appropriate error code here?
            System.exit(1);
        }
        catch(Exception e)
        {
            System.err.println("Error running Styx Grid Service: " + e.getMessage());
            if (runner != null)
            {
                runner.disconnect();
            }
            // TODO: what is an appropriate error code here?
            System.exit(1);
        }
    }
    
}
