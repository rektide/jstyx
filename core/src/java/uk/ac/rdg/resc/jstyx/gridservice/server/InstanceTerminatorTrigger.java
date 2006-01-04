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

package uk.ac.rdg.resc.jstyx.gridservice.server;

import java.util.Date;
import java.util.Calendar;

import org.quartz.SimpleTrigger;
import org.quartz.TriggerListener;

/**
 * Job that is fired by the Quartz scheduler in order to automatically destroy
 * a Styx Grid Service instance (i.e. a garbage collector)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 * Revision 1.1  2006/01/04 16:45:29  jonblower
 * Implemented automatic termination of SGS instances using Quartz scheduler
 *
 */

public class InstanceTerminatorTrigger extends SimpleTrigger
{
    
    private StyxGridServiceInstance instance;
    
    /**
     * Creates a new instance of InstanceTerminator.
     * @param instance The SGS instance to terminate
     * @param jobID A unique ID for the job
     * @param terminationTime The time at which the instance will be terminated
     */
    public InstanceTerminatorTrigger(StyxGridServiceInstance instance,
        String jobID, Date terminationTime)
    {
        super(jobID, null, terminationTime);
        this.instance = instance;
    }
    
    /**
     * Gets the SGS instance that is associated with this Trigger
     */
    public StyxGridServiceInstance getSGSInstance()
    {
        return this.instance;
    }
    
}
