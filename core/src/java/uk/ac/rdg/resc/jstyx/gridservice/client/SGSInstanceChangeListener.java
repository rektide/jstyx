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
 * Interface defining methods that will be fired when an SGSClient receives
 * notification of a change in state of an SGS instance or when new data arrive.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.16  2005/10/18 14:08:14  jonblower
 * Removed inputfiles from namespace
 *
 * Revision 1.15  2005/10/14 18:00:42  jonblower
 * Renamed getInputMethods() to getInputStreams()
 *
 * Revision 1.14  2005/08/12 08:08:39  jonblower
 * Developments to support web interface
 *
 * Revision 1.13  2005/08/02 08:04:52  jonblower
 * Continuing to implement steering
 *
 * Revision 1.12  2005/08/01 16:38:05  jonblower
 * Implemented simple parameter handling
 *
 * Revision 1.11  2005/06/14 07:45:16  jonblower
 * Implemented setting of params and async notification of parameter changes
 *
 * Revision 1.10  2005/06/13 16:46:35  jonblower
 * Implemented setting of parameter values via the GUI
 *
 * Revision 1.9  2005/06/10 07:53:12  jonblower
 * Changed SGS namespace: removed "inurl" and subsumed functionality into "stdin"
 *
 * Revision 1.8  2005/05/26 16:52:06  jonblower
 * Implemented detection and viewing of output streams
 *
 * Revision 1.7  2005/05/20 07:45:27  jonblower
 * Implemented getInputFiles() to find the input files required by the service
 *
 * Revision 1.6  2005/05/18 17:13:51  jonblower
 * Created SGSInstanceGUI
 *
 * Revision 1.5  2005/05/13 16:49:34  jonblower
 * Coded dynamic detection and display of service data, also included streams in config file
 *
 * Revision 1.4  2005/05/11 18:25:00  jonblower
 * Implementing automatic detection of service data elements
 *
 * Revision 1.3  2005/03/19 21:47:02  jonblower
 * Further fixes relating to releasing ByteBuffers
 *
 * Revision 1.2  2005/03/18 13:55:59  jonblower
 * Improved freeing of ByteBuffers, and bug fixes
 *
 * Revision 1.1  2005/03/16 22:16:44  jonblower
 * Added Styx Grid Service classes to core module
 *
 * Revision 1.3  2005/03/16 17:59:35  jonblower
 * Changed following changes to core JStyx library (replacement of java.nio.ByteBuffers with MINA's ByteBuffers)
 *
 * Revision 1.2  2005/02/21 18:12:17  jonblower
 * Following changes to core JStyx library
 *
 * Revision 1.1  2005/02/16 19:22:29  jonblower
 * Commit adding of SGS files to CVS
 *
 */
public interface SGSInstanceChangeListener
{
    
    /**
     * Called when the given service data element changes
     */
    public void serviceDataChanged(String sdName, String newData);
    
    /**
     * Called when we have got the service data elements
     * @param sdeFiles The CStyxFiles representing the SDEs.
     */
    public void gotServiceDataElements(CStyxFile[] sdeFiles);
    
    /**
     * Called when we have got the names of the input streams
     * @param inputStreams The CStyxFiles representing the input streams
     */
    public void gotInputStreams(CStyxFile[] inputStreams);
    
    /**
     * Called when we have discovered the input files that the service instance
     * expects.
     * @param inputFiles Array of CStyxFiles representing all the compulsory
     * input files that must be uploaded to the service
     * @param allowOtherInputFiles If true, we will have the option of uploading
     * other input files to the service instance
     */
    //public void gotInputFiles(CStyxFile[] inputFiles, boolean allowOtherInputFiles);
    
    /**
     * Called when we have got the output streams that can be viewed
     * @param outputStreams Array of CStyxFiles representing the output streams
     */
    public void gotOutputStreams(CStyxFile[] outputStreams);
    
    /**
     * Called when we have got the list of parameters expected by the SGS
     * @param paramFiles CStyxFiles representing the parameters
     */
    public void gotParameters(CStyxFile[] paramFiles);
    
    /**
     * Called when we have a new value for a parameter
     * @param index Index of the parameter in the array of parameters previously
     * returned by the gotParameters() event
     * @param value The new value of the parameter
     */
    public void gotParameterValue(int index, String value);
    
    /**
     * Called when we have got the list of steerable parameters
     * @param steerableFiles CStyxFiles representing the parameters
     */
    public void gotSteerableParameters(CStyxFile[] steerableFiles);
    
    /**
     * Called when we have a new value for a steerable parameter
     * @param index Index of the parameter in the array of parameters previously
     * returned by the gotParameters() event
     * @param value The new value of the parameter
     */
    public void gotSteerableParameterValue(int index, String value);
    
    /**
     * Called when we have a new command line string (for debug purposes)
     * @param newCmdLine The new command line
     */
    public void gotCommandLine(String newCmdLine);
    
    /**
     * Called when we have successfully set the input URL of the SGS
     */
    //public void inputURLSet();
    
    /**
     * Called when the input files have been successfully uploaded
     * @todo: add arguments to this
     */
    public void inputFilesUploaded();
    
    /**
     * Called when the service is started
     */
    public void serviceStarted();
    
    /**
     * Called when the service is stopped before it has finished
     */
    public void serviceAborted();
    
    /**
     * Called when an error occurs
     */
    public void error(String message);
}
