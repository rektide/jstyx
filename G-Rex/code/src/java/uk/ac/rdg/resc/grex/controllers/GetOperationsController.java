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

package uk.ac.rdg.resc.grex.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.config.User;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
import uk.ac.rdg.resc.grex.exceptions.GRexException;
import uk.ac.rdg.resc.grex.server.OutputFile;

// TODO: these instructions are out of date and do not belong here anymore!
/**
 * <p>Entry point for the T-Rex web application. Each HTTP method first finds the
 * resource that the user is asking for, checks permissions, then executes
 * the command.</p>
 *
 * <p>Possible resources (note that trailing slashes are ignored):</p>
 * <table border="1"><tbody><tr><th>Example URI</th><th>Resource</th><th>Formats (default in bold)</th></tr>
 * <tr><td>/T-Rex/</td><td>Root of the server: perform a GET to list the services
 * that are visible to the client (might be different for each client depending
 * on authentication).</td><td><b>XML</b>, HTML</td></tr>
 * <tr><td>/T-Rex/myservice/</td><td>Root of a service: perform a GET to get
 * information about this service (description etc)</td><td><b>XML</b>, HTML</td></tr>
 * <tr><td>/T-Rex/myservice/clone</td><td>Cloning endpoint: POST a message to this
 * endpoint to create a new instance of this service</td><td><b>XML</b> response
 * indicating success or otherwise of clone operation</td></tr>
 * <tr><td>/T-Rex/myservice/config.xml</td><td>Configuration of this service</td><td><b>XML</b></td></tr>
 * <tr><td>/T-Rex/myservice/instances</td><td>Instances of this service that
 * are visible to the user. Can be filtered (TODO) to show only running services,
 * failed services etc</td><td><b>XML</b>, HTML</td></tr>
 * <tr><td>/T-Rex/myservices/instances/0034</td><td>A particular service instance.
 * Perform a GET to obtain information about this instance (TODO: comprising what?)</td>
 * <td><b>XML</b>, HTML</td></tr>
 * <tr><td>/T-Rex/myservice/instances/0034/ctl</td><td>The control endpoint for
 * this service instance. Clients control the instance by POSTing messages to
 * start, stop and cleanup the service</td><td><b>XML</b> responses (simply
 * confirming success or otherwise)</td></tr>
 * <tr><td>/T-Rex/myservice/instances/0034/wd/</td><td>Working directory of a 
 * service instance. Perform a GET on this URI to get a listing of the entire
 * contents of a working directory</td><td><b>XML</b>, HTML</td></tr>
 * <tr><td>/T-Rex/myservice/instances/0034/wd/foo</td><td>A file in the working
 * directory of the instance. Perform a GET to retrieve the contents of the file,
 * or a PUT to replace the file.</td><td>for GET: response is the file contents.
 * For PUT, XML reponse confirming success or otherwise.</td></tr>
 *
 * </tbody></table>
 *
 * <p>Due to the way in which <code>web.xml</code> is set up, the path to the resource
 * (service, service instance etc) is held in the RequestURI property of the
 * HttpServletRequest object.  So, if the client requests the following URL:
 * <code>http://localhost:8084/T-Rex/foo/bar/?baz=34</code>, the RequestURI 
 * will be "/T-Rex/foo/bar/" and the query string will be <code>baz=34</code>.
 * (I don't know whether the getPathInfo() and getPathTranslated() methods are
 * relevant here: they always seem to return null.)</p>
 *
 * @author jdb
 * @version
 */

