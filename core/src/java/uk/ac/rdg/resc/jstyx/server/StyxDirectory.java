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

import org.apache.mina.common.ByteBuffer;

import java.util.Vector;
import java.util.Iterator;

import uk.ac.rdg.resc.jstyx.types.ULong;
import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.messages.StyxBuffer;

/**
 * Class representing a directory on a Styx server. One would only rarely need 
 * to create subclasses of this; an example would be creating a StyxDirectory that
 * represents a directory on the host filesystem (see DirectoryOnDisk).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.8  2005/04/28 08:11:15  jonblower
 * Modified permissions handling in documentation directory of SGS
 *
 * Revision 1.7  2005/03/24 09:48:31  jonblower
 * Changed 'count' from long to int throughout for reading and writing
 *
 * Revision 1.6  2005/03/22 17:48:27  jonblower
 * Removed debug code that tracked ByteBuffer allocation
 *
 * Revision 1.5  2005/03/21 17:57:11  jonblower
 * Trying to fix ByteBuffer leak in SGS server
 *
 * Revision 1.4  2005/03/16 17:56:24  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.3  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.1  2005/03/10 11:53:54  jonblower
 * Modified for MINA framework
 *
 * Revision 1.2  2005/03/01 13:47:43  jonblower
 * Changed default user and group to 'user' and 'group'
 *
 * Revision 1.1.1.1  2005/02/16 18:58:32  jonblower
 * Initial import
 *
 */
public class StyxDirectory extends StyxFile
{   
    
    private Vector children; // All the children of this directory
    
    /** Creates a new instance of StyxDirectory */
    public StyxDirectory(String name, String owner, String group, int permissions)
        throws StyxException
    {
        // Directories cannot be append-only, exclusive or auth files
        super(name, owner, group, permissions, false, false, false);
        this.directory = true;
        this.children = new Vector();
    }
    
    /**
     * Creates a directory with default permissions (0777, rwxrwxrwx)
     */
    public StyxDirectory(String name) throws StyxException
    {
        this(name, "user", "group", 0777);
    }
    
    /**
     * Creates a directory with the given permissions
     */
    public StyxDirectory(String name, int permissions) throws StyxException
    {
        this(name, "user", "group", permissions);
    }
    
    /**
     * This method is overridden to return a more meaningful error message.
     */
    public void checkSetLength(ULong newLength) throws StyxException
    {
        throw new StyxException("cannot change the length of a directory");
    }
    
    /**
     * @return true if this is the root directory (i.e. if the parent is null)
     */
    public boolean isRoot()
    {
        return (this.parent == null);
    }
    
    /**
     * Gets the full path relative to the root of this file system.
     */
    public String getFullPath()
    {
        if (this.isRoot())
        {
            return "";
        }
        return this.parent.getFullPath() + "/" + this.getName();
    }
    
    /**
     * @return the name of the file, or the empty string if this is the root
     * directory
     */
    public String getName()
    {
        if (this.isRoot())
        {
            return "";
        }
        return this.name;
    }
    
    /**
     * Returns the directory contents. This method cannot be overridden.
     */
    public final void read(StyxFileClient client, long offset, int count, int tag)
        throws StyxException
    {
        if (client == null)
        {
            throw new StyxException("Internal error: client is null");
        }

        // Check that the offset is valid; zero offsets are always valid, but
        // non-zero offsets are only valid if this client has read part of
        // the contents of this directory before
        if (offset != 0 && offset != client.getOffset())
        {
            throw new StyxException("Invalid offset when reading directory");
        }

        // We create the bytes to return in a ByteBuffer for convenience
        ByteBuffer buf = ByteBuffer.allocate((int)count);
        StyxBuffer styxBuf = new StyxBuffer(buf);
        StyxFile sf;
        int nextFile = (offset == 0) ? 0 : client.getNextFileToRead();

        while (nextFile < this.children.size())
        {
            sf = (StyxFile)this.children.get(nextFile);
            // check for overflows, or if data written > count
            DirEntry dirEntry = sf.getDirEntry();
            if (dirEntry.getSize() <= buf.remaining())
            {
                styxBuf.putDirEntry(dirEntry);
            }
            else
            {
                break;
            }
            nextFile++;
        }

        buf.flip();
        // remember the number of bytes returned and the index of the 
        // next child file to include in the next message
        client.setOffset(offset + buf.limit());
        client.setNextFileToRead(nextFile);
        
        // Get the bytes from the buffer
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        // Free the buffer so that it can be reused
        buf.release();
        
        this.replyRead(client, bytes, tag);
    }
    
