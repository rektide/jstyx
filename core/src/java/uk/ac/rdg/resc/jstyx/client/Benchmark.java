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
import uk.ac.rdg.resc.jstyx.client.CStyxFileInputStream;

/**
 * Program that compares the downloading of a file via HTTP and Styx, with
 * various message sizes
 * @author jdb
 */
public class Benchmark
{
    
    public static void main (String[] args) throws Exception
    {
        System.setProperty("java.protocol.handler.pkgs", "uk.ac.rdg.resc.jstyx.client.protocol");
        String filename = "jdk-1_5_0-doc.zip";
        long fileSize = 45635523;
        String httpRoot = "http://www.resc.rdg.ac.uk/";
        String styxRoot = "styx://www.resc.rdg.ac.uk:8080/";
        
        double httpTime = 0;
        double styxSmallMsg1Request = 0;
        double styxLargeMsg1Request = 0;
        double styxSmallMsg10Requests = 0;
        double styxLargeMsg10Requests = 0;
        
        for (int i = 0; i < 5; i++)
        {
            httpTime += downloadFromURL(httpRoot + filename);
            styxSmallMsg1Request += downloadFromStyxURL(styxRoot + filename, 8192, 1);
            styxLargeMsg1Request += downloadFromStyxURL(styxRoot + filename, 65536, 1);
            styxSmallMsg10Requests += downloadFromStyxURL(styxRoot + filename, 8192, 10);
            styxLargeMsg10Requests += downloadFromStyxURL(styxRoot + filename, 65536, 10);
            System.out.print(".");
        }
        System.out.println("");
        
        double mbPerSecond = fileSize * 1000 / 1048576;
        
        System.out.println("HTTP: " + mbPerSecond / httpTime + "MB/s");
        System.out.println("Styx (small messages, 1 request): " + (mbPerSecond / styxSmallMsg1Request) + "MB/s");
        System.out.println("Styx (large messages, 1 request): " + (mbPerSecond / styxLargeMsg1Request) + "MB/s");
        System.out.println("Styx (small messages, 10 requests): " + (mbPerSecond / styxSmallMsg10Requests) + "MB/s");
        System.out.println("Styx (large messages, 10 requests): " + (mbPerSecond / styxLargeMsg10Requests) + "MB/s");
        
    }
    
    /**
     * Downloads data via an InputStream
     */
    private static long downloadFromURL(String urlStr) throws Exception
    {
        URL url = new URL(urlStr);
        InputStream is = url.openStream();
        long start = System.currentTimeMillis();
        int n;
        byte[] bytes = new byte[16000];
        do
        {
            n = is.read(bytes);
        } while (n >= 0);
        is.close();
        long time = System.currentTimeMillis() - start;
        System.err.println("HTTP: " + time);
        return time;
    }
    
    /**
     * Downloads data using the CStyxFile.download() method
     */
    private static long downloadFromStyxURL(String urlStr, int maxMessageSize, int numSimRequests) throws Exception
    {
        URL url = new URL(urlStr);
        StyxConnection conn = new StyxConnection(url.getHost(), url.getPort(), maxMessageSize);
        conn.connect();
        CStyxFile file = conn.getFile(url.getPath());
        long start = System.currentTimeMillis();
        // Download the file
        file.download(null, numSimRequests);
        conn.close();
        long time = System.currentTimeMillis() - start;
        System.err.println("Styx(" + maxMessageSize + ", " + numSimRequests + "): " + time);
        return time;
    }
}
