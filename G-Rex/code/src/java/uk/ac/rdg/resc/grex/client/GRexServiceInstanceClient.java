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
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
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
    private int exitCode;  // The exit code from the remote service
    
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
    
    private Thread statusUpdater;
    private List<Thread> fileDownloadThreads = new ArrayList<Thread>();
    
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
        PostMethod setupJob = new PostMethod(this.url + "/setup");
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
        
        // Upload the data to standard input.  Note that we are doing this before
        // we start the job because we are not yet supporting interactive jobs.
        if (this.stdinSource != null)
        {
            this.uploadStdin();
        }
        
        // Now start the service instance
        PostMethod startJob = new PostMethod(this.url + "/control");
        startJob.setParameter("operation", "start");
        this.instanceState = this.serviceClient.executeMethod(startJob,
            InstanceResponse.class);
        
        // Start a regular process of polling the server for status updates
        // and discovering new output files to download
        this.statusUpdater = new StatusUpdater();
        this.statusUpdater.start();
    }
    
    /**
     * Uploads data to the standard input of the service.  We have to do this in
     * chunks because we don't know in advance how big the standard input stream
     * will be.
     * @todo: this method is not very efficient because we will parse the XML
     * response with every chunk we upload.
     */
    private void uploadStdin() throws IOException, GRexException
    {
        log.debug("Uploading data to standard input");
        int len;
        byte[] buf = new byte[8192];
        long totalSize = 0;
        while ((len = this.stdinSource.read(buf)) >= 0)
        {
            if (len > 0)
            {
                // We have to copy the data into a new array because
                // ByteArrayPartSource will use the whole provided data buffer
                byte[] data = new byte[len];
                System.arraycopy(buf, 0, data, 0, len);
                // We still use the setup endpoint
                PostMethod uploadStdin = new PostMethod(this.url + "/setup");
                PartSource dataSource = new ByteArrayPartSource(AbstractJobRunner.STDIN, data);
                Part dataPart = new FilePart(AbstractJobRunner.STDIN, dataSource);
                MultipartRequestEntity mre = new MultipartRequestEntity(new Part[]{dataPart},
                    uploadStdin.getParams());
                uploadStdin.setRequestEntity(mre);
                this.instanceState = this.serviceClient.executeMethod(uploadStdin,
                    InstanceResponse.class);
                totalSize += len;
            }
        }
        log.debug("Completed upload of " + totalSize + " bytes of data to standard input");
    }
    
    /**
     * This method waits (blocks) until the service instance has completed running
     * and all the files have been downloaded. This method must be called
     * <i>after</i> start().
     * @return the exit code from the service instance.
     */
    public int waitUntilComplete()
    {
        log.debug("Waiting for service instance to complete");
        // Wait for the status updater thread to complete: when this happens
        // the job has finished.
        waitThread(this.statusUpdater);
        // Now wait for all the file downloader threads to finish
        // We know that no more file downloader threads will be created after
        // the status updater thread has finished.
        for (Thread thread : this.fileDownloadThreads)
        {
            waitThread(thread);
        }
        log.debug("Service instance complete");
        // The exit code will have been set from the status updater thread
        return this.exitCode;
    }
        
    /**
     * Waits until a thread has finished, swallowing all InterruptedExceptions
     */
    private static void waitThread(Thread thread)
    {
        log.debug("Waiting for thread " + thread.getName() + " to finish");
        boolean finished = false;
        while (!finished)
        {
            try
            {
                thread.join();
                finished = true;
            }
            catch(InterruptedException ie)
            {
                log.debug("Thread " + thread.getName() + " interrupted");
            }
        }
        log.debug("Thread " + thread.getName() + " finished");
    }
    
    /**
     * Thread that polls the server at regular intervals for updates to status,
     * saving the results in the <code>instanceState</code> field.
     */
    private class StatusUpdater extends Thread
    {
        public StatusUpdater()
        {
            super("status-updater");
        }
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
                            // TODO: get the threads from a limited pool to prevent
                            // downloading of too many files at once?
                            Thread downloader = new FileDownloader(baseUrl, outFile.getRelativePath());
                            fileDownloadThreads.add(downloader);
                            downloader.start();
                        }
                    }
                    
                    if (instanceState.getState().meansFinished())
                    {
                        // TODO: getExitCode() could return null, but this would
                        // be an internal error.  Code defensively for this?
                        exitCode = instanceState.getExitCode();
                    }
                    else
                    {
                        // Wait for the required time before getting the next update
                        try { Thread.sleep(updateIntervalMs); } catch (InterruptedException ie) {}
                    }
                    
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
            super("download-" + relativePath);
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
                        // Make the directory(-ies) to contain the output file
                        fout.getCanonicalFile().getParentFile().mkdirs();
                        out = new FileOutputStream(fout);
                    }
                    
                    // Now read the contents of the stream
                    int len;
                    byte[] buf = new byte[1024];
                    while ((len = in.read(buf)) >= 0)
                    {
                        out.write(buf, 0, len);
                        // Make sure the standard streams are kept up to date
                        if (out == System.out || out == System.err)
                        {
                            out.flush();
                        }
                    }
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
                    // Don't close the standard streams
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