/**
 * Controller that handles all the GET operations (i.e. requests for information
 * that do not modify the state of the server)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class GetOperationsController extends AbstractGRexController
{
    private static final Log log = LogFactory.getLog(GetOperationsController.class);
    
    /**
     * Shows the welcome page (in response to a request for welcome.html)
     */
    public ModelAndView showWelcomePage(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        return new ModelAndView("welcomePage");
    }
    
    /**
     * Lists all the services that the user has permission to see
     */
    public ModelAndView listServices(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        User loggedInUser = (User)SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        // Create a list of the services that the user can access
        List<GridServiceConfigForServer> viewables = new ArrayList<GridServiceConfigForServer>();
        for (GridServiceConfigForServer gridService : this.config.getGridServices())
        {
            if (gridService.canBeAccessedBy(loggedInUser))
            {
                viewables.add(gridService);
            }
        }
        // The viewables object will appear in the JSPs with the name "gridservices"
        // The JSP that will be displayed will be "/WEB-INF/jsp/services_[fileExtension].jsp"
        return new ModelAndView("services_" + getFileExtension(request.getRequestURI()),
            "gridservices", viewables);
    }
    
    /**
     * Lists all the instances for the given service that the user has permissions
     * to see
     */
    public ModelAndView listInstancesForService(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        User loggedInUser = (User)SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        
        // Find the service that the user is interested in.  The URI
        // pattern is /G-Rex/serviceName/instances.[xml,html]
        GridServiceConfigForServer gs = this.getServiceConfig(request.getRequestURI());
        
        // Check that the user can access this service
        if (!gs.canBeAccessedBy(loggedInUser))
        {
            throw new GRexException("User " + loggedInUser.getUsername() +
                " does not have permission to view instances of service "
                + gs.getName());
        }
        
        // Get the list of service instance from the store, checking the
        // permissions of each one
        List<GRexServiceInstance> viewables = new ArrayList<GRexServiceInstance>();
        for (GRexServiceInstance instance : this.instancesStore
            .getServiceInstancesByServiceName(gs.getName()))
        {
            if (instance.canBeReadBy(loggedInUser))
            {
                viewables.add(instance);
            }
        }
        
        // Create the model map that will be passed to the JSPs
        Map<String, Object> modelMap = new HashMap<String, Object>();
        modelMap.put("instances", viewables);
        modelMap.put("serviceName", gs.getName());
        return new ModelAndView("instancesForService_" +
            getFileExtension(request.getRequestURI()), modelMap);
    }
    
    /**
     * Shows the configuration information for a particular service
     */
    public ModelAndView showConfigForService(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        User loggedInUser = (User)SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        
        // Find the service that the user is interested in.  The URL
        // pattern is /G-Rex/serviceName/instances.[xml,html]
        GridServiceConfigForServer gs = this.getServiceConfig(request.getRequestURI());
        
        if (!gs.canBeAccessedBy(loggedInUser))
        {
            throw new GRexException("User " + loggedInUser.getUsername() +
                " does not have permission to view configuration information for "
                + gs.getName());
        }
        return new ModelAndView("configForService_" +
            getFileExtension(request.getRequestURI()), "gridservice", gs);
    }
    
    /**
     * Show the information about a specific service instance
     */
    public ModelAndView showServiceInstance(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        User loggedInUser = (User)SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        // Find the service instance that the user is interested in.  The URL
        // pattern is /G-Rex/serviceName/instances/instanceID.[xml,html]
        GRexServiceInstance instance = this.getServiceInstance(request.getRequestURI());
        
        if (!instance.canBeReadBy(loggedInUser))
        {
            throw new GRexException("User " + loggedInUser.getUsername() +
                " does not have permission to view information for instance "
                + instance.getId() + " of service " + instance.getServiceName());
        }
        // Display the details of this instance
        return new ModelAndView("instance_" +
            getFileExtension(request.getRequestURI()), "instance", instance);
    }
    
    /**
     * Downloads an output file from this instance.  Writes the output directly
     * to the response's output stream.
     * @todo maybe adapt error codes based on the user-agent?  HTTP error codes
     * for browsers, XML error messages for custom clients?
     */
    public void downloadOutputFile(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        User loggedInUser = (User)SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        // Find the name of the service and the instance that the user is
        // interested in.  The URL pattern is
        // /G-Rex/serviceName/instances/instanceID/outputs/path/to/file
        GRexServiceInstance instance = this.getServiceInstance(request.getRequestURI());
        if (!instance.canBeReadBy(loggedInUser))
        {
            // TODO: how do we get this sent to the client, avoiding the servlet
            // container's interception?
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        String filePath = this.getFilePath(request.getRequestURI());
        File fileToDownload = new File(instance.getWorkingDirectory(), filePath);
        
        /* Check that the file exists and can be downloaded.  The client should
         *not try to download this file unless both are true.
         The file is ready to be downloaded if it is append-only, if the job
         * it is associated with has finished or if output to the file has finished.
         */
        OutputFile opFile = instance.getMasterJob().getOutputFile(filePath);
        if (opFile == null || !opFile.isReadyForDownload())
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // Set the content-length in the file header if this is not a stream, 
        // if the instance has finished or if output to the file has finished
        if (!opFile.isAppendOnly() || instance.isFinished() || opFile.isOutputFinished())
        {
            // Why won't setContentLength() accept a long integer?
            response.setContentLength((int)opFile.getLengthBytes());
        }
        // TODO: set the MIME type correctly (how?)
        
        // Now output the file to the client.  This logic works for both
        // "live" streams and static files.
        log.debug("Started reading from " + opFile.getFile().getName());
        InputStream in = null;
        OutputStream out = null;
        try
        {
            in = new FileInputStream(opFile.getFile());
            out = response.getOutputStream();
            byte[] buf = new byte[1024];
            int len, maxlen;
            boolean done = false;
            boolean isOutputFinished = false;
            boolean reportedError=false;
            do // Loop until the file is finished and the file has been read completely
            {
                // Make sure our view of the instance is up to date
                instance = this.instancesStore.getServiceInstanceById(instance.getId());
                //done = instance.isFinished();
                
                /*
                 * Check list of finished files
                 */
                //SortedSet<OutputFile> outputFinished = this.jobRunnerFactory.getRunnerForInstance(instance).getOutputFinished();
                //isOutputFinished = outputFinished.contains(opFile);
                //isOutputFinished = this.jobRunnerFactory.getRunnerForInstance(instance).isOutputFinished(opFile);
                isOutputFinished = opFile.isOutputFinished();
                if (isOutputFinished ==true)
                    log.debug("Output to " + opFile.getFile().getName() + " has finished. Reading to end of file...");
                                
                done = ( isOutputFinished || instance.isFinished() );
                
                // Even if the file or the instance have finished we make sure we've read
                // the entire file
                while ((len = in.read(buf)) >= 0)
                {
                    out.write(buf, 0, len);
                    out.flush();
                }
                
                // We've now reached EOF, but if the instance is still running,
                // we'll pause and carry on
                if (!done)
                {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) {}
                }
                else {
                    log.debug("Finished reading " + opFile.getFile().getName());
                }
            } while (!done);
            
            /* Now delete the file.  Deletion should really be initiated by the client */
            //log.debug("Deleting file " + opFile.getFile().getName());
            if (!opFile.getFile().delete())
                    log.error("Error deleting file " + opFile.getFile().getName());
        }
        catch(FileNotFoundException fnfe)
        {
            // TODO: how do we get this sent to the client, avoiding the servlet
            // container's interception?
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        catch(IOException ioe)
        {
            // This is most likely to happen if the client disconnects unexpectedly
            log.error("Error downloading file " + filePath, ioe);
            // TODO: report this to the client?
        }
        finally
        {
            try
            {
                if (in != null) in.close();
                if (out != null) out.close();
            }
            catch (IOException ioe)
            {
                // Unlikely to happen and we don't really care anyway
            }
        }
    }
}
