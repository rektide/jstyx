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

import org.apache.mina.common.IoSession;
import org.apache.mina.common.ByteBuffer;

import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

import uk.ac.rdg.resc.jstyx.messages.RreadMessage;
import uk.ac.rdg.resc.jstyx.messages.RwriteMessage;

import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.types.Qid;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Class representing a file (or directory) on a Styx server. There may
 * be different types of file; a file might map directly to a file on disk, or 
 * it may be a synthetic file representing a program interface.  This class
 * creates a StyxFile that does nothing useful, returning errors when reading
 * from or writing to it.  Subclasses should override the read(), write()
 * and getLength() methods to implement the desired behaviour.
 *
 * Currently each StyxFile has exactly one parent. Therefore symbolic links
 * on the host filesystem cannot currently be handled.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.25  2006/03/21 09:06:15  jonblower
 * Still implementing authentication
 *
 * Revision 1.24  2006/03/20 17:51:50  jonblower
 * Adding authentication to base JStyx system
 *
 * Revision 1.23  2006/02/17 09:22:32  jonblower
 * Added rename() method
 *
 * Revision 1.22  2006/01/04 16:47:29  jonblower
 * Reworked getName() and getFullPath()
 *
 * Revision 1.21  2005/12/01 08:21:56  jonblower
 * Fixed javadoc comments
 *
 * Revision 1.20  2005/11/04 19:33:41  jonblower
 * Changed contentsChanged() to fileContentsChanged() in StyxFileChangeListener
 *
 * Revision 1.19  2005/11/03 21:50:04  jonblower
 * Added clarification to comments
 *
 * Revision 1.18  2005/09/08 07:08:59  jonblower
 * Removed "String user" from list of parameters to StyxFile.write()
 *
 * Revision 1.17  2005/08/30 16:29:00  jonblower
 * Added processAndReplyRead() helper functions to StyxFile
 *
 * Revision 1.16  2005/07/06 17:42:47  jonblower
 * Changed getUniqueID() to be based on creation time in addition to file name
 *
 * Revision 1.15  2005/05/10 08:02:18  jonblower
 * Changes related to implementing MonitoredFileOnDisk
 *
 * Revision 1.14  2005/05/09 07:12:52  jonblower
 * Clarified some comments
 *
 * Revision 1.13  2005/04/28 08:11:15  jonblower
 * Modified permissions handling in documentation directory of SGS
 *
 * Revision 1.12  2005/04/27 16:11:43  jonblower
 * Added capability to add documentation files to SGS namespace
 *
 * Revision 1.11  2005/03/24 14:47:47  jonblower
 * Provided default read() and write() methods for StyxFile so it is no longer abstract
 *
 * Revision 1.10  2005/03/24 09:48:32  jonblower
 * Changed 'count' from long to int throughout for reading and writing
 *
 * Revision 1.9  2005/03/24 07:57:41  jonblower
 * Improved code for reading SSL info from SGSconfig file and included parameter
 * information for the Grid Services in the config file
 *
 * Revision 1.8  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.7  2005/03/18 16:45:18  jonblower
 * Released ByteBuffers after use
 *
 * Revision 1.6  2005/03/18 13:56:00  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.5  2005/03/16 22:16:43  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.4  2005/03/16 17:56:24  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.3  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.2  2005/03/10 11:53:54  jonblower
 * Modified for MINA framework
 *
 * Revision 1.2.2.1  2005/03/09 19:44:18  jonblower
 * Changes concerned with migration to MINA
 *
 * Revision 1.2  2005/03/01 13:47:43  jonblower
 * Changed default user and group to 'user' and 'group'
 *
 * Revision 1.1.1.1  2005/02/16 18:58:32  jonblower
 * Initial import
 *
 */
public class StyxFile
{
    
    protected String name;           // The name of the file
    protected StyxDirectory parent;  // The parent of the file (N.B. the root file
                                     // has no parent so this will be null)
    
    protected boolean directory;     // True if this is a directory
    private boolean appendOnly;      // True if this is an append-only file
    private boolean exclusive;       // True if this file can be opened by only one client at a time
    protected boolean auth;            // True if this is a file to be used by the authentication mechanism
                                     // (normally false)
    private int permissions;         // Permissions represented as a number (e.g. 0755 in octal)
    
    private long version;            // Version of the file (incremented when it is modified)
                                     // This can only be modified through the incrementVersion()
                                     // method
    
