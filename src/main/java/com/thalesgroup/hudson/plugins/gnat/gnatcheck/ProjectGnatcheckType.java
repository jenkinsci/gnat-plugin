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

package com.thalesgroup.hudson.plugins.gnat.gnatcheck;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class ProjectGnatcheckType extends GnatcheckType {


    public final String projectFile;

    public final String options;


    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    @DataBoundConstructor
    public ProjectGnatcheckType(String gnatName,
                                String projectFile,
                                String options,
                                String rule_options) {
        super(gnatName, rule_options);
        this.projectFile = projectFile;
        this.options = options;
    }


    public static final class DescriptorImpl extends GnatcheckTypeDescriptor {
        private DescriptorImpl() {
            super(ProjectGnatcheckType.class);
        }

        public ProjectGnatcheckType newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(ProjectGnatcheckType.class, formData);
        }

        public String getHelpFile() {
            return "/plugin/gnat/gnatcheck/projectCommand.html";
        }

        public String getDisplayName() {
            return "project-wide gnat check";
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }

    private static final long serialVersionUID = 1L;

}
