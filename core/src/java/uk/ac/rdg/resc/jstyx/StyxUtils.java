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

package uk.ac.rdg.resc.jstyx;

import org.apache.mina.common.ByteBuffer;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Set of constants and useful static methods for the Styx protocol
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.8  2005/03/18 13:55:55  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.7  2005/03/16 22:16:41  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.6  2005/03/16 17:55:46  jonblower
 * Replaced use of java.nio.ByteBuffer with MINA's ByteBuffer to minimise copying of buffers
 *
 * Revision 1.5  2005/03/15 15:52:17  jonblower
 * Added constant for maximum allowable message size
 *
 * Revision 1.4  2005/03/11 13:58:24  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.3.2.1  2005/03/10 20:54:55  jonblower
 * Removed references to Netty
 *
 * Revision 1.3  2005/03/09 16:59:51  jonblower
 * Added HEADER_LENGTH
 *
 * Revision 1.2  2005/02/24 07:39:39  jonblower
 * Added getDataSummary()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:16  jonblower
 * Initial import
 *
 */
public class StyxUtils
{
    //private static final Log log = LogFactory.getLog(StyxUtils.class);
    
    // The header length of a StyxMessage
    public static final int HEADER_LENGTH = 7;
    
    /**
     * The maximum length of a single Styx message.  This is actually an
     * arbitrary figure; there is no reason why messages can't be larger than this
     */
    public static final int MAX_MESSAGE_SIZE = 65536;
    
    // Constants relating to max values of unsigned quantities
    public static final int  MAXUBYTE  = 0xff;
    public static final int  MAXUSHORT = 0xffff;
    public static final long MAXUINT   = 0xffffffffL;
    public static final long MAXULONG  = -1;
    
    public static final int NOTAG = MAXUSHORT; // Used by TversionMessages, which don't need a proper tag
    public static final long NOFID = MAXUINT;  // Used by TattachMessages for an unauthenticated connection
    
    public static final int MAXPATHELEMENTS = 16;  // The maximum number of path elements in a Twalk message
    
    // Constants relating to file mode (see TopenMessage)
    // TODO: should OREAD, OWRITE, ORDWR, OEXEC be a type-safe enumeration?
    public static final int OREAD   = 0;    // Open file for reading
    public static final int OWRITE  = 1;    // Open file for writing
    public static final int ORDWR   = 2;    // Open file for reading and writing
    public static final int OEXEC   = 3;    // Open file for execution
    public static final int OTRUNC  = 0x10; // If this bit is set, the file will be truncated
    public static final int ORCLOSE = 0x40; // If this bit is set, the file will be removed 
                                            // when the fid is clunked (requires permission 
                                            // to remove the file)
    
    // Constants relating to file type
    public static final long DMDIR    = 0x80000000L; // A directory
    public static final long DMAPPEND = 0x40000000L; // An append-only file
    public static final long DMEXCL   = 0x20000000L; // File that can only be opened
                                                     // by one client at a time
    public static final long DMAUTH   =  0x8000000L; // File that is used during authentication    
    
    // We know that UTF-8 must be supported on all Java platforms
    private static final String charsetName = "UTF-8";
    public static final Charset UTF8 = Charset.forName(charsetName);
    
    public static String NEWLINE = "\n"; // Newline is character 10 on Inferno
    public static String SYSTEM_NEWLINE = System.getProperty("line.separator"); // The newline character on the host OS
    public static String SYSTEM_FILE_SEPARATOR = System.getProperty("file.separator");
    
    /**
     * Converts a string to a byte array in UTF-8
     */
    public static byte[] strToUTF8(String str)
    {
        try
        {
            // TODO: use the Charset object to do the encoding?
            return str.getBytes(charsetName);
        }
        catch (UnsupportedEncodingException uee)
        {
            // can't happen: UTF-8 should always be supported
            throw new InternalError("Fatal error: " + charsetName + 
                " is not supported on this platform!");
        }
    }
    