    private long creationTime;       // Time of creation (milliseconds since the epoch,
                                     // used to generate a unique ID for the file)
    private long lastAccessTime;     // last access time (seconds since the epoch)
    protected long lastModifiedTime; // last modification time (seconds since the epoch)
    private String owner;            // owner name
    private String group;            // group name
    private String lastModifiedBy;   // name of the user who last modified the file
    
    private Vector clients;          // The clients who have a connection to this file
                                     // (i.e. clients who have opened this file)
    private Vector changeListeners;  // Objects that will get notified when this
                                     // file changes
    
    /**
     * @todo check that the name is valid (no trailing or leading slashes unless
     * it is the root directory, no spaces)
     * @todo according to the Manual, the parent of the root of the tree is itself
     * @throws StyxException if an attempt is made to create a file with the name
     * "", "." or ".."
     */
    public StyxFile(String name, String owner, String group, int permissions,
        boolean isAppendOnly, boolean isExclusive)
        throws StyxException
    {
        name = name.trim();
        if (name.equals("") || name.equals(".") || name.equals(".."))
        {
            throw new StyxException("illegal file name");
        }
        this.parent = null;
        this.name = name;
        this.directory = false;
        this.auth = false;
        this.permissions = permissions;
        this.appendOnly = isAppendOnly;
        this.exclusive = isExclusive;
        this.version = 0;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessTime = StyxUtils.now();
        this.lastModifiedTime = StyxUtils.now();
        this.owner = owner.trim();
        this.group = group.trim();
        this.lastModifiedBy = "";
        this.clients = new Vector();
        this.changeListeners = new Vector();
    }
    
    /**
     * Creates a StyxFile with the default username and group
     */
    public StyxFile(String name, int permissions, boolean isAppendOnly,
        boolean isExclusive) throws StyxException
    {
        // TODO specify the default user and group in a config file?
        this(name, "user", "group", permissions, isAppendOnly, isExclusive);
    }
    
    public StyxFile(String name, int permissions)
        throws StyxException
    {
        this(name, permissions, false, false);
    }
    
    /**
     * Creates a StyxFile with the default permissions (0666, rw-rw-rw-)
     */
    public StyxFile(String name) throws StyxException
    {
        this(name, 0666);
    }
    
    /**
     * @return the name of the file, or the empty string if this is the root
     * directory
     */
    public String getName()
    {
        return this.name;
    }
    
    /**
     * @throws StyxException if the name of this file cannot be changed
     */
    public void checkSetName(String newName) throws StyxException
    {
        return;
    }
    
    /**
     * Changes the name of the file. Must have checked for correct permissions
     * before doing this. Also must call checkSetName() to check if this method
     * will succeed.
     */
    public void setName(String name)
    {
        this.name = name.trim();
    }
    
    /**
     * @return true if this StyxFile is a directory (should always return the
     * same result as "instanceof StyxDirectory")
     */
    public boolean isDirectory()
    {
        return this.directory;
    }
    
    public boolean isAppendOnly()
    {
        return this.appendOnly;
    }
    
    /**
     * @return true if this file is marked for exclusive use
     */
    public boolean isExclusive()
    {
        return this.exclusive;
    }
    
    public boolean isAuth()
    {
        return this.auth;
    }
    
    /**
     * Gets the full path relative to the root of this file system.
     */
    public String getFullPath()
    {
        if (this.auth)
        {
            // Auth files don't have a parent
            return this.getName();
        }
        else
        {
            return this.parent.getFullPath() + this.getName();
        }
    }
    
    /**
     * @return the length of the file. This default implementation returns
     * zero; subclasses should override this method
     */
    public ULong getLength()
    {
        return ULong.ZERO;
    }
    
    /**
     * Returns the parent of this file. The parent of the root directory is
     * the root itself (according to the Inferno manual)
     */
    public StyxDirectory getParent()
    {
        return this.parent;
    }
    
    public String getOwner()
    {
        return this.owner;
    }
    
    public String getGroup()
    {
        return this.group;
    }
    
    public synchronized DirEntry getDirEntry()
    {
        return new DirEntry(this.getQid(), this.getMode(), this.lastAccessTime,
            this.lastModifiedTime, this.getLength(), this.name, this.owner,
            this.group, this.lastModifiedBy);
    }
    
    public synchronized Qid getQid()
    {
        // For the Qid, we only need the high byte of the type
        int type = (int)(this.getType() >> 24);
        return new Qid(type, this.version, this.getUniqueID());
    }
    
