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

package uk.ac.rdg.resc.grex.client.cli;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.rdg.resc.grex.client.GRexServiceClient;
import uk.ac.rdg.resc.grex.client.GRexServiceInstanceClient;
import uk.ac.rdg.resc.grex.config.GridServiceConfigForClient;
import uk.ac.rdg.resc.grex.config.Input;
import uk.ac.rdg.resc.grex.config.Parameter;

/**
 * Entry point for the command-line interface to the G-Rex server
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class GRexRun
{
    private static final Log log = LogFactory.getLog(GRexRun.class);
    
    // These are the common command-line parameters that users can set on any
    // grid service invocation
    private static final String HELP = "grex-help";
    private static final String VERBOSE_HELP = "grex-verbose-help";
    private static final String DEBUG = "grex-debug";
    private static final String DESCRIPTION = "grex-description";
    
    /**
     * Entry point to the command-line client application.
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: GRexRun <service url> [args]");
            System.err.println("   Example: GRexRun http://user:password@myserver.com:8080/G-Rex/testservice input.dat output.dat");
            return; // TODO: use a specific exit code here?
        }
        
        try
        {
            // Download the configuration information from the server
            GRexServiceClient serviceClient = new GRexServiceClient(args[0]);
            GridServiceConfigForClient config = serviceClient.getConfig();
            
            // Create a new object to parse the command-line arguments
            JSAP jsap = new JSAP();
            // Add the parameters that are independent of the particular service
            // (e.g. the ones that allow the user to ask for help)
            addCommonParameters(jsap);
            // Add the parameters that are specified in the configuration object
            addParametersFromConfig(jsap, config);
            
            // Check that the command-line arguments are valid
            String[] otherArgs = new String[args.length - 1];
            System.arraycopy(args, 1, otherArgs, 0, otherArgs.length);
            JSAPResult jsapResult = jsap.parse(otherArgs);
            
            // See if the user has requested help.
            if (jsapResult.getBoolean(VERBOSE_HELP))
            {
                System.out.println(config.getDescription());
                System.out.println("");
                System.out.println("Usage: " + serviceClient.getServiceName() +
                    " " + jsap.getUsage());
                System.out.println("");
                System.out.println(jsap.getHelp());
                return;
            }
            else if (jsapResult.getBoolean(HELP))
            {
                System.out.println(config.getDescription());
                System.out.println("");
                System.out.println("Usage: " + serviceClient.getServiceName() +
                    " " + jsap.getUsage());
                return;
            }

            // See if the command-line arguments were parsed successfully
            if (!jsapResult.success())
            {
                System.err.println("Usage: " + serviceClient.getServiceName() +
                    " " + jsap.getUsage());
                return; // TODO: what is the best error code here?
            }
            
            // See if we're going to print debug messages
            boolean debug = jsapResult.getBoolean(DEBUG);
            
            // If we've got this far we can create a new service instance
            GRexServiceInstanceClient instanceClient =
                serviceClient.createNewServiceInstance(jsapResult.getString(DESCRIPTION));
            log.debug("Created new service instance at " + instanceClient.getUrl());
            if (debug)
            {
                System.out.println("Created new service instance at " + instanceClient.getUrl());
            }
            
            // Look through the fixed-name input files and figure out what we
            // need to upload
            for (Input input : config.getInputs())
            {
                // We'll sort out the inputs that are specified by command-line
                // parameters later
                if (input.getLinkedParameterName() == null)
                {
                    if (input.isStdin())
                    {
                        // The service is expecting the standard input stream
                        // TODO: call instanceClient.setStdinStream(System.in)
                    }
                    // TODO: call instanceClient.setFileToUpload()
                }
            }
            
            // Look through the fixed-name output files and figure out what we
            // need to download.  Note that non-streamed output files can only
            // be downloaded once the service is finished.
            // TODO
            
            // Set all the parameter values
            for (Parameter param : config.getParams())
            {
                // TODO: check that getString() works for greedy options
                String paramValue = jsapResult.getString(param.getName());
                // TODO: call instanceClient.setParameter(param.getName(), paramValue)
                if (param.getLinkedInput() != null)
                {
                    // The value of this parameter specifies an input file that
                    // we must upload
                    // TODO: call instanceClient.setFileToUpload()
                }
                if (param.getLinkedOutput() != null)
                {
                    // The value of this parameter specifies an output file that
                    // we must download
                    // TODO
                }
            }
            
            // Now we can start the service.  This will upload all the input files
            // and set the parameters.  After the service has been started,
            // the standard input stream will be redirected to the remote service
            // instance.
            
        }
        catch(Exception e)
        {
            log.error("Error running GRex service", e);
            e.printStackTrace(); // TODO: not very user-friendly!
        }
    }
    
    /**
     * Adds the JSAP parameters that are independent of the grid service
     * being executed
     * @throws JSAPException if there was an error adding a parameter
     */
    private static void addCommonParameters(JSAP jsap) throws JSAPException
    {
        // Add a switch to allow the user to print out a help message for this SGS
        jsap.registerParameter(new Switch(HELP, JSAP.NO_SHORTFLAG, HELP,
            "Set this switch to print out a short help message"));
        // Add a switch to allow the user to print out a verbose help message for this SGS
        jsap.registerParameter(new Switch(VERBOSE_HELP, JSAP.NO_SHORTFLAG, VERBOSE_HELP,
            "Set this switch to print out a long help message"));
        // Add a switch to enable debugging messages to be printed to stdout
        jsap.registerParameter(new Switch(DEBUG, JSAP.NO_SHORTFLAG, DEBUG,
            "Set this switch in order to enable printing of debug messages"));
        // Add a flagged option to allow the user to attach a description to
        // the service instance that will be created
        jsap.registerParameter(new FlaggedOption(DESCRIPTION, JSAP.STRING_PARSER,
            "", false, JSAP.NO_SHORTFLAG, DESCRIPTION, 
            "A short description of the service instance that will be created"));
    }
    
    /**
     * Populates the JSAP object with paramters created from the given service
     * configuration information 
     * @throws JSAPException if there was an error adding a parameter
     */
    private static void addParametersFromConfig(JSAP jsap,
        GridServiceConfigForClient config) throws JSAPException
    {
        for (Parameter param : config.getParams())
        {
            String name = param.getName();
            String description = param.getDescription();
            char shortFlag = param.getFlag() == null ? JSAP.NO_SHORTFLAG : param.getFlag().charAt(0);
            String longFlag = param.getLongFlag() == null ? JSAP.NO_LONGFLAG : param.getLongFlag();
            boolean required = param.isRequired() ? JSAP.NOT_REQUIRED : JSAP.REQUIRED;
            boolean greedy = param.isGreedy();
            String defaultValue = param.getDefaultValue() == null ? JSAP.NO_DEFAULT : param.getDefaultValue();
            
            if (param.getType() == Parameter.Type.SWITCH)
            {
                Switch sw = new Switch(name, shortFlag, longFlag, description);
                sw.setDefault(defaultValue);
                jsap.registerParameter(sw);
            }
            else if (param.getType() == Parameter.Type.UNFLAGGED_OPTION)
            {
                jsap.registerParameter(new UnflaggedOption(name, JSAP.STRING_PARSER,
                    defaultValue, required, greedy, description));
            }
            else if (param.getType() == Parameter.Type.FLAGGED_OPTION)
            {
                jsap.registerParameter(new FlaggedOption(name, JSAP.STRING_PARSER,
                    defaultValue, required, shortFlag, longFlag, description));
            }
            else
            {
                // Shouldn't happen
                throw new AssertionError("Unrecognized parameter type");
            }
        }
    }
    
}
