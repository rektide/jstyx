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

import java.nio.ByteBuffer;

import net.gleamynode.netty2.Message;
import net.gleamynode.netty2.MessageParseException;
import net.gleamynode.netty2.MessageRecognizer;

import uk.ac.rdg.resc.jstyx.messages.*;

/**
 * When Netty gets a message, this class is called to work out what type of
 * message it is, and package the message up into a class.
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/02/16 18:58:16  jonblower
 * Initial revision
 *
 */
public class StyxMessageRecognizer implements MessageRecognizer
{
    public static final int CLIENT_MODE = 1;
    public static final int SERVER_MODE = 2;
    
    private int mode;
    
    /**
     * Creates a new StyxMessageRecognizer
     * @param mode Must be CLIENT_MODE or SERVER_MODE
     * @throws IllegalArgumentException if the "mode" parameter is not valid
     */
    public StyxMessageRecognizer(int mode)
    {
        switch(mode)
        {
            case CLIENT_MODE:
            case SERVER_MODE:
                this.mode = mode;
                break;
            default:
                throw new IllegalArgumentException("Invalid mode: " + mode);
        }
    }
    
    public Message recognize(ByteBuffer buf) throws MessageParseException
    {
        // return null if the message header has not yet arrived
        if (buf.remaining() < StyxMessage.HEADER_LENGTH)
        {
            return null;
        }
        
        // Wrap the ByteBuffer as a StyxBuffer to make it easy to retrieve
        // primitive Styx types
        StyxBuffer styxBuf = new StyxBuffer(buf);
        
        // Get the message length
        long msgLength = styxBuf.getUInt();
        // Get the message type
        int msgType = styxBuf.getUByte();
        // Get the tag of the message
        int tag = styxBuf.getUShort();
        
        if (this.mode == CLIENT_MODE)
        {
            // Clients will only receive RMessages. These have types designated
            // by odd numbers between 101 and 127 inclusive.
            switch(msgType)
            {
                case 101:
                    return new RversionMessage(msgLength, msgType, tag);
                case 103:
                    return new RauthMessage(msgLength, msgType, tag);
                case 105:
                    return new RattachMessage(msgLength, msgType, tag);
                case 107:
                    return new RerrorMessage(msgLength, msgType, tag);
                case 109:
                    return new RflushMessage(msgLength, msgType, tag);
                case 111:
                    return new RwalkMessage(msgLength, msgType, tag);
                case 113:
                    return new RopenMessage(msgLength, msgType, tag);
                case 115:
                    return new RcreateMessage(msgLength, msgType, tag);
                case 117:
                    return new RreadMessage(msgLength, msgType, tag);
                case 119:
                    return new RwriteMessage(msgLength, msgType, tag);
                case 121:
                    return new RclunkMessage(msgLength, msgType, tag);
                case 123:
                    return new RremoveMessage(msgLength, msgType, tag);
                case 125:
                    return new RstatMessage(msgLength, msgType, tag);
                case 127:
                    return new RwstatMessage(msgLength, msgType, tag);
                default:
                    throw new MessageParseException(msgType +
                        " is not a valid RMessage type.");
            }
        }
        else if (this.mode == SERVER_MODE)
        {
            // Servers will only receive TMessages. These have types designated
            // by even numbers between 100 and 126 inclusive. Type 106
            // is a "Terror" message, which does not exist (you can't transmit
            // an error message to a server)
            
            switch(msgType)
            {
                case 100:
                    return new TversionMessage(msgLength, msgType, tag);
                case 102:
                    return new TauthMessage(msgLength, msgType, tag);
                case 104:
                    return new TattachMessage(msgLength, msgType, tag);
                // 106 is a "TerrorMessage" which cannot exist
                case 108:
                    return new TflushMessage(msgLength, msgType, tag);
                case 110:
                    return new TwalkMessage(msgLength, msgType, tag);
                case 112:
                    return new TopenMessage(msgLength, msgType, tag);
                case 114:
                    return new TcreateMessage(msgLength, msgType, tag);
                case 116:
                    return new TreadMessage(msgLength, msgType, tag);
                case 118:
                    return new TwriteMessage(msgLength, msgType, tag);
                case 120:
                    return new TclunkMessage(msgLength, msgType, tag);
                case 122:
                    return new TremoveMessage(msgLength, msgType, tag);
                case 124:
                    return new TstatMessage(msgLength, msgType, tag);
                case 126:
                    return new TwstatMessage(msgLength, msgType, tag);
                default:
                    throw new MessageParseException(msgType +
                        " is not a valid TMessage type.");
            }
        }
        else
        {
            // Can't happen: the constructor checks that the mode is either
            // CLIENT_MODE or SERVER_MODE
            throw new InternalError();
        }        
    }
    
}
