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

package uk.ac.rdg.resc.jstyx.gridservice.tutorial;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.IOException;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.FlaggedOption;

/**
 * Simple program that reads input one line at a time from standard input, reverses
 * the characters in each line, then prints the reversed line to the standard output
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2006/02/22 13:30:19  jonblower
 * Initial import
 *
 */
public class Reverse
{
    
    public static void main (String[] args)
    {
        try
        {
            // Create a new parameter parsing object
            JSAP jsap = new JSAP();
            // Add parameters that are used to set the input and output file names
            jsap.registerParameter(new FlaggedOption("inputfile", JSAP.STRING_PARSER,
                null, false, 'i', JSAP.NO_LONGFLAG));
            jsap.registerParameter(new FlaggedOption("outputfile", JSAP.STRING_PARSER,
                null, false, 'o', JSAP.NO_LONGFLAG));
            // Parse the command line
            JSAPResult result = jsap.parse(args);
            InputStream in;
            String inputName = result.getString("inputfile");
            if (inputName == null)
            {
                in = System.in;
            }
            else
            {
                in = new FileInputStream(inputName);
            }
            PrintStream out;
            String outputName = result.getString("outputfile");
            if (outputName == null)
            {
                out = System.out;
            }
            else
            {
                out = new PrintStream(outputName);
            }
            reverseLines(in, out);
        }
        catch (JSAPException jsape)
        {
            jsape.printStackTrace();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
    
    /**
     * Reads a line at a time from the given input stream, reverses the
     * characters in each line and prints the reversed characters to the given
     * PrintWriter
     */
    private static void reverseLines(InputStream in, PrintStream out) throws IOException
    {
        BufferedReader bufIn = new BufferedReader(new InputStreamReader(in));
        String line;
        do
        {
            line = bufIn.readLine();
            if (line != null)
            {
                out.println(reverse(line));
            }
        } while (line != null);
    }
    
    /**
     * Reverses the characters in the given string and returns the reversed
     * string
     */
    private static String reverse(String inputStr)
    {
        StringBuffer newStr = new StringBuffer();
        for (int i = inputStr.length() - 1; i >= 0; i--)
        {
            newStr.append(inputStr.charAt(i));
        }
        return newStr.toString();
    }
    
}
