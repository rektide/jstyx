/*
 * Copyright (c) 2006 The University of Reading
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

package uk.ac.rdg.resc.jstyx.ssh;

import javax.swing.*;
import java.io.PrintStream;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.JSchException;

import org.apache.mina.common.IoSession;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;

/**
 * A StyxConnection that reads Styx Rmessages from the standard input and
 * writes Tmessages to its standard output.  See StyxStreamServer.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class StyxSSHConnection extends StyxConnection
{
    private String sshUser;
    private String commandToExec;
    private Session sshSession;
    private Channel channel;
    
    /**
     * Creates a new instance of StyxStreamConnection
     * @param hostname The host to connect to
     * @param sshUser the username on the SSH server (not the username in the
     * Styx hierarchy)
     * @param commandToExec the command to execute on the SSH server (this is 
     * a program that will listen for Styx messages on standard input and write
     * replies to standard output, such as a StyxSSHServer)
     */
    public StyxSSHConnection(String hostname, String sshUser, String commandToExec)
    {
        super(hostname, 22);
        this.sshUser = sshUser;
        this.commandToExec = commandToExec;
    }
    
    /**
     * Connects to the remote server and handshakes. This method returns
     * immediately; when the connection and handshaking are complete, the
     * connectionReady() event will be fired on all registered
     * StyxConnectionListeners. If an error occurred when connecting or
     * handshaking, the connectionError() event will be fired on the listeners.
     * This method will do nothing if we have already connected or are in the
     * process of connecting.
     */
    public synchronized void connectAsync()
    {
        if (this.connecting)
        {
            log.info("Already connected");
        }
        else
        {
            this.connecting = true;
            try
            {
                // Create the secure channel object
                JSch jsch = new JSch();

                // Connect to the SSH server
                this.sshSession = jsch.getSession(this.sshUser, this.host, 22);

                // username and password will be given via UserInfo interface.
                UserInfo ui = new MyUserInfo();
                sshSession.setUserInfo(ui);
                sshSession.connect();

                this.channel = sshSession.openChannel("exec");
                ((ChannelExec)channel).setCommand(this.commandToExec);

                ((ChannelExec)channel).setErrStream(System.err);

                channel.connect();
                
                // Create a new IoSession that writes messages to the output
                // stream of the secure channel
                this.session = new StyxSSHIoSession(this,
                    new PrintStream(channel.getOutputStream(), true));

                // Start a process that listens for Styx messages (i.e. replies from
                // the server) on the secure channel's input stream
                new MessageReader(channel.getInputStream(), this, this.session).start();

                // Start the handshaking process
                this.sessionOpened(this.session);
            }
            catch (Exception e)
            {
                this.fireStyxConnectionError(e);
            }
        }
        log.info("SSH connection established");
    }
    
    public void sessionClosed(IoSession session)
    {
        super.sessionClosed(session);
        log.debug("Disconnecting SSH session");
        if (this.channel != null)
        {
            this.channel.disconnect();
        }
        if (this.sshSession != null)
        {
            this.sshSession.disconnect();
        }
    }
    
    /**
     * Callback class for getting user info (e.g. password)
     */
    public static class MyUserInfo implements UserInfo, UIKeyboardInteractive
    {
        String passwd;
        JTextField passwordField=(JTextField)new JPasswordField(20);
        
        public String getPassword()
        {
            return passwd;
        }
        
        public boolean promptYesNo(String str)
        {
            Object[] options={ "yes", "no" };
            int foo=JOptionPane.showOptionDialog(null,
                str,
                "Warning",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
            return foo==0;
        }
        
        public String getPassphrase()
        {
            return null;
        }
        
        public boolean promptPassphrase(String message)
        {
            return true;
        }
        
        public boolean promptPassword(String message)
        {
            Object[] ob={passwordField};
            int result= JOptionPane.showConfirmDialog(null, ob, message,
                JOptionPane.OK_CANCEL_OPTION);
            if(result == JOptionPane.OK_OPTION)
            {
                passwd = passwordField.getText();
                return true;
            }
            else
            {
                return false;
            }
        }
        
        public String[] promptKeyboardInteractive(String destination, String name,
            String instruction, String[] prompt, boolean[] echo)
        {
            
            if(prompt.length != 1 || echo[0] != false) // || this.password == null)
            {
                return null;
            }
            
            Object[] ob = {passwordField};
            int result= JOptionPane.showConfirmDialog(null, ob, "Enter your password",
                JOptionPane.OK_CANCEL_OPTION);
            if(result==JOptionPane.OK_OPTION)
            {
                return new String[]{passwordField.getText()};
            }
            else
            {
                return null;
            }
        }
        
        public void showMessage(String message)
        {
            JOptionPane.showMessageDialog(null, message);
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        StyxConnection conn = null;
        try
        {
            conn = new StyxSSHConnection("lovejoy.nerc-essc.ac.uk", "resc",
                "~/JStyx/bin/GridServices -ssh");
            conn.connect();
            CStyxFile[] f = conn.getRootDirectory().getChildren();
            for (int i = 0; i < f.length; i++)
            {
                System.out.println(f[i].getPath());
            }
        }
        finally
        {
            System.out.println("Closing connection");
            if (conn != null)
            {
                conn.close();
            }
        }
    }
    
}
