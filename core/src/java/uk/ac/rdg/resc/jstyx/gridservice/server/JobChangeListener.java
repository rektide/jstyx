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

package uk.ac.rdg.resc.jstyx.gridservice.server;

/**
 * Contains methods that will be called by the Job class when changes to the Job
 * occur
 *
 * @author Jon Blower
 * $Revision: 492 $
 * $Date: 2005-11-10 19:55:03 +0000 (Thu, 10 Nov 2005) $
 */
public interface JobChangeListener
{
    /**
     * Called when the status of the job changes
     * @param newStatus The new status of the job
     * @param message Message containing supplementary information
     */
    public void statusChanged(StatusCode newStatus, String message);
    
    /**
     * Called when the progress of the job changes.
     * @param numJobs The total number of sub-jobs in this service
     * @param runningJobs The number of sub-jobs that are in progress (started
     * but not finished)
     * @param failedJobs The number of sub-jobs that have failed
     * @param finishedJobs The number of sub-jobs that have finished (including
     * those that have completed normally and those that have failed)
     */
    public void progressChanged(int numJobs, int runningJobs, int failedJobs,
        int finishedJobs);
    
    /**
     * Called when we have the exit code of the job 
     * @param exitCode The exit code of the job
     */
    public void gotExitCode(int exitCode);
}
