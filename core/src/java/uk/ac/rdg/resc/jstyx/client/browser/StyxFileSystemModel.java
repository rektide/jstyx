/*
 * %W% %E%
 *
 * Copyright 1997, 1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

package uk.ac.rdg.resc.jstyx.client.browser;

import java.io.File;
import java.util.Date;
import java.net.InetSocketAddress;
import javax.swing.JOptionPane;
import uk.ac.rdg.resc.jstyx.StyxException;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.StyxConnection;

/**
 * FileSystemModel is a TreeTableModel representing a hierarchical file
 * system. Nodes in the FileSystemModel are FileNodes which, when they
 * are directory nodes, cache their children to avoid repeatedly querying
 * the real file system.
 *
 * @version %I% %G%
 *
 * @author Philip Milne
 * @author Scott Violet
 * @author Jon Blower: adapted this for a Styx hierarchy
 */

public class StyxFileSystemModel extends AbstractTreeTableModel
    implements TreeTableModel
{
    
    private String host;
    private int port;
    
    // Names of the columns.
    static protected String[] cNames = {"Name", "Size", "Type", "Owner", "Modified"};
    
    // Types of the columns.
    static protected Class[] cTypes = {TreeTableModel.class, Long.class,
        String.class, String.class, Date.class};
    
    // The the returned file length for directories.
    public static final Integer ZERO = new Integer(0);
    
    public StyxFileSystemModel(String host, int port, String user) throws Exception
    {
        super();
        StyxConnection session = new StyxConnection(host, port, user);
        session.connectAsync();
        this.setRoot(new FileNode(session.getRootDirectory()));
    }
    
    //
    // Some convenience methods.
    //
    
    protected CStyxFile getFile(Object node)
    {
        FileNode fileNode = ((FileNode)node);
        return fileNode.getFile();
    }
    
    protected Object[] getChildren(Object node)
    {
        FileNode fileNode = ((FileNode)node);
        return fileNode.getChildren();
    }
    
    //
    // The TreeModel interface
    //
    
    public int getChildCount(Object node)
    {
        Object[] children = getChildren(node);
        return (children == null) ? 0 : children.length;
    }
    
    public Object getChild(Object node, int i)
    {
        return getChildren(node)[i];
    }
    
    // The superclass's implementation would work, but this is more efficient.
    public boolean isLeaf(Object node)
    {
        try
        {
            return !getFile(node).isDirectory();
        }
        catch (StyxException se)
        {
            return true;
        }
    }
    
    //
    //  The TreeTableNode interface.
    //
    
    public int getColumnCount()
    {
        return cNames.length;
    }
    
    public String getColumnName(int column)
    {
        return cNames[column];
    }
    
    public Class getColumnClass(int column)
    {
        return cTypes[column];
    }
    
    public Object getValueAt(Object node, int column)
    {
        CStyxFile file = getFile(node);
        try
        {
            switch(column)
            {
                case 0:
                    return file.getName();
                case 1:
                    return new Long(file.getLength());
                case 2:
                    return file.isDirectory() ? "Directory" : "File";
                case 3:
                    return file.getOwner();
                case 4:
                    return file.getLastModified();
            }
        }
        catch (StyxException ste)
        {
        }
        catch  (SecurityException se)
        {
        }
        
        return null;
    }
}

/* A FileNode is a derivative of the File class - though we delegate to
 * the File object rather than subclassing it. It is used to maintain a
 * cache of a directory's children and therefore avoid repeated access
 * to the underlying file system during rendering.
 */
class FileNode
{
    CStyxFile  file;
    Object[] children;
    
    public FileNode(CStyxFile file)
    {
        this.file = file;
    }
    
    // Used to sort the file names.
    static private MergeSort fileMS = new MergeSort()
    {
        public int compareElementsAt(int a, int b)
        {
            CStyxFile fa = (CStyxFile)toSort[a];
            CStyxFile fb = (CStyxFile)toSort[b];
            return fa.getName().compareTo(fb.getName());
        }
    };
    
    /**
     * Returns the the string to be used to display this leaf in the JTree.
     */
    public String toString()
    {
        return this.file.getName();
    }
    
    public CStyxFile getFile()
    {
        return this.file;
    }
    
    /**
     * Synchronizes the file's attributes and list of children if it has them
     */
    public void refresh()
    {
        try
        {
            this.file.refresh();
            CStyxFile[] files = this.file.getChildren();
            if(files != null)
            {
                fileMS.sort(files);
                // TODO: make more efficient and use the existing array of children?
                children = new FileNode[files.length];
                for (int i = 0; i < files.length; i++)
                {
                    children[i] = new FileNode(files[i]);
                }
            }
        }
        catch (StyxException ste)
        {
            // Show an error dialog box
            new ErrorMessage(ste.getMessage()).start();
        }
        catch (SecurityException se)
        {
        }
    }
    
    /**
     * Loads the children, caching the results in the children ivar.
     */
    protected Object[] getChildren()
    {
        if (children != null)
        {
            return children;
        }
        this.refresh();
        return children;
    }
    
    private class ErrorMessage extends Thread
    {
        private String message;
        public ErrorMessage(String message)
        {
            this.message = message;
            this.setDaemon(true);
        }
        public void run()
        {
            JOptionPane.showMessageDialog(null, this.message);
        }
    }
}


