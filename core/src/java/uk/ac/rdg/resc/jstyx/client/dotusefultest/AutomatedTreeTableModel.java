/*
 * AutomatedTreeTableModel.java
 *
 * Created on 03 December 2004, 12:35
 */

package uk.ac.rdg.resc.jstyx.client.dotusefultest;

import org.dotuseful.ui.tree.AutomatedTreeModel;
import org.dotuseful.ui.tree.AutomatedTreeNode;

/**
 *
 * @author  jdb
 */
public abstract class AutomatedTreeTableModel extends AutomatedTreeModel implements TreeTableModel
{
    
    /** Creates a new instance of AutomatedTreeTableModel */
    public AutomatedTreeTableModel(AutomatedTreeNode root, boolean asksAllowsChildren)
    {
        super(root, asksAllowsChildren);
    }
    
}
