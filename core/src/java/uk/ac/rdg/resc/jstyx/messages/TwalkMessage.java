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

package uk.ac.rdg.resc.jstyx.messages;

import uk.ac.rdg.resc.jstyx.StyxUtils;

import java.util.Vector;

/**
 * Message sent to traverse a directory tree
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/03/11 14:02:16  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.2.2.1  2005/03/10 11:50:59  jonblower
 * Changed to fit with MINA framework
 *
 * Revision 1.2  2005/02/24 07:44:44  jonblower
 * Added getFriendlyString()
 *
 * Revision 1.1.1.1  2005/02/16 18:58:29  jonblower
 * Initial import
 *
 */
public class TwalkMessage extends StyxMessage
{
    
    private long fid;            // The fid of the start point of the traversal
    private long newFid;         // The fid that will be assigned to the destination file
    private Vector pathElements; // Contains the individual elements of the path of the traversal
    
    /** 
     * Creates a new TwalkMessage. This constructor will be called by the
     * MessageRecognizer.
     * @param length The total length of the message (including all header info)
     * @param type The type of the message (a number between 100 and 127)
     * @param tag The tag that identifies this message
     */
    public TwalkMessage(int length, int type, int tag)
    {
        super(length, type, tag);
        this.name = "Twalk";
        this.pathElements = new Vector(5, 2); // Start off with space for 5 elements
    }
    
    /**
     * This constructor should be called when constructing a TwalkMessage from
     * scratch
     * @param fid The fid representing the start point of the traversal
     * @param newFid The fid that will represent the end point of the traversal
     * @param path The /-delimited path
     */
    public TwalkMessage(long fid, long newFid, String path)
    {
        this(0, 110, 0);
        this.fid = fid;
        this.newFid = newFid;
        this.length = StyxUtils.HEADER_LENGTH + 4 + 4 + 2; // This length will be increased
                                                   // as the individual path elements
                                                   // are added by setPath()
        this.setPath(path);
    }
    
    protected final void decodeBody(StyxBuffer buf)
    {
        // Read the original fid
        this.fid = buf.getUInt();
        // Read the new fid
        this.newFid = buf.getUInt();
        // Read the number of path elements
        int numPathElements = buf.getUShort();
        // Read the path elements
        for (int i = 0; i < numPathElements; i++)
        {
            this.pathElements.add(buf.getString());
        }
    }
    
    protected final void encodeBody(StyxBuffer buf)
    {
        // Write the old and new fids
        buf.putUInt(this.fid).putUInt(this.newFid);
        // Write the number of path elements
        buf.putUShort(this.getNumPathElements());
        // Write the path elements
        for (int i = 0; i < this.getNumPathElements(); i++)
        {
            buf.putString((String)this.pathElements.get(i));
        }
    }
    
    public long getFid()
    {
        return this.fid;
    }
    
    public void setFid(long fid)
    {
        this.fid = fid;
    }
    
    public long getNewFid()
    {
        return this.newFid;
    }
    
    public void setNewFid(long newFid)
    {
        this.newFid = newFid;
    }
    
    /**
     * @return the path of this TwalkMessage as a string
     */
    public String getPath()
    {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < this.pathElements.size(); i++)
        {
            s.append((String)this.pathElements.get(i));
            if (i < this.pathElements.size() - 1)
            {
                s.append("/");
            }
        }
        return s.toString();
    }
    
    /**
     * @param path The /-delimited path (if this starts with a / it is an
     * absolute path, otherwise it is a relative path (relative to the file
     * represented by fid)
     */
    public void setPath(String path)
    {
        // Now parse the path elements: first, split the string using the / delimiter
        String[] els = path.split("/");
        // Cycle through the elements in turn, checking them
        for (int i = 0; i < els.length; i++)
        {
            // We ignore elements that represent the current directory
            if (!els[i].equals(".") && !els[i].equals(""))
            {
                // TODO: Check that there are no illegal characters?
                this.addPathElement(els[i]);
            }
        }
    }
    
    /**
     * @return the number of elements in the path
     */
    public int getNumPathElements()
    {
        return this.pathElements.size();
    }
    
    /**
     * @return the path elements as a String array
     */
    public String[] getPathElements()
    {
        return (String[])this.pathElements.toArray(new String[0]);
    }
    
    /**
     * Add an element to the array of path elements
     */
    public void addPathElement(String s)
    {
        this.pathElements.add(s);
        int len = StyxUtils.strToUTF8(s).length;
        this.length += 2 + len;
    }
    
    protected String getElements()
    {
        String s = ", " + this.fid + ", " + this.newFid + ", " + this.getNumPathElements();
        for (int i = 0; i < this.pathElements.size(); i++)
        {
            s += ", " + (String)this.pathElements.get(i);
        }
        return s;
    }
    
    public String toFriendlyString()
    {
        String pathEls = "";
        for (int i = 0; i < this.pathElements.size(); i++)
        {
            pathEls += (String)this.pathElements.get(i);
            if (i < this.pathElements.size() - 1)
            {
                pathEls += "/";
            }
        }
        return "fid: " + this.fid + ", new fid: " + this.newFid + ", path: "
            + pathEls;
    }
    
}
