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

package uk.ac.rdg.resc.jstyx.client;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Wraps a CStyxFile as a java.io.File. This allows Styx servers to be browsed
 * by Java-based filesystem viewers
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/07 08:27:51  jonblower
 * Initial import
 *
 */
public class FileWrapper extends File
{
    private static final Log log = LogFactory.getLog(FileWrapper.class);
    
    private CStyxFile file;
    
    /**
     * Creates a new instance of FileWrapper that wraps the given CStyxFile.
     * @todo What happens if the file isn't connected to an open connection yet?
     */
    public FileWrapper(CStyxFile file)
    {
        super(file.getPath());
        this.file = file;
        log.debug("Created FileWrapper(" + file.getPath() + ")");
    }
    
    /**
     * @return the CStyxFile that this FileWrapper wraps
     */
    public CStyxFile getCStyxFile()
    {
        return this.file;
    }
    
    /**
     * Gets the name of the file, i.e. the last part of the path
     */
    public String getName()
    {
        return this.file.getName();
    }
    
    /**
     * Gets the full path of the file. Makes sure that "/" is used as the separator
     */
    public String getPath()
    {
        return this.file.getPath();
    }
    
    /**
     * @return a string representing the parent of this file, or null if this
     * is the root directory
     */
    public String getParent()
    {
        log.debug("Called getParent() for " + this.getPath());
        String path = this.file.getPath();
        if (path.endsWith("/"))
        {
            path = path.substring(0, path.length() - 1);
        }
        int lastSlash = path.lastIndexOf("/");
        if (lastSlash == -1)
        {
            // This is the root directory
            return null;
        }
        else
        {
            // Return the path up until (and including) the last slash
            return path.substring(0, lastSlash);
        }
    }
    
    /**
     * @return the parent of this FileWrapper as another FileWrapper, or null
     * if this file does not have a parent
     */
    public File getParentFile()
    {
        log.debug("Called getParentFile() for " + this.getPath());
        String parentPath = this.getParent();
        if (parentPath == null)
        {
            return null;
        }
        if (parentPath.equals(this.getPath()))
        {
            // This is the root directory
            return null;
        }
        else
        {
            CStyxFile newFile = new CStyxFile(this.file.getConnection(),
                parentPath);
            return new FileWrapper(newFile);
        }
    }
    
    /**
     * @return true if this path starts with a slash (always return true
     * since CStyxFile always stores the full pathname)
     */
    public boolean isAbsolute()
    {
        if (!this.getPath().startsWith("/"))
        {
            log.warn("Path " + this.getPath() + " is not absolute");
        }
        return true;
    }
    
    /**
     * @return the absolute pathname of this file. Simply returns this.getPath()
     * since pathnames are always absolute.
     */
    public String getAbsolutePath()
    {
        return this.getPath();
    }
    
    /**
     * @return a FileWrapper representing the absolute pathname. Simply returns
     * this object, since pathnames are always absolute
     */
    public File getAbsoluteFile()
    {
        return this;
    }
    
    /**
     * @return the canonical filename, i.e. the unique filename after removing
     * . and .. parts of the path.  Simply returns the absolute path.
     */
    public String getCanonicalPath() throws IOException
    {
        return this.getAbsolutePath();
    }
    
    /**
     * @return a FileWrapper that represents the canonical filename. Simply
     * returns the absolute File (i.e. this object)
     */
    public File getCanonicalFile() throws IOException
    {
        return this.getAbsoluteFile();
    }
    
    /**
     * Tests whether this FileWrapper wraps a directory
     */
    public boolean isDirectory()
    {
        try
        {
            return this.file.isDirectory();
        }
        catch(StyxException se)
        {
            log.error(se.getMessage());
            return false;
        }
    }
    
    /**
     * Tests whether this FileWrapper wraps a normal file (i.e. not a directory)
     * @todo Should test that it's not an auth file, append-only, exclusive etc
     */
    public boolean isFile()
    {
        return !this.isDirectory();
    }
    
    /**
     * @return the length of the file in bytes
     */
    public long length()
    {
        try
        {
            return this.file.getLength();
        }
        catch(StyxException se)
        {
            log.error(se.getMessage());
            return 0;
        }
    }
    
