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

import java.io.InputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;

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
    private String sshPassword;
    private String commandToExec;
    
    private SshClient sshClient;
    private SessionChannelClient channel;
    
    /**
     * Creates a new instance of StyxSSHConnection, using the default SSH port
     * of 22
     * @param hostname The host to connect to
     * @param sshUser the username on the SSH server (not the username in the
     * Styx hierarchy)
     * @param sshPassword the password for the user on the SSH server.  Note that
     * this is provided as a String and so is not 100% secure (this String cannot
     * be overwritten so will persist in memory).
     * @param commandToExec the command to execute on the SSH server (this is 
     * a program that will listen for Styx messages on standard input and write
     * replies to standard output, such as a StyxSSHServer)
     */
    public StyxSSHConnection(String hostname, String sshUser, String sshPassword,
        String commandToExec)
    {
        this(hostname, 22, sshUser, sshPassword, commandToExec);
    }
    
    /**
     * Creates a new instance of StyxSSHConnection, allowing the user to specify
     * a port number
     * @param hostname The host to connect to
     * @param port The port to connect to
     * @param sshUser the username on the SSH server (not the username in the
     * Styx hierarchy)
     * @param sshPassword the password for the user on the SSH server.  Note that
     * this is provided as a String and so is not 100% secure (this String cannot
     * be overwritten so will persist in memory).
     * @param commandToExec the command to execute on the SSH server (this is 
     * a program that will listen for Styx messages on standard input and write
     * replies to standard output, such as a StyxSSHServer)
     */
    public StyxSSHConnection(String hostname, int port, String sshUser,
        String sshPassword, String commandToExec)
    {
        super(hostname, port);
        this.sshUser = sshUser;
        this.sshPassword = sshPassword;
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
                this.sshClient = new SshClient();
                sshClient.connect(this.host, new IgnoreHostKeyVerification());

                PasswordAuthenticationClient auth = new PasswordAuthenticationClient();
                auth.setUsername(this.sshUser);
                auth.setPassword(this.sshPassword);
                int result = sshClient.authenticate(auth, "");
                if (result == AuthenticationProtocolState.FAILED)
                {
                    // TODO: what can we do here?
                    System.err.println("Authentication failed");
                }
                else if (result == AuthenticationProtocolState.PARTIAL)
                {
                    System.err.println("Auth succeeded but more auth required");
                }
                else if (result == AuthenticationProtocolState.COMPLETE)
                {
                    // TODO: can we attach a listener to this channel?
                    this.channel = sshClient.openSessionChannel();

                    if(channel.executeCommand(this.commandToExec))
                    {
                        // Create a new IoSession that writes messages to the output
                        // stream of the secure channel
                        this.session = new StyxSSHIoSession(this,
                            new PrintStream(channel.getOutputStream(), true));
                        // Start a thread that consumes the standard error stream
                        // from the server and prints it out
                        new StderrReader(channel.getStderrInputStream()).start();

                        // Start a process that listens for Styx messages (i.e. replies from
                        // the server) on the secure channel's input stream
                        new MessageReader(channel.getInputStream(), this, this.session).start();

                        // Start the handshaking process
                        this.sessionOpened(this.session);
                    }
                    else
                    {
                        // TODO: should throw exception here
                        log.error("Could not execute command");
                    }
                }
            }
            catch (Exception e)
            {
                this.fireStyxConnectionError(e);
            }
        }
        log.info("SSH connection established");
    }
    
    public void sessionClosed(IoSession session) throws Exception
    {
        super.sessionClosed(session);
        log.debug("Disconnecting SSH session");
        if (this.channel != null)
        {
            this.channel.close();
        }
        if (this.sshClient != null)
        {
            this.sshClient.disconnect();
        }
    }
    
    /**
     * Simple class to consume the standard error stream from the channel
     */
    private class StderrReader extends Thread
    {
        private InputStream in;
        public StderrReader(InputStream in)
        {
            this.in = in;
        }
        public void run()
        {
            try
            {
                byte[] b = new byte[1024];
                int n = 0;
                do
                {
                    n = in.read(b);
                    if (n >= 0)
                    {
                        System.err.print("From stderr: \"" + new String(b, 0, n) + "\"");
                    }
                } while (n >= 0);
            }
            catch(IOException ioe)
            {
                log.error("IOException reading from standard error stream", ioe);
            }
        }
    }
    
    public static void main(String[] args)
    {
        StyxConnection conn = null;
        try
        {
            // Create a StyxSSHConnection.
            // This connects to an inaccessible host on the private network so
            // it doesn't matter that the password is visible in SVN
            // How can we set up the PATH on the remote server such that
            // we don't have to specify the full path to JStyxRun?
            conn = new StyxSSHConnection("192.168.0.40", "test", "testtest",
                "~/jstyx-0.3.0-SNAPSHOT/bin/JStyxRun " +
                "uk.ac.rdg.resc.jstyx.gridservice.server.SGSServer "+
                "~/jstyx-0.3.0-SNAPSHOT/conf/SGSconfig.xml -ssh");
            conn.connect();
            CStyxFile[] contents = conn.getFile("reverse").getChildren();
            for (int i = 0; i < contents.length; i++)
            {
                System.out.println(contents[i].toString());
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(conn != null)
            {
                conn.close();
            }
        }
    }
}
