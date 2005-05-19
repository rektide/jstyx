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

package uk.ac.rdg.resc.jstyx.server;

import java.io.File;
import java.io.IOException;

import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Class representing a directory on a hard disk (i.e. in the host filesystem)
 * See also FileOnDisk.  To create a new file on the hard disk,
 * use createChild().
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2005/05/19 14:46:51  jonblower
 * Changed behaviour of StyxDirectory.createChild(): no longer adds file to namespace in this method
 *
 * Revision 1.3  2005/05/09 07:13:52  jonblower
 * Changed getFileOnDisk() to getFileOrDirectoryOnDisk()
 *
 * Revision 1.2  2005/04/28 08:11:15  jonblower
 * Modified permissions handling in documentation directory of SGS
 *
 * Revision 1.1.1.1  2005/02/16 18:58:31  jonblower
 * Initial import
 *
 */
public class DirectoryOnDisk extends StyxDirectory
{
    
    protected File dir; // The java.io.File representing the directory
    
    public DirectoryOnDisk(String filename) throws StyxException
    {
        this(new File(filename));
    }
    
    public DirectoryOnDisk(File dir) throws StyxException
    {
        this(dir, true);
    }
    
    /**
     * Creates a new instance of DirectoryOnDisk
     * @param dir The directory to wrap
     * @param searchChildren If this is true, we shall search through the
     * immediate children of this directory and create StyxFile wrappers for 
     * all of them
     * @throws StyxException if the given filename does not represent
     * an existing directory
     */
    private DirectoryOnDisk(File dir, boolean searchChildren) throws StyxException
    {
        super(dir.getName());
        if (!(dir.exists() && dir.isDirectory()))
        {
            throw new StyxException(dir.getPath() + " is not a directory");
        }
        this.dir = dir;
        this.refresh(searchChildren);
    }
    
    /**
     * Reads all metadata from underlying disk file
     * @param updateChildren if this is true, all the immediate children of this
     * directory will be updated (this parameter is meaningless if this is not a
     * directory)
     * @todo: check for files that have been deleted in the host filesystem
     */
    protected synchronized void refresh(boolean updateChildren)
    {
        // Update the last modified time
        this.lastModifiedTime = this.dir.lastModified() / 1000;
        // Update the list of child files
        if (updateChildren)
        {
            File[] files = this.dir.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                // Check to see if a file with this name is already known
                StyxFile sf = this.getChild(files[i].getName());
                if (sf == null)
                {
                    try
                    {
                        sf = FileOnDisk.getFileOrDirectoryOnDisk(files[i]);
                        // Set the permissions of the file correctly
                        if (sf instanceof StyxDirectory)
                        {
                            sf.setPermissions(this.getPermissions());
                        }
                        else
                        {
                            // This is a StyxFile (a FileOnDisk). Set to the
                            // same permissions as this host directory without
                            // the "execute" flags
                            sf.setPermissions(this.getPermissions() & 0666);
                        }
                        this.addChild(sf);
                    }
                    catch(StyxException se)
                    {
                        // this exception is thrown if the file already exists.
                        // We know this isn't the case so we can ignore it
                    }
                }
                else
                {
                    // File with this name already exists. Refresh the file
                    // metadata but don't descend into subdirectories (could lead
                    // to deep recursion)
                    if (sf instanceof StyxDirectory)
                    {
                        StyxDirectory sd = (StyxDirectory)sf;
                        sd.refresh(false);
                    }
                    else
                    {
                        sf.refresh();
                    }
                }
            }
        }
    }
    
    /**
     * Creates a new file and adds it to this directory. This method will create
     * a new file in the underlying filesystem, then return it.
     * A StyxException will be thrown if isAppOnly or isExclusive is true.
     */
    public StyxFile createChild(String name, int perm, boolean isDir,
        boolean isAppOnly, boolean isExclusive)
        throws StyxException
    {
        File f = new File(this.dir, name);
        if(isDir)
        {
            if (!f.mkdir())
            {
                throw new StyxException("Directory " + name + " could not be created");
            }
        }
        else
        {
            try
            {
                if (!f.createNewFile())
                {
                    throw new StyxException("File " + name + " could not be created");
                }
            }
            catch(IOException ioe)
            {
                throw new StyxException("An IOException occurred when creating "
                    + name + ": " + ioe.getMessage());
            }
        }
        // If we've got this far we must have created the file/directory
        // successfully.  Now we can create the StyxFile wrapper and add it to
        // this StyxDirectory.
        StyxFile sf = FileOnDisk.getFileOrDirectoryOnDisk(f);
        sf.setPermissions(perm);
        return sf;
    }
    
    /**
     * Removes the underlying directory from the disk
     */
    protected synchronized void delete()
    {
        this.dir.delete();
    }
    
}
