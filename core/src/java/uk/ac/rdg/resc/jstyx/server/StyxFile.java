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

import net.gleamynode.netty2.Session;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

import uk.ac.rdg.resc.jstyx.messages.RreadMessage;
import uk.ac.rdg.resc.jstyx.messages.RwriteMessage;

import uk.ac.rdg.resc.jstyx.StyxBuffer;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.StyxUtils;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.types.Qid;
import uk.ac.rdg.resc.jstyx.types.ULong;

/**
 * Abstract class representing a file (or directory) on a Styx server. There may
 * be different types of file; a file might map directly to a file on disk, or 
 * it may be a synthetic file representing a program interface.
 * Currently each StyxFile has exactly one parent. Therefore symbolic links
 * on the host filesystem cannot currently be handled.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:32  jonblower
 * Initial revision
 *
 */
public abstract class StyxFile
{
    
    protected String name;           // The name of the file
    protected StyxDirectory parent;  // The parent of the file (N.B. the root file
                                     // has no parent so this will be null)
    
    protected boolean directory;     // True if this is a directory
    private boolean appendOnly;      // True if this is an append-only file
    private boolean exclusive;       // True if this file can be opened by only one client at a time
    private boolean auth;            // True if this is a file to be used by the authentication mechanism
                                     // (normally false)
    private int permissions;         // Permissions represented as a number (e.g. 0755 in octal)
    
    private long version;            // Version of the file (incremented when it is modified)
                                     // This can only be modified through the incrementVersion()
                                     // method
    
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
     * @todo: check that the name is valid (no trailing or leading slashes unless
     * it is the root directory, no spaces)
     * @todo: according to the Manual, the parent of the root of the tree is itself
     * @throws StyxException if an attempt is made to create a file with the name
     * "." or ".."
     */
    public StyxFile(String name, String owner, String group, int permissions,
        boolean isAppendOnly, boolean isExclusive, boolean isAuth)
        throws StyxException
    {
        if (name.equals(".") || name.equals(".."))
        {
            throw new StyxException("illegal file name");
        }
        this.parent = null;
        this.name = name.trim();
        this.directory = false;
        this.permissions = permissions;
        this.appendOnly = isAppendOnly;
        this.exclusive = isExclusive;
        this.auth = isAuth;
        this.version = 0;
        this.lastAccessTime = StyxUtils.now();
        this.lastModifiedTime = StyxUtils.now();
        this.owner = owner.trim();
        this.group = group.trim();
        this.lastModifiedBy = "";
        this.clients = new Vector();
        this.changeListeners = new Vector();
    }
    
    public StyxFile(String name, String userID, String groupID, int permissions,
        boolean isAppendOnly, boolean isExclusive)
        throws StyxException
    {
        this(name, userID, groupID, permissions, isAppendOnly,
            isExclusive, false);
    }
    
    /**
     * Creates a StyxFile with the default username and group
     */
    public StyxFile(String name, int permissions, boolean isAppendOnly,
        boolean isExclusive) throws StyxException
    {
        // TODO specify the default user and group in a config file?
        this(name, "jdb", "anongrp", permissions, isAppendOnly, isExclusive);
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
        return this.parent.getFullPath() + "/" + this.getName();
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
    public void setLastModified(long lastModifiedTime, String user)
    {
        this.lastModifiedTime = lastModifiedTime;
        this.setLastAccessTime(lastModifiedTime);
        this.lastModifiedBy = user.trim();
    }
    
    public void setLastAccessTime(long lastAccessTime)
    {
        this.lastAccessTime = lastAccessTime;
    }
    
    /**
     * Gets the unique numeric ID for the path of this file (simply the hashCode
     * of the full path of this file)
     */
    private long getUniqueID()
    {
        return this.getFullPath().hashCode();
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
     * creating a ByteBuffer of data, then calling readReply() (although this 
     * can be done at any time; it does not have to be done within the read()
     * method).
     * @param client The client that is performing the read
     * @param offset The point in the file at which to start reading
     * @param count The maximum number of bytes to read
     * @param tag The tag of the incoming Tread message (this is needed when
     * calling readReply())
     */
    public abstract void read(StyxFileClient client, long offset, long count, int tag)
        throws StyxException;
    
    /**
     * Writes data to this file. Must check that the file is open for writing
     * before this. We have already dealt with the possibility that this is an
     * append-only file before calling this method so subclasses do not need to
     * check this. Subclasses must make sure they reply to the write request by
     * calling writeReply() (although this can be done at any time; it does not
     * have to be done within the write() method).
     * @param client The client that is performing the write operation
     * @param offset The place in the file where the new data will be added
     * @param count The number of bytes to write
     * @param data The data to write
     * @param user The user that is performing the write operation
     * @param truncate If this is true the file will be truncated at the end of 
     * the new data
     * @param tag The tag of the incoming Twrite message (this is needed when
     * calling writeReply())
     */
    public abstract void write(StyxFileClient client, long offset, long count,
        ByteBuffer data, String user, boolean truncate, int tag)
        throws StyxException;
    
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
    public StyxFileClient getClient(Session session, long fid)
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
                Session session = client.getSession();
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
     * Method to reply to a Read message. This must be called by all subclasses
     * when sending data back to the client in response to a read request.
     * @param session The connection on which the reply will be sent
     * @param buf The data to include in the message. The position and limit
     * of this buffer should be set appropriately; all data between the position
     * and the limit will be sent (often this is done by calling flip() on the 
     * buffer)
     * @param tag The tag to be attached to the message
     */
    protected void replyRead(StyxFileClient client, ByteBuffer buf, int tag)
    {
        Session session = client.getSession();
        StyxSessionState sessionState = (StyxSessionState)session.getAttachment();
        synchronized (sessionState)
        {
            // If the tag has been flushed, don't reply
            if (sessionState.tagInUse(tag))
            {
                this.setLastAccessTime(StyxUtils.now());
                RreadMessage rReadMsg = new RreadMessage(buf);
                rReadMsg.setTag(tag);
                session.write(rReadMsg);
                sessionState.releaseTag(tag);
            }
        }
    }
    
    /**
     * Method to reply to a Write message. This must be called by all subclasses
     * when sending data back to the client in response to a write request.
     * @param session The connection on which the reply will be sent
     * @param count The number of bytes actually written to the file in question.
     * @param tag The tag to be attached to the message
     */
    protected void replyWrite(StyxFileClient client, long count, int tag)
    {
        Session session = client.getSession();
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
     * This method is called when the contents of the file change. To be precise,
     * it is called just before the Rwrite message is sent to the client. It is
     * also called when the contentsChanged() method is called.
     */
    protected void fireContentsChanged()
    {
        synchronized(this.changeListeners)
        {
            StyxFileChangeListener listener;
            for (int i = 0; i < this.changeListeners.size(); i++)
            {
                listener = (StyxFileChangeListener)this.changeListeners.get(i);
                listener.contentsChanged();
            }
        }
    }
}
