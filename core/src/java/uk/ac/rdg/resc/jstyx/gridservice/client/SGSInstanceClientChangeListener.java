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
 * Revision 1.5  2006/02/20 08:37:32  jonblower
 * Still working towards handling output data properly in SGSInstanceClient
 *
 * Revision 1.4  2005/12/07 17:50:01  jonblower
 * Changed gotCommandLine() to gotArguments()
 *
 * Revision 1.3  2005/12/01 17:17:07  jonblower
 * Simplifying client interface to SGS instances
 *
 * Revision 1.2  2005/12/01 08:21:56  jonblower
 * Fixed javadoc comments
 *
 * Revision 1.1  2005/11/10 19:47:10  jonblower
 * Renamed SGSInstanceChange* to SGSInstanceClientChange*
 *
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
public interface SGSInstanceClientChangeListener
{
    
    /**
     * Called when the status of the service changes
     */
    public void statusChanged(String newStatus);
    
    /**
     * Called when the progress of the service changes.  This will be called for
     * the first time just after the service has started.
     * @param numJobs The total number of sub-jobs in this service (will not
     * change)
     * @param runningJobs The number of sub-jobs that are in progress (started
     * but not finished)
     * @param failedJobs The number of sub-jobs that have failed
     * @param finishedJobs The number of sub-jobs that have finished (including
     * those that have completed normally and those that have failed)
     */
    public void progressChanged(int numJobs, int runningJobs, int failedJobs,
        int finishedJobs);
    
    /**
     * Called when the given service data element changes (this is called for
     * SDEs that are not status, progress or exit code)
     */
    public void gotServiceDataValue(String sdName, String newData);
    
    /**
     * Called when we have a new value for a parameter
     * @param name Name of the parameter
     * @param value The new value of the parameter
     */
    public void gotParameterValue(String name, String value);
    
    /**
     * Called when we have a new value for a steerable parameter
     * @param name Name of the parameter
     * @param value The new value of the parameter
     */
    public void gotSteerableParameterValue(String name, String value);
    
    /**
     * Called when we have a new string of command line arguments (for debug purposes)
     * @param newArgs The new arguments
     */
    public void gotArguments(String newArgs);
    
    /**
     * Called when the input files have been successfully uploaded
     * @todo add arguments to this
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
     * Called when all the output data have been downloaded
     */
    public void allOutputDataDownloaded();
    
    /**
     * Called when the exit code from the service is received: this signals that
     * the remote executable has completed.
     */
    public void gotExitCode(int exitCode);
    
    /**
     * Called when an error occurs
     */
    public void error(String message);
}
