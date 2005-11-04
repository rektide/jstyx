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

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxFileChangeListener;

/**
 * A file that is used to provide input (as a file or as stdin stream) to a 
 * Styx Grid Service.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/11/04 09:12:23  jonblower
 * Initial import
 *
 */
public class SGSInputFile extends StyxFile implements StyxFileChangeListener
{
    /**
     * This is the prefix given to files that take their name from the value 
     * of a parameter
     */
    public static final String PARAM_FILE_PREFIX = "__fromParam-";
    
    /**
     * Creates a new SGSInputFile with the given (fixed) name.  This name will
     * never change
     */
    public SGSInputFile(String name) throws StyxException
    {
        super(name, 0222); // A write-only file
    }
    
    /**
     * Creates an SGSInputFile whose name is given by the contents of the given
     * parameter file
     */
    public SGSInputFile(SGSParamFile paramFile) throws StyxException
    {
        super(PARAM_FILE_PREFIX + paramFile.getName(), 0222);
        // Make sure this class is notified if the value of the parameter changes:
        // when this happens, this.contentsChanged() will be called
        paramFile.addChangeListener(this);
    }
    
    /**
     * This method will be called when the contents of the associated parameter
     * file has changed, if this input file is specified by an input parameter
     */
    public void contentsChanged()
    {
        
    }
    
}
