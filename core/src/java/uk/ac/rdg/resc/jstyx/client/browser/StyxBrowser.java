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

package uk.ac.rdg.resc.jstyx.client.browser;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Main class for a graphical StyxBrowser; simply puts a JTreeTable in a JFrame
 * @todo Make into an applet?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/02/26 09:59:34  jonblower
 * Now reads connection details from command line
 *
 * Revision 1.2  2005/02/21 18:09:43  jonblower
 * *** empty log message ***
 *
 * Revision 1.1  2005/02/18 17:52:40  jonblower
 * Added client.browser package
 *
 */

public class StyxBrowser
{
    public static void main(String[] args) throws Throwable
    {
        if (args.length < 2 || args.length > 3)
        {
            System.err.println("Usage: java StyxBrowser <hostname> <port> [user]");
            return;
        }
        int port;
        try
        {
            port = Integer.parseInt(args[1]);
        }
        catch(NumberFormatException nfe)
        {
            System.err.println("Invalid port number");
            return;
        }
        if (port < 0 || port > StyxUtils.MAXUSHORT)
        {
            System.err.println("Invalid port number (must be between 0 and " +
                StyxUtils.MAXUSHORT + ")");
            return;
        }
        String user = args.length == 3 ? args[2] : "";
        
        new StyxBrowser(args[0], port, user);
    }
    
    public StyxBrowser(String host, int port, String user) throws Throwable
    {
        String s = "";
        if (!user.trim().equalsIgnoreCase(""))
        {
            s += user + "@";
        }
        JFrame frame = new JFrame(s + host + ":" + port);
        JTreeTable treeTable = new JTreeTable(new StyxFileSystemModel(host, port, user));
        
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
        frame.show();
    }
}

