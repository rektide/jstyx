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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import simple.xml.Attribute;
import simple.xml.Root;
import simple.xml.load.Persister;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForClient;
import uk.ac.rdg.resc.grex.exceptions.GRexException;

/**
 * A client for a G-Rex service.  Allows the configuration information to be
 * downloaded and new GRexServiceInstanceClients to be created.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */

public class GRexServiceClient
{
    private static final Log log = LogFactory.getLog(GRexServiceClient.class);
    
    /**
     * The name of the G-Rex servlet
     */
    private static final String GREX_SERVLET_NAME = "G-Rex";
    
    // The details of the server
    private String protocol = "http";
    private String host;
    private int port;
    
    // Maximum number of downloader threads per service instance
    public static int maxSimultaneousDownloads = 40;
    
    // Maximum number of downloader threads in total. This is used to set
    // two parameters of the HTTP connection manager object: the maximum number
    // of connections and the maximum number of connections per host (i.e.
    // server).  This is set to a number much larger than the maximum number of
    // simultaneous downloads for each service instance (defined above).  The
    // aim is to allow more than one service instance to bring back
    // output at the same time, something that did not seem to work with the
    // previous version of this file.  However, it does not seem likely that
    // the properties of the HTTP connection manager for this service client
    // can have any effect on other service instance clients created by other
    // instances of GRexRun (from different executions of grexrun.sh).
    // Nevertheless, making maxTotalSimultaneousDownloads >>
    // maxSimultaneousDownloads does seem to allow two service instances to bring
    // back output at the same time (though I'm not sure why).
    // 
    // We do need to find
    // a way to share the available HTTP connections among service
    // instances.  Ideally we would have a pool of available connections for
    // all service instances to share.  If this feature were implemented, each service
    // instance would return a
    // connection to the pool when it was no longer needed.
    public static int maxTotalSimultaneousDownloads = 180; 
    
    // Authentication information    
    private String user;
    private String password;
    
    // The service that we will access
    private String serviceName;
    
    // Client object for all HTTP transfers
    private HttpClient client;
    
    /**
     * Zero-argument constructor: clients must use the setter methods to 
     * set properties before making a connection to the server
     */
    public GRexServiceClient()
    {
        // Allow many threads to use this connection
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        
        // Increase total maximum number of connections and maximum number of
        // connections per host for HttpClient objects. This is necessary
        // because the default maximums are 20 and 2 respectively, but
        // one connection is required for each of the output files
        //
        // First create object to store connection manager parameters
        HttpConnectionManagerParams connectionManagerParams;
        // Get the current parameters for this connection manager
        connectionManagerParams = connectionManager.getParams();
        // Change max total connections parameter in params object
        connectionManagerParams.setMaxTotalConnections(maxTotalSimultaneousDownloads);
        //
        // Now deal with maximum connections per host parameter.
        // First create a default host configuration object   
        HostConfiguration hostConfiguration = new HostConfiguration();
        // Now change max connections per host parameter in params object 
        connectionManagerParams.setMaxConnectionsPerHost(hostConfiguration, maxTotalSimultaneousDownloads);
        //
        // Finally, apply these new parameters to the connection manager object
        connectionManager.setParams(connectionManagerParams);
        
        // Create the HttpClient object for this G-Rex service client
        this.client = new HttpClient(connectionManager);
    }
    
    /**
     * Creates a new instance of GRexServiceClient from a URL of the form:
     * <code>protocol://user:password@host:port/G-Rex/serviceName</code>, e.g.
     * <code>http://fred:fish@myserver.com:8080/G-Rex/testService</code>.
     * @param url the URL to the grid service
     * @throws MalformedURLException if the url is not valid
     */
    public GRexServiceClient(String urlStr) throws MalformedURLException
    {
        this();
        URL url = new URL(urlStr);
        this.setProtocol(url.getProtocol());
        this.setHost(url.getHost());
        this.setPort(url.getPort());
        
        // Find the service name that we're looking for
        String[] pathEls = url.getPath().split("/");
        if (pathEls.length < 3)
        {
            throw new MalformedURLException("Service name not present");
        }
        this.setServiceName(pathEls[2]);
        
        // Set the user information if present.  If not present, must use
        // setUser() and setPassword() before connecting
        if (url.getUserInfo() != null)
        {
            String[] userAndPass = url.getUserInfo().split(":");
            if (userAndPass.length == 1)
            {
                this.setUser(userAndPass[0]);
            }
            else if (userAndPass.length == 2)
            {
                this.setUser(userAndPass[0]);
                this.setPassword(userAndPass[1]);
            }
            else
            {
                throw new MalformedURLException("User info not valid");
            }
        }        
    }
    
