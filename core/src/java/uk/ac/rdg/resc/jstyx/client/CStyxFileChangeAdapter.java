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

import java.nio.ByteBuffer;
import uk.ac.rdg.resc.jstyx.types.DirEntry;
import uk.ac.rdg.resc.jstyx.messages.TwriteMessage;
import uk.ac.rdg.resc.jstyx.messages.TreadMessage;

/**
 * Convenience class that provides empty implementations for all the methods in
 * CStyxFileChangeListener.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:17  jonblower
 * Initial revision
 *
 */
public class CStyxFileChangeAdapter implements CStyxFileChangeListener
{
    
    /**
     * Called when the file has been opened.
     * @param file The file that has been opened
     * @param mode The mode with which the file was opened
     */
    public void fileOpen(CStyxFile file, int mode){}
    
    /**
     * Called when new data have been read from the file (after the Rread message
     * arrives). Note that the offset of the file (i.e. the file position) will
     * not have changed before this method is called.  It is up to clients to 
     * update the offset of the file if required (e.g.
     * <code>file.setOffset(offset + data.remaining())</code>).
     * @param file The CStyxFile containing the data
     * @param offset The offset (i.e. file position) of the start of the new data
     * @param data The new data that has been read from the file
     * @param tag The tag of the Tread/Rread messages
     */
    public void dataArrived(CStyxFile file, TreadMessage tReadMsg, ByteBuffer data){}
    
    /**
     * Called when data have been written to the file (after the Rwrite message
     * arrives). Note that the offset of the file (i.e. the file position) will
     * not have changed before this method is called.  It is up to clients to 
     * update the offset of the file if required (e.g.
     * <code>file.setOffset(offset + data.remaining())</code>).
     * @param file The CStyxFile containing the data
     * @param tWriteMsg The TwriteMessage that caused this event to be fired
     */
    public void dataSent(CStyxFile file, TwriteMessage tWriteMsg){}
    
    /**
     * Called when an Rerror message arrives
     * @param file the CStyxFile from which the error originated
     * @param message the error message
     */
    public void error(CStyxFile file, String message){}
    
    /**
     * Called after the stat of a file (permissions etc) has been changed.
     * Actually, this is called after an Rstat message arrives; it does not
     * necessarily mean that the stat has changed.
     */
    public void statChanged(CStyxFile file, DirEntry newDirEntry){}
    
}
