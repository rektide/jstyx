/*
 * StyxURLTest.java
 *
 * Created on 11 January 2005, 18:40
 */

package uk.ac.rdg.resc.jstyx.client.protocol.styx;

import java.net.URL;
import java.io.*;

/**
 * Simple test of the operation of styx:// URLs
 * @author  Jon
 */
public class StyxURLTest
{
    
    public static void main(String[] args) throws Exception
    {
        System.setProperty("java.protocol.handler.pkgs", "uk.ac.rdg.resc.jstyx.client.protocol");
        URL url = new URL("file:///c:/inferno/LICENCE");
        InputStream is = url.openStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String s;
        do
        {
            s = in.readLine();
            if (s != null)
            {
                System.out.println(s);
            }
        } while (s != null);
        in.close();
    }
    
}
