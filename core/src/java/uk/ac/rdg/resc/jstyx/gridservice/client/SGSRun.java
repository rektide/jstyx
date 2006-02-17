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

import java.net.UnknownHostException;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.FlaggedOption;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;

import uk.ac.rdg.resc.jstyx.messages.TreadMessage;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

import uk.ac.rdg.resc.jstyx.gridservice.config.SGSConfig;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSParam;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSInput;
import uk.ac.rdg.resc.jstyx.gridservice.config.SGSOutput;

/**
 * Simple program that logs on to an SGS server, creates a new service instance,
 * runs it and redirects the output streams to the console and local files
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.18  2006/02/17 17:34:44  jonblower
 * Implemented (but didn't test) proper handling of output files
 *
 * Revision 1.17  2006/02/17 09:27:50  jonblower
 * Working towards handling output files properly
 *
 * Revision 1.16  2006/02/16 17:34:16  jonblower
 * Working towards handling output files and references thereto
 *
 * Revision 1.15  2006/01/05 16:06:34  jonblower
 * SGS clients now deal with possibility that client could be created on a different server
 *
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
public class SGSRun
{
    private static final String OUTPUT_ALL_REFS = "sgs-allrefs";
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
     * @throws UnknownHostException if the remote host could not be found
     */
    public SGSRun(String hostname, int port, String serviceName)
        throws StyxException, UnknownHostException
    {
        this.debug = false;
        this.openStreams = 0;
        this.exitCode = null;
        this.serviceStarted = false;
        
        // Get a client for this server
        SGSServerClient serverClient = SGSServerClient.getServerClient(hostname, port);
        
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
            // Add a switch to allow outputting of references to all output files
            // instead of the actual files themselves
            jsap.registerParameter(new Switch(OUTPUT_ALL_REFS, JSAP.NO_SHORTFLAG, OUTPUT_ALL_REFS,
                "Set this switch in order to get URLs to all output files rather than actual files"));
            
            // Add a parameter for each fixed input file so that the user can set the
            // URL with an argument like --sgs-ref-input.txt=
            Vector inputs = this.config.getInputs();
            for (int i = 0; i < inputs.size(); i++)
            {
                SGSInput input = (SGSInput)inputs.get(i);
                if (input.getType() == SGSInput.FILE)
                {
                    jsap.registerParameter(new FlaggedOption("sgs-ref-" + input.getName(),
                        JSAP.STRING_PARSER, null, false, JSAP.NO_SHORTFLAG,
                        "sgs-ref-" + input.getName(),
                        "If set, will cause the input file " + input.getName() +
                        " to be uploaded from the given URL"));
                }
            }
            
            // Add a parameter for each fixed output file so that the user can
            // choose to receive a URL to the data instead of the data themselves,
            // with the switch 
            Vector outputs = this.config.getOutputs();
            for (int i = 0; i < outputs.size(); i++)
            {
                SGSOutput output = (SGSOutput)outputs.get(i);
                if (output.getType() == SGSOutput.FILE ||
                    output.getType() == SGSOutput.STREAM)
                {
                    // This is a fixed output file, i.e. not specified by a parameter
                    jsap.registerParameter(new Switch("sgs-ref-" + output.getName(),
                        JSAP.NO_SHORTFLAG, "sgs-ref-" + output.getName(),
                        "If set, will download a URL to " + output.getName() +
                        " instead of the actual data"));
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
        String instanceUrl = this.sgsClient.createNewInstance();
        this.instanceClient = new SGSInstanceClient(instanceUrl);
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
            if (param.getType() == SGSParam.OUTPUT_FILE)
            {
                // We'll deal with this later, in readOutputFiles()
            }
            else
            {
                // Just set the parameter value
                // TODO need to check that getStringArray() also works for 
                // switches (we hope it returns "true" or "false")
                this.instanceClient.setParameterValue(param,
                    this.result.getStringArray(param.getName()));
            }
        }
    }
    
    /**
     * Sets the input sources for the Styx Grid Service
     */
    public void setInputSources() throws StyxException
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
        this.serviceStarted = true;
    }
    
    /**
     * Starts reading from the output files.  Note that we have
     * already set the destinations for the outputs that are set via a parameter
     * in the setParameters() method
     */
    public void readOutputFiles() throws StyxException, FileNotFoundException
    {
        // Go through all the output files in the config object.  Each of these
        // will be represented by a file in the server's namespace
        boolean allRefs = this.result.getBoolean(OUTPUT_ALL_REFS);
        Vector outputFiles = this.config.getOutputs();
        for (int i = 0; i < outputFiles.size(); i++)
        {
            SGSOutput output = (SGSOutput)outputFiles.get(i);
            if (output.getType() == SGSOutput.FILE_FROM_PARAM)
            {
                // The name of this output file is specified by the value of the
                // parameter with the same name
                String filename = this.result.getStringArray(output.getName())[0].trim();
                PrintStream prtStr = this.getPrintStream(filename);
                if (allRefs || filename.endsWith(".sgsref"))
                {
                    // We output a reference to this file
                    prtStr.print("readfrom:" +
                        this.instanceClient.getOutputFileURL(output.getName()));
                    prtStr.close();
                }
                else
                {
                    // We must redirect this output to the filename that was given
                    // by the parameter value
                    this.instanceClient.redirectOutput(output.getName(), prtStr);
                }
            }
            else
            {
                // This is a fixed-name file or a standard stream
                PrintStream prtStr = this.getPrintStream(output.getName());
                if (allRefs || this.result.getBoolean("sgs-ref-" + output.getName()))
                {
                    prtStr.print("readfrom:" + this.instanceClient.getOutputFileURL(output.getName()));
                    if (prtStr != System.out && prtStr != System.err)
                    {
                        prtStr.close();
                    }
                }
                else
                {
                    this.instanceClient.redirectOutput(output.getName(), prtStr);
                }
            }
        }
    }
    
    /**
     * @return a PrintStream for the given output file name.  If a file with the
     * given name already exists, this truncates the file to zero length.
     * @throws FileNotFoundException if the file exists but is a directory.
     */
    private PrintStream getPrintStream(String filename) throws FileNotFoundException
    {
        if (filename.equals("stdout"))
        {
            return System.out;
        }
        else if (filename.equals("stderr"))
        {
            return System.err;
        }
        else
        {
            return new PrintStream(filename);
        }
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
        
        // Make sure we can understand styx:// URLs
        System.setProperty("java.protocol.handler.pkgs", "uk.ac.rdg.resc.jstyx.client.protocol");
        
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
            
            // Set the input sources
            runner.setInputSources();
            
            // Start the service
            runner.start();
            
            // Start reading from the output files
            runner.readOutputFiles();
            
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
