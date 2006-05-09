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

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import org.apache.mina.common.IoSession;

import uk.ac.rdg.resc.jstyx.types.Qid;
import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Class containing information about the state of a particular Styx connection,
 * from the point of view of the server.  This is attached to the Session object.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.4  2006/03/20 17:51:50  jonblower
 * Adding authentication to base JStyx system
 *
 * Revision 1.3  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.1  2005/03/09 19:44:18  jonblower
 * Changes concerned with migration to MINA
 *
 * Revision 1.2  2005/02/24 17:52:32  jonblower
 * Constructor for StyxSessionState no longer throws StyxException
 *
 * Revision 1.1.1.1  2005/02/16 18:58:35  jonblower
 * Initial import
 *
 */
public class StyxSessionState
{
    private IoSession session;         // The object representing the 
                                       // connection to the client
    private boolean versionNegotiated; // True when the version and message size
                                       // has been negotiated (i.e. after exchange
                                       // of Tversion/Rversion messages)
    private long maxMessageSize;       // The maximum size of message that will
                                       // be sent or received on this connection
    private User user;               // The name of the remote user
    private boolean authenticated;     // True if this is an authenticated connection
    
    // Need to keep track of all open fids and tags
    private Hashtable fidsInUse;       // Maps fids to StyxFiles
    private Vector tagsInUse;          // The tags that are currently awaiting
                                       // reply on this connection
    
    private static final int EXECUTE = 0; // These constants are used by checkPermissions()
    private static final int WRITE = 1;   // The values of these constants are meaningful
    private static final int READ = 2;    // and should NOT be changed
    
    /** Creates a new instance of StyxSessionState */
    public StyxSessionState(IoSession session)
    {
        this.versionNegotiated = false;
        this.maxMessageSize = 0;
        this.user = null;
        this.fidsInUse = new Hashtable();
        this.tagsInUse = new Vector();
        this.authenticated = false;
        this.session = session;
    }
    
    /**
     * @return true if the protocol version has been negotiated. If this returns
     * false then the protocol version must be negotiated with a Tversion/Rversion
     * exchange before any other messages can be processed.
     */
    public boolean isVersionNegotiated()
    {
        return this.versionNegotiated;
    }
    
    /**
     * @return the maximum size of message that is permitted on this connection.
     * Note that the max message size can only be set by resetting the session
     * in response to a TversionMessage (see resetSession())
     */
    public long getMaxMessageSize()
    {
        return this.maxMessageSize;
    }    
    
    /**
     * Aborts all outstanding i/o on this connection (called after receiving
     * a TversionMessage)
     */
    public synchronized void resetSession(long maxMessageSize)
    {
        // TODO: Clunk all outstanding fids
        // TODO: Release all outstanding tags
        this.versionNegotiated = true;
        this.maxMessageSize = maxMessageSize;
    }

    /**
     * @return the remote user
     */
    public User getUser()
    {
        return user;
    }

    /**
     * Sets the remote user (in response to an RattachMessage)
     */
    public void setUser(User user)
    {
        this.user = user;
    }
    
    /**
     * Associates a fid with a file.  You must check that the fid is not in 
     * use before calling this or the old fid will be forgotten about.
     */
    public void associate(long fid, StyxFile file)
    {
        synchronized(this.fidsInUse)
        {
            // First remove any previous association with this fid
            // (used legitimately when using a TwalkMessage where the newFid is
            // the same as the old fid, or when a TcreateMessage arrives with
            // a fid that is the same as an existing one)
            this.fidsInUse.remove(new Long(fid));
            this.fidsInUse.put(new Long(fid), file);
        }
    }
    
    /**
     * @return true if the given fid is already in use
     */
    public boolean fidInUse(long fid)
    {
        return this.fidsInUse.containsKey(new Long(fid));
    }
    
    /**
     * @return true if the given tag is already in use
     */
    public boolean tagInUse(int tag)
    {
        return this.tagsInUse.contains(new Integer(tag));
    }
    
