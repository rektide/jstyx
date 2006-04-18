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

import java.io.File;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.StyxDirectory;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Directory that holds input files for a Styx Grid Service instance.  New files
 * can be created in this directory: these will be <code>SGSInputFile.File</code>s.
 * New directories can also be created in this directory: these will be
 * <code>SGSInputDirectory</code>s.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
class SGSInputDirectory extends StyxDirectory
{
    private AbstractJob job;
    private File baseDir;   // This is the directory on the disk where the files
                            // will physically be stored
    
    /**
     * Creates a new instance of SGSInputDirectory with rwxrwxrwx permissions.
     * @param name The name of this directory
     * @param baseDir The directory on the disk that backs this SGSInputDirectory
     * @param job The AbstractJob to which this input directory belongs
     * @throws StyxException if <code>baseDir</code> already exists as a regular
     * file or if the SGSInputDirectory could not be created for some other
     * reason (e.g. a name clash)
     * @todo be more restrictive with permissions and set owner/group properly
     */
    public SGSInputDirectory(String name, File baseDir, AbstractJob job)
        throws StyxException
    {
        this(name, 0777, baseDir, job);
    }
    
    /**
     * Creates a new instance of SGSInputDirectory with the given permissions.
     * @param name The name of this directory
     * @param permissions The permissions of this directory (e.g. 0755)
     * @param baseDir The directory on the disk that backs this SGSInputDirectory
     * @param job The AbstractJob to which this input directory belongs
     * @throws StyxException if <code>baseDir</code> already exists as a regular
     * file or if the SGSInputDirectory could not be created for some other
     * reason (e.g. a name clash)
     */
    private SGSInputDirectory(String name, int permissions, File baseDir,
        AbstractJob job) throws StyxException
    {
        super(name, permissions);
        if (baseDir.exists())
        {
            if (!baseDir.isDirectory())
            {
                throw new StyxException(baseDir.getPath() +
                    " already exists as a regular file");
            }
        }
        else
        {
            // Try to make the directory
            if (!baseDir.mkdir())
            {
                throw new StyxException("Could not create " + baseDir.getPath());
            }
        }
        this.baseDir = baseDir;
        this.job = job;
    }
    
    /**
     * Creates a new file to be added to this directory.  If the new file is a
     * directory, it will be of type SGSInputDirectory; otherwise it will be an
     * SGSInputFile.File.
     * This method does <b>not</b> add the new file to the directory. This will
     * be done in StyxServerProtocolHandler.replyCreate() if creating the file
     * via the Styx interface.
     * @return The newly-created file
     * @throws StyxException if the new file/directory could not be created.
     */
    public StyxFile createChild(String name, int perm, boolean isDir,
        boolean isAppOnly, boolean isExclusive) throws StyxException
    {
        File newFile = new File(this.baseDir, name);
        if (isDir)
        {
            // Create a new directory in this directory
            return new SGSInputDirectory(name, perm, newFile, this.job);
        }
        else
        {
            // This is a regular file
            return new SGSInputFile.File(newFile, this.job);
        }
    }
    
}