    /**
     * Gets the type of the file as a number representing the OR of DMDIR,
     * DMAPPEND, DMEXCL and DMAUTH as appropriate, used to create the Qid
     */
    private long getType()
    {
        long type = 0;
        if (this.directory)
        {
            type |= StyxUtils.DMDIR;
        }
        if (this.appendOnly)
        {
            type |= StyxUtils.DMAPPEND;
        }
        if (this.exclusive)
        {
            type |= StyxUtils.DMEXCL;
        }
        if (this.auth)
        {
            type |= StyxUtils.DMAUTH;
        }
        return type;
    }
    
    /**
     * @return the permissions of the file as an integer (e.g. 0755). Does not
     * include the DMDIR, DMAPPEND, DMEXCL, DMAUTH flags (check these with the
     * accessor methods isDirectory(), isAppendOnly(), isExclusive(), isAuth()
     */
    public int getPermissions()
    {
        return this.permissions;
    }
    
    /**
     * Sets the permissions of the file
     * @param permissions the permissions of the file as an integer (e.g. 0755).
     */
    public void setPermissions(int permissions)
    {
        this.permissions = permissions;
    }
    
    /**
     * Makes the file read-only (e.g. rwxrwxr-x gets turned to r-xrr-xr-x).
     */
    public void setReadOnly()
    {
        this.permissions |= 0555;
    }
    
    /**
     * Checks to see if this file allows the mode (permissions plus flags) of
     * the file to be changed. This is called when the server receives a Twstat
     * message. This default implementation does nothing.
     * @param newMode the new mode of the file (permissions plus any other flags
     * such as DMDIR, DMAPPEND, DMEXCL, DMAUTH)
     * @throws StyxException if the mode of this file cannot be changed
     */
    public void checkSetMode(long newMode) throws StyxException
    {
        return;
    }
    
    /**
     * Sets the mode of the file (permissions plus other flags). Must check all 
     * relevant permissions and call checkSetMode() before calling this method
     * as the system will assume that this method will always succeed.
     */
    public void setMode(long newMode)
    {
        this.appendOnly = ((newMode & StyxUtils.DMAPPEND) == StyxUtils.DMAPPEND);
        this.exclusive = ((newMode & StyxUtils.DMEXCL) == StyxUtils.DMEXCL);
        this.auth = ((newMode & StyxUtils.DMAUTH) == StyxUtils.DMAUTH);
        this.permissions = (int)(newMode & 1023);
    }
    
    /**
     * Check to see if the length of this file can be changed to the given
     * value. If this does not throw an exception then the setLength() method
     * will be assumed to be guaranteed to succeed. Subclasses should make sure
     * that this method throws an exception if this StyxFile is a directory.
     * This default implementation always throws an exception; subclasses should
     * override this method if they wish to allow the length of this file to
     * be changed.
     */
    public void checkSetLength(ULong newLength) throws StyxException
    {
        throw new StyxException("cannot change the length of this file directly");
    }
    
    /**
     * Sets the length of the file. Should check for all relevant
     * permissions and call checkSetLength() before calling this. The
     * system will assume that this method is guaranteed to succeed (it throws
     * no exceptions and returns no value). This default implementation does
     * nothing; subclasses should override it.
     */
    public void setLength(ULong newLength)
    {
        return;
    }
    
    /**
     * @throws StyxException if the last modified time cannot be changed directly
     * (i.e. with a Twstat message)
     */
    public void checkSetLastModifiedTime(long lastModifiedTime) throws StyxException
    {
        return;
    }
    
    /**
     * Sets the last modified time of the file. Should check for all relevant
     * permissions and, for a Twstat message,  call checkSetLastModifiedTime()
     * before calling this. The system will assume that this method is guaranteed
     * to succeed (it throws no exceptions and returns no value).
     * This method also always sets the last access time to the same value.
     * @param lastModifiedTime time as represented in a stat entry (e.g. in a 
     * TwstatMessage), i.e. the number of seconds (not milliseconds) since the
     * epoch (Jan 1 00:00 1970 GMT)
     * @param user The user who is modifying the file
     */
    public void setLastModified(long lastModifiedTime, User user)
    {
        this.lastModifiedTime = lastModifiedTime;
        this.setLastAccessTime(lastModifiedTime);
        this.lastModifiedBy = user.getUsername();
    }
    
