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

package com.thalesgroup.hudson.plugins.gnat.gnatmake;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormFieldValidator;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.thalesgroup.hudson.plugins.gnat.GnatInstallation;
import com.thalesgroup.hudson.plugins.gnat.util.GnatException;
import com.thalesgroup.hudson.plugins.gnat.util.GnatUtil;

/**
 * @author Gregory Boissinot
 */
public class GnatmakeBuilder extends Builder {

	/**
	 * Identifies {@link GnatInstallation} to be used.
	 */
	private final String gnatName;

	private final String switches;

	private final String fileNames;

	private final String modeSwitches;

	public String getSwitches() {
		return switches;
	}

	@DataBoundConstructor
	public GnatmakeBuilder(String gnatName, String switches, String fileNames,
			String modeSwitches) {
		this.gnatName = gnatName;
		this.switches = switches;
		this.fileNames = fileNames;
		this.modeSwitches = modeSwitches;
	}




	public String getFileNames() {
		return fileNames;
	}

	public String getModeSwitches() {
		return modeSwitches;
	}

	public String getGnatName() {
		return gnatName;
	}


	public boolean perform(Build<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException {
		Project proj = build.getProject();

		ArgumentListBuilder args = new ArgumentListBuilder();

		String execPathGnatmake=null;
		try{
			execPathGnatmake=GnatUtil.getExecutable(DESCRIPTOR.getInstallations(), gnatName, launcher,listener,GnatInstallation.GNAT_TYPE.GNATMAKE);
			args.add(execPathGnatmake);
		}
		catch (GnatException ge){
			ge.printStackTrace(listener.fatalError("error"));
			build.setResult(Result.FAILURE);
			return false;
		}		

		String normalizedSwitches = switches.replaceAll("[\t\r\n]+", " ");
		String normalizedFileNames = fileNames.replaceAll("[\t\r\n]+", " ");
		String normalizedModeSwitches = modeSwitches.replaceAll("[\t\r\n]+"," ");		
		
		if (normalizedSwitches != null
				&& normalizedSwitches.trim().length() != 0) {
			args.addTokenized(normalizedSwitches);
		}

		if (normalizedFileNames == null
				|| normalizedFileNames.trim().length() == 0) {
			listener.fatalError("The GNAT file_name field is mandatory.");
			return false;
		}
		args.addTokenized(normalizedFileNames);

		if (normalizedModeSwitches != null
				&& normalizedModeSwitches.trim().length() != 0) {
			args.addTokenized(normalizedModeSwitches);
		}

		if (!launcher.isUnix()) {
			// on Windows, executing batch file can't return the correct error
			// code,
			// so we need to wrap it into cmd.exe.
			// double %% is needed because we want ERRORLEVEL to be expanded
			// after
			// batch file executed, not before. This alone shows how broken
			// Windows is...
			args.prepend("cmd.exe", "/C");
			args.add("&&", "exit", "%%ERRORLEVEL%%");
		}

		try {
			int r = launcher.launch(args.toCommandArray(), build.getEnvVars(),
					listener.getLogger(), proj.getModuleRoot()).join();
			return r == 0;
		} catch (IOException e) {
			Util.displayIOException(e, listener);
			e.printStackTrace(listener.fatalError("command execution failed"));
			return false;
		}
	}

	public Descriptor<Builder> getDescriptor() {
		return DESCRIPTOR;
	}

	public static final GnatmakeBuilderDescriptor DESCRIPTOR = new GnatmakeBuilderDescriptor();

	
	@Extension
	public static final class GnatmakeBuilderDescriptor extends Descriptor<Builder> {

		@CopyOnWrite
		private volatile GnatInstallation[] installations = new GnatInstallation[0];

		private GnatmakeBuilderDescriptor() {
			super(GnatmakeBuilder.class);
			load();
		}


		public String getHelpFile() {
			return "/plugin/gnat/gnatmake/help.html";
		}

		public String getDisplayName() {
			return "Invoke gnatmake script";
		}

		public GnatInstallation[] getInstallations() {
			return installations;
		}

		public boolean configure(StaplerRequest req) {
			installations = req.bindParametersToList(GnatInstallation.class,
					"gnat.").toArray(new GnatInstallation[0]);
			save();
			return true;
		}

		/**
		 * Checks if the specified Hudson GNATMAKE_HOME is valid.
		 */
		public void doCheckGnatmakeHome(StaplerRequest req, StaplerResponse rsp)
				throws IOException, ServletException {

			new FormFieldValidator(req, rsp, true) {
				public void check() throws IOException, ServletException {
					File f = getFileParameter("value");

					if (!f.isDirectory()) {
						error(f + " is not a directory");
						return;
					}

					if (!new File(f, "bin").exists()
							&& !new File(f, "lib").exists()) {
						error(f
								+ " doesn't look like a GNAT installation directory");
						return;
					}

					ok();
				}
			}.process();
		}
	}

}
