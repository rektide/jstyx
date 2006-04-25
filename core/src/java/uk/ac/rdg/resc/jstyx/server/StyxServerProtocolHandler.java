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

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;

import org.apache.log4j.Logger;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.StyxException;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.types.Qid;

import uk.ac.rdg.resc.jstyx.messages.*;

/**
 * ProtocolHandler listener for a StyxServer (replaces StyxServerSessionListener
 * from Netty).
 * @todo Should these methods be more thread-safe, i.e. should we make sure that
 * each method that affects a StyxFile is synchronized on that StyxFile?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.16  2006/03/21 14:58:42  jonblower
 * Implemented clear-text password-based authentication and did some simple tests
 *
 * Revision 1.15  2006/03/21 09:06:15  jonblower
 * Still implementing authentication
 *
 * Revision 1.14  2006/03/20 17:51:50  jonblower
 * Adding authentication to base JStyx system
 *
 * Revision 1.13  2005/12/01 08:21:56  jonblower
 * Fixed javadoc comments
 *
 * Revision 1.12  2005/11/03 17:09:27  jonblower
 * Created more efficient RreadMessage that involves less copying of buffers (still reliable)
 *
 * Revision 1.11  2005/09/08 07:08:59  jonblower
 * Removed "String user" from list of parameters to StyxFile.write()
 *
 * Revision 1.10  2005/05/19 14:46:51  jonblower
 * Changed behaviour of StyxDirectory.createChild(): no longer adds file to namespace in this method
 *
 * Revision 1.9  2005/05/10 19:19:44  jonblower
 * Added call to StyxMessage.dispose() in messageSent() callback
 *
 * Revision 1.8  2005/05/05 16:57:38  jonblower
 * Updated MINA library to revision 168337 and changed code accordingly
 *
 * Revision 1.7  2005/04/28 08:11:15  jonblower
 * Modified permissions handling in documentation directory of SGS
 *
 * Revision 1.6  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.5  2005/03/16 22:16:43  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.4  2005/03/16 17:56:24  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.3  2005/03/15 15:51:41  jonblower
 * Removed hard limit on maximum message size
 *
 * Revision 1.2  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.1.2.4  2005/03/11 08:30:30  jonblower
 * Moved to log4j logging system (from apache commons logging)
 *
 * Revision 1.1.2.3  2005/03/10 20:55:40  jonblower
 * Removed references to Netty
 *
 * Revision 1.1.2.2  2005/03/10 11:55:00  jonblower
 * Replaced StyxFile with StyxDirectory for root of server
 *
 * Revision 1.1.2.1  2005/03/10 08:10:41  jonblower
 * Initial import
 *
 */
public class StyxServerProtocolHandler implements IoHandler
{
    private static final Logger log = Logger.getLogger(StyxServerProtocolHandler.class);
    
    private StyxDirectory root; // Root of the file tree
    private StyxSecurityContext securityContext; // Security context
    
    public StyxServerProtocolHandler(StyxDirectory fileTreeRoot,
        StyxSecurityContext securityContext)
    {
        this.root = fileTreeRoot;
        this.securityContext = securityContext;
    }
    
    /**
     * Invoked when the session is created.  Initialize default socket
     * parameters and user-defined attributes here.
     */
    public void sessionCreated( IoSession session ) throws Exception
    {
        log.info( session.getRemoteAddress() + " CREATED" );
        ProtocolCodecFactory codec = StyxCodecFactory.getInstance();
        session.getFilterChain().addFirst(
            "protocolFilter", new ProtocolCodecFilter( codec ) );
    }
    
    public void sessionOpened(IoSession  session )
    {
        log.info( session.getRemoteAddress() + " OPENED" );
        session.setAttachment(new StyxSessionState(session));
    }
    
    public void sessionClosed(IoSession session )
    {
        StyxSessionState sessionState = (StyxSessionState)session.getAttachment();
        sessionState.clunkAll();
        sessionState.flushAll();
        log.info( session.getRemoteAddress() + " CLOSED" );
    }
    
