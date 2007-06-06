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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.rdg.resc.trex.db.InstanceDatabase;
import uk.ac.rdg.resc.trex.db.TrexServiceInstance;

/**
 * Resource representing the directory of instances of a particular Service
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */

public class InstancesDirectory extends Resource
{
    private String serviceId;
    private InstanceDatabase instanceDB;
    
    /**
     * Creates a new instance of InstancesDirectory
     * @param instanceDB Database containing service instances
     * @param serviceId The unique identifier of the Service
     */
    public InstancesDirectory(InstanceDatabase instanceDB, String serviceId)
    {
        this.instanceDB = instanceDB;
        this.serviceId = serviceId;
    }
    
    /**
     * Gets a list of instances of the service
     * @param request The HTTP request object
     * @param response The HTTP response object
     * @throws IOException if data could not be written to the client
     */
    public void get(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        // TODO: check permissions
        // Get a list of instances for this service (TODO: check that the instances
        // are not hidden from the user)
        List<TrexServiceInstance> instances = null;
        try
        {
            instances = this.instanceDB.getServiceInstances(this.serviceId);
        }
        catch(DatabaseException dbe)
        {
            // TODO log the error
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error reading instances for service " + this.serviceId + " from database");
            return;
        }
        String format = request.getParameter("format");
        if (format != null && format.equalsIgnoreCase("html"))
        {
            // return a web page containing the list of services
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><head>");
            out.println("<title>Instances of service " + this.serviceId + "</title>");
            out.println("</head><body>");
            out.println("<table><tbody>");
            out.println("<tr><th>ID</th></tr>"); // TODO: more
            for (TrexServiceInstance inst : instances)
            {
                out.println("<tr><td>" + inst.getId() + "</td></tr>");
            }
            out.println("</body></html>");
            out.close();
            response.setStatus(HttpServletResponse.SC_OK);
        }
        else
        {
            // default to XML
            response.setContentType("text/xml");
            PrintWriter out = response.getWriter();
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<instances></instances>");
            out.close();
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }
    
}
