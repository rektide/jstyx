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

package uk.ac.rdg.resc.grex.db;

import com.sleepycat.persist.model.Persistent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForServer;
import uk.ac.rdg.resc.grex.config.Output;
import uk.ac.rdg.resc.grex.server.OutputFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a job that belongs to a service instance, e.g. one element of a parameter
 * sweep or Monte Carlo simulation.  Sub-jobs have parameters and input files
 * that override those of the service instance.  Parameters and input files that
 * are common to all sub-jobs are stored in GRexServiceInstance.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Persistent
public class Job
{
    private static final Log log = LogFactory.getLog(Job.class);
    
    // The id is unique within the GRexServiceInstance
    private int id = -1; // Negative number indicates the Master job
    
    // Maximum number of files that G-Rex can account for at once. This includes
    // files that are being downloaded and files that are ready to download.
    // This should be a number bigger than GRexServiceInstanceClient.maxSimultaneousDownloads
    // so the client always sees some files that are not already being downloaded
    private static final int MAX_FILES = 250;
    
    // Contains the names and values of all parameters that are set on this sub-job
    private Map<String, String> params = new HashMap<String, String>();
    
    // Directory in which all files relating to this sub-job will be kept
    // Note that the database engine doesn't know how to persist java.io.File
    private String workingDirectory;
    
    /* Stores names of files to which output has finished */
    //private transient SortedSet<OutputFile> outputFinished = new TreeSet<OutputFile>();
     
    /* Stores names of downloadable output files */
    //private transient SortedSet<OutputFile> outputFiles = new TreeSet<OutputFile>();
    
    private Integer exitCode = null; // Will be set when the job has finished
    
    private transient GRexServiceInstance instance; // The instance to which this job belongs
    
    // WARNING!  The state is stored in the database as an index number, not a string
    // so if you add a new state to the start or middle of this list the indices
    // will change and the database will appear to contain the wrong state!
    public enum State
    {
        CREATED,  // The instance has been created
        DOWNLOADING_INPUTS, // The instance is downloading its input files
        PENDING,  // The instance has been submitted to the underlying system but is not yet running
        RUNNING,  // The instance is running
        FINISHED, // The instance has completed normally
        ABORTED,  // The instance has been aborted by the user
        ERROR;    // The instance has failed due to an error
            
        /**
         * @return true if this state means that the service instance has finished,
         * either normally or due to an error or being aborted
         */
        public boolean meansFinished()
        {
            return this == FINISHED || this == ABORTED || this == ERROR;
        }
    };

    // The state of this particular job.  For the master job this describes
    // the state of the whole service instance.
    private State state = State.CREATED;
    
    public Job()
    {
    }
    
    Job(GRexServiceInstance instance)
    {
        this.instance = instance;
    }
    
