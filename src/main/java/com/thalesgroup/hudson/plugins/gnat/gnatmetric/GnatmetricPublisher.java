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

import com.thalesgroup.hudson.plugins.gnat.GnatInstallation;
import com.thalesgroup.hudson.plugins.gnat.util.GnatException;
import com.thalesgroup.hudson.plugins.gnat.util.GnatUtil;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixProject;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ArgumentListBuilder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class GnatmetricPublisher extends Recorder implements Serializable {

    private static final long serialVersionUID = 1L;

    public final GnatmetricType[] types;

    public GnatmetricPublisher(GnatmetricType[] types) {
        this.types = types;
    }

    @Extension(ordinal = 9)
    @SuppressWarnings("unused")
    public static final class GnatmetricPublisherDescriptor extends BuildStepDescriptor<Publisher> {


        public GnatmetricPublisherDescriptor() {
            super(GnatmetricPublisher.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Run gnatmetric";
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            List<GnatmetricType> buildTypes = Descriptor.newInstancesFromHeteroList(
                    req, formData, "types", getBuildTypes());
            return new GnatmetricPublisher(buildTypes.toArray(new GnatmetricType[buildTypes.size()]));
        }

        @SuppressWarnings("unused")
        public List<GnatmetricTypeDescriptor> getBuildTypes() {
            return Hudson.getInstance().getDescriptorList(GnatmetricType.class);
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gnat/gnatmetric/help.html";
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType) || MatrixProject.class.isAssignableFrom(jobType);
        }


    }

//    @Override
//    public boolean needsToRunAfterFinalized() {
//        return true;
//    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return true;
    }


    @SuppressWarnings("unchecked")
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {

        if (build.getResult().equals(Result.SUCCESS) || (build.getResult().equals(Result.UNSTABLE))) {

            for (GnatmetricType type : types) {

                ArgumentListBuilder args = new ArgumentListBuilder();

                if (type instanceof ProjectGnatmetricType) {

                    ProjectGnatmetricType projectGnatMetricType = (ProjectGnatmetricType) type;

                    String execPathGnat;
                    try {
                        execPathGnat = GnatUtil.getExecutable(projectGnatMetricType.getDescriptor().getInstallations(), type.gnatName, launcher, listener, GnatInstallation.GNAT_TYPE.GNAT);
                    }
                    catch (GnatException ge) {
                        ge.printStackTrace(listener.fatalError("error"));
                        build.setResult(Result.FAILURE);
                        return false;
                    }


                    args.add(execPathGnat);
                    args.add("metric");
                    args.add("-P");

                    String normalizedProjectFile = projectGnatMetricType.projectFile.replaceAll("[\t\r\n]+", " ");
                    GnatUtil.addTokenIfExist(build.getModuleRoot() + File.separator + normalizedProjectFile, args, true, build);
                    GnatUtil.addTokenIfExist(projectGnatMetricType.options, args, false, build);

                } else {

                    FreeStyleGnatmetricType freeStyleGnatMetricType = (FreeStyleGnatmetricType) type;

                    String execPathGnatmetric;
                    try {
                        execPathGnatmetric = GnatUtil.getExecutable(freeStyleGnatMetricType.getDescriptor().getInstallations(), type.gnatName, launcher, listener, GnatInstallation.GNAT_TYPE.GNATMETRIC);
                    }
                    catch (GnatException ge) {
                        ge.printStackTrace(listener.fatalError("error"));
                        build.setResult(Result.FAILURE);
                        return false;
                    }

                    args.add(execPathGnatmetric);
                    GnatUtil.addTokenIfExist(freeStyleGnatMetricType.switches, args, false, build);
                    GnatUtil.addTokenIfExist(freeStyleGnatMetricType.filename, args, false, build);
                    GnatUtil.addTokenIfExist(freeStyleGnatMetricType.gcc_switches, args, false, build, "-cargs");
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
                    int r = launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener).pwd(build.getModuleRoot()).join();
//                    if (r != 0) {
//                        build.setResult(Result.FAILURE);
//                        return false;
//                    }
                } catch (IOException e) {
                    Util.displayIOException(e, listener);
                    e.printStackTrace(listener.fatalError("error"));
                    build.setResult(Result.FAILURE);
                    return false;
                }
            }
        }

        build.setResult(Result.SUCCESS);
        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }


}