    public void messageReceived(IoSession session, Object message )
    {
        if (log.isDebugEnabled())
        {
            log.debug( session.getRemoteAddress() + " RCVD: " + message );
        }
        
        // The cast is safe because we know we're only going to get StyxMessages
        StyxMessage styxMessage = (StyxMessage)message;
        
        // Get the tag for the message
        int tag = styxMessage.getTag();
        
        // Get the object representing the IoSession state
        StyxSessionState sessionState = (StyxSessionState)session.getAttachment();
        
        try
        {
            // Add the message's tag to the list of tags in use.
            sessionState.addTag(tag); // this will throw an exception if the tag is in use
            
            if (message instanceof TversionMessage)
            {
                replyVersion(session, sessionState, (TversionMessage)message, tag);
            }
            else if (message instanceof TauthMessage)
            {
                replyAuth(session, sessionState, (TauthMessage)message, tag);
            }
            else if (message instanceof TattachMessage)
            {
                replyAttach(session, sessionState, (TattachMessage)message, tag);
            }
            else if (message instanceof TflushMessage)
            {
                replyFlush(session, sessionState, (TflushMessage)message, tag);
            }
            else if (message instanceof TwalkMessage)
            {
                replyWalk(session, sessionState, (TwalkMessage)message, tag);
            }
            else if (message instanceof TopenMessage)
            {
                replyOpen(session, sessionState, (TopenMessage)message, tag);
            }
            else if (message instanceof TcreateMessage)
            {
                replyCreate(session, sessionState, (TcreateMessage)message, tag);
            }
            else if (message instanceof TreadMessage)
            {
                replyRead(session, sessionState, (TreadMessage)message, tag);
            }
            else if (message instanceof TwriteMessage)
            {
                replyWrite(session, sessionState, (TwriteMessage)message, tag);
            }
            else if (message instanceof TclunkMessage)
            {
                replyClunk(session, sessionState, (TclunkMessage)message, tag);
            }
            else if (message instanceof TremoveMessage)
            {
                replyRemove(session, sessionState, (TremoveMessage)message, tag);
            }
            else if (message instanceof TstatMessage)
            {
                replyStat(session, sessionState, (TstatMessage)message, tag);
            }
            else if (message instanceof TwstatMessage)
            {
                replyWstat(session, sessionState, (TwstatMessage)message, tag);
            }
            else
            {
                throw new StyxException("Not a valid Styx Tmessage");
            }
        }
        catch(TagInUseException tiue)
        {
            // Can't reply with an error to the client because we don't know 
            // what tag to use! Simply log the error and don't reply.
            log.error("tag " + tiue.getTag() + " already in use");
        }
        catch(FidNotFoundException fnfe)
        {
            reply(session, new RerrorMessage("fid " + fnfe.getFid() + 
                " not recognised"), tag);
        }
        catch(StyxException se)
        {
            reply(session, new RerrorMessage(se.getMessage()), tag);
        }
    }
    
    private void replyVersion(IoSession session, StyxSessionState sessionState,
        TversionMessage tVerMsg, int tag) throws StyxException
    {
        long proposedMessageSize = tVerMsg.getMaxMessageSize();
        long finalMessageSize = Math.min(proposedMessageSize, StyxUtils.MAX_MESSAGE_SIZE);
        if (!tVerMsg.getVersion().equals("9P2000"))
        {
            throw new StyxException("Error negotiating protocol version (must be 9P2000)");
        }
        // Reset the IoSession (aborts all outstanding i/o, sets the message
        // size and confirms that the version has been negotiated)
        sessionState.resetSession(finalMessageSize);
        reply(session, new RversionMessage(finalMessageSize, "9P2000"), tag);
    }
    
    private void replyAuth(IoSession session, StyxSessionState sessionState,
        TauthMessage tAuthMsg, int tag) throws StyxException
    {
        // This message isn't used by Inferno (which uses a different
        // authentication mechanism) but could be used in a different
        // scheme
        if (this.securityContext.supportsAuthentication())
        {
            // Check that the supplied fid isn't already in use
            if (sessionState.fidInUse(tAuthMsg.getAfid()))
            {
                throw new StyxException("Fid already in use");
            }
            // Create a new auth file for exchange of auth information
            // This will throw a StyxException if the given username is not 
            // recognized in the security context
            AuthFile authFile = new AuthFile(this.securityContext, tAuthMsg.getUName());
            sessionState.setUser(authFile.getUser());
            // Associate this with the given fid
            sessionState.associate(tAuthMsg.getAfid(), authFile);
            // Reply with the Qid of this auth file
            reply(session, new RauthMessage(authFile.getQid()), tag);
        }
        else
        {
            // Server does not support authentication
            throw new StyxException("Authentication not supported");
        }
    }
    
