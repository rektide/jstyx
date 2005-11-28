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
import java.io.StringReader;
import java.io.File;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.DocumentException;

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

import uk.ac.rdg.resc.jstyx.gridservice.config.*;

/**
 * Simple program that logs on to an SGS server, creates a new service instance,
 * runs it and redirects the output streams to the console and local files
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
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
    private static final String OUTPUT_REFS = "output-refs";
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
    
    // The files we're going to upload to the service
    private Vector/*<File>*/ filesToUpload; 
    
    private CStyxFile[] osFiles;
    private Hashtable/*<CStyxFile, PrintStream>*/ outputStreams; // The streams from which we can read output data
    
    private SGSConfig config;  // Config info for this SGS, read from the server
    
    private boolean usingStdin;
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
        this.usingStdin = false;
        this.debug = false;
        this.openStreams = 0;
        this.exitCode = null;
        this.serviceStarted = false;
        
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
            // Now we can check that the input files exist
            this.filesToUpload = new Vector();
            Vector inputs = this.config.getInputs();
            for (int i = 0; i < inputs.size(); i++)
            {
                SGSInput input = (SGSInput)inputs.get(i);
                // See if a URL has been set for this input
                String url = this.result.getString(input.getName() + "-url");
                if (url != null && !url.trim().equals(""))
                {
                    if (this.debug)
                    {
                        System.out.println("Set URL for " + input.getName() +
                            ": " + url);
                    }
                }
                else
                {
                    // No URL has been set for this input file
                    if (input.getType() == SGSInput.STREAM)
                    {
                        this.usingStdin = true;
                    }
                    else if (input.getType() == SGSInput.FILE)
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
                        // This is dealt with in getParameterValue()
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
     * Gets the JSAP object from the config object and adds the extra parameters
     * we need to parse the command line
     */
    private JSAP getJSAP() throws StyxException
    {
        JSAP jsap = this.config.getParamParser();
        // Add a switch that the user can set to force all outputs to be URLs,
        // not actual content
        try
        {
            // Add a switch to allow the user to print out a help message for this SGS
            Switch help = new Switch(HELP, JSAP.NO_SHORTFLAG, HELP,
                "Set this switch to print out a short help message");
            jsap.registerParameter(help);
            // Add a switch to allow the user to print out a verbose help message for this SGS
            Switch verboseHelp = new Switch(VERBOSE_HELP, JSAP.NO_SHORTFLAG, VERBOSE_HELP,
                "Set this switch to print out a long help message");
            jsap.registerParameter(verboseHelp);
            // Add a switch to enable debugging messages to be printed to stdout
            Switch debug = new Switch(DEBUG, JSAP.NO_SHORTFLAG, DEBUG,
                "Set this switch in order to enable printing of debug messages");
            jsap.registerParameter(debug);
            // Add a switch to allow outputting of references to files instead of
            // the actual files themselves
            Switch outputUrls = new Switch(OUTPUT_REFS, JSAP.NO_SHORTFLAG, OUTPUT_REFS,
                "Set this switch in order to get URLs to output files rather than actual files");
            jsap.registerParameter(outputUrls);
            // Add a parameter for each fixed input file so that the user can set the
            // URL with an argument like --input.txt-ref=
            Vector inputs = this.config.getInputs();
            for (int i = 0; i < inputs.size(); i++)
            {
                SGSInput input = (SGSInput)inputs.get(i);
                if (input.getType() == SGSInput.FILE)
                {
                    FlaggedOption fo = new FlaggedOption(input.getName() + "-ref",
                        JSAP.STRING_PARSER, null, false, JSAP.NO_SHORTFLAG,
                        input.getName() + "-ref",
                        "If set, will cause the input file " + input.getName() +
                        " to be uploaded from the given URL");
                    jsap.registerParameter(fo);
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
     * Creates a new service instance
     */
    public void createNewServiceInstance() throws StyxException
    {
        String id = this.sgsClient.createNewInstance();
        this.instanceClient = this.sgsClient.getClientForInstance(id);
        // Start reading the exit code
        CStyxFile instanceRoot = this.instanceClient.getInstanceRoot();
        // TODO: should get the file through SGSInstanceClient interface
        this.exitCodeFile = instanceRoot.getFile("serviceData/exitCode");
        this.exitCodeFile.addChangeListener(this);
        // Start reading from the exit code file
        this.exitCodeFile.readAsync(0);
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
                        if (val.startsWith("readfrom:"))
                        {
                            // We are setting a URL to a file. Do nothing.
                        }
                        else
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
            if (this.debug)
            {
                System.out.print("Uploading " + file.getName() + " to "
                    + targetFile.getPath() + "...");
            }
            targetFile.upload(file);
            if (this.debug)
            {
                System.out.println(" complete");
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
        if (this.usingStdin)
        {
            // Start writing to the input stream
            new StdinReader().start();
        }
        this.readOutputStreams();
        this.serviceStarted = true;
    }
    
    /**
     * Prepares the output streams and starts reading from them
     */
    private void readOutputStreams() throws StyxException
    {
        // Get handles to the output streams and register this class as a listener
        this.osFiles = this.instanceClient.getOutputStreams();
        this.outputStreams = new Hashtable()/*<CStyxFile, PrintStream>*/;
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
     * Reads from standard input and redirects to the SGS
     * @todo recreates code in StyxGridServiceInstance.RedirectStream: refactor
     */
    private class StdinReader extends Thread
    {
        public void run()
        {
            openStreams++;
            OutputStream stdin = null;
            try
            {
                stdin = new
                    CStyxFileOutputStream(instanceClient.getInputStream("stdin"));
                byte[] b = new byte[1024]; // Read 1KB at a time
                int n = 0;
                do
                {
                    n = System.in.read(b);
                    if (n >= 0)
                    {
                        stdin.write(b, 0, n);
                        stdin.flush();
                    }
                } while (n >= 0);
            }
            catch (StyxException se)
            {
                System.err.println("Error opening stream to standard input");
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
            finally
            {
                try
                {
                    if (stdin != null)
                    {
                        // This will write a zero-byte message to confirm EOF
                        stdin.close();
                    }
                }
                catch (IOException ex)
                {
                    // Ignore errors here
                }
                openStreams--;
                checkEnd();
            }
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
                System.out.flush();
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
            
            // Upload the input files to the server
            runner.uploadInputFiles();
            
            // Start the service
            runner.start();
        }
        catch(NumberFormatException nfe)
        {
            System.err.println("Invalid port number");
            if (runner != null)
            {
                runner.disconnect();
            }
            // TODO: what is an appropriate error code here?
            System.exit(1);
        }
        catch(StyxException se)
        {
            System.err.println("Error running Styx Grid Service: " + se.getMessage());
            if (runner != null)
            {
                runner.disconnect();
            }
            // TODO: what is an appropriate error code here?
            System.exit(1);
        }
        finally
        {
            runner.checkEnd();
        }
        // Note that the program will carry on running until all the streams
        // are closed
    }
    
}