    public void setLastAccessTime(long lastAccessTime)
    {
        this.lastAccessTime = lastAccessTime;
    }
    
    /**
     * Renames this file to the given name.  If a file with the given name already
     * exists in the parent directory this method will throw a StyxException.
     * Also throws a StyxException if this is the root directory.
     */
    public void rename(String newName) throws StyxException
    {
        if (this.getParent() == null && this.getParent() == this)
        {
            throw new StyxException("Cannot change the name of the root directory");
        }
        else
        {
            // This has a valid parent that is not itself (so this isn't the root
            // directory)
            if (this.getParent().childExists(newName))
            {
                throw new StyxException("A file with name " + newName + 
                    " already exists in this directory");
            }
            else
            {
                this.name = newName;
            }
        }
    }
    
    /**
     * Gets the unique numeric ID for the path of this file (generated from the
     * low-order bytes of the creation time and the hashcode of the full path).
     * If the file is deleted and re-created the unique ID will change (except
     * for the extremely unlikely case in which the low-order bytes of the creation
     * time happen to be the same in the new file and the old file).
     */
    private long getUniqueID()
    {
        // Get the low-order bytes of the creation time
        long timeBytes = this.creationTime & 0xffffffffL;
        // Create the ID from the low-order bytes of the creation time and use
        // the hashcode of the path as the high-order bytes of the ID
        return (this.getFullPath().hashCode() << 32) | timeBytes;
    }
    
    /**
     * Gets the mode of this file (permissions and flags)
     */
    private long getMode()
    {
        return this.getType() | this.permissions;
    }
    
    /**
     * Reads data from this file. This method could be synchronized in subclasses,
     * but watch out for blocks if the read is expected to take some time to
     * complete. Subclasses must make sure they reply to the read request by
     * creating a java.nio.ByteBuffer or byte array of data, then calling the 
     * appropriate readReply() (this can be done at any time; it does not have 
     * to be done within the read() method).
     *
     * This default implementation simply throws a StyxException, which will 
     * result in an Rerror message being sent back to the client. Subclasses 
     * should override this to provide the desired behaviour when the file is
     * read.
     *
     * @param client The client that is performing the read
     * @param offset The point in the file at which to start reading
     * @param count The maximum number of bytes to read
     * @param tag The tag of the incoming Tread message (this is needed when
     * calling readReply())
     */
    public void read(StyxFileClient client, long offset, int count, int tag)
        throws StyxException
    {
        throw new StyxException("Cannot read from this file");
    }
    
    /**
     * Writes data to this file. Must check that the file is open for writing
     * before this. We have already dealt with the possibility that this is an
     * append-only file before calling this method so subclasses do not need to
     * check this. Subclasses must make sure they reply to the write request by
     * calling writeReply() (although this can be done at any time; it does not
     * have to be done within the write() method).
     *
     * After this method is called, ByteBuffer containing the data will be 
     * returned to the pool. If subclasses wish to keep the ByteBuffer after
     * this method is complete, they should call data.acquire() to increase
     * the reference count to the buffer.
     *
     * This default implementation simply throws a StyxException, which will 
     * result in an Rerror message being sent back to the client. Subclasses 
     * should override this to provide the desired behaviour when the file is
     * written to.
     *
     * @param client The client that is performing the write operation
     * @param offset The place in the file where the new data will be added
     * @param count The number of bytes to write
     * @param data The data to write. The position and limit of this ByteBuffer
     * will be set correctly, but subclasses should note that the position might
     * not be zero.
     * @param truncate If this is true the file will be truncated at the end of 
     * the new data
     * @param tag The tag of the incoming Twrite message (this is needed when
     * calling writeReply())
     */
    public void write(StyxFileClient client, long offset, int count,
        ByteBuffer data, boolean truncate, int tag)
        throws StyxException
    {
        throw new StyxException("Cannot write to this file");
    }
    
    /**
     * Refreshes this file (if it represents another entity, such as a file on disk,
     * this method is used to make sure that the file metadata (length, access
     * time etc) are up to date. This default implementation does nothing; 
     * subclasses must override this to provide the correct functionality
     */
    public synchronized void refresh()
    {
        return;
    }
    
    /**
     * Removes this file from the Styx server
     */
    public synchronized void remove() throws StyxException
    {
        // TODO: abort outstanding i/o (search for outstanding tags)
        this.delete();
        // TODO: what if we're trying to remove the root directory?
        this.getParent().removeChild(this);
        return;
    }    
    
