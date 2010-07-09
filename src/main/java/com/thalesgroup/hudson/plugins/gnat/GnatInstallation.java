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


package com.thalesgroup.hudson.plugins.gnat;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;

/**
 * Gnatmake installation.
 *
 * @author Gregory Boissinot
 */
public final class GnatInstallation {

    public static enum GNAT_TYPE {

        GNAT("gnat", false),
        GNATMAKE("gnatmake", false),
        GNATMETRIC("gnatmetric", false),
        GNATCHECK("gnatcheck", false),
        GNATHTML("gnathtml", true);

        private final String execName;

        private final boolean perl;

        private GNAT_TYPE(String execName, boolean perl) {
            this.execName = execName;
            this.perl = perl;
        }

        public String getExecName(boolean isUnix) {
            if (perl) {
                return execName + ".pl";
            } else if (!isUnix) {
                return execName + ".exe";
            }

            return execName;
        }
    }

    private final String name;
    private final String gnatmakeHome;

    @DataBoundConstructor
    public GnatInstallation(String name, String home) {
        this.name = name;
        this.gnatmakeHome = home;
    }

    public String getGnatmakeHome() {
        return gnatmakeHome;
    }

    public String getName() {
        return name;
    }

    public File getExecutable(GNAT_TYPE gnattype) {
        String execName;
        if (File.separatorChar == '\\') {
            execName = gnattype.getExecName(false);
        } else {
            execName = gnattype.getExecName(true);
        }
        return new File(getGnatmakeHome(), "bin/" + execName);
    }


    /**
     * Returns true if the executable exists
     * @return true if the executable exists, false otherwise
     */
    public boolean getExists() {
        return true;
    }
}