    /**
     * This always throws a StyxException as it is illegal to write to a
     * directory
     */
    public void write(StyxFileClient client, long offset, int count,
        ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException
    {
        throw new StyxException("Cannot write to a directory");
    }
    
    /**
     * @return the length of the file. This is always zero: subclasses cannot
     * override this.
     */
    public final ULong getLength()
    {
        return ULong.ZERO;
    }
    
    /**
     * Refreshes this file (if it represents another entity, such as a file on disk,
     * this method is used to make sure that the file metadata (length, access
     * time etc) are up to date.
     */
    public synchronized void refresh()
    {
        this.refresh(true);
    }
    
    /**
     * @param updateChildren If this is set true, this method will also refresh
     * all the immediate children of this directory. This default method does
     * nothing; subclasses must override this.
     */
    protected synchronized void refresh(boolean updateChildren)
    {
        return; // it is up to subclasses to implement this if necessary
    }
    
    /**
     * Gets all the direct descendants of this directory
     * @return the children as an array of StyxFiles or null if this StyxFile
     * is not a directory
     */
    public synchronized StyxFile[] getChildren()
    {
        if (this.children == null)
        {
            return null;
        }
        // TODO: toArray() should work without the dummy 
        // argument - why doesn't it?
        return (StyxFile[])this.children.toArray(new StyxFile[0]);
    }
    
    /**
     * Gets the number of direct descendants of this directory
     */
    public int getNumChildren()
    {
        if (this.children == null)
        {
            return 0;
        }
        return this.children.size();
    }
    
    /**
     * Adds a file to this directory.  If a file with the same name already
     * exists, throws a FileExistsException
     */
    public synchronized void addChild(StyxFile sf) throws FileExistsException
    {
        // check that a file with this name does not already exist
        synchronized (this.children)
        {
            if (childExists(sf.getName()))
            {            
                throw new FileExistsException(sf.getName() + " already exists");
            }
            sf.parent = this;
            this.children.add(sf);
        }
        // Notify all interested parties that the contents of this directory
        // have changed
        this.fireContentsChanged();
    }
    
    /**
     * @return true if a child with the given name exists in this directory
     */
    public boolean childExists(String name)
    {
        name = name.trim();
        // check that a file with this name does not already exist
        synchronized (this.children)
        {
            for (int i = 0; i < this.children.size(); i++)
            {
                StyxFile f = (StyxFile)this.children.get(i);
                if (f.getName().equals(name))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Creates a new file and adds it to this directory. This method will only
     * be called if this file is a directory. This default implementation throws
     * an exception; subclasses should override this method to allow files to
     * be created.  Implementations should create a new file, then call
     * this.addChild() to add it to this directory.
     */
    public StyxFile createChild(String name, int perm, boolean isDir,
        boolean isAppOnly, boolean isExclusive) throws StyxException
    {
        throw new StyxException("cannot create a new file in this type of directory");
    }
    
    /**
     * Removes the given file from this directory.
     */
    public synchronized void removeChild(StyxFile child)
    {
        this.children.remove(child);
        // Notify all interested parties that the contents of this directory
        // have changed
        this.fireContentsChanged();
    }
    
    /**
     * Removes this directory from the server. This directory must be empty
     * (see this.removeAllChildren())
     * @throws StyxException if this is the root directory, or if it is not empty
     */
    public synchronized void remove() throws StyxException
    {
        if (this.isRoot())
        {
            throw new StyxException("Cannot remove the root directory of the server");
        }
        if (this.getNumChildren() != 0)
        {
            throw new StyxException("Cannot remove a directory unless it is empty");
        }
        super.remove();
    }
    
    /**
     * Recursively removes all children from this directory.
     */
    public synchronized void removeAllChildren()
    {
        synchronized(this.children)
        {
            StyxFile sf;
            while(this.children.size() > 0)
            {
                // Always get the first child: the children are progressively removed
                sf = (StyxFile)this.children.get(0);
                if (sf instanceof StyxDirectory)
                {
                    // If this is a directory, remove all its children
                    StyxDirectory sd = (StyxDirectory)sf;
                    sd.removeAllChildren();
                }
                // Now remove this file or directory
                try
                {
                    // This will also remove the StyxFile from the Vector of children
                    sf.remove();
                }
                catch(StyxException se)
                {
                    // TODO: is this the best thing to do here? We probably
                    // shouldn't abort the whole operation so it's probably best
                    // not to throw the exception
                    se.printStackTrace();
                    // Just to be sure, let's remove the StyxFile from the Vector
                    // of children (otherwise this loop will never end)
                    this.children.remove(sf);
                }
            }
        }
    }
    
    /**
     * Gets the child with the given name or null if it does not exist
     */
    public StyxFile getChild(String name)
    {
        if (name.equals(".."))
        {
            return this.getParent();
        }
        if (name.equals("."))
        {
            return this; // Should not happen: TwalkMessage should filter out
                         // path elements representing the current directory
        }
        for (int i = 0; i < this.children.size(); i++)
        {
            StyxFile sf = (StyxFile)this.children.get(i);
            String sfName = sf.getName();
            if (name.equals(sfName))
            {
                return sf;
            }
        }
        return null;
    }
    
    /**
     * Returns the parent of this directory. The parent of the root directory is
     * the root itself (according to the Inferno manual)
     */
    public final StyxDirectory getParent()
    {
        return this.parent == null ? this : this.parent;
    }
    
}
