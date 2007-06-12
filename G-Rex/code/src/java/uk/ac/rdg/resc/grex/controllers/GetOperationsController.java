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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import uk.ac.rdg.resc.grex.config.GRexConfig;
import uk.ac.rdg.resc.grex.config.GridService;
import uk.ac.rdg.resc.grex.config.Parameter;
import uk.ac.rdg.resc.grex.db.GRexServiceInstancesStore;
import uk.ac.rdg.resc.grex.db.GrexServiceInstance;
import uk.ac.rdg.resc.grex.exceptions.GRexException;

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
public class GetOperationsController extends MultiActionController
{
    
    /**
     * Configuration information for this G-Rex server.
     */
    private GRexConfig config;
    /**
     * Store of service instances.
     */
    private GRexServiceInstancesStore instancesStore;
    
    public ModelAndView listServices(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // Get the list of services from the config object
        Vector<GridService> gridServices = this.config.getGridServices();
        // The gridServices object will appear in the JSPs with the name "gridservices"
        // TODO: restrict viewing of services to certain groups
        // The JSP that will be displayed will be "/WEB-INF/jsp/hello_[fileExtension].jsp"
        return new ModelAndView("services_" + getFileExtension(request.getRequestURI()),
            "gridservices", gridServices);
    }
    
    public ModelAndView listInstancesForService(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // Find the name of the service that the user is interested in.  The URL
        // pattern is /G-Rex/serviceName/instances.[xml,html]
        String serviceName = request.getRequestURI().split("/")[2];
        System.out.println("Service name: " + serviceName);
        // Check that this service actually exists!
        if (this.config.getGridServiceByName(serviceName) == null)
        {
            throw new GRexException("There is no service called " + serviceName);
        }
        
        // Get the list of service instance from the store
        List<GrexServiceInstance> instances =
            this.instancesStore.getServiceInstancesByServiceName(serviceName);
        // TODO: restrict viewing of instances to the correct users
        
        // Create the model map that will be passed to the JSPs
        Map<String, Object> modelMap = new HashMap<String, Object>();
        modelMap.put("instances", instances);
        modelMap.put("serviceName", serviceName);
        return new ModelAndView("instancesForService_" +
            getFileExtension(request.getRequestURI()), modelMap);
    }
    
    /**
     * Shows the configuration information for a particular service
     */
    public ModelAndView showConfigForService(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // Find the name of the service that the user is interested in.  The URL
        // pattern is /G-Rex/serviceName/instances.[xml,html]
        String serviceName = request.getRequestURI().split("/")[2];
        GridService gs = this.config.getGridServiceByName(serviceName);
        if (gs == null)
        {
            throw new GRexException("There is no service called " + serviceName);
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
        // Find the name of the service that the user is interested in.  The URL
        // pattern is /G-Rex/serviceName/instances/instanceID.[xml,html]
        String serviceName = request.getRequestURI().split("/")[2];
        String instanceIdStr = request.getRequestURI().split("/")[4].split("\\.")[0];
        try
        {
            int instanceId = Integer.parseInt(instanceIdStr);
            // Retrieve the instance object from the store
            GrexServiceInstance instance = this.instancesStore.getServiceInstanceById(instanceId);
            System.out.println("serviceName from URL: " + serviceName);
            System.out.println("serviceName from instance: " + instance.getServiceName());
            // Check that the service names match
            if (instance == null || !instance.getServiceName().equals(serviceName))
            {
                throw new GRexException("There is no instance of " + serviceName + 
                    " with id " + instanceIdStr);
            }
            // Display the details of this instance
            return new ModelAndView("instance_" +
                getFileExtension(request.getRequestURI()), "instance", instance);
        }
        catch(NumberFormatException nfe)
        {
            throw new GRexException("There is no instance of " + serviceName + 
                " with id " + instanceIdStr);
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
    
    /**
     * Finds the file extension for the given request URI.  For example, if the 
     * requestURI is "/foo/bar/baz.html" this method will return "html"
     */
    private static final String getFileExtension(String requestURI)
    {
        return requestURI.substring(requestURI.lastIndexOf(".") + 1);
    }
}
