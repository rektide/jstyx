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

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Main class for a graphical StyxBrowser; simply puts a JTreeTable in a JFrame
 * @todo Make into an applet?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.8  2005/03/15 15:51:41  jonblower
 * Removed hard limit on maximum message size
 *
 * Revision 1.7  2005/03/11 15:19:07  jonblower
 * Removed misleading comment
 *
 * Revision 1.6  2005/03/11 15:06:21  jonblower
 * Replaced deprecated frame.show() with frame.setVisible(true)
 *
 * Revision 1.5  2005/03/11 13:58:54  jonblower
 * Merged MINA-Test_20059309 into main line of development
 *
 * Revision 1.4.2.1  2005/03/10 11:47:36  jonblower
 * Changed to create the StyxConnection in the StyxBrowser class so it can be closed cleanly
 *
 * Revision 1.4  2005/02/28 16:16:26  jonblower
 * Specified anonymous user when logging on without a user name
 *
 * Revision 1.3  2005/02/26 09:59:34  jonblower
 * Now reads connection details from command line
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
        String s = user.trim().equals("") ? "anonymous" : user;
        JFrame frame = new JFrame(s + "@" + host + ":" + port);
        final StyxConnection conn = new StyxConnection(host, port, user, 65536);
        JTreeTable treeTable = new JTreeTable(new StyxFileSystemModel(conn));
        
        frame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                conn.close();
                System.exit(0);
            }
        });
        
        frame.getContentPane().add(new JScrollPane(treeTable));
        frame.pack();
        frame.setVisible(true);
    }
}

