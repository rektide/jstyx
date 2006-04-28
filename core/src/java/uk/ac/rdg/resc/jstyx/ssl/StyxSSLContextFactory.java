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
package uk.ac.rdg.resc.jstyx.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Factory to create a bougus SSLContext.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class StyxSSLContextFactory
{

    /**
     * Protocol to use.
     */
    private static final String PROTOCOL = "TLS";

    // NOTE: The keystore was generated using keytool:
    //   keytool -genkey -alias bogus -keysize 512 -validity 3650
    //           -keyalg RSA -dname "CN=bogus.com, OU=XXX CA,
    //               O=Bogus Inc, L=Stockholm, S=Stockholm, C=SE"
    //           -keypass boguspw -storepass boguspw -keystore bogus.cert

    private static SSLContext serverInstance = null;

    private static SSLContext clientInstance = null;

    /**
     * Get server SSLContext singleton.
     *
     * @param keystoreLocation Name of file containing keystore (must be in classpath)
     * @param password Password to keystore
     * @return SSLContext
     * @throws java.security.GeneralSecurityException
     *
     */
    public static SSLContext getServerSSLContext(String keystoreLocation,
        String password) throws GeneralSecurityException
    {
        if( serverInstance == null )
        {
            synchronized( StyxSSLContextFactory.class )
            {
                if( serverInstance == null )
                {
                    try
                    {
                        serverInstance = createStyxServerSSLContext(keystoreLocation,
                            password);
                    }
                    catch( Exception ioe )
                    {
                        throw new GeneralSecurityException(
                                "Can't create Server SSLContext: " + ioe );
                    }
                }
            }
        }
        return serverInstance;
    }
    
     /**
     * Get client SSLContext singleton.
     *
     * @return SSLContext
     * @throws java.security.GeneralSecurityException
     *
     */
    public static SSLContext getClientSSLContext()
            throws GeneralSecurityException
    {
        if( clientInstance == null )
        {
            synchronized( StyxSSLContextFactory.class )
            {
                if( clientInstance == null )
                {
                    clientInstance = createStyxClientSSLContext();
                }
            }
        }
        return clientInstance;
    }

    private static SSLContext createStyxServerSSLContext(String keystoreLocation,
        String password) throws GeneralSecurityException, IOException
    {
        // Create keystore
        KeyStore ks = KeyStore.getInstance( "JKS" );
        InputStream in = null;
        try
        {
            in = new FileInputStream( keystoreLocation );
            ks.load( in, password.toCharArray() );
        }
        finally
        {
            if( in != null )
            {
                try
                {
                    in.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }

        // Set up key manager factory to use our key store
        KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
        kmf.init( ks, password.toCharArray() );

        // Initialize the SSLContext to work with our key managers.
        SSLContext sslContext = SSLContext.getInstance( PROTOCOL );
        sslContext.init( kmf.getKeyManagers(),
                StyxTrustManagerFactory.X509_MANAGERS, null );

        return sslContext;
    }

    private static SSLContext createStyxClientSSLContext()
            throws GeneralSecurityException
    {
        SSLContext context = SSLContext.getInstance( PROTOCOL );
        context.init( null, StyxTrustManagerFactory.X509_MANAGERS, null );
        return context;
    }

}
