package uk.ac.rdg.resc.jstyx.client.dotusefultest;

import java.util.Random;
import javax.swing.tree.MutableTreeNode;
import org.dotuseful.ui.tree.AutomatedTreeNode;

public class DynaTreeNode extends AutomatedTreeNode
{
    protected int totalChildCount = 0;
    
    public DynaTreeNode(Object userObject)
    {
        super(userObject);
    }
    
    public void addPressed()
    {
        totalChildCount++;
        DynaTreeNode newChild = new DynaTreeNode(getUserObject() + "'s "
            + totalChildCount + " - " + newName());
        add(newChild);
    }
    
    protected String newName()
    {
        String[] names =
        {
            "Mercury", "Venus", "Earth", "Mars", "Phaeton",
            "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"
        };
        Random rand = new Random();
        return names[(rand.nextInt(10))];
    }
    
    public void removePressed(MutableTreeNode node)
    {
        remove(node);
    }
}
