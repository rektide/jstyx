/*
 * Benchmark.java
 *
 * Created on 20 June 2005, 14:26
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package uk.ac.rdg.resc.jstyx.client;

import java.net.URL;
import java.io.BufferedInputStream;
import java.io.InputStream;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.StyxFileInputStream;

/**
 * Program that compares the downloading of a file via HTTP and Styx, with
 * various message sizes
 * @author jdb
 */
public class Benchmark
{
    
    public static void main (String[] args) throws Exception
    {
        String filename = "jdk-1_5_0_03-linux-i586.rpm";
        String httpRoot = "http://www.resc.rdg.ac.uk/";
        String styxRoot = "styx://www.resc.rdg.ac.uk:9876/";
        String infernoRoot = "styx://www.resc.rdg.ac.uk:6666/";
        downloadFromURL(httpRoot + filename);
        downloadFromStyxURL(styxRoot + filename);
        downloadFromStyxURL(infernoRoot + filename);
        downloadFromStyxURL2(styxRoot + filename);
        downloadFromStyxURL2(infernoRoot + filename);
    }
    
    /**
     * Downloads data via an InputStream
     */
    private static void downloadFromURL(String urlStr) throws Exception
    {
        System.out.println("Downloading from " + urlStr);
        URL url = new URL(urlStr);
        InputStream is = url.openStream();
        long start = System.currentTimeMillis();
        int n;
        byte[] bytes = new byte[16000];
        do
        {
            n = is.read(bytes);
        } while (n >= 0);
        long time = System.currentTimeMillis() - start;
        System.out.println("Download took " + time + " milliseconds");
        is.close();
    }
    
    /**
     * Downloads data via an InputStream
     */
    private static void downloadFromStyxURL(String urlStr) throws Exception
    {
        System.out.println("Downloading from " + urlStr);
        URL url = new URL(urlStr);
        StyxConnection conn = new StyxConnection(url.getHost(), url.getPort(), 8192);
        conn.connect();
        CStyxFile file = conn.getFile(url.getPath());
        StyxFileInputStream is = new StyxFileInputStream(file);
        long start = System.currentTimeMillis();
        int n;
        byte[] bytes = new byte[16000];
        do
        {
            n = is.read(bytes);
        } while (n >= 0);
        long time = System.currentTimeMillis() - start;
        System.out.println("Download took " + time + " milliseconds");
        is.close();
        conn.close();
    }
    
    /**
     * Downloads data using the CStyxFile.download() method
     */
    private static void downloadFromStyxURL2(String urlStr) throws Exception
    {
        System.out.println("Downloading from " + urlStr);
        URL url = new URL(urlStr);
        StyxConnection conn = new StyxConnection(url.getHost(), url.getPort(), 65536);
        conn.connect();
        CStyxFile file = conn.getFile(url.getPath());
        long start = System.currentTimeMillis();
        // Download the file but don't write to a local file
        file.download(null);
        long time = System.currentTimeMillis() - start;
        System.out.println("Download took " + time + " milliseconds using CStyxFile.download()");
        conn.close();
    }
}