    /**
     * Called when the file is removed from the server. Aborts all outstanding
     * i/o and frees any resources associated with the file. This default
     * implementation does nothing; subclasses should implement appropriate
     * methods if necessary.
     */
    protected synchronized void delete()
    {
        return;
    }
    
    /**
     * Adds the given client to the file's list of connected clients.
     * Records the mode with which the client has the file open. Fires the
     * clientConnected() event
     */
    public void addClient(StyxFileClient client)
    {
        this.clients.add(client);
        this.clientConnected(client);
    }
    
    /**
     * Called after a client connects to this file (i.e. opens it).
     * This default implementation does nothing, but subclasses might want to
     * catch this event and do something, e.g. open file handles when the first
     * client has connected (i.e. when getNumClients() == 0)
     */
    protected void clientConnected(StyxFileClient client)
    {
        return;
    }
    
    /**
     * Gets the StyxFileClient associated with the given Session and fid, or null
     * if client does not exist
     */
    public StyxFileClient getClient(IoSession session, long fid)
    {
        synchronized (this.clients)
        {
            for (int i = 0; i < this.clients.size(); i++)
            {
                StyxFileClient client = (StyxFileClient)this.clients.get(i);
                if ( (client.getSession() == session) && (client.getFid() == fid) )
                {
                    return client;
                }
            }
        }
        return null;
    }
    
    /**
     * @return The number of clients that have this file open (rememember that
     * several open handles to this file might exist on the same connection.
     * This essentially counts the number of unique client/fid pairs)
     */
    public int getNumClients()
    {
        synchronized(this.clients)
        {
            this.removeDeadClients();
            return this.clients.size();
        }
    }
    
    /**
     * Checks for clients that have disconnected or been lost and removes them.
     * This may not be necessary if we can trust the architecture to reliably
     * determine when a client has disconnected, in which case this method
     * just adds unnecessary overhead.
     * @todo do logging to find out whether this method is ever necessary?
     */
    private void removeDeadClients()
    {
        synchronized(this.clients)
        {
            // Get the list of keys; the keys are the Session objects
            for (int i = 0; i < this.clients.size(); i++)
            {
                StyxFileClient client = (StyxFileClient)this.clients.get(i);
                IoSession session = client.getSession();
                if (session == null || !session.isConnected())
                {
                    this.removeClient(client);
                }
            }
        }
    }
    
    /**
     * Removes the client that is connected on the given session. Fires the
     * clientDisconnected event. If client is null, this will do nothing
     */
    public void removeClient(StyxFileClient client)
    {
        if (client != null)
        {
            this.clients.remove(client);
            this.clientDisconnected(client);
        }
    }
    
    /**
     * Called after a client disconnects from this file (i.e. clunks the fid).
     * This default implementation does nothing, but subclasses might want to
     * catch this event and do something, e.g. close file handles when the last
     * client has disconnected (i.e. when getNumClients() == 0).
     * @param client The client that has just disconnected from the file. This
     * will not be null.
     */
    protected void clientDisconnected(StyxFileClient client)
    {
        return;
    }
    
    /**
     * Method for processing a read request and replying appropriately to the
     * client, based on the contents of the file.  This method is used
     * for StyxFiles whose contents can be represented as a String (i.e. fairly
     * short files).
     * @param fileContents String representing the <b>entire contents</b> of 
     * the file.
     * @param client the StyxFileClient making the request
     * @param offset the index of the first byte in the file to return to the client
     * @param count the maximum number of bytes to return to the client
     * @param tag the tag of the incoming read message
     */
    public void processAndReplyRead(String fileContents, StyxFileClient client,
        long offset, int count, int tag)
    {
        // Convert the string to bytes using the UTF-8 character set
        byte[] bytes = StyxUtils.strToUTF8(fileContents);
        this.processAndReplyRead(bytes, client, offset, count, tag);
    }
    
