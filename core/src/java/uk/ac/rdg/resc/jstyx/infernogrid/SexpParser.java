/*
 * Copyright (c) 2005 The University of Reading
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

package uk.ac.rdg.resc.jstyx.infernogrid;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import uk.ac.rdg.resc.jstyx.client.StyxConnection;
import uk.ac.rdg.resc.jstyx.client.CStyxFile;
import uk.ac.rdg.resc.jstyx.client.CStyxFileInputStream;
import uk.ac.rdg.resc.jstyx.client.CStyxFileInputStreamReader;
import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Class for parsing an InputStream into sets of S-expressions.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/08/31 17:03:19  jonblower
 * Renamed "StyxFile*putStream*" to "CStyxFile*putStream*" for consistency with CStyxFile class
 *
 * Revision 1.2  2005/08/10 18:34:28  jonblower
 * Implemented working S-expression parser
 *
 * Revision 1.1  2005/08/08 07:43:03  jonblower
 * Initial import
 *
 */
public class SexpParser
{
    
    private InputStreamReader reader;
    
    private static final char OPEN_PAREN = new Character('(').charValue();
    private static final char CLOSE_PAREN = new Character(')').charValue();
    private static final char QUOTES = new Character('"').charValue();
    
    /** Creates a new instance of SexpParser */
    public SexpParser(InputStream is)
    {
        this.reader = new InputStreamReader(is, StyxUtils.UTF8);
    }
    
    /** Creates a new instance of SexpParser */
    public SexpParser(InputStreamReader reader)
    {
        this.reader = reader;
    }
    
    /**
     * @return the next Sexpression in the stream, or null if we have reached
     * the end of the stream
     * @throws SexpParseException if the input stream does not contain a valid
     * Sexpression
     * @throws IOException if there was an error reading the input stream
     */
    public Sexpression getNextSexp() throws SexpParseException, IOException
    {
        return getChildren(null);
    }
    
    /**
     * Gets the children of the given S-expression.  The given S-expression can
     * be null (when we haven't found an S-expression yet).
     */
    private Sexpression getChildren(Sexpression parent)
        throws SexpParseException, IOException
    {
        StringBuffer atomBuf = null;
        boolean quotesOpen = false;
        
        while(true)
        {
            int ch = this.reader.read();
            //System.out.println((char)ch);
            if (Character.isWhitespace(ch))
            {
                if (quotesOpen)
                {
                    atomBuf.append((char)ch);
                }
                else if (atomBuf != null)
                {
                    //System.out.println("Found end of String value: " + atomBuf);
                    parent.add(atomBuf.toString());
                    atomBuf = null;
                }
            }
            else if (ch == OPEN_PAREN)
            {
                if (quotesOpen)
                {
                    atomBuf.append(OPEN_PAREN);
                }
                else
                {
                    //System.out.println("Found start of S-expression");
                    Sexpression sexp = new Sexpression();
                    getChildren(sexp);
                    if (parent == null)
                    {
                        //System.out.println("Got complete S-expression");
                        return sexp;
                    }
                    else
                    {
                        parent.add(sexp);
                    }
                }
            }
            else if (ch == CLOSE_PAREN)
            {
                if (quotesOpen)
                {
                    atomBuf.append(CLOSE_PAREN);
                }
                else
                {
                    //System.out.println("Found end of S-expression");
                    if (parent == null)
                    {
                        throw new SexpParseException("Found end of Sexp with no start");
                    }
                    if (atomBuf != null)
                    {
                        parent.add(atomBuf.toString());
                        atomBuf = null;
                    }
                    return parent;
                }
            }
            else if (ch < 0)
            {
                if (parent != null)
                {
                    throw new SexpParseException("Got EOF in middle of parsing an S-expression");
                }
                this.close();
                return null;
            }
            else
            {
                // Got another character that is not whitespace, a parenthesis or EOF
                if (parent == null)
                {
                    throw new SexpParseException("Format error: atom value found but " +
                        "S-expression not started");
                }
                if (ch == QUOTES)
                {
                    // Quotes mark the start and end of string literals (i.e. ones
                    // that can contain parentheses)
                    if (quotesOpen)
                    {
                        quotesOpen = false;
                        parent.add(atomBuf.toString());
                        atomBuf = null;
                    }
                    else
                    {
                        quotesOpen = true;
                        atomBuf = new StringBuffer();
                        // We don't add the quotes to the string buffer
                    }
                }
                else
                {
                    if (atomBuf == null)
                    {
                        //System.out.println("Found start of String value");
                        atomBuf = new StringBuffer();
                    }
                    atomBuf.append((char)ch);
                }
            }
        }
    }
    
    /**
     * Closes the underlying InputStream
     */
    public void close()
    {
        try
        {
            this.reader.close();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
    
    /**
     * Test function that reads the list of nodes from an Inferno Grid
     * installation
     */
    public static void main(String[] args) throws Exception
    {
        StyxConnection conn = new StyxConnection("wagner.rdg.ac.uk", 7777);
        conn.connect();
        CStyxFile nodesFile = conn.getFile("admin/nodes");
        nodesFile.open(StyxUtils.ORDWR | StyxUtils.OTRUNC);
        nodesFile.setContents("(name ipaddr blacklisted ncompleted)");
        InputStreamReader reader = new CStyxFileInputStreamReader(new CStyxFileInputStream(nodesFile));
        SexpParser parser = new SexpParser(reader);
        Sexpression se;
        do
        {
            se = parser.getNextSexp();
            if (se != null)
            {
                System.out.println(se);
            }
        } while (se != null);
        parser.close();
    }
    
}
