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
import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.config.Parameter;
import uk.ac.rdg.resc.grex.config.User;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
import uk.ac.rdg.resc.grex.db.Job;
import uk.ac.rdg.resc.grex.exceptions.GRexException;
import uk.ac.rdg.resc.grex.exceptions.InstanceNotReadyException;
import uk.ac.rdg.resc.grex.server.AbstractJobRunner;
import uk.ac.rdg.resc.grex.server.JobRunner;

/**
 * Controller that handles all the POST operations (i.e. requests for information
 * that modify the state of the server)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class PostOperationsController extends AbstractGRexController
{
    private static final Log log = LogFactory.getLog(PostOperationsController.class);
    
    /**
     * The name of the parameter that the user can set to give the number of
     * sub-jobs in a service instance
     */
    private static final String NUMSUBJOBS_PARAMETER_NAME = "numSubJobs";
    
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
        // pattern is /G-Rex/serviceName/clone
        GridServiceConfigForServer gs = this.getServiceConfig(request.getRequestURI());
        
        // Check that the user is allowed to create a new instance
        if (!gs.canBeAccessedBy(loggedInUser))
        {
            throw new GRexException("User " + loggedInUser.getUsername() +
                " does not have permission to create a new instance of " + gs.getName());
        }
        
        // Check that the user has set "operation=create"
        if (request.getParameter("operation") == null ||
            !request.getParameter("operation").equals("create"))
        {
            throw new GRexException("Illegal command POSTed to " + request.getRequestURI());
        }
        
        // Create a new instance of the service
        GRexServiceInstance newInstance = new GRexServiceInstance();
        newInstance.setServiceName(gs.getName());
        newInstance.setPersistentDirName(gs.getPersistentDirName());
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
            .replaceFirst("clone", "instances/"));
        
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
     * Sets up a service instance by setting parameter values, creating sub-jobs,
     * uploading input files, etc.  This method can be called many times: each
     * invocation will overwrite any existing data.
     */
    public ModelAndView setupServiceInstance(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        User loggedInUser = (User)SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        
        // Work out which service instance we're interested in.  The URL
        // pattern is /G-Rex/serviceName/instances/instanceId/setup
        GRexServiceInstance instance = this.getServiceInstance(request.getRequestURI());
        
        if (!instance.canBeModifiedBy(loggedInUser))
        {
            throw new GRexException("User " + loggedInUser.getUsername() +
                " does not have permission to modify instance "
                + instance.getId() + " of service " + instance.getServiceName());
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
                if (instance.getState() != Job.State.CREATED)
                {
                    throw new GRexException("Cannot change a parameter of an" +
                        " instance in state " + instance.getState());
                }
                String value = Streams.asString(stream);
                if (name.trim().equals(NUMSUBJOBS_PARAMETER_NAME))
                {
                    // We're being asked to create a number of sub-jobs for
                    // this service instance
                    try
                    {
                        int numSubJobsToCreate = Integer.parseInt(value);
                        if (numSubJobsToCreate <= 0)
                        {
                            throw new GRexException("Parameter " + 
                                NUMSUBJOBS_PARAMETER_NAME + " must be a positive integer");
                        }
                        if (instance.getNumSubJobs() != 0 && 
                            instance.getNumSubJobs() != numSubJobsToCreate)
                        {
                            throw new GRexException("Already created sub-jobs for instance "
                                + instance.getId());
                        }
                        for (int i = 0; i < numSubJobsToCreate; i++)
                        {
                            Job subJob = new Job();
                            subJob.setId(i);
                            // Create the working directory for this sub-job
                            File subJobWd = new File(instance.getWorkingDirectoryFile()
                                .getParentFile(), "" + i);
                            if (!subJobWd.mkdir())
                            {
                                throw new GRexException("Error creating working "
                                    + "directory for sub-job " + i + " of instance "
                                    + instance.getId());
                            }
                            subJob.setWorkingDirectory(subJobWd.getPath());
                            instance.getSubJobs().add(subJob);
                        }
                        log.debug("Created " + numSubJobsToCreate + " sub-jobs");
                    }
                    catch(NumberFormatException nfe)
                    {
                        throw new GRexException("Invalid integer for parameter "
                            + NUMSUBJOBS_PARAMETER_NAME);
                    }
                }
                else
                {
                    instance.setParameter(name, value);
                }
            }
            else
            {
                // This is a file to upload and we can now process the input
                // stream.  In future we could send this stream to a database,
                // or to a remote file store.
                
                // TODO: check we're allowed to upload a file.  If the service
                // has already started we can only upload a file if it's the 
                // standard input and the job is interactive
                
                log.debug("Detected upload of file called " + name);
                // Make sure we only save files in the working directory itself
                // (otherwise a client could set the name to "..\..\123\wd\foo.dat"
                // and overwrite data in another instance)
                File targetFile = new File(instance.getWorkingDirectory(), name);
                if (!isChild(instance.getWorkingDirectory(), targetFile))
                {
                    log.error("Not allowed to write a file to " + targetFile.getCanonicalPath());
                    throw new GRexException("Not allowed to write a file that" +
                        " is not in the working directory of the service instance");
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
                // Append to the existing file if this is the standard input stream
                OutputStream fout = new FileOutputStream(targetFile,
                    name.equals(AbstractJobRunner.STDIN));
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
     * Controls a service instance: starts it running or aborts it.  In future
     * we might be able to support pausing of a run (if required).  Users must
     * set the parameter "operation" to "start" or "abort" (using a 
     * straightforward POST operation).
     * Returns a view of the service instance (which contains the new state, etc)
     */
    public ModelAndView controlServiceInstance(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        User loggedInUser = (User)SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        // Work out which service instance we're interested in.  The URL
        // pattern is /G-Rex/serviceName/instances/instanceId/control
        GRexServiceInstance instance = this.getServiceInstance(request.getRequestURI());
        
        if (!instance.canBeModifiedBy(loggedInUser))
        {
            throw new GRexException("User " + loggedInUser.getUsername() +
                " does not have permission to modify instance "
                + instance.getId() + " of service " + instance.getServiceName());
        }
        
        // Get the JobRunner for this instance
        JobRunner jobRunner = this.jobRunnerFactory.getRunnerForInstance(instance);
        
        // Check the operation that the user wants to perform
        if (request.getParameter("operation") == null)
        {
            throw new GRexException("Must provide a parameter called \"operation\"");
        }
        else if (request.getParameter("operation").trim().equals("start"))
        {
            if (instance.getState() != Job.State.CREATED)
            {
                throw new GRexException("Cannot start an instance in state " +
                    instance.getState());
            }
            // Check that all the required parameters have been set
            // TODO: should be checked for every sub-job or not at all
            for (Parameter param : instance.getGridServiceConfig().getParams())
            {
                String paramValue = instance.getParamValue(param.getName());
                if (paramValue == null && param.isRequired())
                {
                    throw new InstanceNotReadyException("Parameter " +
                        param.getName() + " must have a value");
                }
            }
            
            // Check that all the required input files have been uploaded
            // TODO: this is a bit complicated because:
            //    1) The user might have set URLs to the input files.
            //    2) If the name of the input file is given by a parameter
            //       that uses wildcards (e.g. "*.txt") we don't know at this
            //       stage how many files to expect.
            //    3) In future we might support interactive jobs and therefore
            //       the standard input file might not exist.
            
            jobRunner.start();
        }
        else if (request.getParameter("operation").trim().equals("abort"))
        {
            if (instance.getState() == Job.State.CREATED ||
                instance.getState() == Job.State.FINISHED ||
                instance.getState() == Job.State.ERROR)
            {
                throw new GRexException("Cannot abort an instance in state " +
                    instance.getState());
            }
            else if (instance.getState() != Job.State.ABORTED)
            {
                // We ignore the request if the job has already been aborted
                jobRunner.abort();
            }
        }
        // TODO: add a method for cleanup
        else
        {
            throw new GRexException("Invalid value for \"operation\" parameter");
        }
        
        // TODO: do something else if we've come from a web page
        return new ModelAndView("instance_xml", "instance", instance);
    }
}
