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

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.gleamynode.netty2.IoProcessor;

/**
 * Set of constants and useful static methods for the Styx protocol
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/02/24 07:39:39  jonblower
 * Added getDataSummary()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:16  jonblower
 * Initial import
 *
 */
public class StyxUtils
{
    private static final Log log = LogFactory.getLog(StyxUtils.class);
    
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
    
    // We share the IoProcessor and EventDispatcher between connections;
    // there is only one of each of these objects per JVM
    private static IoProcessor ioProcessor;
    private static final int DISPATCHER_THREAD_POOL_SIZE = 16;
    private static int numIoProcessors;
    private static int numEventDispatchers;
    
    static
    {
        // Initialise the ioProcessor and eventDispatcher
        //log.info("Initialising ioProcessor and eventDispatcher");
        ioProcessor = new IoProcessor();
        numIoProcessors = 0; // The number of times we have returned an IoProcessor
    }
    
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
        if (buf.hasArray())
        {
            // We can get the bytes out of the buffer without having to make a copy
            return utf8ToString(buf.array(), buf.position(), buf.remaining());
        }
        else
        {
            // Buffer does not have a backing array, so we have to extract the
            // bytes ourselves
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
    }
    
    
    
    /**
     * @return the first n bytes of the data in the given bufferas a String
     * (in quotes), then the number of bytes remaining. Leaves the 
     */
    public static String getDataSummary(int n, ByteBuffer data)
    {
        StringBuffer s = new StringBuffer();
        byte[] bytes;
        synchronized(data)
        {
            int numBytes = data.remaining() < n ? data.remaining() : n;
            bytes = new byte[numBytes];
            data.get(bytes);
            // Reset the position of the data buffer
            data.position(data.position() - numBytes);
        }
        s.append("\"");
        s.append(StyxUtils.utf8ToString(bytes));
        s.append("\"");
        int moreBytes = data.remaining() - bytes.length;
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
    
    /**
     * Gets the IoProcessor for the system - always returns the same object, 
     * which can be shared between connections.  The IoProcessor is started
     * within this method.
     * @throws IOException if the IoProcessor could not be started
     */
    public static IoProcessor getIoProcessor() throws IOException
    {
        synchronized(ioProcessor)
        {
            if (numIoProcessors == 0)
            {
                //log.info("Starting ioProcessor");
                ioProcessor.start();
            }
            numIoProcessors++;
            //log.info("Number of references to ioProcessor: " + numIoProcessors);
            return ioProcessor;
        }
    }
    
    /**
     * Stops the IoProcessor. Actually this just decrements the count of active
     * references to the IoProcessor.  The IoProcessor will only be stopped when
     * the count reaches zero.
     */
    public static void stopIoProcessor()
    {
        synchronized(ioProcessor)
        {
            numIoProcessors--;
            // TODO: should be debug-level logging
            //log.info("Number of references to ioProcessor: " + numIoProcessors);
            if (numIoProcessors <= 0)
            {
                // numIoProcessors should not be <0 but this is defensive
                //log.info("Stopping ioProcessor");
                ioProcessor.stop();
            }
        }
    }
    
}