    private void replyAttach(IoSession session, StyxSessionState sessionState,
        TattachMessage tAttMsg, int tag) throws StyxException
    {
        if (!sessionState.isVersionNegotiated())
        {
            throw new StyxException("Tversion not seen");
        }
        if (tAttMsg.getAfid() == StyxUtils.NOFID)
        {
            // Client is seeking an unauthenticated connection
            if (this.securityContext.allowsAnonymousLogin())
            {
                sessionState.setUser(this.securityContext.getAnonymousUser());
            }
            else
            {
                throw new StyxException("Server does not allow anonymous logins");
            }
        }
        else
        {
            // Client is seeking an authenticated connection
            if (this.securityContext.supportsAuthentication())
            {
                // Get the AuthFile associated with the provided afid
                // This will throw a StyxException if the auth file has not been created
                AuthFile authFile = (AuthFile)sessionState.getStyxFile(tAttMsg.getAfid());
                // See if the user is properly authenticated with this file
                if (!authFile.isAuthenticated(tAttMsg.getUname()))
                {
                    throw new StyxException("User has not authenticated");
                }
            }
            else
            {
                throw new StyxException("Authentication not supported");
            }
        }
        // Check that the supplied fid isn't already in use
        if (sessionState.fidInUse(tAttMsg.getFid()))
        {
            throw new StyxException("Fid already in use");
        }
        // Associate the fid with the root of the server
        sessionState.associate(tAttMsg.getFid(), this.root);
        // Ignore the aname part of the TattachMessage (TODO)
        this.root.setLastAccessTime(StyxUtils.now());
        reply(session, new RattachMessage(this.root.getQid()), tag);
    }
    
    private void replyFlush(IoSession session, StyxSessionState sessionState,
        TflushMessage tFlushMsg, int tag) throws StyxException
    {
        sessionState.flushTag(tFlushMsg.getOldTag());
        reply(session, new RflushMessage(), tag);
    }
    
    private void replyWalk(IoSession session, StyxSessionState sessionState,
        TwalkMessage tWalkMsg, int tag) throws StyxException
    {
        if (tWalkMsg.getNumPathElements() > StyxUtils.MAXPATHELEMENTS)
        {
            throw new StyxException("Too many path elements in Twalk message");
        }
        long fid = tWalkMsg.getFid();
        
        // Have to check that this fid has not been opened by the client
        StyxFile sf = sessionState.getStyxFile(fid);
        StyxFileClient client = (StyxFileClient)sf.getClient(session, fid);
        if (client != null)
        {
            throw new StyxException("cannot walk an open fid");
        }
        
        long newFid = tWalkMsg.getNewFid();
        String[] pathEls = tWalkMsg.getPathElements();

        if (newFid != fid)
        {
            // If the original and new fids are different, check that the 
            // new fid isn't already in use
            if (sessionState.fidInUse(newFid))
            {
                throw new StyxException("Fid already in use");
            }
        }

        // Construct a blank RwalkMessage
        RwalkMessage rWalkMsg = new RwalkMessage(new Qid[0]);

        for (int i = 0; i < pathEls.length; i++)
        {
            if (!(sf instanceof StyxDirectory))
            {
                throw new StyxException(sf.getName() + " is not a directory");
            }
            if (!pathEls[i].equals(".."))
            {
                if (!sessionState.checkExecute(sf))
                {
                    // Only check file permissions if we're descending
                    // the hierarchy
                    throw new StyxException(sf.getName() + ": permission denied");
                }
            }
            sf.setLastAccessTime(StyxUtils.now());
            sf = ((StyxDirectory)sf).getChild(pathEls[i]);
            if (sf == null)
            {
                if (i == 0)
                {
                    throw new StyxException("file does not exist");
                }
                break;
            }
            // Note that this allows a client to get a fid representing
            // the directory at the end of the walk, even if the client
            // does not have execute permissions on that directory.
            // Therefore in Inferno, a client could cd into the directory,
            // but not read any of its contents.
            rWalkMsg.putQid(sf.getQid());
            // Refresh the directory (in the case of disk files, this will
            // ensure that we have an up-to-date list of direct children
            // of this directory)
            sf.refresh();
        }

        if (rWalkMsg.getNumSuccessfulWalks() == pathEls.length)
        {
            // The whole walk operation was successful. Associate the 
            // new fid with the returned file
            sessionState.associate(newFid, sf);
        }
        reply(session, rWalkMsg, tag);
    }
    
