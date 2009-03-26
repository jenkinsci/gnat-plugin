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

package com.thalesgroup.hudson.plugins.gnat.gnathtml;



import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.maven.AbstractMavenProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Project;
import hudson.model.ProminentProjectAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.thalesgroup.hudson.plugins.gnat.GnatInstallation;
import com.thalesgroup.hudson.plugins.gnat.gnatmake.GnatmakeBuilder;
import com.thalesgroup.hudson.plugins.gnat.util.GnatException;
import com.thalesgroup.hudson.plugins.gnat.util.GnatUtil;

/**
 * 
 * @author Gregory Boissinot
 * @version 1.0 Initial Version
 */
public class GnathtmlArchiver extends Publisher implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
    private static final String GNATHTML_DEFAULT_GENERATED_DIRECTORY = "html";
    
    private static final String GNATHTML_GENERATED_DIRECTORY_OTION= "-o";
    	
	
    public final static GnathtmlArchiverDescriptor DESCRIPTOR = new GnathtmlArchiverDescriptor();
	
    private final String gnatName;
    
	private final String switches;
	
	/**
	 * Many Ada files as you want. gnathtml will generate an html file for every ada file
	 */
	private final String adafiles;	
	
    /**
     * If true, retain gnathtml for all the successful builds.
     */
    private final boolean keepAll;    
    

	public String getSwitches() {
		return switches;
	}
	
	public String getAdafiles() {
		return adafiles;
	}
	
	public boolean isKeepAll() {
		return keepAll;
	}

	public String getGnatName() {
		return gnatName;
	}
	
	
    @DataBoundConstructor
	public GnathtmlArchiver(final String gnatName, final String switches, final String adafiles, final boolean keepAll) {
		this.gnatName=gnatName;
		this.switches = switches;
		this.adafiles = adafiles;
		this.keepAll = keepAll;
	}
    
    
    public static final class GnathtmlArchiverDescriptor extends Descriptor<Publisher>{

        public GnathtmlArchiverDescriptor() {
            super(GnathtmlArchiver.class);
        }

        @Override
        public String getDisplayName() {
            return "Publish gnathtml reports";
        }

        @Override
        public GnathtmlArchiver newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        	//return req.bindJSON(GnathtmlArchiver.class, formData);
        	GnathtmlArchiver p = new GnathtmlArchiver(
        			req.getParameter("gnathtml.gnatName"),
        			req.getParameter("gnathtml.switches"),
        			req.getParameter("gnathtml.adafiles"),
        			req.getParameter("gnathtml.keepall")!=null);
            return p;

        }

        @Override
        public String getHelpFile() {
        	return "/plugin/gnat/gnathtml/help.html";
        }
        
        
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return !AbstractMavenProject.class.isAssignableFrom(jobType);
        }      
        
    	public GnatInstallation[] getInstallations() {
    		return GnatmakeBuilder.DESCRIPTOR.getInstallations();					
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
        


    
    /**
     * Gets the directory where the gnathtml is generated for the given build.
     *
     */
    private  FilePath getGnathtmlGeneratedDir(AbstractBuild<?,?> build) throws InterruptedException{
        
    	//default generated directory
    	String generatedDir = GNATHTML_DEFAULT_GENERATED_DIRECTORY;

        Pattern p = Pattern.compile("[\\s]+");
        // Split input with the pattern
        String[] result = p.split(switches);  
        for (int i=0; i<result.length; i++){
            if (GNATHTML_GENERATED_DIRECTORY_OTION.equals(result[i])  && (i+1 <= result.length-1)){
            	return build.getParent().getModuleRoot().child(result[i+1]);  	
            }
    	}
        return build.getParent().getModuleRoot().child(generatedDir);  	
    	
    }        
    
    /**
     * Gets the directory where the gnathtml is stored for the given project.
     */
    private static File getGnathtmlDir(AbstractItem project) {
    			return new File(project.getRootDir(),"gnathtml");
    }

    /**
     * Gets the directory where the gnathtml is stored for the given build.
     */
    private static File getGnathtmlDir(Run run) {
    		return new File(run.getRootDir(),"gnathtml");
    }    
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean perform(Build<?,?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
    	
    	Project proj = build.getProject();	
		String execPathGnathtml=null;
		try{
			execPathGnathtml=GnatUtil.getExecutable(DESCRIPTOR.getInstallations(), gnatName, launcher,listener,GnatInstallation.GNAT_TYPE.GNATHTML);
		}
		catch (GnatException ge){
			ge.printStackTrace(listener.fatalError("error"));
			build.setResult(Result.FAILURE);
			return false;
		}	    	
    	
		ArgumentListBuilder args = new ArgumentListBuilder();		
		args.add(execPathGnathtml);
										
		GnatUtil.addTokenIfExist(switches,args, false, build);
		GnatUtil.addTokenIfExist(adafiles,args, true, build);		
		
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
		
		
		//Retrieve the generate gnathtml directory from the build
        FilePath gnathtmlGenerateDir = getGnathtmlGeneratedDir(build);
        
        //Determine the stored gnathtml directory
        FilePath target = new FilePath(keepAll ? getGnathtmlDir(build) : getGnathtmlDir(build.getProject()));    	

     
        try {
          if (gnathtmlGenerateDir.copyRecursiveTo("**/*",target)==0) {
              if(build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
                    // If the build failed, don't complain that there was no gnathtml.
                    // The build probably didn't even get to the point where it produces gnathtml. 
                }
                build.setResult(Result.FAILURE);
                return true;
            }
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace(listener.fatalError("error"));
            build.setResult(Result.FAILURE);
            return true;
        }
        
        FilePath originIndex = new FilePath(target,"index.htm");                
        originIndex.renameTo(new FilePath(target,"index.html"));
     
        // add build action, if gnathtml is recorded for each build
        if(keepAll)                
            build.addAction(new GnathtmlBuildAction(build));		
 

		return true;
	}


    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public Action getProjectAction(Project project) {
        return new GnathtmlAction(project);
    }



 
    protected static abstract class BaseGnathtmlAction implements Action {
        public String getUrlName() {
            return "gnathtml";
        }

        public String getDisplayName() {
        	return "Gnat HTML";
        }

        public String getIconFileName() {
            if(dir().exists())
                return "help.gif";
            else
                // hide it since we don't have gnathtml yet.
                return null;
        }

        public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
            new DirectoryBrowserSupport(this, getTitle())
                .serveFile(req, rsp, new FilePath(dir()), "help.gif", false);
        }

        protected abstract String getTitle();

        protected abstract File dir();
    }

    public static class GnathtmlAction extends BaseGnathtmlAction implements ProminentProjectAction {
        private final AbstractItem project;

        public GnathtmlAction(AbstractItem project) {
            this.project = project;
        }

        protected File dir() {

            if (project instanceof AbstractProject) {
                AbstractProject abstractProject = (AbstractProject) project;

                Run run = abstractProject.getLastSuccessfulBuild();
                if (run != null) {
                    File gnathtmlDir = getGnathtmlDir(run);

                    if (gnathtmlDir.exists())
                        return gnathtmlDir;
                }
            }

            return getGnathtmlDir(project);
        }

        protected String getTitle() {
            return project.getDisplayName()+" gnathtml";
        }
    }
    
    public static class GnathtmlBuildAction extends BaseGnathtmlAction {
    	private final AbstractBuild<?,?> build;
    	
    	public GnathtmlBuildAction(AbstractBuild<?,?> build) {
    	    this.build = build;
    	}

        protected String getTitle() {
            return build.getDisplayName()+" gnathtml";
        }

        protected File dir() {
            return new File(build.getRootDir(),"gnathtml");
        }
    }





        
}

