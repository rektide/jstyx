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

import java.util.Vector;

/**
 * Simple representation of an S-expression.  S-expressions contain <i>atoms</i>
 * and other S-expressions.  So the S-expression <code>(a (b c))</code> contains
 * the atom <code>a</code> and the S-expression <code>(b c)</code>
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/08/10 18:34:28  jonblower
 * Implemented working S-expression parser
 *
 * Revision 1.1  2005/08/08 07:43:03  jonblower
 * Initial import
 *
 */
public class Sexpression
{
    
    private Vector contents; // Vector of objects contained in this S-expression
                             // They might be Strings (representing atoms) or 
                             // other S-expressions
    
    /** Creates a new instance of Sexpression */
    public Sexpression()
    {
        this.contents = new Vector();
    }
    
    /**
     * Adds a token (an atom or another Sexpression) to this Sexpression
     */
    public void add(Object token)
    {
        this.contents.add(token);
        //System.out.println("Added a " + token.getClass());
    }
    
    /**
     * @return the number of tokens (atoms and Sexpressions) contained in this
     * Sexpression
     */
    public int getSize()
    {
        return this.contents.size();
    }
    
    /**
     * @return the token (atom or Sexpression) at the given index within this
     * Sexpression.  If the type of the returned object is String, the token is
     * an atom (string literal).  If the type is a Sexpression (the only other
     * option), the token is a nested Sexpression
     * @throws ArrayIndexOutOfBoundsException if the given index is out of 
     * range
     */
    public Object getToken(int i)
    {
        if (i >= this.contents.size() || i < 0)
        {
	    throw new ArrayIndexOutOfBoundsException(i);
        }
        return this.contents.get(i);
    }
    
    /**
     * Recursive method to display the contents of this Sexpression
     */
    public String toString()
    {
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < this.contents.size(); i++)
        {
            Object token = this.contents.get(i);
            if (token instanceof String)
            {
                strBuf.append(" \"" + token + "\" ");
            }
            else
            {
                strBuf.append("(" + token + ")");
            }
        }
        strBuf.append(" [" + this.getSize() + " tokens]");
        return strBuf.toString();
    }
    
}
