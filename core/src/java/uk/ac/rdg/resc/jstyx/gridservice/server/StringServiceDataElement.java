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

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * A ServiceDataElement represented as a String
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.1  2005/02/16 19:22:32  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public class StringServiceDataElement extends ServiceDataElement
{
    
    /** Creates a new instance of StringServiceDataElement */
    public StringServiceDataElement(String name, boolean readOnly, String value,
        float minUpdateInterval)
    {
        super(name, readOnly, minUpdateInterval);
        this.setValue(value);
    }
    
    public StringServiceDataElement(String name, boolean readOnly, String value)
    {
        this(name, readOnly, value, 0.0f);
    }
    
    public StringServiceDataElement(String name, String value)
    {
        this(name, false, value);
    }
    
    public StringServiceDataElement(String name, boolean readOnly)
    {
        this(name, readOnly, "");
    }
    
    public StringServiceDataElement(String name)
    {
        this(name, false, "");
    }
    
    public byte[] getBytes()
    {
        return StyxUtils.strToUTF8((String)this.getValue());
    }
    
    public Object getObject(byte[] bytes)
    {
        return StyxUtils.utf8ToString(bytes);
    }
    
}
