package uk.ac.rdg.resc.jstyx.client.dotusefultest;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class DynaTreeApp extends JFrame implements ActionListener
{
    protected DynaTree tree;
    
    protected JButton addButton;
    
    protected JButton removeButton;
    
    public DynaTreeApp()
    {
        super("Dynamic Nodes in Action");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel p = createPanel();
        setContentPane(p);
        pack();
        setVisible(true);
    }
    
    private JPanel createPanel()
    {
        tree = new DynaTree();
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(tree));
        mainPanel.add(createControlPanel(), BorderLayout.LINE_END);
        return mainPanel;
    }
    
    protected JPanel createControlPanel()
    {
        JPanel cp = new JPanel();
        cp.setLayout(new BoxLayout(cp, BoxLayout.PAGE_AXIS));
        cp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        //
        // Add Button
        addButton = new JButton("Add");
        addButton.addActionListener(this);
        cp.add(addButton);
        // Space
        cp.add(Box.createRigidArea(new Dimension(0, 10)));
        //
        // Remove Button
        removeButton = new JButton("Remove");
        removeButton.addActionListener(this);
        cp.add(removeButton);
        return cp;
    }
    
    public static void main(String[] args)
    {
        new DynaTreeApp();
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource().equals(addButton))
        {
            tree.addPressed();
        }
        else
        {
            tree.removePressed();
        }
    }
}