    /**
     * Method for processing a read request and replying appropriately to the
     * client, based on the contents of the file.  This method is used
     * for StyxFiles whose contents can be represented as a byte array.
     * @param fileContents Byte array representing the <b>entire contents</b> of 
     * the file.
     * @param client the StyxFileClient making the request
     * @param offset the index of the first byte in the file to return to the client
     * @param count the maximum number of bytes to return to the client
     * @param tag the tag of the incoming read message
     */
    public void processAndReplyRead(byte[] fileContents, StyxFileClient client,
        long offset, int count, int tag)
    {
        // Check to see if the offset is beyond the end of the file
        if (offset >= fileContents.length)
        {
            // The client has reached end-of-file.  Return no bytes.
            this.replyRead(client, new byte[0], tag);
        }
        else
        {
            // Calculate the number of bytes to return to the client 
            int numBytesToReturn = Math.min(fileContents.length - (int)offset, count);
            // Now reply to the client
            this.replyRead(client, fileContents, (int)offset, numBytesToReturn, tag);
        }
    }
    
    /**
     * Method for processing a read request and replying appropriately to the
     * client, based on the contents of the file.  This method is used
     * for StyxFiles whose contents can be represented as a ByteBuffer.
     * @param fileContents ByteBuffer representing the <b>entire contents</b> of 
     * the file. The position and limit of this buffer will be unchanged by this
     * method.  This can be null: in this case all read requests will return zero bytes.
     * @param client the StyxFileClient making the request
     * @param offset the index of the first byte in the file to return to the client
     * @param count the maximum number of bytes to return to the client
     * @param tag the tag of the incoming read message
     */
    public void processAndReplyRead(ByteBuffer fileContents, StyxFileClient client,
        long offset, int count, int tag)
    {
        if (fileContents == null || offset >= fileContents.limit())
        {
            // Attempt to read off the end of the file, or no data have yet
            // been written to the file
            this.replyRead(client, new byte[0], tag);
        }
        else
        {
            int numBytesToReturn = Math.min(fileContents.limit() - (int)offset, count);
            // Remember the position and limit of the buffer
            int oldPos = fileContents.position();
            int oldLimit = fileContents.limit();
            // Set the position and limit of the buffer so that the correct bytes
            // get returned
            fileContents.position((int)offset);
            fileContents.limit((int)offset + numBytesToReturn);
            this.replyRead(client, fileContents, tag);
            // Reset the buffer position and limit
            fileContents.position(oldPos);
            fileContents.limit(oldLimit);
        }
    }
    
    /**
     * Method to reply to a Read message. One of the replyRead() methods
     * must be called by all subclasses when sending data back to the client in
     * response to a read request.
     * @param client The connection on which the reply will be sent
     * @param bytes The data to include in the message. All the data in this 
     * array will be written
     * @param tag The tag to be attached to the message
     */
    protected void replyRead(StyxFileClient client, byte[] bytes, int tag)
    {
        this.replyRead(client, bytes, 0, bytes.length, tag);
    }
    
    /**
     * Method to reply to a Read message. One of the replyRead() methods
     * must be called by all subclasses when sending data back to the client in
     * response to a read request. Leaves the position of the input ByteBuffer
     * unchanged.
     * @param client The connection on which the reply will be sent
     * @param buf a java.nio.ByteBuffer containing the data to write to the file.
     * All the remaining data in the buffer will be sent back to the client.
     * @param tag The tag to be attached to the message
     */
    protected void replyRead(StyxFileClient client, java.nio.ByteBuffer buf, int tag)
    {
        byte[] bytes;
        if (buf.hasArray())
        {
            // We can just use the backing array for this buffer
            bytes = buf.array();
            // Write the right number of bytes from the right position in the array
            this.replyRead(client, bytes, buf.position(), buf.remaining(), tag);
        }
        else
        {
            // We must copy the data from the array
            int oldPos = buf.position();
            bytes = new byte[buf.remaining()];
            buf.get(bytes);
            buf.position(oldPos);
            this.replyRead(client, bytes, tag);
        }
    }
    
    /**
     * Method to reply to a Read message. One of the replyRead() methods
     * must be called by all subclasses when sending data back to the client in
     * response to a read request. Leaves the position of the input ByteBuffer
     * unchanged.  The buffer that is provided to this method will be released
     * automatically so users of this method should not release the buffer
     * themselves.
     * @param client The connection on which the reply will be sent
     * @param buf a org.apache.mina.common.ByteBuffer containing the data to
     * write to the file.  All the remaining data in the buffer will be sent
     * back to the client.
     * @param tag The tag to be attached to the message
     */
    protected void replyRead(StyxFileClient client, ByteBuffer buf, int tag)
    {
        RreadMessage rReadMsg = new RreadMessage(buf);
        rReadMsg.setTag(tag);
        this.replyRead(client, rReadMsg);
    }
    
