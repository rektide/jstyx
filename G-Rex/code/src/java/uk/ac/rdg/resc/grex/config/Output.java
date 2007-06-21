/*
 * Copyright (c) 2007 The University of Reading
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

package uk.ac.rdg.resc.grex.config;

import simple.xml.Attribute;
import simple.xml.Root;
import simple.xml.load.Commit;

/**
 * Class describing an output file from a service instance
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="output")
public class Output
{
    @Attribute(name="name")
    private String name;
    @Attribute(name="stream", required=false)
    private boolean stream = false; // True if this is a stream, i.e. a file that is only
                            // ever appended to: not a random-access file.  This allows
                            // clients to begin downloading this file during execution
                            // This will default to "true" if the name of this output
                            // is "stdout" or "stderr"
    
    private String linkedParamName = null; // Set on commit
    
    /** Creates a new instance of Output */
    public Output()
    {
    }
    
    /**
     * Called after we have parsed this output.  Sets stream=true for
     * stdout and stderr, and checks to see if the name of this output file
     * links to a parameter
     */
    @Commit
    public void commit()
    {
        if (this.name.equals("stdout") || this.name.equals("stderr"))
        {
            this.stream = true;
        }
        // TODO: repeats code in Input: refactor?
        int i1 = this.name.indexOf("${");
        int i2 = this.name.indexOf("}");
        if (i1 >= 0 && i2 >= 0 && i1 < i2)
        {
            this.linkedParamName = this.name.substring(i1 + 2, i2);
        }
    }

    public String getName()
    {
        return name;
    }

    public boolean isStream()
    {
        return stream;
    }
    
    /**
     * @return true if this is the standard output stream (detected by
     * name.equals("stdout")
     */
    public boolean isStdout()
    {
        return this.name.trim().equals("stdout");
    }
    
    /**
     * @return true if this is the standard error stream (detected by
     * name.equals("stderr")
     */
    public boolean isStderr()
    {
        return this.name.trim().equals("stderr");
    }
    
    /**
     * If the name of this output file is given by the value of a parameter,
     * this method returns the name of that parameter.  Otherwise, this returns
     * null.
     */
    public String getLinkedParameterName()
    {
        return this.linkedParamName;
    }
    
}
