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

import java.io.InputStreamReader;
import java.io.InputStream;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Convenience class for reading character data from a Styx input stream. Simply
 * makes sure that UTF-8 encoding is used when reading from the stream
 * 
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/08/31 17:03:18  jonblower
 * Renamed "StyxFile*putStream*" to "CStyxFile*putStream*" for consistency with CStyxFile class
 *
 * Revision 1.2  2005/03/02 08:08:54  jonblower
 * Changed constructor to accept any InputStream
 * 
 * Revision 1.1.1.1  2005/02/16 18:58:19  jonblower
 * Initial import
 */
public class CStyxFileInputStreamReader extends InputStreamReader
{
    
    /** Creates a new instance of StyxFileReader */
    public CStyxFileInputStreamReader(InputStream in)
    {
        super(in, StyxUtils.UTF8);
    }
    
}