    private void replyOpen(IoSession session, StyxSessionState sessionState,
        TopenMessage tOpenMsg, int tag) throws StyxException
    {
        StyxFile sf = sessionState.getStyxFile(tOpenMsg.getFid());
        int mode = tOpenMsg.getMode();
        sessionState.checkOpen(sf, mode);
        // Now add this client to the file's list of connected clients
        sf.addClient(new StyxFileClient(session, tOpenMsg.getFid(), mode));
        if ((mode & StyxUtils.OTRUNC) == StyxUtils.OTRUNC)
        {
            // If we're opening with truncation, we must update the last modified
            // time and user of the file.
            sf.setLastModified(StyxUtils.now(), sessionState.getUser());
        }
        reply(session, new RopenMessage(sf.getQid(), sessionState.getIOUnit()), tag);
    }
    
    private void replyCreate(IoSession session, StyxSessionState sessionState,
        TcreateMessage tCrtMsg, int tag) throws StyxException
    {
        StyxFile sf = sessionState.getStyxFile(tCrtMsg.getFid());
        if (!(sf instanceof StyxDirectory))
        {
            throw new StyxException("can't create a file inside another file");
        }
        StyxDirectory dir = (StyxDirectory)sf;
        // Check that the user has write permissions in this directory
        if (!sessionState.checkWrite(dir))
        {
            throw new StyxException("permission denied: need write permissions in parent directory");
        }
        // Check the file type
        long perm = tCrtMsg.getPerm();
        boolean isDir = ((perm & StyxUtils.DMDIR) == StyxUtils.DMDIR);
        boolean isAppOnly = ((perm & StyxUtils.DMAPPEND) == StyxUtils.DMAPPEND);
        boolean isExclusive = ((perm & StyxUtils.DMEXCL) == StyxUtils.DMEXCL);
        boolean isAuth = ((perm & StyxUtils.DMAUTH) == StyxUtils.DMAUTH);
        if (isAuth)
        {
            // We won't allow auth files to be created via Styx messages
            throw new StyxException("can't create a file of type DMAUTH");
        }
        // Get the low 9 bits of the permissions number (these low 9
        // bits are the rwxrwxrwx file permissions)
        int operm = (int)(tCrtMsg.getPerm() & 1023L);
        // Get the real permissions for this file. This depends on the
        // permissions of the parent directory.
        int realPerm;
        if (isDir)
        {
            realPerm = operm & (~0777 | (dir.getPermissions() & 0777));
            // Directories must be opened with OREAD (no other bits set)
            if (tCrtMsg.getMode() != StyxUtils.OREAD)
            {
                throw new StyxException("when creating a directory, " +
                    "must open with read permissions");
            }
        }
        else
        {
            realPerm = operm & (~0666 | (dir.getPermissions() & 0666));
        }
        // Create the file
        StyxFile newFile = dir.createChild(tCrtMsg.getFileName(), realPerm, 
            isDir, isAppOnly, isExclusive);
        // Add the new file to the directory tree
        dir.addChild(newFile);
        // Associate the new file with the given fid
        sessionState.associate(tCrtMsg.getFid(), newFile);
        // Now open the file with the given mode (note that the mode is
        // not checked against the new permissions)
        // TODO: if we are creating a directory, must we be asking for
        // read permissions?
        newFile.addClient(new StyxFileClient(session, tCrtMsg.getFid(), tCrtMsg.getMode()));
        reply(session, new RcreateMessage(newFile.getQid(), sessionState.getIOUnit()), tag);
    }
    
    private void replyRead(IoSession session, StyxSessionState sessionState,
        TreadMessage tReadMsg, int tag) throws StyxException
    {
        StyxFile sf = sessionState.getStyxFile(tReadMsg.getFid());
        // check that file is open for reading by this client
        StyxFileClient client = sf.getClient(session, tReadMsg.getFid());
        if (client == null || !client.canRead())
        {
            throw new StyxException("File is not open for reading");
        }
        if (tReadMsg.getCount() > sessionState.getIOUnit())
        {
            throw new StyxException("can't request more than " +
                sessionState.getIOUnit() + " bytes in a single read");
        }
        // The last access time is set automatically by sf.replyRead()
        sf.read(client, tReadMsg.getOffset().asLong(), tReadMsg.getCount(), tag);
    }
    
