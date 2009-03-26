/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
* Author : Gregory Boissinot                                                   *
*                                                                              *
* Permission is hereby granted, free of charge, to any person obtaining a copy *
* of this software and associated documentation files (the "Software"), to deal*
* in the Software without restriction, including without limitation the rights *
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
* copies of the Software, and to permit persons to whom the Software is        *
* furnished to do so, subject to the following conditions:                     *
*                                                                              *
* The above copyright notice and this permission notice shall be included in   *
* all copies or substantial portions of the Software.                          *
*                                                                              *
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
* THE SOFTWARE.                                                                *
*******************************************************************************/

package com.thalesgroup.hudson.plugins.gnat.aunit;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.FilePath.FileCallable;
import hudson.maven.AbstractMavenProject;
import hudson.maven.agent.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Publisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResultProjectAction;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.StaplerRequest;

public class AUnitPublisher extends Publisher implements Serializable{


	private static final long serialVersionUID = 1L;

	public final static Descriptor<Publisher> DESCRIPTOR = new AUnitPublisherDescriptor();
	
    private final List<AUnitEntry>  executableListTestProj   = new ArrayList<AUnitEntry>();	
	

	public List<AUnitEntry> getExecutableListTestProj() {
		return executableListTestProj;
	}

	public Descriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}

	public static final class AUnitPublisherDescriptor extends Descriptor<Publisher> {

		public AUnitPublisherDescriptor() {
			super(AUnitPublisher.class);
		}

		@Override
		public String getDisplayName() {
			return "Execute AUnit Test";
		}
		
        @Override
        public Publisher newInstance(StaplerRequest req) {
            AUnitPublisher pub = new AUnitPublisher();
            req.bindParameters(pub, "aunit.");
            pub.getExecutableListTestProj().addAll(req.bindParametersToList(AUnitEntry.class, "aunit.entry."));
            return pub;
        }		
		

		@Override
		public String getHelpFile() {
			return "/plugin/gnat/aunit/help.html";
		}

		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// Only for free-style projects
			return !AbstractMavenProject.class.isAssignableFrom(jobType);
		}
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		return true;
	}
	 
	 public static final String JUNIT_REPORTS_PATH = "temporary-junit-reports";

	 
	 @Override
	 public Action getProjectAction(hudson.model.Project project) {
	    TestResultProjectAction action = project.getAction(TestResultProjectAction.class);
	    if (action == null) {
	        return new TestResultProjectAction(project);
	    } else {
	        return null;
	     }
	 }	 
	 
	@SuppressWarnings("unchecked")
	@Override
	public boolean perform(Build<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		boolean result = true;
		
		if (build.getResult().equals(Result.SUCCESS) || (build.getResult().equals(Result.UNSTABLE))) {

			Project proj = build.getProject();
			
			
            FilePath junitOutputPath = new FilePath(proj.getWorkspace(), JUNIT_REPORTS_PATH);
            junitOutputPath.mkdirs();			
			
			for (AUnitEntry entry:executableListTestProj){
				
				ArgumentListBuilder args = new ArgumentListBuilder();
				String normalizedExecutableProjTest = entry.executableTestProj.replaceAll("[\t\r\n]+", " ");
				normalizedExecutableProjTest = Util.replaceMacro(normalizedExecutableProjTest, build.getEnvVars());
				args.add(proj.getModuleRoot() + File.separator+ normalizedExecutableProjTest);

				if (!launcher.isUnix()) {
					// on Windows, executing batch file can't return the correct
					// error code,
					// so we need to wrap it into cmd.exe.
					// double %% is needed because we want ERRORLEVEL to be expanded
					// after
					// batch file executed, not before. This alone shows how broken
					// Windows is...
					args.prepend("cmd.exe", "/C");
					args.add("&&", "exit", "%%ERRORLEVEL%%");
				}	

				try {

					
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					int r = launcher.launch(args.toCommandArray(),build.getEnvVars(), baos,proj.getModuleRoot()).join();
				    if (r != 0){
				    	return false;
				    }
				    listener.getLogger().write(baos.toByteArray());				    
				    String vAUnitExecLog = baos.toString();
				    baos.close();
				    				    
					AUnitParsing vAUnitParsing=new AUnitParsing();
					vAUnitParsing.process(junitOutputPath, vAUnitExecLog, normalizedExecutableProjTest);
					
				} catch (IOException e) {
					Util.displayIOException(e, listener);
					e.printStackTrace(listener.fatalError("error"));
					build.setResult(Result.FAILURE);
					return false;
				}
			}
			
            result  = recordTestResult(JUNIT_REPORTS_PATH + "/TEST-*.xml", build, listener);
            build.getProject().getWorkspace().child(JUNIT_REPORTS_PATH).deleteRecursive();			
		}
		
		return result;
	}
	

    /**
     * Collect the test results from the files
     * @param junitFilePattern
     * @param build
     * @param existingTestResults existing test results to add results to
     * @param buildTime
     * @return a test result
     * @throws IOException
     * @throws InterruptedException
     */
    private TestResult getTestResult(
    		final String junitFilePattern, 
    		AbstractBuild<?, ?> build,
            final TestResult existingTestResults, 
            final long buildTime) 
    throws IOException, InterruptedException {
    
    	TestResult result = build.getProject().getWorkspace().act(
    			
    			new FileCallable<TestResult>() {
    					public TestResult invoke(File ws, VirtualChannel channel) throws IOException {
    						FileSet fs = Util.createFileSet(ws,junitFilePattern);
    						DirectoryScanner ds = fs.getDirectoryScanner();
    						String[] files = ds.getIncludedFiles();
    						if(files.length==0) {
    							// no test result. Most likely a configuration error or fatal problem
    							throw new AbortException("No test report files were found. Configuration error?");
    						}
    						if (existingTestResults == null) {
    							return new TestResult(buildTime, ds);
    						} else {
    							existingTestResults.parse(buildTime, ds);
    							return existingTestResults;
    						}
    					}
    			});
        	return result;
    	}	
	
    /**
     * Record the test results into the current build.
     * @param junitFilePattern
     * @param build
     * @param listener
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private boolean recordTestResult(String junitFilePattern, AbstractBuild<?, ?> build, BuildListener listener)
            throws InterruptedException, IOException {
    	
    	
        TestResultAction existingAction = build.getAction(TestResultAction.class);
        TestResultAction action;

        try {
            final long buildTime = build.getTimestamp().getTimeInMillis();

            TestResult existingTestResults = null;
            if (existingAction != null) {
                existingTestResults = existingAction.getResult();
            }
            TestResult result = getTestResult(junitFilePattern, build, existingTestResults, buildTime);

            if (existingAction == null) {
                action = new TestResultAction(build, result, listener);
            } else {
                action = existingAction;
                action.setResult(result, listener);
            }
            if(result.getPassCount()==0 && result.getFailCount()==0)
                new AbortException("None of the test reports contained any result");
        } catch (AbortException e) {
            if(build.getResult()==Result.FAILURE)
                // most likely a build failed before it gets to the test phase.
                // don't report confusing error message.
                return true;

            listener.getLogger().println(e.getMessage());
            build.setResult(Result.FAILURE);
            return true;
        }

        if (existingAction == null) {
            build.getActions().add(action);
        }

        if(action.getResult().getFailCount()>0)
            build.setResult(Result.UNSTABLE);

        return true;
    }	

}
