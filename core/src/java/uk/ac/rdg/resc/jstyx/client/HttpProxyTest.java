/*
 * HttpProxyTest.java
 *
 * Created on 03 February 2005, 12:44
 */

package uk.ac.rdg.resc.jstyx.client;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Tests connection to HTTP proxy server through an authenticating proxy server
 * @todo How do we specify that certain domains do not require a proxy server
 * (e.g. *.rdg.ac.uk, *.nerc-essc.ac.uk)?
 * @author jdb
 */
public class HttpProxyTest
{
    
    public static void main(String[] args)
    {
        try
        {
            System.setProperty("http.proxyHost", "wwwcache.rdg.ac.uk");
            System.setProperty("http.proxyPort", "8080");
            Authenticator.setDefault(new SimpleAuthenticator("sgs02jdb", "T3553ll4!"));
            URL url = new URL("http://www.vitanuova.com/inferno/liblicence.txt");
            InputStream in = url.openStream();
            BufferedReader buf = new BufferedReader(new InputStreamReader(in));
            String line = null;
            do
            {
                line = buf.readLine();
                if (line != null)
                {
                    System.out.println(line);
                }
            } while (line != null);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
}

class SimpleAuthenticator extends Authenticator
{
    private String username;
    private String password;
    
    public SimpleAuthenticator(String username, String password)
    {
        this.username = username;
        this.password = password;
    }
    
    protected PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(username, password.toCharArray());
    }
}