    private void replyWrite(IoSession session, StyxSessionState sessionState,
        TwriteMessage tWriteMsg, int tag) throws StyxException
    {         
        StyxFile sf = sessionState.getStyxFile(tWriteMsg.getFid());
        // check that file is open for writing by this client
        StyxFileClient client = sf.getClient(session, tWriteMsg.getFid());
        if (client == null || !client.canWrite())
        {
            throw new StyxException("File is not open for writing");
        }
        if (tWriteMsg.getCount() > sessionState.getIOUnit())
        {
            throw new StyxException("can't write more than " +
                sessionState.getIOUnit() + " bytes in a single operation");
        }
        boolean truncate = client.truncate();
        long offset = tWriteMsg.getOffset().asLong();
        // If this is an append-only file we ignore the specified offset and just
        // write to the end of the file, without truncation. This relies on the
        // getLength() method returning an accurate value.
        if (sf.isAppendOnly())
        {
            offset = sf.getLength().asLong();
            truncate = false;
        }
        // The last modified time is set automatically by sf.replyWrite()
        sf.write(client, offset, tWriteMsg.getCount(), tWriteMsg.getData(),
            truncate, tag);
    }
    
    private void replyClunk(IoSession session, StyxSessionState sessionState,
        TclunkMessage tClkMsg, int tag) throws StyxException
    {
        sessionState.clunk(tClkMsg.getFid());
        reply(session, new RclunkMessage(), tag);
    }
    
    private void replyRemove(IoSession session, StyxSessionState sessionState,
        TremoveMessage tRmMsg, int tag) throws StyxException
    {
        StyxFile sf = sessionState.getStyxFile(tRmMsg.getFid());
        synchronized (sf)
        {
            // A remove is considered as a clunk with the side-effect of
            // removing the file if permissions allow
            sessionState.clunk(tRmMsg.getFid());
            // Check that the user has write permissions on the parent directory
            // (N.B. user doesn't need write permissions on the file itself;
            // see the Inferno manual entry for rm)
            StyxFile parent = sf.getParent();
            if (!sessionState.checkWrite(parent))
            {
                // User doesn't have write permissions on the parent directory
                throw new StyxException("permission denied");
            }
            // If sf is a directory, it needs to be empty
            if (sf instanceof StyxDirectory)
            {
                StyxDirectory sd = (StyxDirectory)sf;
                if (sd.getNumChildren() != 0)
                {
                    throw new StyxException("directory not empty");
                }
            }
            sf.remove();
        }
        // Set the last modified time and user of the parent directory
        sf.getParent().setLastModified(StyxUtils.now(), sessionState.getUser());
        reply(session, new RremoveMessage(), tag);
    }
    
    private void replyStat(IoSession session, StyxSessionState sessionState,
        TstatMessage tStatMsg, int tag) throws StyxException
    {
        // Stat requests require no special permissions
        StyxFile sf = sessionState.getStyxFile(tStatMsg.getFid());
        // Return the information about this file/directory
        reply(session, new RstatMessage(sf.getDirEntry()), tag);
    }
    
