package uk.ac.rdg.resc.jstyx.client.dotusefultest;

import javax.swing.JTree;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import org.dotuseful.ui.tree.AutomatedTreeModel;

public class DynaTree extends JTree
{
    public DynaTree()
    {
        super(new AutomatedTreeModel(new DynaTreeNode("The Mother")));
    }
    
    public void addPressed()
    {
        DynaTreeNode selectedNode;
        TreePath parentPath = getSelectionPath();
        if (parentPath != null)
        {
            selectedNode = (DynaTreeNode) (parentPath.getLastPathComponent());
            selectedNode.addPressed();
        }
    }
    
    public void removePressed()
    {
        DynaTreeNode parentNode;
        TreePath parentPath = getSelectionPath();
        if (parentPath != null)
        {
            parentNode = (DynaTreeNode) (parentPath.getPathComponent(parentPath.getPathCount() - 2));
            parentNode.removePressed((MutableTreeNode) (parentPath.getLastPathComponent()));
        }
    }
}