    /**
     * Converts an array of bytes in UTF-8 to a String
     */
    public static String utf8ToString(byte[] bytes)
    {
        return utf8ToString(bytes, 0, bytes.length);
    }
    
    /**
     * Converts an array of bytes in UTF-8 to a String
     * @param bytes The array of bytes to convert
     * @param offset The index of the first byte in the array to convert
     * @param length The number of bytes to convert
     */
    public static String utf8ToString(byte[] bytes, int offset, int length)
    {
        try
        {
            return new String(bytes, offset, length, charsetName);
        }
        catch (UnsupportedEncodingException uee)
        {
            // can't happen: UTF-8 should always be supported
            throw new InternalError("Fatal error: " + charsetName + 
                " is not supported on this platform!");
        }
    }
    
    /**
     * Gets the remaining contents of the given ByteBuffer (i.e. the bytes
     * between its position and limit) as a String.  Leaves the position of the
     * ByteBuffer unchanged.
     */
    public static String dataToString(ByteBuffer buf)
    {
        // MINA's ByteBuffers do not have a backing array (they are all created
        // with allocateDirect), so we have to extract the bytes ourselves
        byte[] bytes;
        synchronized (buf)
        {
            // Remember the original position of the buffer
            int pos = buf.position();
            bytes = new byte[buf.remaining()];
            buf.get(bytes);
            // Reset the original position of the buffer
            buf.position(pos);
        }
        return utf8ToString(bytes);
    }
    
    /**
     * Gets the remaining contents of the given java.nio.ByteBuffer (i.e. the bytes
     * between its position and limit) as a String.  Leaves the position of the
     * ByteBuffer unchanged.
     */
    public static String dataToString(java.nio.ByteBuffer buf)
    {
        // First check to see if the input buffer has a backing array; if so,
        // we can just use it, to save making a copy of the data
        byte[] bytes;
        if (buf.hasArray())
        {
            bytes = buf.array();
            return utf8ToString(bytes, buf.position(), buf.remaining());
        }
        else
        {
            synchronized (buf)
            {
                // Remember the original position of the buffer
                int pos = buf.position();
                bytes = new byte[buf.remaining()];
                buf.get(bytes);
                // Reset the original position of the buffer
                buf.position(pos);
            }
            return utf8ToString(bytes);
        }
    }
    
    /**
     * @return the first n bytes of the data in the given buffer as a String
     * (in quotes), then the number of bytes remaining. Leaves the position of
     * the ByteBuffer unchanged.
     */
    public static String getDataSummary(int n, ByteBuffer data)
    {
        byte[] bytes;
        synchronized(data)
        {
            int numBytes = data.remaining() < n ? data.remaining() : n;
            bytes = new byte[numBytes];
            data.get(bytes);
            // Reset the position of the data buffer
            data.position(data.position() - numBytes);
        }
        return getDataSummary(bytes.length, bytes);
    }
    
    /**
     * @return the first n bytes of the data in the given byte array as a String
     * (in quotes), then the number of bytes remaining.
     */
    public static String getDataSummary(int n, byte[] bytes)
    {
        StringBuffer s = new StringBuffer("\"");
        int bytesToWrite = bytes.length < n ? bytes.length : n;
        s.append(StyxUtils.utf8ToString(bytes, 0, bytesToWrite));
        s.append("\"");
        int moreBytes = bytes.length - bytesToWrite;
        if (moreBytes > 0)
        {
            s.append(" (plus " + moreBytes + " more bytes)");
        }
        return s.toString();
    }
    
    /**
     * @return the current time in seconds since the epoch (Jan 1 00:00 1970 GMT),
     * suitable for use in stat messages
     */
    public static long now()
    {
        // TODO: use rounding instead of integer truncation? Does it really matter?
        return System.currentTimeMillis() / 1000;
    }
    
}
