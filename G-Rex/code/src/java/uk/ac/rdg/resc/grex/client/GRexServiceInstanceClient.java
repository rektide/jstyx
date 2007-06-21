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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.rdg.resc.grex.exceptions.GRexException;

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
    
    private String url;
    private GRexServiceClient serviceClient;
    
    /**
     * Map of parameter names and values that we will set on the remote service
     * instance
     */
    private Map<String, String> parameters = new HashMap<String, String>();
    
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
     * The PrintStream that will be used to write the standard output from the
     * remote service instance.
     */
    private PrintStream stdoutDestination = null;
    /**
     * The PrintStream that will be used to write the standard error stream from
     * the remote service instance.
     */
    private PrintStream stderrDestination = null;
    
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
     * Gets the status of this service instance
     * @throws GRexException if there was an error reading the information from 
     * the server (e.g. the user does not have permissions to read the config info)
     * @throws IOException if an i/o error occurred, e.g. cannot connect to server 
     */
    public Object getStatus() throws IOException, GRexException
    {
        String statusUrl = this.getUrl() + "/status.xml";
        log.debug("Getting status information from " + statusUrl);
        GetMethod get = new GetMethod(statusUrl);
        return this.serviceClient.executeMethod(get, Object.class);
    }
    
    /**
     * Sets the name and value of a parameter that will be set on the remote
     * service instance, prior to the instance being started.  This performs
     * no checks on whether the name of the parameter is valid (TODO).
     */
    public void setParameter(String name, String value)
    {
        this.parameters.put(name, value);
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
     * Exactly equivalent to addFileToUpload(file, file.getPath()).
     * @param file The File to upload (the file will have the same path on the
     * server, relative to the working directory of the instance)
     * @throws FileNotFoundException if the file does not exist
     */
    public void addFileToUpload(File file) throws FileNotFoundException
    {
        this.addFileToUpload(file, file.getPath());
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
     * Sets the PrintStream that will be used to write data coming from the
     * standard output of the remote service instance.
     */
    public void setStdoutDestination(PrintStream ps)
    {
        this.stdoutDestination = ps;
    }
    
    /**
     * Sets the PrintStream that will be used to write data coming from the
     * standard output of the remote service instance.
     */
    public void setStderrDestination(PrintStream ps)
    {
        this.stderrDestination = ps;
    }
    
    /**
     * Sets the parameters of the service, uploads the required input files,
     * starts the service running, starts the redirection of the standard streams,
     * waits for the service to finish, then downloads the output.
     * @todo Should this return immediately (doing all the stuff in threads
     * and reporting progress via listeners)?
     */
    public void start()
    {
        // TODO
    }
    
    /**
     * Uploads a file to the server
     */
    public void uploadFile() throws IOException, GRexException
    {
        PostMethod post = new PostMethod("url");
        //MultipartRequestEntity mre = new MultipartRequestEntity();
        //post.setRequestEntity(mre);
        
    }
    
    public String getUrl()
    {
        return this.url;
    }

    public Map<File, String> getFilesToUpload()
    {
        return filesToUpload;
    }
    
}
