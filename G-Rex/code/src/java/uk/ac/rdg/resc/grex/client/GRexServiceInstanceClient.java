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

    // String preceding relative path in names of downloader
    // threads.
    public static final String DOWNLOADER_PREFIX = "download-";
    
    
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
    
    // Will store all the downloader threads that have been created
    private List<Thread> fileDownloadThreads = new ArrayList<Thread>();
    
    // Will store the files that are currently being downloaded
    private List<String> filesBeingDownloaded = new ArrayList<String>();

    /* Will store the files that have already been downloaded. This is not
     * used at the moment and is probably not necessary at all. IF the status
     * updater thread (launched by job runner) does not check this then there
     * is a small chance of a file being downloaded twice */
    private List<String> filesAlreadyDownloaded = new ArrayList<String>();

    // Will store the files that failed to download properly.  Not populated
    // at the moment, so list stays empty.
    private List<String> filesDownloadFailed = new ArrayList<String>();

    // Will store the files that failed to delete properly
    //private List<String> filesDeleteFailed = new ArrayList<String>();

    // Will store the files that are not accounted for when the service
    // instance has finished
    private List<String> filesNotAccountedFor = new ArrayList<String>();
    
    
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
        log.debug("Got post method for setup = " + setupJob.toString());
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
        log.debug("About to set up job");
        MultipartRequestEntity mre = new MultipartRequestEntity(partsArray,
            setupJob.getParams());
        setupJob.setRequestEntity(mre);
        log.debug("About to check status");
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
        log.debug("Got post method for starting job = " + startJob.toString());
        log.debug("About to start job");
        startJob.setParameter("operation", "start");
        log.debug("About to check status");
        this.instanceState = this.serviceClient.executeMethod(startJob,
            InstanceResponse.class);
        
        // Start a regular process of polling the server for status updates
        // and discovering new output files to download
        log.debug("About to launch status updater");
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
                // We still use the setup.action endpoint
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
        
        /* The StatusUpdater now waits for downloads to complete.
         /*
        for (Thread thread : this.fileDownloadThreads)
        {
            waitThread(thread);
        }
         */
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
            try
            {
                // Get maximum number of simultaneous downloads from service
                // client
                int maxSimultaneousDownloads = serviceClient.getMaxSimultaneousDownloads();
                long numFilesDownloaded=0;
                
                log.debug("About to go into loop checking status every " + updateIntervalMs + " milliseconds");
                do
                {
                    // Method to get the latest information about the service instance
                    // from the server
                    GetMethod getStatus = new GetMethod(url + ".xml");
                    instanceState = serviceClient.executeMethod(getStatus,
                        InstanceResponse.class);
                    
                    // Look through the list of output files and kick off a thread
                    // to download each new one
                    String baseUrl = instanceState.getOutputFilesBaseUrl();
                    long bytesServer=0;
                    long bytesClient=0;
                    long bytesToDownload=0;
                    
                    if (instanceState.getOutputFiles().size() > 0) for (OutputFile outFile : instanceState.getOutputFiles())
                    {                        
                        if (outFile.isReadyForDownload()) {
                            
                            // Start download thread for this file if necessary
                            //if (!filesBeingDownloaded.contains(outFile.getRelativePath()) &&
                            //!filesAlreadyDownloaded.contains(outFile.getRelativePath())) {
                            if (!filesBeingDownloaded.contains(outFile.getRelativePath())) {
                                if (filesBeingDownloaded.size() < maxSimultaneousDownloads) {
                                    /* Calculate difference between size of file on server
                                    * and size of file at client */
                                    File fout = new File(outFile.getRelativePath());                                              
                                    bytesClient = fout.getCanonicalFile().length();
                                    bytesServer = outFile.getFileLengthBytes();
                                    bytesToDownload=bytesServer-bytesClient;
                                    /*
                                    log.debug("File " + outFile.getRelativePath() + ": Size at client = "
                                        + bytesClient + ", size at server = " + bytesServer
                                        + ", bytes to download = " + bytesToDownload);*/
                            
                                    Thread downloader = new FileDownloader(baseUrl, outFile, bytesToDownload, this);
                                    fileDownloadThreads.add(downloader);
                                    downloader.start();
                                    
                                    // Update file lists
                                    filesBeingDownloaded.add(outFile.getRelativePath());
                                    if (filesNotAccountedFor.contains(outFile.getRelativePath()))
                                        filesNotAccountedFor.remove(outFile.getRelativePath());
                                    log.debug("Started downloading from " + outFile.getRelativePath() +
                                        ". Number of files being downloaded is now " + filesBeingDownloaded.size());
                                    
                                }
                                else {
                                    if (!filesNotAccountedFor.contains(outFile.getRelativePath()))
                                        filesNotAccountedFor.add(outFile.getRelativePath());
                                }
                            }
                        }                        
                    }
                    
                    //
                    // Find out if any downloader threads have finished, and
                    // update lists if necessary
                    //
                    List<Thread> finishedDownloadThreads = new ArrayList<Thread>();
                    for (Thread thread : fileDownloadThreads) {
                        // Extract relative path from thread name
                        String relativePath = thread.getName().substring(DOWNLOADER_PREFIX.length());
                        
                        // Update file lists if the thread has finished, or if the instance
                        // has finished and the thread is for stdout or stderr. Downloaders
                        // for stdin and stdout can sometimes keep going indefinitely.
                        if (!thread.isAlive() || (instanceState.getState().meansFinished() &&
                                (relativePath.contains(AbstractJobRunner.STDOUT) ||
                                relativePath.contains(AbstractJobRunner.STDERR)))) {
                            finishedDownloadThreads.add(thread);
                            filesBeingDownloaded.remove(relativePath);
                            //filesAlreadyDownloaded.add(relativePath);
                            numFilesDownloaded++;
                            log.debug("Finished downloading from " + relativePath +
                                    ". Total No. files downloaded is now " + numFilesDownloaded);
                        }
                        else if (instanceState.getState().meansFinished() &&
                                (relativePath.contains(AbstractJobRunner.STDOUT) ||
                                relativePath.contains(AbstractJobRunner.STDIN)) ) {
                            // ??
                        }
                                
                    }
                    // Now remove the threads that have finished from the list of
                    // active threads
                    for (Thread thread : finishedDownloadThreads) {
                        fileDownloadThreads.remove(thread);
                    }
                    

                    if (instanceState.getState().meansFinished())
                    {
                        // TODO: getExitCode() could return null, but this would
                        // be an internal error.  Code defensively for this?
                        if ( instanceState.getExitCode() == null ) {
                            log.debug("getExitCode() returned null");
                        }
                        else exitCode = instanceState.getExitCode();
                    
                        /* Files are unaccounted for if they are available for download
                         * but no attempt to download them had yet been made. This
                         * situation can arise because there is a limit to the number of
                         * downloads that can take place simultaneously. */
                        log.debug("Instance " + instanceState.getId() + " has finished.");
                        if (filesNotAccountedFor.size()>0) {
                            log.debug(filesNotAccountedFor.size() + " files are not accounted for:");
                            //for (String relativePath : filesNotAccountedFor) {
                            //    log.debug(relativePath);
                            //}
                        }
                        else {
                            log.debug("The following " + filesBeingDownloaded.size() + " files are still being downloaded:");
                            for (String relativePath : filesBeingDownloaded) {
                                log.debug(relativePath);
                            }
                        }
                    }
                    
                    // Wait for the required time before getting the next update
                    try {
                        Thread.sleep(updateIntervalMs);
                    } catch (InterruptedException ie) {}
                    
                /* We must keep going if the instance has not yet finished, if there are still files
                 * being downloaded or if there are files still unnacounted for. */
                } while (!instanceState.getState().meansFinished() || filesBeingDownloaded.size()>0
                        || filesNotAccountedFor.size()>0);
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
        private long bytesToDownload;
        private OutputFile outFile;
        private StatusUpdater updater;
        
        public FileDownloader(String baseUrl, OutputFile outFile,
                long bytes, StatusUpdater updater)
        {
            super(DOWNLOADER_PREFIX + outFile.getRelativePath());
            this.baseUrl = baseUrl;
            this.outFile = outFile;
            this.relativePath = outFile.getRelativePath();
            this.bytesToDownload = bytes; /* Not using this yet */
            this.updater = updater;
        }
        
        public OutputFile getOutputFile() {
            return this.outFile;
        }
        
        public void run()
        {
            String fileUrl = this.baseUrl + this.relativePath;
            //log.debug("Downloading from " + fileUrl);
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
                        fout.getCanonicalFile().getParentFile().mkdirs();
                        out = new FileOutputStream(fout);
                    }
                    
                    // Now read the contents of the stream
                    int len, bufsize=1024;
                    byte[] buf = new byte[bufsize];
                    
                    while ((len = in.read(buf)) >= 0)
                    {
                        out.write(buf, 0, len);
                        // Make sure the standard streams are kept up to date
                        if (out == System.out || out == System.err)
                        {
                            out.flush();
                        }
                    }
                    //log.debug("Finished downloading from " + fileUrl +
                    //        ", checksum at server is " + outFile.getCheckSum());
                    
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
