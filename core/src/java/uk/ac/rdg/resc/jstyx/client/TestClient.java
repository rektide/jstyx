package uk.ac.rdg.resc.jstyx.client;

import java.net.InetSocketAddress;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.rdg.resc.jstyx.StyxUtils;
import uk.ac.rdg.resc.jstyx.types.*;
import uk.ac.rdg.resc.jstyx.messages.*;
import uk.ac.rdg.resc.jstyx.client.*;

/**
 * Simple test harness for the JStyx library
 * @author  jdb
 */
public class TestClient extends CStyxFileChangeAdapter
{
    private static final Log log = LogFactory.getLog(TestClient.class);
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 7777;
    
    private static StyxConnection conn = null;
    private static CStyxFile file;
    private static long fid = -1;
    private static long openFid = -1;
    private static int mode = -1;
    
    public static void main(String[] args)
    {
        try
        {
            conn = new StyxConnection(HOSTNAME, PORT);
            file = new CStyxFile(conn, "bbe.txt");
            file.addChangeListener(new TestClient());
            //Thread.sleep(1000);
            file.openAsync(StyxUtils.OREAD);
            //file.refreshAsync();
            //System.out.println("file length = " + file.getLength());
            //System.out.println("last modified = " + file.getLastModified());
            Thread.sleep(1000);
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }
        finally
        {
            System.err.println("In finally block");
            if (conn != null)
            {
                conn.close();
            }
        }
    }
    
    public void fileOpen(CStyxFile file, int mode)
    {
        System.out.println("File is open with mode " + mode);
    }
    
    public void statChanged(CStyxFile file, DirEntry newDirEntry)
    {
        System.out.println("Stat changed!");
    }
    
}