/*
 * StyxFileTreeDemo.java
 *
 * Created on 02 December 2004, 16:41
 */

package uk.ac.rdg.resc.jstyx.client.dotusefultest;

/**
 *
 * @author  jdb
 */
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.JScrollPane;

import java.net.InetSocketAddress;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.StyxException;

import org.dotuseful.ui.tree.MouseAdaptedTree;
import org.dotuseful.ui.tree.AutomatedTreeModel;

public class StyxFileTreeDemo extends JFrame
{
    private StyxConnection sess;
    
    public static void main(String[] args) throws Throwable
    {
        /*StyxClientSession session = StyxClientSession.createSession(new InetSocketAddress("localhost", 8080));
        session.connect();
        
        // TODO: make connection here, then pass to StyxFileSystemModel?
        JFrame frame = new JFrame("TreeTable");
        JTreeTable treeTable = new JTreeTable(new StyxFileTreeNode(session.getRootDirectory()));
        
        frame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                // TODO: disconnect from server cleanly
                System.exit(0);
            }
        });
        
        frame.getContentPane().add(new JScrollPane(treeTable));
        frame.pack();
        frame.show();*/
        
        //new StyxFileTreeDemo(session);
    }
    
    public StyxFileTreeDemo(StyxConnection session)
    {
        super("Creating a Dynamic JTree");
        this.sess = session;
        Container content = getContentPane();
        JTree tree = new MouseAdaptedTree(new AutomatedTreeModel(new StyxFileTreeNode(session.getRootDirectory()), true));
        content.add(new JScrollPane(tree), BorderLayout.CENTER);
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                sess.close();
                System.exit(0);
            }
        });
        setSize(300, 475);
        setVisible(true);
    }
}
