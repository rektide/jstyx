/*
 * Copyright (c) 2005 The University of Reading
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

package uk.ac.rdg.resc.jstyx.gridservice.server;

import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Class containing configuration info for a single Styx Grid Service
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/24 17:34:58  jonblower
 * Initial import
 *
 */
class SGSConfig
{
    private String name;    // The name of this SGS
    private String command; // The command that is run by this SGS
    private String workDir; // The working directory of this SGS
    private Vector params;  // The parameters for this SGS

    /**
     * @param gridService The Element in the XML config file that is at the
     * root of this Styx Grid Service
     * @param sgsRootDir The root of the working directory of this SGS
     * @throws IllegalArgumentException if the name of the SGS contains
     * a space.
     */
    public SGSConfig(Element gridService, String sgsRootDir)
    {
        this.name = gridService.getAttribute("name");
        // Check that the name is valid
        if (this.name.indexOf(" ") != -1)
        {
            // TODO: check for other whitespace characters
            throw new IllegalArgumentException("The name of an SGS cannot contain a space");
        }
        this.command = gridService.getAttribute("command");
        this.workDir = sgsRootDir + "\\" + name;

        // Now create the parameters
        this.params = new Vector();
        NodeList paramList = gridService.getElementsByTagName("param");
        for (int i = 0; i < paramList.getLength(); i++)
        {
            Element paramEl = (Element)paramList.item(i);
            this.params.add(new SGSParam(paramEl));
        }
    }

    /**
     * @return the name of the service
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return the command string that is run when the SGS is started.  This
     * is the string that is passed to Runtime.exec().
     */ 
    public String getCommand()
    {
        return this.command;
    }

    /**
     * @return the working directory of this SGS. Each instance of the SGS
     * will use a subdirectory of this directory as its working directory.
     */
    public String getWorkingDirectory()
    {
        return this.workDir;
    }

    /** 
     * @return Vector of SGSParam objects containing details of all the 
     * parameters
     */
    public Vector getParams()
    {
        return this.params;
    }
}