    /**
     * Adds the given tag to the list of tags in use, first checking to see if
     * it is already in use (i.e. no need to call tagInUse() before calling
     * this)
     * @throws TagInUseException if the given tag is already in use
     */
    public synchronized void addTag(int tag) throws TagInUseException
    {
        synchronized (this.tagsInUse)
        {
            if (this.tagInUse(tag))
            {
                throw new TagInUseException(tag);
            }
            this.tagsInUse.add(new Integer(tag));
        }
    }
    
    /**
     * Called when a message is replied to, releasing the tag. TODO: should we 
     * throw an exception if the tag does not exist?
     */
    public void releaseTag(int tag)
    {
        this.tagsInUse.remove(new Integer(tag));
    }
    
    /**
     * Called in response to a Tflush message to abort a previous message
     */
    public void flushTag(int tag)
    {
        // TODO: abort any outstanding i/o (reads or writes) associated with
        // the previous message
        this.releaseTag(tag);
    }
    
    /**
     * Flushes all tags open in this session (called when a client disconnects)
     */
    public void flushAll()
    {
        synchronized (this.tagsInUse)
        {
            Enumeration en = this.tagsInUse.elements();
            while(en.hasMoreElements())
            {
                int tag = ((Integer)en.nextElement()).intValue();
                this.flushTag(tag);
            }
        }
    }
    
    /**
     * @return The StyxFile that's associated with the given fid
     * @throws FidNotFoundException if there is no StyxFile associated with the
     * given fid
     */
    public StyxFile getStyxFile(long fid) throws FidNotFoundException
    {
        StyxFile sf = (StyxFile)this.fidsInUse.get(new Long(fid));
        if (sf == null)
        {
            throw new FidNotFoundException(fid);
        }
        return sf;
    }
    
    /**
     * Forgets about the file represented by the fid (i.e. closes it)
     * @throws FidNotFoundException if there is no StyxFile associated with
     * this fid
     */
    public void clunk(long fid) throws FidNotFoundException
    {
        synchronized(this.fidsInUse)
        {
            if (!fidInUse(fid))
            {
                throw new FidNotFoundException(fid);
            }
            StyxFile sf = this.getStyxFile(fid);
            synchronized(sf)
            {
                // Check to see if client has requested that this file is
                // deleted on clunk
                StyxFileClient sfc = sf.getClient(this.session, fid);
                // sfc could be null - the client might not have this file open
                if (sfc != null && sfc.deleteOnClunk())
                {
                    try
                    {
                        sf.remove();
                    }
                    catch(StyxException se)
                    {
                        // if there was a problem removing the file, ignore it
                        // (the particular StyxFile may not allow itself to be
                        // removed)
                    }
                }
                // Remove this client from the StyxFile. This fires the 
                // clientDisconnected event on the StyxFile.
                sf.removeClient(sfc);
            }
            this.fidsInUse.remove(new Long(fid));
        }
    }
    
    /**
     * Clunks all fids open in this session (called when a client disconnects)
     */
    public void clunkAll()
    {
        synchronized(this.fidsInUse)
        {
            Enumeration en = this.fidsInUse.keys();
            while (en.hasMoreElements())
            {
                try
                {
                    long fid = ((Long)en.nextElement()).longValue();
                    this.clunk(fid);
                }
                catch (FidNotFoundException fnfe)
                {
                    // ignore this, we are closing down anyway
                }
            }
        }
    }
    
