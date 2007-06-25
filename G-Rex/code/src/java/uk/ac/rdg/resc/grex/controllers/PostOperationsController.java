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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import uk.ac.rdg.resc.grex.config.GRexConfig;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.config.User;
import uk.ac.rdg.resc.grex.db.GRexServiceInstancesStore;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
import uk.ac.rdg.resc.grex.exceptions.GRexException;
import uk.ac.rdg.resc.grex.server.JobRunnerFactory;

/**
 * Controller that handles all the POST operations (i.e. requests for information
 * that modify the state of the server)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class PostOperationsController extends MultiActionController
{
    private static final Log log = LogFactory.getLog(PostOperationsController.class);
    
    /**
     * Configuration information for this G-Rex server.
     */
    private GRexConfig config;
    /**
     * Store of service instances.
     */
    private GRexServiceInstancesStore instancesStore;
    /**
     * Factory for JobRunner objects
     */
    private JobRunnerFactory jobRunnerFactory;
    
    /**
     * Creates a new instance of a particular service
     */
    public ModelAndView createNewServiceInstance(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // Get the user that is logged in
        User loggedInUser = (User)SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        
        // Find the name of the service that the user is interested in.  The URL
        // pattern is /G-Rex/serviceName/clone.action
        String serviceName = request.getRequestURI().split("/")[2];
        
        // Check that the service exists
        GridServiceConfigForServer gs = this.config.getGridServiceByName(serviceName);
        if (gs == null)
        {
            throw new GRexException("There is no service called " + serviceName);
        }
        
        // Check that the user is allowed to create a new instance
        if (!gs.canBeAccessedBy(loggedInUser))
        {
            throw new GRexException("User " + loggedInUser.getUsername() +
                " does not have permission to create a new instance of " + serviceName);
        }
        
        // Check that the user has set "operation=create"
        if (request.getParameter("operation") == null ||
            !request.getParameter("operation").equals("create"))
        {
            throw new GRexException("Illegal command POSTed to " + request.getRequestURI());
        }
        
        // Create a new instance of the service
        GRexServiceInstance newInstance = new GRexServiceInstance();
        newInstance.setServiceName(serviceName);
        if (request.getParameter("description") != null)
        {
            newInstance.setDescription(request.getParameter("description"));
        }
        newInstance.setOwner(loggedInUser.getUsername());
        newInstance.setGroup(loggedInUser.getDefaultGroup().getName());
        // Set the base of the URL to the new instance (allows for the fact that
        // in future versions the instance may be hosted on a remote machine,
        // although I haven't really thought that through... ;-)
        // The full url will be retrievable through getUrl() when the instance
        // is added to the store (and hence the instance id is known)
        newInstance.setBaseUrl(request.getRequestURL().toString()
            .replaceFirst("clone.action", "instances/"));
        
        // Add the instance to the store.  This also creates the unique ID of
        // the instance and creates the working directory for the instance
        this.instancesStore.addServiceInstance(newInstance,
            gs.getWorkingDirectory());
        
        if (request.getParameter("source") != null &&
            request.getParameter("source").equals("web"))
        {
            // We've come from the web so display an HTML page that redirects
            // to the information page for the instance.  We do the redirect
            // to prevent the user from accidentally creating more instances by
            // pressing refresh
            return new ModelAndView("newInstanceCreated_html", "instance", newInstance);
        }
        else
        {
            // We didn't come from the web, so we assume we've done this programmatically
            // (i.e. via a REST web service call) and we'll return XML to the client
            // We just show the information about the new instance
            return new ModelAndView("instance_xml", "instance", newInstance);
        }
    }
    
    /**
     * Sets up a service instance by setting parameter values, uploading input
     * files, etc.  This method can be called many times: each invocation will
     * overwrite any existing data.
     */
    public ModelAndView setupServiceInstance(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        User loggedInUser = (User)SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        
        // Work out which service instance we're interested in.  The URL
        // pattern is /G-Rex/serviceName/instances/instanceId/setup.action
        String serviceName = request.getRequestURI().split("/")[2];
        String instanceIdStr = request.getRequestURI().split("/")[4];
        
        // TODO: repetitive of code in GetOperationsController.showServiceInstance(): refactor?
        try
        {
            int instanceId = Integer.parseInt(instanceIdStr);
            // Retrieve the instance object from the store
            GRexServiceInstance instance = this.instancesStore.getServiceInstanceById(instanceId);
            // Check that the instance exists and that the service names match
            if (instance == null || !instance.getServiceName().equals(serviceName))
            {
                throw new GRexException("There is no instance of " + serviceName + 
                    " with id " + instanceIdStr);
            }
            if (!instance.canBeModifiedBy(loggedInUser))
            {
                throw new GRexException("User " + loggedInUser.getUsername() +
                    " does not have permission to modify instance "
                    + instance.getId() + " of service " + serviceName);
            }
            
            // Create a new file upload handler
            // See http://jakarta.apache.org/commons/fileupload/streaming.html
            ServletFileUpload upload = new ServletFileUpload();            
            FileItemIterator iter = upload.getItemIterator(request);
            while (iter.hasNext())
            {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                InputStream stream = item.openStream();
                if (item.isFormField())
                {
                    instance.setParameter(name, Streams.asString(stream));
                }
                else
                {
                    // This is a file to upload and we can now process the input
                    // stream.  In future we could send this stream to a database,
                    // or to a remote file store.
                    log.debug("Detected upload of file called " + name);
                    // Make sure we only save files in the working directory itself
                    // (otherwise a client could set the name to "..\..\123\wd\foo.dat"
                    // and overwrite data in another instance
                    File targetFile = new File(instance.getWorkingDirectory(), name);
                    if (!isChild(instance.getWorkingDirectory(), targetFile))
                    {
                        log.error("Not allowed to write a file to " + targetFile.getCanonicalPath());
                        throw new GRexException("Not allowed to write a file to this location");
                    }
                    // Create the directory to hold this input file if it doesn't
                    // already exist
                    if (!targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs())
                    {
                        log.error("Could not create directory for " + targetFile.getPath());
                        throw new GRexException("Internal error: could not create directory" +
                            " for input file " + name);
                    }
                    // Copy the input stream to the target file
                    log.debug("Saving file " + name + " to " + targetFile.getPath());
                    // TODO: monitor progress somehow?
                    OutputStream fout = new FileOutputStream(targetFile);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = stream.read(buf)) > 0)
                    {
                        fout.write(buf, 0, len);
                    }
                    stream.close();
                    fout.close();
                }
            }
            
            // Update the service instance object in the store
            this.instancesStore.updateServiceInstance(instance);
            
            // Return a document describing the new instance
            // TODO: do something else if we've come from a web page
            return new ModelAndView("instance_xml", "instance", instance);
        }
        catch(NumberFormatException nfe)
        {
            // thrown by Integer.parseInt(instanceIdStr)
            throw new GRexException("There is no instance of " + serviceName + 
                " with id " + instanceIdStr);
        }
    }
    
    /**
     * Checks to see if one file is contained within a given directory
     * (as a direct child or in a sub-directory), by comparing their canonical
     * paths.
     * @throws IOException (might be thrown when calculating the canonical path)
     */
    private static boolean isChild(String parent, File child)
        throws IOException
    {
        File parentFile = new File(parent);
        if (parentFile.isDirectory())
        {
            String parentPath = parentFile.getCanonicalPath();
            String childPath = child.getCanonicalPath();
            return childPath.startsWith(parentPath);
        }
        return false;
    }
    
    /**
     * This will be used by the Spring framework to inject the config object
     */
    public void setGrexConfig(GRexConfig config)
    {
        this.config = config;
    }
    
    /**
     * This will be used by the Spring framework to inject an object that can
     * be used to read the service instances from a database
     */
    public void setInstancesStore(GRexServiceInstancesStore instancesStore)
    {
        this.instancesStore = instancesStore;
    }
    
    /**
     * This will be called by the Spring framework to inject an object that
     * can be used to get and create JobRunners
     */
    public void setJobRunnerFactory(JobRunnerFactory factory)
    {
        this.jobRunnerFactory = factory;
    }
}
