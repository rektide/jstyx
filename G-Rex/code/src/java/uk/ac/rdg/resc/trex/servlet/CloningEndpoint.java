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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.rdg.resc.trex.db.InstanceDatabase;
import uk.ac.rdg.resc.trex.db.TrexServiceInstance;

/**
 * Clients POST a message to this resource to create a new instance of a Service.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */

public class CloningEndpoint extends Resource
{
    private String serviceId;
    private InstanceDatabase instanceDB;
    
    /**
     * Creates a new instance of CloningEndpoint
     * @param instanceDB Database of service instances
     * @param serviceId The id of the Service to which this belongs
     */
    public CloningEndpoint(InstanceDatabase instanceDB, String serviceId)
    {
        this.instanceDB = instanceDB;
        this.serviceId = serviceId;
    }
    
    /**
     * Performs the POST operation on this resource
     * TODO: temporarily set to GET for easier debugging in browser
     * @param request The HTTP request object
     * @param response The HTTP response object
     * @throws IOException if data could not be written to the client
     */
    public void get(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        String op = request.getParameter("operation");
        if (op != null && op.equalsIgnoreCase("create"))
        {
            // We shall create a new service instance and return its id
            TrexServiceInstance inst = new TrexServiceInstance();
            // TODO: set the correct working directory
            inst.setServiceID(this.serviceId);
            // Add to the database
            try
            {
                int id = this.instanceDB.addServiceInstance(inst);
                response.setContentType("text/xml");
                PrintWriter out = response.getWriter();
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                // TODO: Return the full URL to the instance, based on the URL the
                // user called to get here
                out.println("<id>" + id + "</id>");
                response.setStatus(HttpServletResponse.SC_OK);
            }
            catch(DatabaseException dbe)
            {
                // Error writing the instance to the database
                // TODO: log the error
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error adding new instance to database");
            }
        }
        else
        {
            // TODO What do we do here?
            response.getOutputStream().close();
        }
    }
}
