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

package uk.ac.rdg.resc.trex.servlet;

import com.sleepycat.je.DatabaseException;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import uk.ac.rdg.resc.trex.db.InstanceDatabase;

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
public class TRexServlet extends HttpServlet
{
    private static final String CLONING_ENDPOINT_NAME = "clone";
    private static final String CONFIG_FILE_NAME = "config.xml";
    private static final String INSTANCES_DIRECTORY_NAME = "instances";
    private static final String CONTROL_FILE_NAME = "control";
    private static final String WORKING_DIRECTORY_NAME = "wd";
    
    private InstanceDatabase instanceDB; // Database of service instances
    
    /**
     * Initialize this servlet.  Sets up the database of service instances
     * @throws ServletException if there was an error initializing the servlet
     */
    public void init() throws ServletException
    {
        // By default, create the database in a hidden directory in the user's
        // home directory
        String userHome = System.getProperty("user.home");
        File trexDir = new File(userHome, ".trex");
        trexDir.mkdir(); // TODO: deal with case in which "$HOME/.trex" exists
                         // but is not a directory
        File dbPath = new File(trexDir, "db");
        dbPath.mkdir();
        try
        {
            this.instanceDB = new InstanceDatabase(dbPath);
        }
        catch(DatabaseException dbe)
        {
            // TODO: log the error somewhere
            throw new ServletException(dbe);
        }
        // TODO: log that we have initialized
    }
    
    /**
     * Called before the servlet is destroyed.  Disconnects from the database
     * of service instances
     */
    public void destroy()
    {
        if (this.instanceDB != null)
        {
            try
            {
                this.instanceDB.close();
            }
            catch(DatabaseException dbe)
            {
                // TODO: log the error somewhere
            }
        }
        // TODO: log that we have destroyed
    }
    
    /**
     * <p>Identifies the resource (service, service instance, output file etc) that
     * the client wishes to operate upon. Splits the requestURI using
     * requestURI.split("/"), then parses the 
     * elements of the URI according to the following scheme:</p>
     * <table border="1"><tbody><tr><th>Index in array</th><th>Example</th><th>Meanings</th></tr>
     * <tr><td>0</td><td>(empty string)</td><td>Never changes: ignored</td></tr>
     * <tr><td>1</td><td>T-Rex</td><td>Never changes: ignored</td></tr>
     * <tr><td>2</td><td>myservice</td><td>Identifier of the Service</td></tr>
     * <tr><td rowspan="3">3</td><td>clone</td><td>Cloning endpoint</td></tr>
     *                           <td>config.xml</td><td>Configuration file</td></tr>
     *                           <td>instances</td><td>Directory of service instances</td></tr>
     * <tr><td>4</td><td>0034</td><td>Unique identifier for a service instance</td></tr>
     * <tr><td rowspan="2">5</td><td>ctl</td><td>Control endpoint for an instance</td></tr>
     *                           <td>wd</td><td>Working directory of an instance</td></tr>
     * <tr><td>6</td><td>foo.txt</td><td>File in the working directory of an instance</td></tr>
     * </tbody></table>
     * @param requestURI The result of HttpServletRequest.getRequestURI(), e.g.
     * "/T-Rex/myservice/instances/0034"
     */
    private Resource identifyResource(String requestURI)
    {
        String[] els = requestURI.split("/");
        if (els.length < 2)
        {
            // Shouldn't happen
            return null;
        }
        else if (els.length == 2)
        {
            // We are interested in the root of the server
            return new ServerRoot(); // TODO: do we need to create a new object every time?
        }
        else
        {
            String serviceId = els[2];
            if (els.length == 3)
            {
                // We are interested in a particular service
                return new Service(serviceId);
            }
            else if (els.length == 4)
            {
                // The Resource is the cloning endpoint or the config file for
                // this service
                if (els[3].equals(CLONING_ENDPOINT_NAME))
                {
                    return new CloningEndpoint(this.instanceDB, serviceId);
                }
                else if (els[3].equals(CONFIG_FILE_NAME))
                {
                    return new ConfigFile(serviceId);
                }
                else if (els[3].equals(INSTANCES_DIRECTORY_NAME))
                {
                    return new InstancesDirectory(this.instanceDB, serviceId);
                }
            }
            else if (els[3].equals(INSTANCES_DIRECTORY_NAME))
            {
                String instanceId = els[4];
                if (els.length == 5)
                {
                    // We are interested in information about a particular instance
                    return new ServiceInstance(serviceId, instanceId);
                }
                else if (els.length == 6)
                {
                    if (els[5].equals(CONTROL_FILE_NAME))
                    {
                        return new ControlFile(serviceId, instanceId);
                    }
                    else if (els[5].equals(WORKING_DIRECTORY_NAME))
                    {
                        // We're interested in the contents of the working directory
                    }
                }
                else if (els[5].equals(WORKING_DIRECTORY_NAME))
                {
                    // We're referring to a particular file in the working directory
                }
            }
        }
        // We haven't found a matching resource
        return null;
    }
    
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        // Find out what the client is requesting
        Resource resource = identifyResource(request.getRequestURI());
        if (resource == null)
        {
            // TODO: how do we stop this from generating an HTML error page?
            // We want the error code to go all the way back to the client
            response.sendError(HttpServletResponse.SC_NOT_FOUND, request.getRequestURI());
        }
        else
        {
            // Perform the operation. The particular Resource might need access to 
            // many more properties and methods of the HttpServlet[Request, Response]
            // objects so we pass these in, allowing the Resource to handle errors
            // etc, as it wishes.
            resource.get(request, response);
        }
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        throw new ServletException("POST method not yet implemented");
    }
    
    /**
     * Returns a short description of the servlet.
     */
    public String getServletInfo()
    {
        return "T-Rex entry point";
    }
}
