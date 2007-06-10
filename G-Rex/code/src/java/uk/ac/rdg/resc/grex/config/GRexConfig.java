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

package uk.ac.rdg.resc.grex.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.load.PersistenceException;
import simple.xml.load.Persister;
import simple.xml.load.Validate;

/**
 * Class that configures a G-Rex server by describing all the services it
 * exposes.  The configuration information is read from an XML file using the
 * Simple XML library.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="grex")
public class GRexConfig
{
    private static final Log log = LogFactory.getLog(GRexConfig.class);
    
    /**
     * The services that are exposed by this G-Rex server
     */
    @ElementList(name="gridservices", type=GridService.class)
    private Vector<GridService> gridServices;
    
    /**
     * Creates a new instance of GRexConfig by reading the config information
     * from the file with the given path
     */
    public static GRexConfig readConfig(String configFilePath) throws Exception
    {
        File configFile = new File(configFilePath);
        GRexConfig config = new GRexConfig();
        GridService gs = new GridService();
        gs.setName("foo1");
        gs.setCommand("/path/to/foo");
        gs.setDescription("description of foo1");
        GridService gs2 = new GridService();
        gs2.setName("bar");
        gs2.setDescription("description of bar");
        config.gridServices = new Vector<GridService>();
        config.gridServices.add(gs);
        config.gridServices.add(gs2);
        /*GRexConfig config = new Persister().read(GRexConfig.class, configFile);
        log.debug("Loaded configuration from " + configFile.getPath());*/
        return config;
    }
    
    /**
     * Private constructor to prevent direct instantiation
     */
    private GRexConfig()
    {
    }

    public Vector<GridService> getGridServices()
    {
        return gridServices;
    }
    
    /**
     * @return the GridService with the given name, or null if there is no
     * service with this name
     */
    public GridService getGridServiceByName(String serviceName)
    {
        for (GridService gs : this.gridServices)
        {
            if (gs.getName().equals(serviceName))
            {
                return gs;
            }
        }
        return null;
    }
    
    /**
     * Checks that the data we have read are valid.  Checks that there are no
     * duplicate names for GridServices
     */
    @Validate
    public void validate() throws PersistenceException
    {
        List<String> names = new ArrayList<String>();
        for (GridService gs : this.gridServices)
        {
            String name = gs.getName();
            if (name.contains(name))
            {
                throw new PersistenceException("Duplicate gridservice name %s", name);
            }
            names.add(name);
        }
    }
    
}
