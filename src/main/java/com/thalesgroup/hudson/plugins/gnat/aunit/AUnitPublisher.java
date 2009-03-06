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

import hudson.Launcher;
import hudson.Util;
import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Result;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;

public class AUnitPublisher extends Publisher {

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

	@SuppressWarnings("unchecked")
	@Override
	public boolean perform(Build<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		if (build.getResult().equals(Result.SUCCESS) || (build.getResult().equals(Result.UNSTABLE))) {

			Project proj = build.getProject();
			
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
					int r = launcher.launch(args.toCommandArray(),build.getEnvVars(), listener.getLogger(),proj.getModuleRoot()).join();
				    if (r != 0){
				    	return false;
				    }
				} catch (IOException e) {
					Util.displayIOException(e, listener);
					e.printStackTrace(listener.fatalError("error"));
					build.setResult(Result.FAILURE);
					return false;
				}
			}

		}
		
		return true;
	}

}