    /**
     * Queries the server for the configuration information for the service
     * @return the configuration object
     * @throws GRexException if there was an error reading the information from 
     * the server (e.g. the user does not have permissions to read the config info)
     * @throws IOException if an i/o error occurred, e.g. cannot connect to server 
     */
    public GridServiceConfigForClient getConfig() throws GRexException, IOException
    {
        String configUrl = this.getRootUrl() + "config.xml";
        log.debug("Getting config information from " + configUrl);
        GetMethod get = new GetMethod(configUrl);
        return this.executeMethod(get, GridServiceConfigForClient.class);
    }
    
    /**
     * Creates a new instance of this grid service and returns an object that 
     * can be used to manipulate it.
     * @param description A short (human-readable) description of this instance
     * to aid the user to identify it later
     * @throws GRexException if there was an error creating the instance
     * (e.g. the user does not have permissions to do so)
     * @throws IOException if an i/o error occurred, e.g. cannot connect to server 
     */
    public GRexServiceInstanceClient createNewServiceInstance(String description)
        throws GRexException, IOException
    {
        // Construct the URL to the clone file to which we will POST a request
        // to create a new instance
        String cloneUrl = this.getRootUrl() + "clone";
        log.debug("Creating new service instance using " + cloneUrl);
        
        PostMethod post = new PostMethod(cloneUrl);
        post.setParameter("operation", "create"); // The instruction to create a new instance
        if (description != null)
        {
            post.setParameter("description", description); // The instruction to create a new instance
        }
        
        String newInstanceUrl = this.executeMethod(post, InstanceResponse.class).getUrl();
        
        return new GRexServiceInstanceClient(newInstanceUrl, this);
    }
    
    /**
     * Executes an HTTP method, checks the response code, then attempts to
     * deserialize the XML response, firstly into an object of the given type,
     * then into a GRexException.
     * @throws IOException if there was an i/o error (e.g. cannot connect to
     * server)
     * @throws GRexException if the server returned an error (e.g. current user
     * cannot perform the given operation)
     */
    <T> T executeMethod(HttpMethod method, Class<T> returnType)
        throws IOException, GRexException
    {
        try
        {
            method.setDoAuthentication(true);
            int status = this.client.executeMethod(method);
            if (status == HttpServletResponse.SC_OK)
            {
                // This loads the whole response into memory: OK as will be short
                String xml = method.getResponseBodyAsString();
                //log.debug("received from server: " + xml);
                // Parse the XML and return
                try
                {
                    return new Persister().read(returnType, xml);
                }
                catch(Exception e)
                {
                    // Couldn't parse the XML as an object of the required type,
                    // let's see if it's a GRexException
                    GRexException gre = null;
                    try
                    {
                        gre = new Persister().read(GRexException.class, xml);
                    }
                    catch(Exception ex)
                    {
                        // It's not an exception either.
                        log.error("unrecognized response from server: " + xml);
                        throw new RuntimeException("Unrecognized response from server: " + xml);
                    }
                    // If we've got this far, this must be a valid exception
                    throw gre;
                }
            }
            else if (status == HttpServletResponse.SC_UNAUTHORIZED)
            {
                // The user's username/password were not recognized by the server
                throw new GRexException("User and password are not valid on the server");
            }
            else
            {
                // TODO: parse more return types explicitly
                log.error("Operation failed with HTTP error code " + status);
                throw new GRexException("Operation failed with HTTP error code " + status);
            }
        }
        finally
        {
            // Clean up the HttpClient
            method.releaseConnection();
        }
    }
    
    /**
     * @return the URL to the root of this grid service, e.g.
     * "http://myserver.com:8080/G-Rex/testservice/".  Note the trailing slash.
     */
    public String getRootUrl()
    {
        return this.protocol + "://" + this.host + ":" + this.port
            + "/" + GREX_SERVLET_NAME + "/" + this.serviceName + "/"; 
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
        this.setCredentials();
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
        this.setCredentials();
    }
    
    /**
     * Sets the credentials of the internal HttpClient.  Called when we change
     * the username or password
     */
    private void setCredentials()
    {
        Credentials creds = new UsernamePasswordCredentials(this.user, this.password);
        this.client.getState().setCredentials(new AuthScope(this.host, this.port), creds);
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public int getMaxSimultaneousDownloads()
    {
        return maxSimultaneousDownloads;
    }

    public void setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
    }
    
    /**
     * @return a String representation of this client, e.g. 
     * "Server: http://myhost.com:8080, Service: testservice, User: fred"
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer("Server: ");
        buf.append(this.getProtocol() + "://" + this.getHost() + ":" + this.getPort());
        buf.append(", Service: ");
        buf.append(this.getServiceName());
        buf.append(", User: ");
        buf.append(this.getUser());
        return buf.toString();
    }

    HttpClient getHttpClient()
    {
        return client;
    }
    
}
