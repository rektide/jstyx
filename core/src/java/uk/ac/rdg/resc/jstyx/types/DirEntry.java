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

import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * A directory entry on a Styx server (i.e. a file stat)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/11/28 17:18:00  jonblower
 * Changed comments to explain type, dev and DMEXCL
 *
 * Revision 1.1.1.1  2005/02/16 18:58:35  jonblower
 * Initial import
 *
 */
public class DirEntry
{
    // A stat message contains a 2-byte dirEntrySize that is not included in
    // this class: it is calculated automatically when needed
    private int type;              // 2 bytes, for kernel use
    private long dev;              // 4 bytes, for kernel use
    private Qid qid;               // the server's representation of this file
    private long mode;             // 4 bytes, permissions and flags
    private long lastAccessTime;   // 4 bytes, last access time (seconds since the epoch)
    private long lastModifiedTime; // 4 bytes, last modification time (seconds since the epoch)
    private ULong fileLength;      // length of file in bytes (8 bytes)
    private String fileName;       // file name
    private String owner;         // owner name
    private String group;        // group name
    private String lastModifiedBy; // name of the user who last modified the file
    
    /** 
     * Creates a new instance of DirEntry.
     * This constructor is most useful when parsing an RstatMessage or similar
     */
    public DirEntry(int type, long dev, Qid qid, long mode,
        long lastAccessTime, long lastModifiedTime, ULong fileLength,
        String fileName, String owner, String group, String lastModifiedBy)
    {
        this.type = type;
        this.dev = dev;
        this.qid = qid;
        this.mode = mode;
        this.lastAccessTime = lastAccessTime;
        this.lastModifiedTime = lastModifiedTime;
        this.fileLength = fileLength;
        this.fileName = fileName;
        this.owner = owner;
        this.group = group;
        this.lastModifiedBy = lastModifiedBy;
    }
    
    /**
     * This constructor is most useful when creating a DirEntry from a StyxFile
     */
    public DirEntry(Qid qid, long mode, long lastAccessTime, long lastModifiedTime,
        ULong fileLength, String fileName, String owner, String group, 
        String lastModifiedBy)
    {
        this.type = 0;  // The type and dev are not needed in JStyx: they are
        this.dev = 0;   // used to distinguish devices in Inferno / Plan9
        this.qid = qid;
        this.mode = mode;
        this.lastAccessTime = lastAccessTime;
        this.lastModifiedTime = lastModifiedTime;
        this.fileLength = fileLength;
        this.fileName = fileName;
        this.owner = owner;
        this.group = group;
        this.lastModifiedBy = lastModifiedBy;
    }
    
    public String toString()
    {
        return type + ", " + dev + ", " + qid + ", " +
            mode + ", " + lastAccessTime + ", " + lastModifiedTime + ", " +
            fileLength + ", " + fileName + ", " + owner + ", " + group +
            ", " + lastModifiedBy;
    }
    
    /**
     * Formats the dirEntry as a string similar to how it appears with an
     * "ls -l" command in Inferno
     */
    public String asLsEntry()
    {
        String s = "";
        if ((this.mode & StyxUtils.DMDIR) == StyxUtils.DMDIR)
        {
            s += "d";
        }
        else if ((this.mode & StyxUtils.DMAPPEND) == StyxUtils.DMAPPEND)
        {
            s += "a";
        }
        else if ((this.mode & StyxUtils.DMAUTH) == StyxUtils.DMAUTH)
        {
            s += "A";
        }
        else
        {
            s += "-";
        }
        if ((this.mode & StyxUtils.DMEXCL) == StyxUtils.DMEXCL)
        {
            s += "l"; // "l" stands for "lock"
        }
        else
        {
            s += "-";
        }
        int operm = (int)(this.mode & 1023);
        s += getPerms(operm >> 6); // owner permissions
        s += getPerms(operm >> 3); // group permissions
        s += getPerms(operm);      // everyone permissions
        
        // We shan't print out the device type or device instance number
        
        s += " " + this.owner + " " + this.group + " " + this.fileLength + " ";
        
        // Now format the last modified time as in Inferno
        Date lmt = new Date(this.lastModifiedTime * 1000);
        // Format the date as, e.g. "Nov 11 08:23" or "Nov 11 2003" if the year
        // is not the current year
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        cal.setTime(lmt);
        int fileYear = cal.get(Calendar.YEAR);
        String fmtStr;
        if (currentYear == fileYear)
        {
            fmtStr = "MMM d HH:mm";
        }
        else
        {
            fmtStr = "MMM d  yyyy";
        }
        // TODO: if the year is not the current year, should we display this?
        SimpleDateFormat formatter = new SimpleDateFormat(fmtStr);
        s += formatter.format(lmt);
        
        s += " " + this.fileName;     
        
        return s;
    }
    
    private String getPerms(int perm)
    {
        // Mask off the last three bits
        perm = perm & 7;
        switch(perm)
        {
            case 0:
                return "---";
            case 1:
                return "--x";
            case 2:
                return "-w-";
            case 3:
                return "-wx";
            case 4:
                return "r--";
            case 5:
                return "r-x";
            case 6:
                return "rw-";
            case 7:
                return "rwx";
            default:
                throw new IllegalArgumentException("perm argument must be in the range 0-7");
        }
    }
    
    /**
     * Gets the total size of the DirEntry, including the two-byte size field
     * at the start of the stat entry.
     */
    public int getSize()
    {
        int len = 2 + 2 + 4 + 13 + 4 + 4 + 4 + 8;
        // Count the length of the string quantities
        len += StyxUtils.strToUTF8(this.fileName).length + 2;
        len += StyxUtils.strToUTF8(this.owner).length + 2;
        len += StyxUtils.strToUTF8(this.group).length + 2;
        len += StyxUtils.strToUTF8(this.lastModifiedBy).length + 2;
        return len;
    }
    
    public int getType()
    {
        return type;
    }
    
    public void setType(int type)
    {
        this.type = type;
    }
    
    public long getDev()
    {
        return dev;
    }
    
    public void setDev(long dev)
    {
        this.dev = dev;
    }
    
    public Qid getQid()
    {
        return qid;
    }
    
    public void setQid(Qid qid)
    {
        this.qid = qid;
    }
    
    public long getMode()
    {
        return mode;
    }
    
    public void setMode(long mode)
    {
        this.mode = mode;
    }
    
    public long getLastAccessTime()
    {
        return lastAccessTime;
    }
    
    public void setLastAccessTime(long lastAccessTime)
    {
        this.lastAccessTime = lastAccessTime;
    }
    
    public long getLastModifiedTime()
    {
        return lastModifiedTime;
    }
    
    public void setLastModifiedTime(long lastModifiedTime)
    {
        this.lastModifiedTime = lastModifiedTime;
    }
    
    public ULong getFileLength()
    {
        return fileLength;
    }
    
    public void setFileLength(ULong fileLength)
    {
        this.fileLength = fileLength;
    }
    
    public String getFileName()
    {
        return fileName;
    }
    
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
    
    public String getOwner()
    {
        return owner;
    }
    
    public void setOwner(String owner)
    {
        this.owner = owner;
    }
    
    public String getGroup()
    {
        return group;
    }
    
    public void setGroup(String group)
    {
        this.group = group;
    }
    
    public String getLastModifiedBy()
    {
        return lastModifiedBy;
    }
    
    public void setLastModifiedBy(String lastModifiedBy)
    {
        this.lastModifiedBy = lastModifiedBy;
    }
    
}
