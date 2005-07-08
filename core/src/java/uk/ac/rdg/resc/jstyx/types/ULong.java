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
 * Unsigned long integer
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/07/08 12:18:50  jonblower
 * Implemented equals() methods
 *
 * Revision 1.1.1.1  2005/02/16 18:58:35  jonblower
 * Initial import
 *
 */
public class ULong
{    
    private static final int ULONG_LENGTH = 8;
    public static final ULong ZERO = new ULong(0);
    
    private byte[] bytes;
    
    /**
     * Constructs a ULong from an array of 8 bytes in little-endian order
     * @throws IllegalArgumentException if the array is not 8 bytes long or if 
     * it is null
     */
    public ULong(byte[] b)
    {
        if (b == null)
        {
            throw new IllegalArgumentException("Input array of bytes cannot be null");
        }
        if (b.length != ULONG_LENGTH)
        {
            throw new IllegalArgumentException("ULong must be "
                + ULONG_LENGTH + " bytes long");
        }
        this.bytes = b;
    }
    
    public ULong(long l)
    {        
        this.bytes = new byte[ULONG_LENGTH];
        this.setValue(l);
    }
    
    public BigInteger asBigInteger()
    {
        // Need to reverse the array to get into big-endian order
        byte[] revBytes = new byte[8];
        for (int i = 0; i < ULONG_LENGTH; i++)
        {
            revBytes[i] = this.bytes[ULONG_LENGTH - 1 - i];
        }
        return new BigInteger(revBytes);
    }
    
    public long asLong()
    {
        return this.asBigInteger().longValue();
    }
    
    public byte[] getBytes()
    {
        return bytes;
    }
    
    public void setValue(long l)
    {
        this.bytes[0] = (byte)l;
        this.bytes[1] = (byte)(l >> 8);
        this.bytes[2] = (byte)(l >> 16);
        this.bytes[3] = (byte)(l >> 24);
        this.bytes[4] = (byte)(l >> 32);
        this.bytes[5] = (byte)(l >> 40);
        this.bytes[6] = (byte)(l >> 48);
        this.bytes[7] = (byte)(l >> 56);        
    }
    
    public String toString()
    {
        return this.asBigInteger().toString();
    }
    
    public boolean equals(Object otherULong)
    {
        if (otherULong == null)
        {
            return false;
        }
        else if (otherULong instanceof ULong)
        {
            ULong ul2 = (ULong)otherULong;
            // compare byte-by-byte
            for (int i = 0; i < ULONG_LENGTH; i++)
            {
                if (this.bytes[i] != ul2.bytes[i])
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * TODO: implement hashCode()
     */
    
}
