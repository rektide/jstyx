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

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;

/**
 * A FileSystemView for a Styx filesystem that can be used by a JFileChooser
 * to select files on a remote Styx server
 * @todo How can we make sure the correct FileSystemView is gotten by 
 * FileSystemView.getFileSystemView()?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2005/03/07 08:27:51  jonblower
 * Initial import
 *
 */
public class StyxFileSystemView extends FileSystemView
{
    
    private static final Log log = LogFactory.getLog(StyxFileSystemView.class);
    private StyxConnection conn;
    private FileWrapper[] roots;  // The filesystem roots (actually only one root)
    
    /**
     * Creates a new instance of StyxFileSystemView to view the contents of the
     * Styx server on the given StyxConnection.
     * @todo Should the connection be open, or shall we just call connect()
     * anyway?
     */
    public StyxFileSystemView(StyxConnection conn)
    {
        log.debug("Creating StyxFileSystemView");
        this.conn = conn;
        this.roots = new FileWrapper[]{new FileWrapper(this.conn.getRootDirectory())};
    }
    
    // TODO: how do we get a FileSystemView in a static method for a StyxConnection?
    public static FileSystemView getFileSystemView()
    {
        log.debug("Called static getFileSystemView");
        return FileSystemView.getFileSystemView();
    }
    
    /**
     * @return true if the given File is a root in the filesystem. Simply returns
     * true if the file's path is "/"
     */
    public boolean isRoot(File f)
    {
        log.debug("Called isRoot(" + f.getClass().getName() + ", " + f.getPath() + ")");
        if (!(f instanceof FileWrapper))
        {
            log.warn("in isRoot: f is not a FileWrapper");
        }
        return f.getPath().equals("/");
    }
    
    /**
     * @return true if this file is the root directory (i.e. its path equals "/")
     * Simply calls this.isRoot(f)
     */
    public boolean isFileSystemRoot(File dir)
    {
        log.debug("Called isFileSystemRoot()");
        return this.isRoot(dir);
    }
    
    /**
     * @return FileWrapper that wraps the root directory of the remote Styx server
     */
    public File getHomeDirectory()
    {
        log.debug("Called getHomeDirectory()");
        return this.roots[0];
    }
    
    /**
     * Simply calls getHomeDirectory();
     */
    public File getDefaultDirectory()
    {
        log.debug("Called getDefaultDirectory()");
        return this.getHomeDirectory();
    }
    
    /**
     * gets the list of files in the given directory
     */
    public File[] getFiles(File dir, boolean useFileHiding)
    {
        log.debug("Called getFiles(" + dir.getClass().getName() + ", " +
            dir.getPath() + ", " + useFileHiding + ")");
        return dir.listFiles();
    }
    
    /**
     * @return a FileWrapper representing the parent directory of the given File
     */
    public File getParentDirectory(File f)
    {
        log.debug("Called getParentDirectory(" + f.getPath() + ")");
        // TODO: implement properly
        return super.getParentDirectory(f);
    }
    
    /** 
     * Gets the root of the remote Styx server, as a (single-membered) array
     * of FileWrappers that wraps the CStyxFile that represents the root of 
     * the server
     */
    public File[] getRoots()
    {
        log.debug("Called getRoots()");
        return this.roots;
    }
    
    /**
     * Creates a new folder with a default folder name
     * @todo doesn't do anything yet!
     */
    public File createNewFolder(File containingDir)
    {
        log.debug("Called createNewFolder(" + containingDir.toString() + ")");
        return new File("/");
    }
    
    /**
     * @return a FileWrapper object constructed from the given directory and
     * filename
     */
    public File createFileObject(File dir, String filename)
    {
        log.debug("Called createFileObject(" + dir.getPath() + ", " + filename + ")");
        if (dir instanceof FileWrapper)
        {
            CStyxFile file = ((FileWrapper)dir).getCStyxFile();
            CStyxFile newFile = file.getFile(filename);
            return new FileWrapper(newFile);
        }
        else
        {
            log.error("In createFileObject, dir argument was not a FileWrapper");
            return null;
        }
    }
    
    /**
     * @return a FileWrapper object constructed from the given path string
     */
    public File createFileObject(String path)
    {
        log.debug("Called createFileObject(" + path + ")");
        CStyxFile file = new CStyxFile(this.conn, path);
        return new FileWrapper(file);
    }
    
    /**
     * @return a new FileWrapper representing the given child of the given
     * directory
     */
    public File getChild(File parent, String fileName)
    {
        log.debug("Called getChild(" + parent.getPath() + ", " + fileName);
        // TODO implement properly
        return super.getChild(parent, fileName);
    }
    
    /**
     * @return true if folder contains file
     */
    public boolean isParent(File folder, File file)
    {
        log.debug("Called isParent(" + folder.getPath() + ", " + file.getPath());
        // TODO implement properly
        return super.isParent(folder, file);
    }
    
    /**
     * Always returns false (all files are "real" files)
     */
    public boolean isFileSystem(File f)
    {
        log.debug("Called isFileSystem(" + f.getPath() + ")");
        return false;
    }
    
    /**
     * Always returns the name of the file (not the full path)
     */
    public String getSystemDisplayName(File f)
    {
        //log.debug("Called getSystemDisplayName(" + f.getPath() + ")");
        return f.getName();
    }
    
    /**
     * Always returns false
     * @todo Should return true if filename starts with a period?
     */
    public boolean isHiddenFile(File f)
    {
        log.debug("Called isHiddenFile(" + f.getPath() + ")");
        return false;
    }
    
    public boolean isDrive(File f)
    {
        return false;
    }
    
    public boolean isFloppyDrive(File f)
    {
        return false;
    }
    
    public boolean isComputerNode(File f)
    {
        return false;
    }
    
    /**
     * Test function for StyxFileSystemView
     */
    public static void main (String[] args)
    {
        StyxConnection conn = null;
        try
        {
            conn = new StyxConnection("localhost", 7778);
            conn.connect();
            JFileChooser chooser = new JFileChooser(new StyxFileSystemView(conn));
            int returnVal = chooser.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION)
            {
                System.out.println("You chose to open this file: " +
                    chooser.getSelectedFile().getPath());
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (conn != null)
            {
                // TODO: why don't all the threads close down?
                conn.close();
            }
        }
    }
    
    /**
     * @return true if the file is a directory
     */
    public Boolean isTraversable(File f)
    {
        log.debug("Called isTraversable(" + f.getClass().getName() 
            + ", " + f.getPath() + "); returning " + f.isDirectory());
        return Boolean.valueOf(f.isDirectory());
    }
    
    protected File createFileSystemRoot(File f)
    {
        log.debug("Called createFileSystemRoot(" + f.getPath() + ")");
        // TODO: implement properly
        return super.createFileSystemRoot(f);
    }
    
}