    /**
     * Checks that the given file can be opened with the given mode
     * @throws StyxException if not successful
     */
    public void checkOpen(StyxFile sf, int mode) throws StyxException
    {
        // Check to see if the file is permanently an exclusive-use file
        if (sf.isExclusive())
        {
            if (sf.getNumClients() != 0)
            {
                throw new StyxException("can't open locked file");
            }
        }
        // Mask off the last two bits; these contain the type of I/O (r, w, x)
        int openMode = mode & 3;
        switch (openMode)
        {
            case StyxUtils.OREAD:
                if (!checkPermissions(sf, this.READ))
                {
                    throw new StyxException("read permission denied");
                }
                break;
            case StyxUtils.OWRITE:
                if (!checkPermissions(sf, this.WRITE))
                {
                    throw new StyxException("write permission denied");
                }
                break;
            case StyxUtils.ORDWR:
                if (!checkPermissions(sf, this.READ))
                {
                    throw new StyxException("read permission denied");
                }
                if (!checkPermissions(sf, this.WRITE))
                {
                    throw new StyxException("write permission denied");
                }
                break;
            case StyxUtils.OEXEC:
                if (!checkPermissions(sf, this.EXECUTE))
                {
                    throw new StyxException("execute permission denied");
                }
                break;
            default:
                // Shouldn't happen
                throw new IllegalStateException("openMode = " + openMode + 
                    ": should be between 0 and 3");
        }
        if ((mode & StyxUtils.OTRUNC) == StyxUtils.OTRUNC)
        {
            // Can't truncate a directory
            if(sf.isDirectory())
            {
                throw new StyxException("Cannot truncate a directory");
            }
            // Need to have write permission to truncate            
            if (!checkPermissions(sf, this.WRITE))
            {
                throw new StyxException("need write permissions to truncate a file");
            }
        }
        if ((mode & StyxUtils.ORCLOSE) == StyxUtils.ORCLOSE)
        {
            // Can't delete a directory on closing
            if (sf.isDirectory())
            {
                throw new StyxException("Cannot automatically delete a directory when fid is clunked");
            }
            // Need to have write permissions on the parent directory and the
            // file itself to delete the file on clunking its fid
            if (!checkPermissions(sf.getParent(), this.WRITE))
            {
                throw new StyxException("need write permissions on the parent "
                    + "directory to delete the file when fid is clunked");
            }
            // TODO: do we need write permissions on the file itself?
        }
        return;
    }
    
    /**
     * Checks to see if the current user has permission to write in the given
     * file or directory
     */
    public boolean checkWrite(StyxFile sf)
    {
        return this.checkPermissions(sf, this.WRITE);
    }
    
    /**
     * Checks to see if the current user has permissions to execute the given
     * file (or, in the case of a directory, to open it)
     * @todo Call this "CheckEnter" and make sure that sf is a directory?
     */
    public boolean checkExecute(StyxFile sf)
    {
        return this.checkPermissions(sf, this.EXECUTE);
    }
    
    /**
     * Checks the permissions for a given mode
     * @param perms the file permissions (e.g. 0755)
     * @param mode the mode to check (EXECUTE, WRITE or READ)
     */
    private boolean checkPermissions(StyxFile sf, int mode)
    {
        if (mode < 0 || mode > 2)
        {
            throw new IllegalArgumentException("Internal error: mode should be 0, 1 or 2");
        }
        // This is the cunning bit: we bit-shift the permissions value so that 
        // the mode in question is represented by the last bit (all), the
        // fourth-to-last bit (group) and the seventh-to-last bit (user).
        // So if we started with a mode of 0755 (binary 111101101, rwxrwxrwx)
        // and we want to check write permissions, we shift by one bit so that 
        // the value of "perms" is 11110110, rwxrwxrw)
        int perms = sf.getPermissions() >> mode;
        // Check permissions for "all"; this is the last bit in the
        // permissions number
        if (perms % 2 == 1)
        {
            return true;
        }
        // Check group permissions
        if ( (perms >> 3) % 2 == 1)
        {
            // The group has the right permissions; now we have to find if the user
            // is a member of the group to which the file belongs
            if (this.user.isMemberOf(sf.getGroup()))
            {
                return true;
            }
        }
        // Check owner permissions
        if ( (perms >> 6) % 2 == 1)
        {
            // the owner has the right permissions; now we check that the user
            // is the owner of the file
            if (this.user.getUsername().equals(sf.getOwner()))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the maximum amount of data (not including the header) that can be
     * sent in a Styx Message
     */
    public long getIOUnit()
    {
        // A Twrite message has 23 bytes of header. We add a header byte "for luck",
        // since this is what Inferno seems to do
        return this.maxMessageSize - 24;
    }
    
}
