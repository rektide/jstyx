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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import uk.ac.rdg.resc.grex.config.GRexConfig;
import uk.ac.rdg.resc.grex.db.GRexServiceInstancesStore;
import uk.ac.rdg.resc.grex.db.GrexServiceInstance;
import uk.ac.rdg.resc.grex.exceptions.GRexException;

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
    
    /**
     * Configuration information for this G-Rex server.
     */
    private GRexConfig config;
    /**
     * Store of service instances.
     */
    private GRexServiceInstancesStore instancesStore;
    
    public ModelAndView createNewServiceInstance(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // Find the name of the service that the user is interested in.  The URL
        // pattern is /G-Rex/serviceName/clone
        String serviceName = request.getRequestURI().split("/")[2];
        System.out.println("Doing a CLONE on " + serviceName);
        // Check that the service exists
        if (this.config.getGridServiceByName(serviceName) == null)
        {
            throw new GRexException("There is no service called " + serviceName);
        }
        
        // Check that the user has set "operation=create"
        if (request.getParameter("operation") == null ||
            !request.getParameter("operation").equals("create"))
        {
            throw new GRexException("Illegal command POSTed to " + request.getRequestURI());
        }
        
        // Create a new instance of the service
        GrexServiceInstance newInstance = new GrexServiceInstance();
        newInstance.setServiceName(serviceName);
        if (request.getParameter("description") != null)
        {
            newInstance.setDescription(request.getParameter("description"));
        }
        // TODO: set more properties
        
        // Add the instance to the store, getting the new ID
        int id = this.instancesStore.addServiceInstance(newInstance);
        
        if (request.getParameter("source") != null &&
            request.getParameter("source").equals("web"))
        {
            // We've come from the web so display an HTML page
            return new ModelAndView("newInstanceCreated_html", "instance", newInstance);
        }
        else
        {
            // We didn't come from the web, so we assume we've done this programmatically
            // (i.e. via a REST web service call) and we'll return XML to the client
            return new ModelAndView("newInstanceCreated_xml", "instance", newInstance);
        }
    }
    
    /**
     * This will be used by the Spring framework to inject the config object
     * before handleRequestInternal is called
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
}
