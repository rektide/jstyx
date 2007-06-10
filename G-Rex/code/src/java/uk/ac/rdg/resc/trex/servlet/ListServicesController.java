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

import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import uk.ac.rdg.resc.grex.config.GRexConfig;
import uk.ac.rdg.resc.grex.config.GridService;

/**
 * Controller that lists the services hosted on this server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class ListServicesController extends AbstractController
{
    
    /**
     * Configuration information for this G-Rex server.  This object will be
     * injected by the Spring framework
     */
    private GRexConfig config;
    
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // Get the list of services from the config object
        Vector<GridService> gridServices = this.config.getGridServices();
        // TODO: automatically append the file extension to the view name
        // The gridServices object will appear in the JSPs with the name "gridservices"
        // TODO: restrict viewing of services to certain groups
        String fileExtension = request.getRequestURI().substring(request.getRequestURI().lastIndexOf(".") + 1);
        // The JSP that will be displayed will be "/WEB-INF/jsp/hello_[fileExtension].jsp"
        return new ModelAndView("hello_" + fileExtension, "gridservices", gridServices);
    }
    
    /**
     * This will be used by the Spring framework to inject the config object
     * before handleRequestInternal is called
     */
    public void setGrexConfig(GRexConfig config)
    {
        this.config = config;
    }
    
}
