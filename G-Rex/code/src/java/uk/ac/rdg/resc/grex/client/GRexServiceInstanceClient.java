/*
 * Copyright (c) 2007 The University of Reading
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

package uk.ac.rdg.resc.grex.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.rdg.resc.grex.exceptions.GRexException;
import uk.ac.rdg.resc.grex.server.AbstractJobRunner;

/**
 * An object that is used to manipulate a particular instance of a grid service.
 * Contains methods to upload input files, start the service, download the output
 * and more.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class GRexServiceInstanceClient
{
    private static final Log log = LogFactory.getLog(GRexServiceInstanceClient.class);
 
    /**
     * Default interval in milliseconds between polling the server for updates
     * to status
     */
    public static final long DEFAULT_UPDATE_INTERVAL_MS = 2000;
    
    private String url;
    private GRexServiceClient serviceClient;
    private long updateIntervalMs = DEFAULT_UPDATE_INTERVAL_MS;
    private InstanceResponse instanceState;  // The state of the instance as
                                             // read from the server
    
    /**
     * Map of parameter names and values that we will set on the remote service
     * instance
     */
    private Map<String, String> params = new HashMap<String, String>();
    
    /**
     * Contains the files that must be uploaded to the remote server before the
     * service instance is started.  The keys are File objects and the values are
     * the paths of the files on the server, relative to the working directory of 
     * the instance
     */
    private Map<File, String> filesToUpload = new HashMap<File, String>();
    /**
     * The stream that represents the standard input.  This will be streamed
     * to the server once the instance has been started.
     */
    private InputStream stdinSource = null;
    /**
     * The OutputStream that will be used to write the standard output from the
     * remote service instance.
     */
    private OutputStream stdoutDestination = System.out;
    /**
     * The OutputStream that will be used to write the standard error stream from
     * the remote service instance.
     */
    private OutputStream stderrDestination = System.err;
    
    /**
     * Creates a new instance of GRexServiceInstanceClient
     * @param url Full URL to this service instance, e.g.
     * "http://myserver.com/G-Rex/helloworld/instances/3".  Note there is no
     * trailing slash (to allow URLs like ".../3.html" and ".../3/wd/input.txt"
     * @param serviceClient GRexServiceClient (used to make Http calls)
     */
    GRexServiceInstanceClient(String url, GRexServiceClient serviceClient)
    {
        this.url = url;
        this.serviceClient = serviceClient;
    }
    
    /**
     * Sets the name and value of a parameter that will be set on the remote
     * service instance, prior to the instance being started.  This performs
     * no checks on whether the name of the parameter is valid (TODO).
     */
    public void setParameter(String name, String value)
    {
        this.params.put(name, value);
    }
    
    /**
     * Adds a file to the list of files that must be uploaded before the service
     * instance is started (this method does not actually upload the file).
     * @param file The File to upload
     * @param pathOnServer The destination location of the file on the server,
     * relative to the working directory of the instance.
     * @throws FileNotFoundException if the file does not exist
     */
    public void addFileToUpload(File file, String pathOnServer)
        throws FileNotFoundException
    {
        if (!file.exists())
        {
            throw new FileNotFoundException(file.getPath());
        }
        this.filesToUpload.put(file, pathOnServer);
    }
    
    /**
     * Adds a file to the list of files that must be uploaded before the service
     * instance is started (this method does not actually upload the file).
     * @param file path to the file to upload
     * @param pathOnServer The destination location of the file on the server,
     * relative to the working directory of the instance.
     * @throws FileNotFoundException if the file does not exist
     */
    public void addFileToUpload(String filePath, String pathOnServer)
        throws FileNotFoundException
    {
        this.addFileToUpload(new File(filePath), pathOnServer);
    }
    
    /**
     * Adds a file to the list of files that must be uploaded before the service
     * instance is started (this method does not actually upload the file).
     * Exactly equivalent to addFileToUpload(file, file.getName()).
     * @param file The File to upload (the file will have the same name on the
     * server inside the working directory of the instance)
     * @throws FileNotFoundException if the file does not exist
     */
    public void addFileToUpload(File file) throws FileNotFoundException
    {
        this.addFileToUpload(file, file.getName());
    }
    
    /**
     * Adds a file to the list of files that must be uploaded before the service
     * instance is started (this method does not actually upload the file).
     * Exactly equivalent to addFileToUpload(new File(filePath)).
     * @param file The path to the File to upload (the file will have the same
     * name on the server and will appear in the working directory of the instance)
     * @throws FileNotFoundException if the file does not exist
     */
    public void addFileToUpload(String filePath) throws FileNotFoundException
    {
        this.addFileToUpload(new File(filePath));
    }
    
    /**
     * Sets the InputStream that will provide data for the standard input
     * stream of the remote service instance.
     */
    public void setStdinSource(InputStream in)
    {
        this.stdinSource = in;
    }
    
    /**
     * Sets the OutputStream that will be used to write data coming from the
     * standard output of the remote service instance.
     */
    public void setStdoutDestination(OutputStream out)
    {
        this.stdoutDestination = out;
    }
    
    /**
     * Sets the OutputStream that will be used to write data coming from the
     * standard output of the remote service instance.
     */
    public void setStderrDestination(OutputStream err)
    {
        this.stderrDestination = err;
    }
    
    public String getUrl()
    {
        return this.url;
    }

    /**
     * Sets the interval in milliseconds between polling the server for status
     * updates.
     */
    public void setUpdateIntervalMs(long updateIntervalMs)
    {
        this.updateIntervalMs = updateIntervalMs;
    }
    
    /**
     * Sets the parameters of the service, uploads the required input files,
     * starts the service running, then starts threads to monitor the status
     * of the service and download the output files.
     */
    public void start() throws IOException, GRexException
    {
        // Setup the job by setting the parameters and uploading the input files
        PostMethod setupJob = new PostMethod(this.url + "/setup.action");
        List<Part> parts = new ArrayList<Part>();
        // Add the parameters
        for (String paramName : this.params.keySet())
        {
            parts.add(new StringPart(paramName, this.params.get(paramName)));
        }
        // Add the input files
        for (File fileToUpload : this.filesToUpload.keySet())
        {
            String pathOnServer = this.filesToUpload.get(fileToUpload);
            parts.add(new FilePart(pathOnServer, fileToUpload));
        }
        Part[] partsArray = parts.toArray(new Part[0]);
        MultipartRequestEntity mre = new MultipartRequestEntity(partsArray,
            setupJob.getParams());
        setupJob.setRequestEntity(mre);
        this.instanceState = this.serviceClient.executeMethod(setupJob,
            InstanceResponse.class);
        
        // Now start the service instance
        PostMethod startJob = new PostMethod(this.url + "/control.action");
        startJob.setParameter("operation", "start");
        this.instanceState = this.serviceClient.executeMethod(startJob,
            InstanceResponse.class);
        
        // Start a regular process of polling the server for status updates
        // and discovering new output files to download
        new StatusUpdater().start();
    }
    
    /**
     * Thread that polls the server at regular intervals for updates to status,
     * saving the results in the <code>instanceState</code> field.
     */
    private class StatusUpdater extends Thread
    {
        public void run()
        {
            // Will store the files that are currently being downloaded
            List<String> filesBeingDownloaded = new ArrayList<String>();
            // Method to get the latest information about the service instance
            // from the server
            GetMethod getStatus = new GetMethod(url + ".xml");
            try
            {
                do
                {
                    instanceState = serviceClient.executeMethod(getStatus,
                        InstanceResponse.class);
                    
                    // Look through the list of output files and kick off a thread
                    // to download each new one
                    String baseUrl = instanceState.getOutputFilesBaseUrl();
                    for (OutputFile outFile : instanceState.getOutputFiles())
                    {
                        if (outFile.isReadyForDownload() &&
                            !filesBeingDownloaded.contains(outFile.getRelativePath()))
                        {
                            filesBeingDownloaded.add(outFile.getRelativePath());
                            new FileDownloader(baseUrl, outFile.getRelativePath()).start();
                        }
                    }
                    
                    // Wait for the required time before getting the next update
                    try { Thread.sleep(updateIntervalMs); } catch (InterruptedException ie) {}
                    
                } while (!instanceState.getState().meansFinished());
            }
            catch(Exception e)
            {
                e.printStackTrace();
                log.error("Error getting status of instance", e);
                // TODO: what do we do here?
            }
        }
    }
    
    /**
     * Thread that handles the downloading of an output file from the server.
     */
    private class FileDownloader extends Thread
    {
        private String baseUrl;
        private String relativePath;
        
        public FileDownloader(String baseUrl, String relativePath)
        {
            this.baseUrl = baseUrl;
            this.relativePath = relativePath;
        }
        
        public void run()
        {
            String fileUrl = this.baseUrl + this.relativePath;
            log.debug("Downloading from " + fileUrl);
            InputStream in = null;
            OutputStream out = null;
            GetMethod downloader = new GetMethod(fileUrl);
            try
            {
                int status = serviceClient.getHttpClient().executeMethod(downloader);
                if (status == HttpServletResponse.SC_OK)
                {
                    in = downloader.getResponseBodyAsStream();
                    // Set the output stream
                    if (this.relativePath.equals(AbstractJobRunner.STDOUT))
                    {
                        out = stdoutDestination;
                    }
                    else if (this.relativePath.equals(AbstractJobRunner.STDERR))
                    {
                        out = stderrDestination;
                    }
                    else
                    {
                        File fout = new File(this.relativePath);
                        fout.getParentFile().mkdirs();
                        out = new FileOutputStream(fout);
                    }
                    
                    // Now read the contents of the stream
                    int len;
                    byte[] buf = new byte[1024];
                    while ((len = in.read(buf)) >= 0)
                    {
                        out.write(buf, 0, len);
                    }
                    out.flush();
                    log.debug("Finished downloading from " + fileUrl);
                }
                else
                {
                    // TODO: do something better here
                    log.error("Got status " + status + " from " + fileUrl);
                }
            }
            catch(IOException ioe)
            {
                // TODO: do something more friendly here
                ioe.printStackTrace();
                log.error("Error downloading from " + fileUrl, ioe);
            }
            finally
            {
                try
                {
                    if (in != null) in.close();
                    if (out != null && out != System.out && out != System.err) out.close();
                }
                catch (IOException ioe)
                {
                    // Unlikely to happen and we don't really care anyway
                }
                downloader.releaseConnection();
            }
        }
    }
    
}
