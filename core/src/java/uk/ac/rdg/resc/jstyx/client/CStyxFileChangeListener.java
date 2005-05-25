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

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;

/**
 * Interface defining methods that will be fired when messages pertaining to 
 * a StyxFile arrive at the client (e.g. data has been read, permissions changed
 * etc)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.7  2005/05/25 16:57:07  jonblower
 * Added fileCreated() event
 *
 * Revision 1.6  2005/05/12 14:20:55  jonblower
 * Changed dataSent() method to dataWritten() (more accurate name)
 *
 * Revision 1.5  2005/05/12 08:00:34  jonblower
 * Added getChildrenAsync() to CStyxFile and childrenFound() to CStyxFileChangeListener
 *
 * Revision 1.4  2005/05/05 07:08:37  jonblower
 * Improved handling of buffers in change listeners
 *
 * Revision 1.3  2005/03/19 21:46:58  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.2  2005/03/16 17:55:52  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.1.1.1  2005/02/16 18:58:18  jonblower
 * Initial import
 *
 */
public interface CStyxFileChangeListener
{
    
    /**
     * Called when the file has been opened.
     * @param file The file that has been opened
     * @param mode The mode with which the file was opened
     */
    public void fileOpen(CStyxFile file, int mode);
    
    /**
     * Called when the file has been created.
     * @param file The file that has been created
     * @param mode The mode with which the file was created
     */
    public void fileCreated(CStyxFile file, int mode);
    
    /**
     * Called when new data have been read from the file (after the Rread message
     * arrives). Note that the offset of the file (i.e. the file position) will
     * not have changed before this method is called.  It is up to clients to 
     * update the offset of the file if required (e.g.
     * <code>file.setOffset(offset + data.remaining())</code>).
     *
     * After this method is finished, the ByteBuffer will be returned to the pool.
     * If you want to delay this happening, call data.acquire() within this
     * method.  Then when you no longer need the data in the buffer, call 
     * data.release().
     *
     * @param file The CStyxFile containing the data
     * @param tReadMsg The original TreadMessage that was sent (contains the offset,
     * tag etc of the message)
     * @param data The new data that have been read from the file
     */
    public void dataArrived(CStyxFile file, TreadMessage tReadMsg, ByteBuffer data);
    
    /**
     * Called when data have been written to the file (after the Rwrite message
     * arrives). Note that the offset of the file (i.e. the file position) will
     * not have changed before this method is called.  It is up to clients to 
     * update the offset of the file if required (e.g.
     * <code>file.setOffset(offset + data.remaining())</code>).
     * @param file The CStyxFile containing the data
     * @param tWriteMsg The TwriteMessage that caused this event to be fired
     */
    public void dataWritten(CStyxFile file, TwriteMessage tWriteMsg);
    
    /**
     * Called when an Rerror message arrives
     * @param file the CStyxFile from which the error originated
     * @param message the error message
     */
    public void error(CStyxFile file, String message);
    
    /**
     * Called after the stat of a file (permissions etc) has been changed.
     * Actually, this is called after an Rstat message arrives; it does not
     * necessarily mean that the stat has changed.
     */
    public void statChanged(CStyxFile file, DirEntry newDirEntry);
    
    /**
     * Called after a successful read on a directory. (Result of a call to
     * CStyxFile.getChildrenAsync())
     */
    public void childrenFound(CStyxFile file, CStyxFile[] children);
    
    /**
     * Called after a file has been successfully uploaded
     * @param targetFile The file to which we have written
     */
    public void uploadComplete(CStyxFile targetFile);
    
}
