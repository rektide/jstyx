/*
 * TestValidateCert.java
 *
 * Created on 17 November 2004, 09:29
 */

package uk.ac.rdg.resc.jstyx.security;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.PKIXParameters;

import java.io.FileInputStream;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Test of validating a certificate
 * See http://webdocs.math.univ-rennes1.fr/java/j2sdk-1.4/guide/security/certpath/CertPathProgGuide.html
 * @author  jdb
 */
public class TestValidateCert
{
    
    public static void main(String[] args) throws Throwable
    {
        // instantiate a KeyStore with type JKS
        KeyStore ks = getKeyStore(".keystore");
        // fetch certificate chain stored with alias "jdb"
        Certificate[] certArray = ks.getCertificateChain("jdb");
        // convert chain to a List
        List certList = Arrays.asList(certArray);
        // instantiate a CertificateFactory for X.509
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // extract the certification path from
        // the List of Certificates
        CertPath cp = cf.generateCertPath(certList);
        // print each certificate in the path
        List certs = cp.getCertificates();
        Iterator i = certs.iterator();
        while (i.hasNext())
        {
            X509Certificate cert = (X509Certificate) i.next();
            System.out.println(cert);
        }
        
        // Now validate the certificate path
        CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
        //PKIXParameters params = 
    }
    
    // Gets a trusted certificate from the keystore
    private static X509Certificate getTrustedCert() throws Exception
    {
        KeyStore ks = getKeyStore(".keystore");
        // fetch certificate chain stored with alias "jdb"
        Certificate[] certArray = ks.getCertificateChain("jdb");
        // convert chain to a List
        List certList = Arrays.asList(certArray);
        // instantiate a CertificateFactory for X.509
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // extract the certification path from
        // the List of Certificates
        CertPath cp = cf.generateCertPath(certList);
        // print each certificate in the path
        List certs = cp.getCertificates();
        Iterator i = certs.iterator();
        while (i.hasNext())
        {
            X509Certificate cert = (X509Certificate) i.next();
            System.out.println(cert);
        }
        return null; //TODO
    }
    
    private static KeyStore getKeyStore(String name) throws Exception
    {
        // instantiate a KeyStore with type JKS
        KeyStore ks = KeyStore.getInstance("JKS");
        // Get the user home directory
        String userHome = System.getProperty("user.home");
        // load the contents of the KeyStore
        ks.load(new FileInputStream(userHome + "/" + name), "r3sc#E5sC".toCharArray());
        return ks;
    }
    
}
