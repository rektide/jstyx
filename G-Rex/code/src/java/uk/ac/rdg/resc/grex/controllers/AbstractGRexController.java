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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import uk.ac.rdg.resc.grex.config.GRexConfig;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.db.GRexServiceInstance;
import uk.ac.rdg.resc.grex.db.GRexServiceInstancesStore;
import uk.ac.rdg.resc.grex.db.Job;
import uk.ac.rdg.resc.grex.exceptions.GRexException;
import uk.ac.rdg.resc.grex.exceptions.InstancesStoreException;
import uk.ac.rdg.resc.grex.server.JobRunnerFactory;

/**
 * Contains common methods and fields for all controllers in the G-Rex application
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class AbstractGRexController extends MultiActionController
{
    private static final Log log = LogFactory.getLog(AbstractGRexController.class);
    
    /**
     * Configuration information for this G-Rex server. Injected by Spring.
     */
    protected GRexConfig config;
    
    /**
     * Store of service instances. Injected by Spring.
     */
    protected GRexServiceInstancesStore instancesStore;
    
    /**
     * Factory for JobRunner objects. Injected by Spring.
     */
    protected JobRunnerFactory jobRunnerFactory;
    
    /**
     * Finds the configuration information for the service being referred to in
     * the given request URI.
     * @throws GRexException if there is no service that matches the URI
     */
    protected GridServiceConfigForServer getServiceConfig(String requestURI)
        throws GRexException
    {
        String[] requestUriEls = getRequestUriEls(requestURI);
        if (requestUriEls.length < 3)
        {
            // Shouldn't happen: this method should not have been called if
            // the requestURI was not of the correct format
            throw new GRexException("Internal error: Request URI format error");
        }
        String serviceName = requestUriEls[2];
        GridServiceConfigForServer gs = this.config.getGridServiceByName(serviceName);
        if (gs == null)
        {
            throw new GRexException("There is no service called " + serviceName);
        }
        return gs;
    }
    
    /**
     * Searches the store of service instances for the requested service
     * instance and checks that it belongs to the correct service.  Does not 
     * check permissions of the instance.
     * 
     * This method used to also make sure that all
     * the transient (i.e. non-persistent) properties of the instances and sub-jobs
     * are set correctly.  Now these properties are set by
     * this.instancesStore.getServiceInstanceById. The checks on the properties
     * are still done here though they might not be strictly necessary as there
     * are also checks performed in this.instancesStore.getServiceInstanceById
     * @throws GRexException if the service instance could not be retrieved
     */
    protected GRexServiceInstance getServiceInstance(String requestURI)
        throws GRexException
    {
        GridServiceConfigForServer gsConfig = this.getServiceConfig(requestURI);
        String[] requestUriEls = getRequestUriEls(requestURI);
        if (requestUriEls.length < 5)
        {
            // Shouldn't happen: this method should not have been called if
            // the requestURI was not of the correct format
            throw new GRexException("Internal error: Request URI format error");
        }
        
        // Look for the instance and job IDs
        String[] instanceAndJobIds = requestUriEls[4].split("\\.");
        String instanceIdStr = instanceAndJobIds[0];
        Integer subJobId = null;
        if (instanceAndJobIds.length > 1)
        {
            try
            {
                subJobId = Integer.decode(instanceAndJobIds[1]);
            }
            catch(NumberFormatException nfe)
            {
                // Ignore: this could be a file extension.
            }
        }
        try
        {
            int instanceId = Integer.parseInt(instanceIdStr);
            // Retrieve the instance object from the store
            GRexServiceInstance instance = this.instancesStore.getServiceInstanceById(instanceId);
            // Check that the service names match
            if (instance == null || !instance.getServiceName().equals(gsConfig.getName()))
            {
                throw new GRexException("There is no instance of " + gsConfig.getName()
                    + " with id " + instanceIdStr);
            }
            // Make sure the configuration information is set, because this is
            // not stored in the database.
            //instance.setGridServiceConfig(gsConfig); //Now done by this.instancesStore.getServiceInstanceById
            
            // We need to set the instance property of the sub-jobs because
            // the database does not remember this.
            //instance.getMasterJob().setInstance(instance);  //Now done by this.instancesStore.getServiceInstanceById
            // TODO: set this for all sub-jobs too
            
            if (subJobId != null)
            {
                // We're interested in a particular sub-job
                Job subJob = instance.getSubJob(subJobId);
                if (subJob == null)
                {
                    throw new GRexException("There is no subjob with id " +
                        subJobId + " in instance " + instanceId + " of service "
                        + gsConfig.getName());
                }
                //subJob.setInstance(instance); //Now done by this.instancesStore.getServiceInstanceById
            }
            
            return instance;
        }
        catch(NumberFormatException nfe)
        {
            // thrown by Integer.parseInt(instanceIdStr)
            throw new GRexException("There is no instance of " + gsConfig.getName()
                + " with id " + instanceIdStr);
        }
        catch(InstancesStoreException ise)
        {
            String message = "Error retrieving instance with id " + instanceIdStr
                + " from database";
            log.error(message, ise);
            throw new GRexException(message + ": " + ise.getMessage());
        }
    }
    
    /**
     * @return the path to a file in the working directory of this instance or
     * job.
     */
    protected String getFilePath(String requestURI) throws GRexException
    {
        String[] requestUriEls = getRequestUriEls(requestURI);
        if (requestUriEls.length < 7)
        {
            // Shouldn't happen: this method should not have been called if
            // the requestURI was not of the correct format
            throw new GRexException("Internal error: Request URI format error");
        }
        // Find the path to the file, relative to the working directory
        StringBuffer pathBuf = new StringBuffer(requestUriEls[6]);
        for (int i = 7; i < requestUriEls.length; i++)
        {
            pathBuf.append("/"); // We use a forward slash even on Windows because
                                 // this is necessary for pattern matching in
                                 // instance.getOutputFile()
            pathBuf.append(requestUriEls[i]);
        }
        return pathBuf.toString();
    }
    
    // TODO: method for getting a particular job
    
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
    
    /**
     * @return the file extension for the given request URI.  For example, if the 
     * requestURI is "/foo/bar/baz.html" this method will return "html".  Returns
     * null if there is no file extension.
     */
    protected static final String getFileExtension(String requestURI)
    {
        // This is just a way of extracting the last portion of the path
        String filename = new File(requestURI).getName();
        if (filename.contains("."))
        {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Decodes the requestURI and splits into individual elements
     */
    private static final String[] getRequestUriEls(String requestURI)
    {
        return decode(requestURI).split("/");
    }
    
    /**
     * Decodes the given request URI by replacing HTML escape sequences with
     * their proper characters
     * @todo Implement properly: only handles spaces at the moment
     */
    private static final String decode(String uri)
    {
        return uri.replaceAll("%20", " ");
    }
    
}