    private void replyWstat(IoSession session, StyxSessionState sessionState,
        TwstatMessage tWstatMsg, int tag) throws StyxException
    {
        DirEntry stat = tWstatMsg.getDirEntry();
        StyxFile sf = sessionState.getStyxFile(tWstatMsg.getFid());
        synchronized (sf)
        {                
            // TODO: check to see if all the parts of the DirEntry are
            // "do not change" flags - can we use this as a cue to refresh()
            // the file?

            // Check if we are changing the name of the file
            if (!stat.getFileName().equals(""))
            {
                // Need write permissions on the parent directory to do this
                StyxDirectory dir = sf.getParent();
                if (!sessionState.checkWrite(dir))
                {
                    throw new StyxException("write permissions required on parent directory to change file name");
                }
                // Check that a file with the same name doesn't exist
                if (dir.childExists(stat.getFileName()))
                {
                    throw new StyxException("cannot rename file to the name of an existing file");
                }
                sf.checkSetName(stat.getFileName());
            }

            // Check if we are changing the length of a file
            if (stat.getFileLength().asLong() != -1)
            {
                // Need write permission on the file to change length
                if (!sessionState.checkWrite(sf))
                {
                    throw new StyxException("write permissions required to change file length");
                }
                sf.checkSetLength(stat.getFileLength());
            }

            // Check if we are changing the mode of a file
            if (stat.getMode() != StyxUtils.MAXUINT)
            {
                // Must be the file owner to change the file mode.
                // TODO: allow the "group leader" also to change the file mode
                if (!sf.getOwner().equals(sessionState.getUser()))
                {
                    throw new StyxException("must be owner to change file mode");
                }
                boolean setDir = ((stat.getMode() & StyxUtils.DMDIR) == StyxUtils.DMDIR);
                // Can't change the directory bit
                if (setDir != sf.isDirectory())
                {
                    throw new StyxException("can't change a file to a directory or vice-versa");
                }
                sf.checkSetMode(stat.getMode());
            }

            // Check if we are changing the last modification time of a file
            if (stat.getLastModifiedTime() != StyxUtils.MAXUINT)
            {                    
                // Must be the file owner to change the file mode.
                // TODO: allow the "group leader" also to change the file mode
                if (!sf.getOwner().equals(sessionState.getUser()))
                {
                    throw new StyxException("must be owner to change last modified time");
                }
                sf.checkSetLastModifiedTime(stat.getLastModifiedTime());
            }

            // Check if we are changing the group ID of a file
            if (!stat.getGroup().equals(""))
            {
                // Disallow changing of group ID for the moment
                throw new StyxException("can't change group ID on this server");
            }

            // no other changes are possible
            if (stat.getType() != StyxUtils.MAXUSHORT)
            {
                throw new StyxException("can't change type");
            }
            if (stat.getDev() != StyxUtils.MAXUINT)
            {
                throw new StyxException("can't change dev");
            }
            Qid qid = stat.getQid();
            if (qid.getType() != StyxUtils.MAXUBYTE ||
                qid.getVersion() != StyxUtils.MAXUINT ||
                qid.getPath().asLong() != StyxUtils.MAXULONG)
            {
                throw new StyxException("can't change qid");
            }
            if (stat.getLastAccessTime() != StyxUtils.MAXUINT)
            {
                throw new StyxException("can't change last access time directly");
            }
            if (!stat.getOwner().equals(""))
            {
                throw new StyxException("can't change file owner");
            }
            if (!stat.getLastModifiedBy().equals(""))
            {
                throw new StyxException("can't directly change user who last modified the file");
            }

            // Now we've checked all the permissions we can actually go ahead
            // with all the changes
            if (!stat.getFileName().equals(""))
            {
                sf.setName(stat.getFileName());
            } 
            if (stat.getFileLength().asLong() != -1)
            {
                // Set the file length; this might be rejected by the StyxFile itself
                sf.setLength(stat.getFileLength());
            }
            if (stat.getMode() != StyxUtils.MAXUINT)
            {
                // the permissions do not depend on the parent directory's permissions
                // as they do in the Tcreate message
                sf.setMode(stat.getMode());
            }
            // Always set the last modified time/user for a directory even if
            // not specifically requested
            if (sf.isDirectory() || stat.getLastModifiedTime() != StyxUtils.MAXUINT)
            {                    
                sf.setLastModified(stat.getLastModifiedTime(), sessionState.getUser());
            }
        }
        
        reply(session, new RwstatMessage(), tag);
    }
    
    public void messageSent( IoSession session, Object message )
    {
        if (log.isDebugEnabled())
        {
            log.debug( session.getRemoteAddress() + " SENT: " + message );
        }
        // Release any resources associated with the message.
        if (message instanceof StyxMessage)
        {
            ((StyxMessage)message).dispose();
        }
    }
    
    public void sessionIdle( IoSession session, IdleStatus status )
    {
        log.debug( session.getRemoteAddress() + " IDLE(" + status + ")" );
        // Sessions are never disconnected if they are idle - is this OK?
    }
    
    public void exceptionCaught( IoSession session, Throwable cause )
    {
        if (log.isDebugEnabled())
        {
            cause.printStackTrace();
        }
        log.error( session.getRemoteAddress() + " EXCEPTION: " + cause.getMessage());
        // close the connection on exceptional situation
        session.close();
    }
    
    /**
     * Convenience method for sending a message back to the client.
     */
    public static void reply(IoSession session, StyxMessage message, int tag)
    {
        StyxSessionState sessionState = (StyxSessionState)session.getAttachment();
        synchronized(sessionState)
        {
            // If the tag has been flushed, don't reply
            if (sessionState.tagInUse(tag))
            {
                message.setTag(tag);
                session.write(message);
                sessionState.releaseTag(tag);
            }
        }
    }
    
}
