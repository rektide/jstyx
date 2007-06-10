/*
 * Copyright (c) 2007 The University of Reading
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

package uk.ac.rdg.resc.grex.client;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Test client to check authentication works with command-line clients.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class TestClient
{
    private static final String HOST = "localhost";
    private static final int PORT = 8084;
    
    public static void main(String[] args) throws Exception
    {
        // Allow many threads to use this connection
        MultiThreadedHttpConnectionManager connectionManager = 
            new MultiThreadedHttpConnectionManager();
        HttpClient client = new HttpClient(connectionManager);
        
        // Set the credentials for authentication
        Credentials creds = new UsernamePasswordCredentials("marissa", "koala");
        client.getState().setCredentials(new AuthScope(HOST, PORT, "G-Rex Realm via Digest Authentication"), creds);
        
        GetMethod get = new GetMethod("http://" + HOST + ":" + PORT + "/G-Rex/adfsadf.html");
        get.setDoAuthentication(true); // Seems to work even if this is commented out!
        
        try
        {
            int status = client.executeMethod(get);
            System.out.println("Status: " + status);
            for (Header header : get.getResponseHeaders())
            {
                System.out.println(header.toString());
            }
            // N.B. Use getResponseBodyAsStream() if the content-length is unknown
            // to avoid using too much memory
            System.out.println(get.getResponseBodyAsString());
        }
        finally
        {
            // we must do this to return the connection to the pool
            get.releaseConnection();
        }
    }
    
}
