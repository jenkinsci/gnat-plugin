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

package com.thalesgroup.hudson.plugins.gnat.util;

import com.thalesgroup.hudson.plugins.gnat.GnatInstallation;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;

public class GnatUtil {


    public static String getExecutable(GnatInstallation[] installations, String selectGnatLogicalName, Launcher launcher, BuildListener listener, GnatInstallation.GNAT_TYPE gnatType) throws GnatException {

        String execPath = null;

        GnatInstallation ai = getGnatlogicalName(installations, selectGnatLogicalName);

        if (ai == null) {
            execPath = gnatType.getExecName(launcher.isUnix());
        } else {

            File exec = ai.getExecutable(gnatType);
            if (!ai.getExists()) {
                listener.fatalError(exec + " doesn't exist");
                throw new GnatException();
            }
            execPath = exec.getPath();
        }

        return execPath;
    }


    public static void addTokenIfExist(String token, ArgumentListBuilder args, boolean replaceMacro, AbstractBuild<?, ?> build, String... beforeArgs) throws IOException, InterruptedException {

        String normalizedToken = token.replaceAll("[\t\r\n]+", " ");

        if (replaceMacro)
            normalizedToken = Util.replaceMacro(normalizedToken, build.getEnvironment(TaskListener.NULL));

        if (normalizedToken != null && normalizedToken.trim().length() != 0) {
            for (String arg : beforeArgs) {
                args.add(arg);
            }
            args.addTokenized(normalizedToken);
        }
    }


    /**
     * Gets the Gnat to invoke, or null to invoke the default one.
     */
    private static GnatInstallation getGnatlogicalName(GnatInstallation[] installations, String selectGnatLogicalName) {
        for (GnatInstallation i : installations) {
            if (selectGnatLogicalName != null && i.getName().equals(selectGnatLogicalName))
                return i;
        }
        return null;
    }
}