    /**
     * @return The time that the file was last modified, in milliseconds
     * since the epoch
     */
    public long lastModified()
    {
        try
        {
            return this.file.getLastModified().getTime();
        }
        catch(StyxException se)
        {
            log.error(se.getMessage());
            return 0;
        }
    }
    
    /**
     * @return Array of strings naming the files and directories in the directory
     * that this FileWrapper represents. Returns null if this is not a directory.
     */
    public String[] list()
    {
        log.debug("Called (String[])list() for " + this.getPath());
        try
        {
            CStyxFile[] files = this.file.getChildren();
            if (files == null)
            {
                return null;
            }
            String[] filenames = new String[files.length];
            for (int i = 0; i < files.length; i++)
            {
                filenames[i] = files[i].getPath();
            }
            return filenames;
        }
        catch(StyxException se)
        {
            log.error(se.getMessage());
            return null;
        }
    }
    
    /**
     * Returns an array of abstract pathnames denoting the files and
     * directories in the directory denoted by this abstract pathname that
     * satisfy the specified filter.
     */
    public String[] list(FilenameFilter filter)
    {
        log.debug("Called (String[])list(FilenameFilter) for " + this.getPath());
        try
        {
            CStyxFile[] files = this.file.getChildren();
            if (files == null)
            {
                return null;
            }
            ArrayList v = new ArrayList();
            for (int i = 0; i < files.length; i++)
            {
                String name = files[i].getPath();
                if ((filter == null) || filter.accept(this, name))
                {
                    v.add(name);
                }
            }
            return (String[])(v.toArray(new String[0]));
        }
        catch(StyxException se)
        {
            log.error(se.getMessage());
            return null;
        }
    }
    
    /**
     * Returns an array of FileWrappers denoting the files in this
     * directory.
     */
    public File[] listFiles()
    {
        log.debug("Called (File[])listFiles() for " + this.getPath());
	try
        {
            CStyxFile[] files = this.file.getChildren();
            if (files == null)
            {
                return null;
            }
            FileWrapper[] wrappers = new FileWrapper[files.length];
            for (int i = 0; i < files.length; i++)
            {
                wrappers[i] = new FileWrapper(files[i]);
            }
            return wrappers;
        }
        catch(StyxException se)
        {
            log.error(se.getMessage());
            return null;
        }
    }
    
    /**
     * Returns an array of abstract pathnames denoting the files and
     * directories in the directory denoted by this abstract pathname that
     * satisfy the specified filter.
     */
    public File[] listFiles(FilenameFilter filter)
    {
        log.debug("Called (File[])listFiles(FilenameFilter) for " + this.getPath());
        try
        {
            CStyxFile[] files = this.file.getChildren();
            if (files == null)
            {
                return null;
            }
            ArrayList v = new ArrayList();
            for (int i = 0; i < files.length; i++)
            {
                String name = files[i].getPath();
                if ((filter == null) || filter.accept(this, name))
                {
                    v.add(new FileWrapper(files[i]));
                }
            }
            return (FileWrapper[])(v.toArray(new FileWrapper[0]));
        }
        catch(StyxException se)
        {
            log.error(se.getMessage());
            return null;
        }
    }
    
    /**
     * Returns an array of abstract pathnames denoting the files and
     * directories in the directory denoted by this abstract pathname that
     * satisfy the specified filter.
     */
    public File[] listFiles(FileFilter filter)
    {
        log.debug("Called (File[])listFiles(FileFilter) for " + this.getPath());
        try
        {
            CStyxFile[] files = this.file.getChildren();
            if (files == null)
            {
                return null;
            }
            ArrayList v = new ArrayList();
            for (int i = 0; i < files.length; i++)
            {
                FileWrapper wrapper = new FileWrapper(files[i]);
                if ((filter == null) || filter.accept(wrapper))
                {
                    v.add(wrapper);
                }
            }
            return (FileWrapper[])(v.toArray(new FileWrapper[0]));
        }
        catch(StyxException se)
        {
            log.error(se.getMessage());
            return null;
        }
    }
    
    /**
     * Lists available filesystem roots. At the moment this just returns a
     * single-membered array of java.io.File with the member "/". So it is
     * not connected to the root of the remote Styx filesystem. Is this 
     * going to work? We can't get the root of the real remote filesystem because
     * this is a static method.
     */
    public static File[] listRoots()
    {
        log.debug("Called FileWrapper.listRoots()");
        return new File[]{new File("/")};
    }
    
}
