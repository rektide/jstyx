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

package uk.ac.rdg.resc.jstyx.gridservice.client;

import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.StyxUtils;

/**
 * Simple (minimal) stream viewer for viewing a text stream
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.2  2005/05/27 17:05:07  jonblower
 * Changes to incorporate GeneralCachingStreamReader
 *
 * Revision 1.1  2005/05/26 21:33:40  jonblower
 * Added method for viewing streams in a window
 *
 */
public class TextStreamViewer extends StreamViewer
{
    
    private JTextArea textArea;
    
    public TextStreamViewer()
    {
        this.setLayout(new BorderLayout());
        this.textArea = new JTextArea();
        this.textArea.setEditable(false);
        this.getContentPane().add(new JScrollPane(this.textArea),
            BorderLayout.CENTER);
        this.setBounds(200, 200, 400, 400);
    }
    
    /**
     * Called when new data arrive from the server. Simply adds the new data to
     * the JTextArea.
     */
    public void newDataArrived(byte[] data, int size)
    {
        this.textArea.append(new String(data, 0, size));
    }
    
}
