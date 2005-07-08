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

package uk.ac.rdg.resc.jstyx.types;

import java.math.BigInteger;

/**
 * A 13-byte type representing the server's view of a file
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/07/08 12:18:50  jonblower
 * Implemented equals() methods
 *
 * Revision 1.2  2005/02/24 07:46:05  jonblower
 * Added toFriendlyString() for benefit of StyxMon
 *
 * Revision 1.1.1.1  2005/02/16 18:58:35  jonblower
 * Initial import
 *
 */
public class Qid
{
    
    private int type;        // The type of the file as an unsigned byte
    private long version;    // The version of the file as an unsigned 4-byte integer
    private ULong path;      // The path of the file as an unsigned 8-byte integer
    
    public Qid(int type, long version, ULong path)
    {
        this.type = type;
        this.version = version;
        this.path = path;
    }
    
    public Qid(int type, long version, long path)
    {
        this(type, version, new ULong(path));
    }
    
    public int getType()
    {
        return this.type;
    }
    
    public long getVersion()
    {
        return this.version;
    }
    
    public ULong getPath()
    {
        return this.path;
    }
    
    /**
     * @return string representation of this Qid
     */
    public String toString()
    {
        return this.type + ", " + this.version + ", " + this.path.asBigInteger();
    }
    
    /**
     * @return human-readable representation of this Qid
     */
    public String toFriendlyString()
    {
        // TODO: friendlier representation of type
        return "type: " + this.type + ", version: " + this.version +
            ", pathcode: " + this.path;
    }
    
    /**
     * @return true if the qids are identical
     */
    public boolean equals(Qid otherQid)
    {
        if (otherQid == null)
        {
            return false;
        }
        if (otherQid instanceof Qid)
        {
            Qid qid2 = (Qid)otherQid;
            return (this.type    == qid2.type &&
                    this.version == qid2.version &&
                    this.path.equals(qid2.path));
        }
        else
        {
            return false;
        }
    }
    
    /**
     * TODO: implement hashCode
     */
}
