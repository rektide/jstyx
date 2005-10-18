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

import org.apache.mina.common.ByteBuffer;

import uk.ac.rdg.resc.jstyx.client.CStyxFile;

/**
 * Convenience class that provides blank implementations for all files in the
 * SGSInstanceChangeListener
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.3  2005/10/18 14:08:14  jonblower
 * Removed inputfiles from namespace
 *
 * Revision 1.2  2005/10/14 18:00:42  jonblower
 * Renamed getInputMethods() to getInputStreams()
 *
 * Revision 1.1  2005/08/12 08:08:39  jonblower
 * Developments to support web interface
 *
 */
public class SGSInstanceChangeAdapter implements SGSInstanceChangeListener
{
    
    /**
     * Called when the given service data element changes
     */
    public void serviceDataChanged(String sdName, String newData) {}
    
    /**
     * Called when we have got the names of the service data elements
     * @param sdeFiles The CStyxFiles representing the SDEs.
     */
    public void gotServiceDataElements(CStyxFile[] sdeFiles) {}
    
    
    /**
     * Called when we have got the names of the input streams
     * @param inputStreams The CStyxFiles representing the input streams
     */
    public void gotInputStreams(CStyxFile[] inputStreams) {}
    
    /**
     * Called when we have discovered the input files that the service instance
     * expects.
     * @param inputFiles Array of CStyxFiles representing all the compulsory
     * input files that must be uploaded to the service
     * @param allowOtherInputFiles If true, we will have the option of uploading
     * other input files to the service instance
     */
    //public void gotInputFiles(CStyxFile[] inputFiles, boolean allowOtherInputFiles) {}
    
    /**
     * Called when we have got the output streams that can be viewed
     * @param outputStreams Array of CStyxFiles representing the output streams
     */
    public void gotOutputStreams(CStyxFile[] outputStreams) {}
    
    /**
     * Called when we have got the list of parameters expected by the SGS
     * @param paramFiles CStyxFiles representing the parameters
     */
    public void gotParameters(CStyxFile[] paramFiles) {}
    
    /**
     * Called when we have a new value for a parameter
     * @param index Index of the parameter in the array of parameters previously
     * returned by the gotParameters() event
     * @param value The new value of the parameter
     */
    public void gotParameterValue(int index, String value) {}
    
    /**
     * Called when we have got the list of steerable parameters
     * @param steerableFiles CStyxFiles representing the parameters
     */
    public void gotSteerableParameters(CStyxFile[] steerableFiles) {}
    
    /**
     * Called when we have a new value for a steerable parameter
     * @param index Index of the parameter in the array of parameters previously
     * returned by the gotParameters() event
     * @param value The new value of the parameter
     */
    public void gotSteerableParameterValue(int index, String value) {}
    
    /**
     * Called when we have a new command line string (for debug purposes)
     * @param newCmdLine The new command line
     */
    public void gotCommandLine(String newCmdLine) {}
    
    /**
     * Called when we have successfully set the input URL of the SGS
     */
    //public void inputURLSet() {}
    
    /**
     * Called when the input files have been successfully uploaded
     * @todo: add arguments to this
     */
    public void inputFilesUploaded() {}
    
    /**
     * Called when the service is started
     */
    public void serviceStarted() {}
    
    /**
     * Called when the service is stopped before it has finished
     */
    public void serviceAborted() {}
    
    /**
     * Called when an error occurs
     */
    public void error(String message) {}
}
