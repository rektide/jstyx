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

package uk.ac.rdg.resc.jstyx.gridservice.client;

import org.apache.mina.common.ByteBuffer;

/**
 * Interface defining methods that will be fired when an SGSClient receives
 * notification of a change in state of an SGS instance or when new data arrive.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.5  2005/05/13 16:49:34  jonblower
 * Coded dynamic detection and display of service data, also included streams in config file
 *
 * Revision 1.4  2005/05/11 18:25:00  jonblower
 * Implementing automatic detection of service data elements
 *
 * Revision 1.3  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.2  2005/03/18 13:55:59  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.3  2005/03/16 17:59:35  jonblower
 * Changed following changes to core JStyx library (replacement of java.nio.ByteBuffers with MINA's ByteBuffers)
 *
 * Revision 1.2  2005/02/21 18:12:17  jonblower
 * Following changes to core JStyx library
 *
 * Revision 1.1  2005/02/16 19:22:29  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public interface SGSInstanceChangeListener
{
    
    /**
     * Called when the given service data element changes
     */
    public void serviceDataChanged(String sdName, String newData);
    
    /**
     * Called when we have got the possible service data elements
     * @param sdeNames The names of the SDEs as a String array
     */
    public void gotServiceDataElements(String[] sdeNames);
    
    /**
     * Called when new data arrive from the standard output of the SGS instance.
     * After this method is called, the ByteBuffer will be released. If you 
     * want to prevent this, call newData.acquire().  When you have finished
     * with the data in the buffer, call newData.release().
     */
    public void newStdoutData(ByteBuffer newData);
    
    /**
     * Called when new data arrive from the standard error of the SGS instance.
     * After this method is called, the ByteBuffer will be released. If you 
     * want to prevent this, call newData.acquire().  When you have finished
     * with the data in the buffer, call newData.release().
     */
    public void newStderrData(ByteBuffer newData);
    
    /**
     * Called when the service is started
     */
    public void serviceStarted();
    
    /**
     * Called when the service is stopped before it has finished
     */
    public void serviceAborted();
    
    /**
     * Called when an error occurs
     */
    public void error(String message);
}
