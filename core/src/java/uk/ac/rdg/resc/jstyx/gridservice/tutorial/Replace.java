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

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintStream;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.UnflaggedOption;

/**
 * Program that searches through an input file and replaces all instances of
 * a given string with another string.  It writes the resulting file to an output
 * file and prints a message to its standard output saying how many replacements
 * it made.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2006/02/22 08:47:25  jonblower
 * Added tutorial files
 *
 */
public class Replace
{
    
    public static void main(String[] args)
    {
        try
        {
            // Set up the argument parser
            JSAP jsap = new JSAP();
            jsap.registerParameter(new FlaggedOption("inputfile", JSAP.STRING_PARSER,
                null, true, 'i', JSAP.NO_LONGFLAG, "name of the input file"));
            jsap.registerParameter(new FlaggedOption("outputfile", JSAP.STRING_PARSER,
                null, true, 'o', JSAP.NO_LONGFLAG, "name of the output file"));
            jsap.registerParameter(new UnflaggedOption("stringToFind", JSAP.STRING_PARSER,
                true, null));
            jsap.registerParameter(new UnflaggedOption("stringToReplace", JSAP.STRING_PARSER,
                true, null));
            jsap.registerParameter(new Switch("verbose", 'v', "verbose", "set true for verbose output"));
            
            // Parse the command line
            JSAPResult result = jsap.parse(args);
            // Check for success, printing a usage message if
            if (!result.success())
            {
                System.err.println("Usage: Replace " + jsap.getUsage());
                System.exit(-1);
            }
            
            // Open the input and output files
            BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(result.getString("inputfile"))));
            PrintStream out = new PrintStream(result.getString("outputfile"));
            
            // Read the input file a line at a time
            String strToFind = result.getString("stringToFind");
            String strToReplace = result.getString("stringToReplace");
            boolean verbose = result.getBoolean("verbose");
            String line;
            int i = 1;
            int n = 0;
            do
            {
                line = in.readLine();
                if (line != null)
                {
                    // Replace all instances of the string to find with the
                    // string to replace
                    String newLine = line.replace(strToFind, strToReplace);
                    out.println(newLine);
                    // Check to see if replacement(s) have been made
                    if (!newLine.equals(line))
                    {
                        n++;
                        if (verbose)
                        {
                            System.out.println("Replacement(s) made in line " + i);
                        }
                    }
                    i++;
                }
            } while(line != null);
            
            // Close the input and output files
            in.close();
            out.close();
            
            System.out.println("Done. Replacements made in " + n + " lines out of " + i);
        }
        catch(Exception e)
        {
            System.err.println(e.getMessage());
        }
        
    }
    
    
}