    /**
     * Method to reply to a Read message. One of the replyRead() methods
     * must be called by all subclasses when sending data back to the client in
     * response to a read request.
     * @param client The connection on which the reply will be sent
     * @param bytes The data to include in the message.
     * @param pos The index of the first byte in the array to be written
     * @param count The number of bytes in the array to write
     * @param tag The tag to be attached to the message
     */
    protected void replyRead(StyxFileClient client, byte[] bytes, int pos,
        int count, int tag)
    {
        RreadMessage rReadMsg = new RreadMessage(bytes, pos, count);
        rReadMsg.setTag(tag);
        this.replyRead(client, rReadMsg);
    }
    
    /**
     * Method to reply to a Read message. One of the replyRead() methods
     * must be called by all subclasses when sending data back to the client in
     * response to a read request.
     * @param client The connection on which the reply will be sent
     * @param rReadMsg The RreadMessage to send back to the client. The tag of
     * this message must be set correctly.
     */
    protected void replyRead(StyxFileClient client, RreadMessage rReadMsg)
    {
        IoSession session = client.getSession();
        StyxSessionState sessionState = (StyxSessionState)session.getAttachment();
        synchronized (sessionState)
        {
            int tag = rReadMsg.getTag();
            // If the tag has been flushed, don't reply
            if (sessionState.tagInUse(tag))
            {
                this.setLastAccessTime(StyxUtils.now());
                session.write(rReadMsg);
                sessionState.releaseTag(tag);
            }
        }
    }
    
    /**
     * Method to reply to a Write message. This must be called by all subclasses
     * when sending data back to the client in response to a write request.
     * @param client The connection on which the reply will be sent
     * @param count The number of bytes actually written to the file in question.
     * @param tag The tag to be attached to the message
     */
    protected void replyWrite(StyxFileClient client, int count, int tag)
    {
        IoSession session = client.getSession();
        StyxSessionState sessionState = (StyxSessionState)session.getAttachment();
        synchronized (sessionState)
        {
            // If the tag has been flushed, don't reply
            if (sessionState.tagInUse(tag))
            {
                this.setLastModified(StyxUtils.now(), sessionState.getUser());
                this.contentsChanged();
                RwriteMessage rWriteMsg = new RwriteMessage(count);
                rWriteMsg.setTag(tag);
                session.write(rWriteMsg);
                sessionState.releaseTag(tag);
            }
        }
    }
    
    public long getVersion()
    {
        return this.version;
    }
    
    /**
     * Call this to indicate that the file's data have changed. This is called
     * automatically when the file is written to with a TwriteMessage. This
     * default implementation simply increments the version number with a call
     * to this.incrementVersion(). Subclasses may override this method, for
     * example to notify waiting clients that the data have changed. This will
     * fire the contentsChanged() event on any registered StyxFileChangeListeners.
     */
    public void contentsChanged()
    {
        this.incrementVersion();
        this.fireContentsChanged();
    }
    
    /**
     * Increments the file's version number and ensures that the version number
     * never exceeds the maximum value of an unsigned 4-byte integer (it wraps
     * back to zero if the version number gets this large)
     */
    protected final void incrementVersion()
    {
        this.version++;
        if (this.version > StyxUtils.MAXUINT)
        {
            this.version = 0;
        }        
    }
    
    /**
     * Registers a StyxFileChangeListener.  If it is already registered, this
     * method does nothing.
     */
    public void addChangeListener(StyxFileChangeListener listener)
    {
        synchronized(this.changeListeners)
        {
            if (!this.changeListeners.contains(listener))
            {
                this.changeListeners.add(listener);
            }
        }
    }
    
    /**
     * Removes a change listener.  If the given change listener is not registered,
     * this method does nothing.
     */
    public void removeChangeListener(StyxFileChangeListener listener)
    {
        synchronized(this.changeListeners)
        {
            this.changeListeners.remove(listener);
        }
    }
    
    /**
     * This method is called when the contents of the file change. This can be 
     * due to two things: (1) When a client writes to the file using a
     * TwriteMessage. In this case this method is called just before the Rwrite
     * message is sent to the client. (2) It is also called when
     * this.contentsChanged() is called.
     */
    protected void fireContentsChanged()
    {
        synchronized(this.changeListeners)
        {
            StyxFileChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (StyxFileChangeListener)this.changeListeners.get(i);
                listener.fileContentsChanged();
            }
        }
    }
}