    public GRexServiceInstance getInstance() {
        return this.instance;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
    
    /**
     * @return true if this Job is the "master job" of the instance, i.e. it 
     * represents the overall state of the instance and the files and parameters
     * that are shared by all sub-jobs
     */
    public boolean isMasterJob()
    {
        return this.id < 0;
    }
    
    /**
     * @return a Map of the parameters that have been set on this service instance
     */
    public Map<String, String> getParameters()
    {
        return this.params;
    }
    
    /**
     * @return the value of the parameter with the given name, or null if a
     * value has not been set
     * @todo should this read the value from the parent instance if not set
     * here?  Requires a link to the parent to be stored.
     */
    public String getParamValue(String name)
    {
        return this.params.get(name);
    }
    
    /**
     * Sets a value of a parameter on this sub-job
     */
    public void setParameter(String name, String value)
    {
        this.params.put(name, value);
    }

    public State getState()
    {
        return state;
    }

    public void setState(State state)
    {
        this.state = state;
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }   
    
    /**
     * @return a java.io.File representation of the working directory of this
     * job.  A new File object is created with each invocation.
     */
    public File getWorkingDirectoryFile()
    {
        return new File(this.workingDirectory);
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }
    
    public boolean isFinished()
    {
        return this.state.meansFinished();
    }

    /**
     * Gets the exit code as an Integer, or null if the exit code has not yet
     * been set
     */
    public Integer getExitCode()
    {
        return this.exitCode;
    }

    public void setExitCode(Integer exitCode)
    {
        this.exitCode = exitCode;
    }
    
    /**
     * @return a List of OutputFiles in the working directory of this
     * job that are available for downloading now, or will be available for
     * downloading when the job has finished.  This method is called by the JSPs
     * that provide XML and HTML output to the web.
     * @todo What happens with directories?
     *
     * @todo Provide list in increading order of last modified time, so the number
     * of files the client knows about does not increase too much if files start
     * to accumulate in the working direcory at the server.  One way to achieve this
     * is to read from the outputFiles set in the job runner object (currently
     * not used). The problem with the
     * current method is that the list of MAX_FILES files will, in general, contain
     * a mixture of files the client has seen before and files that have just been
     * created.  This means that as data starts to accumulate on the server the
     * client has to account for an increasing number of files.
     *
     */
    public List<OutputFile> getCurrentOutputFiles()
    {
        List<OutputFile> files = new ArrayList<OutputFile>();
        
        /*
        // Get first MAX_FILES files in outputFiles set.  This set is sorted in
        // increasing order of last time modified.
        log.debug("No. of output files is now " + this.getInstance().getRunner().getOutputFiles().size());
        for (OutputFile opFile : this.getInstance().getRunner().getOutputFiles()) {
            if (files.size() < MAX_FILES) {
                files.add(opFile);
                log.debug(opFile.getFile().getName() + " was last modified at " +
                        opFile.getFile().lastModified());
            }
            else break;
        }
        return files;
         */
        
        /* Call recursive method to traverse directory structure in working
         * directory
         */
        this.getCurrentOutputFiles("", files);
        return files;
    }
    
    /**
     * Recursive method to get the output files in the the directory whose
     * path (relative to the instance's working directory) is given by
     * relativeDirPath.  The results are added to the given List of OutputFiles.
     * If relativeDirPath is not the empty string, it must end with a forward
     * slash (irrespective of operating system).
     */
     private void getCurrentOutputFiles(String relativeDirPath, List<OutputFile> files)
    {
        File dir = new File(this.getWorkingDirectoryFile(), relativeDirPath);
        for (String filename : dir.list())
        {
            String relativePath = relativeDirPath + filename;
            File f = new File(dir, relativePath);
            if (f.isDirectory())
            {
                // recursively call this method
                // We must always use forward slashes even on Windows for the
                // pattern matching in getOutputFile() to work
                //log.debug("Checking directory " + relativePath + "....");
                this.getCurrentOutputFiles(relativePath + "/", files);
            }
            else
            {
                if (files.size() < MAX_FILES) {
                    // Check to see if this file is downloadable
                    //log.debug("Checking file " + relativePath + "....");
                    OutputFile opFile = this.getOutputFile(relativePath);
                    if (opFile != null) {
                        files.add(opFile);
                    }
                }
                else break;
            }
            if (files.size() >= MAX_FILES) break;
        }
    }
    
    
    /**
     * @return an OutputFile corresponding with the given path relative to the 
     * working directory of this instance, or null if the
     * service configuration says that the given file cannot be downloaded
     * through the web interface.  Note that the relativeFilePath must be delimited
     * by forward slashes ("/") on all platforms for the pattern matching to work.
     * relativeFilePath must not start with a slash.
     * Matches according to Ant syntax.
     * @see org.springframework.util.AntPathMatcher
     */
    public OutputFile getOutputFile(String relativeFilePath)
    {
        // Look through all the output definitions in the configuration and
        // see if we have a match.  Note that if this path matches more than one
        // pattern the later patterns take priority
        OutputFile opFile = null;
        //log.debug("Checking file " + relativeFilePath + "....");
        if (instance == null) {
            log.debug("instance is null!!!");
            return null;
        }
        GridServiceConfigForServer gsConfig = this.instance.getGridServiceConfig();
        if (gsConfig == null) {
            log.debug("gsConfig is null!!!");
            /*log.debug("Waiting 5 seconds to see if this is temporary");
            Process proc;
            try {
                proc = Runtime.getRuntime().wait(5000);
            }
            catch (InterruptedException ie) {
                log.error("wait interrupted." + ie.toString());
            }
             */
            return null;
        }
        List<Output> outputs = gsConfig.getOutputs();
        if (outputs == null) {
            log.debug("outputs is null!!!");
            return null;
        }
        for (Output op : outputs)
        {
            String pattern = op.getName();
            if (op.getLinkedParameterName() != null)
            {
                /*
                 *The pattern to match comes from the value of this parameter.
                 *Additional characters may have been specified before and after
                 *the parameter identifier (i.e. ${<paramName>}) in GRexConfig.xml,
                 *wildcards for example, e.g. "*${<paramName>}.dat"
                 */
                int i1 = op.getName().indexOf("${");
                int i2 = op.getName().indexOf("}");
                // First add any characters preceding the parameter identifier.
                if (i1>0) {
                    // One or more preceding characters has been specified
                    pattern = op.getName().substring(0,i1);
                    pattern = pattern.concat(this.getParamValue(op.getLinkedParameterName()));
                }
                else {
                    // No preceding characters specified
                    pattern = this.getParamValue(op.getLinkedParameterName());                    
                }
                // Now add any characters after the parameter identifier
                if (i2 < op.getName().length()-1) {
                    // One or more characters after parameter ID have been specified
                    pattern = pattern.concat(op.getName().substring(i2+1));
                }
                //log.debug("Output " + op.getName() + " is linked to parameter " + op.getLinkedParameterName());
                //log.debug("Output file pattern derived from linked parameter is " + pattern);
            }
            PathMatcher pathMatcher = new AntPathMatcher();
            if (pathMatcher.match(pattern, relativeFilePath))
            {
                File f = new File(this.getWorkingDirectoryFile(), relativeFilePath);
                if (!f.isDirectory())
                {
                    // TODO: watch out for unmodified input files in the working directory
                    opFile = new OutputFile(relativeFilePath, this, op.isAppendOnly(), op.deleteAfter());
                }
            }
        }
        return opFile;
    }

    public void setInstance(GRexServiceInstance instance)
    {
        this.instance = instance;
    }
}
