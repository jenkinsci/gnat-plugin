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

package com.thalesgroup.hudson.plugins.gnat.gnatmetric;

import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Result;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import com.thalesgroup.hudson.plugins.gnat.GnatInstallation;
import com.thalesgroup.hudson.plugins.gnat.util.GnatException;
import com.thalesgroup.hudson.plugins.gnat.util.GnatUtil;

public class GnatmetricPublisher extends Publisher implements Serializable{

	private static final long serialVersionUID = 1L;

	public final GnatmetricType[] types;
	
	public final static GnatmetricPublisherDescriptor DESCRIPTOR = new GnatmetricPublisherDescriptor();
	
	public Descriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}		
	
	public GnatmetricPublisher(GnatmetricType[] types){
		this.types= types;
	}
	
	
	public static final class GnatmetricPublisherDescriptor extends Descriptor<Publisher>{


			public GnatmetricPublisherDescriptor() {
				super(GnatmetricPublisher.class);
				load();
			}
			
			@Override
			public String getDisplayName() {
				return "Run gnatmetric";
			}
			
	        @Override
	        public Publisher newInstance(StaplerRequest req, JSONObject formData)throws FormException {
	            List<GnatmetricType> buildTypes = Descriptor.newInstancesFromHeteroList(
	                    req, formData, "types", GnatmetricTypeDescriptor.LIST);
	            return new GnatmetricPublisher(buildTypes.toArray(new GnatmetricType[buildTypes.size()]));
	        }	
	        
	        public List<GnatmetricTypeDescriptor> getBuildTypes() {
	            return GnatmetricTypeDescriptor.LIST; 
	        }	        
	        
			@Override
			public String getHelpFile() {
				return "/plugin/gnat/gnatmetric/help.html";
			}

			public boolean isApplicable(Class<? extends AbstractProject> jobType) {
				return FreeStyleProject.class.isAssignableFrom(jobType) || MatrixProject.class.isAssignableFrom(jobType);
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
			
			for (GnatmetricType type:types){
			
				ArgumentListBuilder args = new ArgumentListBuilder();
				
				if (type instanceof ProjectGnatmetricType){
					
					ProjectGnatmetricType projectGnatMetricType = (ProjectGnatmetricType)type;
					
					String execPathGnat=null;
					try{
						execPathGnat=GnatUtil.getExecutable(projectGnatMetricType.getDescriptor().getInstallations(), type.gnatName, launcher,listener,GnatInstallation.GNAT_TYPE.GNAT);
					}
					catch (GnatException ge){
						ge.printStackTrace(listener.fatalError("error"));
						build.setResult(Result.FAILURE);
						return false;
					}	
					
					
					args.add(execPathGnat);
					args.add("metric");
					args.add("-P");
					
					String normalizedProjectFile = projectGnatMetricType.projectFile.replaceAll("[\t\r\n]+", " ");								
					GnatUtil.addTokenIfExist(proj.getModuleRoot() + File.separator+ normalizedProjectFile,args, true, build);
					GnatUtil.addTokenIfExist(projectGnatMetricType.options,args, false, build);
					
				}
				else {
				
					FreeStyleGnatmetricType freeStyleGnatMetricType = (FreeStyleGnatmetricType)type;
					
					String execPathGnatmetric=null;
					try{
						execPathGnatmetric=GnatUtil.getExecutable(freeStyleGnatMetricType.getDescriptor().getInstallations(), type.gnatName, launcher,listener,GnatInstallation.GNAT_TYPE.GNATMETRIC);					
					}
					catch (GnatException ge){
						ge.printStackTrace(listener.fatalError("error"));
						build.setResult(Result.FAILURE);
						return false;
					}
							
					args.add(execPathGnatmetric);
					GnatUtil.addTokenIfExist(freeStyleGnatMetricType.switches,args, false, build);
					GnatUtil.addTokenIfExist(freeStyleGnatMetricType.filename,args, false, build);
					GnatUtil.addTokenIfExist(freeStyleGnatMetricType.gcc_switches,args, false, build, "-cargs");					
				}
				

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
						build.setResult(Result.FAILURE);
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
