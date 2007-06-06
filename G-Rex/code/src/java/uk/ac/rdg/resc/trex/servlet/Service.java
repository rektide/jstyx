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

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A Resource that represents a Service hosted by this server.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */

public class Service extends Resource
{
    private String serviceId;
    
    /**
     * Creates a new object that we can use to access Service properties
     * @param serviceId Unique identifier for this Service
     */
    public Service(String serviceId)
    {
        this.serviceId = serviceId;
    }
    
    /**
     * Gets information about this Service
     * @param request The HTTP request object
     * @param response The HTTP response object
     * @throws IOException if data could not be written to the client
     */
    public void get(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        // TODO: check permissions
        // TODO: check this service exists and get its properties
        String format = request.getParameter("format");
        if (format != null && format.equalsIgnoreCase("html"))
        {
            // return a web page containing the list of services
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.println("<html><head>");
            out.println("<title>Information about service " + this.serviceId + "</title>");
            out.println("</head><body>TODO</body></html>");
            out.close();
            response.setStatus(HttpServletResponse.SC_OK);
        }
        else
        {
            // default to XML
            response.setContentType("text/xml");
            PrintWriter out = response.getWriter();
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<service></service>");
            out.close();
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }
    
}
