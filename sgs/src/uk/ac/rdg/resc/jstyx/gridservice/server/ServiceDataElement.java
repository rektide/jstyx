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

package uk.ac.rdg.resc.jstyx.gridservice.server;

import uk.ac.rdg.resc.jstyx.server.StyxFile;
import uk.ac.rdg.resc.jstyx.server.AsyncStyxFile;
import uk.ac.rdg.resc.jstyx.StyxException;

/**
 * Class representing an element of Service Data (e.g. progress, status, 
 * number of invocations, etc).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 19:22:32  jonblower
 * Commit adding of SGS files to CVS
 *
 */
abstract class ServiceDataElement
{    
    
    private Object value;
    private String name;
    private boolean readOnly;
    private float minUpdateInterval;
    
    private StyxFile styxFile = null;
    private AsyncStyxFile blockStyxFile = null;
    
    /**
     * Creates a ServiceDataElement.
     * @param name The name of the element, i.e. the name of the files that
     * expose the element through the Styx interface
     * @param readOnly If this is true, the value of the SDE cannot be set
     * through the Styx interface
     * @param minUpdateInterval The minimum amount of time between successive
     * asynchronous updates if reading this SDE through a AsyncStyxFile
     */
    public ServiceDataElement(String name, boolean readOnly, float minUpdateInterval)
    {
        this.name = name;
        this.readOnly = readOnly;
        this.minUpdateInterval = minUpdateInterval;
    }
    
    public ServiceDataElement(String name, boolean readOnly)
    {
        this(name, readOnly, 0.0f);
    }
    
    /**
     * Creates a ServiceDataElement that can be both read and written through
     * the Styx interface
     */
    public ServiceDataElement(String name, float minUpdateInterval)
    {
        this(name, false, minUpdateInterval);
    }
    
    /**
     * Creates a ServiceDataElement that can be both read and written through
     * the Styx interface
     */
    public ServiceDataElement(String name)
    {
        this(name, false);
    }
    
    /**
     * Gets the ServiceDataElement, represented as a byte array
     */
    public abstract byte[] getBytes();
    
    /**
     * Converts the given byte array into an object of the appropriate type
     */
    public abstract Object getObject(byte[] bytes);
    
    /**
     * Sets a new value for the SDE from the given byte array.
     */
    public void setValue(byte[] bytes)
    {
        Object obj = this.getObject(bytes);
        this.setValue(obj);
    }
    
    /**
     * Gets the value of the ServiceDataElement
     */
    public Object getValue()
    {
        return this.value;
    }
    
    /**
     * Sets the ServiceDataElement to a new value and notifies the StyxFile
     * interfaces to this SDE
     */
    public synchronized void setValue(Object newValue)
    {
        this.value = newValue;
        if (this.styxFile != null)
        {
            // Notify that the contents of the underlying StyxFile have changed.
            // This will automatically notify any waiting StyxFileChangeListeners,
            // including the AsyncStyxFile
            this.styxFile.contentsChanged();
        }
    }
    
    /**
     * Gets the name of this SDE
     */
    public String getName()
    {
        return this.name;
    }
    
    /**
     * @return true if this SDE is read-only
     */
    public boolean isReadOnly()
    {
        return this.readOnly;
    }
    
    /**
     * Gets the StyxFile that provides non-blocking access to this SDE
     */
    public StyxFile getStyxFile() throws StyxException
    {
        if (this.styxFile == null)
        {
            this.styxFile = new SDEFile(this);
        }
        return this.styxFile;
    }
    
    /**
     * Gets the StyxFile that provides asynchronous access to this SDE
     * @todo The StyxFile that is returned should be read-only
     */
    public StyxFile getAsyncStyxFile() throws StyxException
    {
        if (this.blockStyxFile == null)
        {
            this.blockStyxFile = new AsyncStyxFile(this.getStyxFile());
            this.blockStyxFile.setMinReplyInterval(this.minUpdateInterval);
        }
        return this.blockStyxFile;
    }
    
    /**
     * Forces all waiting clients to get an update on this quantity, irrespective
     * of how long they have waited since the last update.
     */
    public void flush()
    {
        this.blockStyxFile.contentsChanged(true);
    }
    
}
