/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
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
 * @author Per Widerlund (per@minq.se)
 * @author Jan Andersson (janne@minq.se)
 * 
 * @version $Rev: 156507 $, $Date$
 */
public class JonSSLContextFactory
{

    /**
     * Protocol to use.
     */
    private static final String PROTOCOL = "TLS";

    /**
     * Bougus Server certificate keystore file name.
     */
    private static final String BOGUS_KEYSTORE = "c:\\bogus.cert";

    // NOTE: The keystore was generated using keytool:
    //   keytool -genkey -alias bogus -keysize 512 -validity 3650
    //           -keyalg RSA -dname "CN=bogus.com, OU=XXX CA,
    //               O=Bogus Inc, L=Stockholm, S=Stockholm, C=SE"
    //           -keypass boguspw -storepass boguspw -keystore bogus.cert

    /**
     * Bougus keystore password.
     */
    private static final char[] BOGUS_PW = { 'b', 'o', 'g', 'u', 's', 'p',
                                            'w' };

    private static SSLContext serverInstance = null;

    private static SSLContext clientInstance = null;

    /**
     * Get SSLContext singleton.
     *
     * @return SSLContext
     * @throws java.security.GeneralSecurityException
     *
     */
    public static SSLContext getInstance( boolean server, String keystore )
            throws GeneralSecurityException
    {
        SSLContext retInstance = null;
        if( server )
        {
            if( serverInstance == null )
            {
                synchronized( JonSSLContextFactory.class )
                {
                    if( serverInstance == null )
                    {
                        try
                        {
                            serverInstance = createBougusServerSSLContext(keystore);
                        }
                        catch( Exception ioe )
                        {
                            throw new GeneralSecurityException(
                                    "Can't create Server SSLContext:" + ioe );
                        }
                    }
                }
            }
            retInstance = serverInstance;
        }
        else
        {
            if( clientInstance == null )
            {
                synchronized( JonSSLContextFactory.class )
                {
                    if( clientInstance == null )
                    {
                        clientInstance = createBougusClientSSLContext();
                    }
                }
            }
            retInstance = clientInstance;
        }
        return retInstance;
    }

    private static SSLContext createBougusServerSSLContext(String keystore)
            throws GeneralSecurityException, IOException
    {
        // Create keystore
        KeyStore ks = KeyStore.getInstance( "JKS" );
        InputStream in = null;
        try
        {
            //in = JonSSLContextFactory.class
            //        .getResourceAsStream( BOGUS_KEYSTORE );
            in = new FileInputStream(keystore);
            if (in == null)
            {
                System.err.println("Could not read keystore");
            }
            ks.load( in, BOGUS_PW );
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
        kmf.init( ks, BOGUS_PW );

        // Initialize the SSLContext to work with our key managers.
        SSLContext sslContext = SSLContext.getInstance( PROTOCOL );
        sslContext.init( kmf.getKeyManagers(),
                JonTrustManagerFactory.X509_MANAGERS, null );

        return sslContext;
    }

    private static SSLContext createBougusClientSSLContext()
            throws GeneralSecurityException
    {
        System.err.println("Creating bogus Client SSL context");
        SSLContext context = SSLContext.getInstance( PROTOCOL );
        context.init( null, JonTrustManagerFactory.X509_MANAGERS, null );
        return context;
    }

}