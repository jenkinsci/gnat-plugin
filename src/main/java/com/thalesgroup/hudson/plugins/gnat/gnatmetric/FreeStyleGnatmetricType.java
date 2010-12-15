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

import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import hudson.Extension;

public class FreeStyleGnatmetricType extends GnatmetricType {

    public final String switches;

    public final String filename;

    public final String gcc_switches;


    public DescriptorImpl getDescriptor() {
        return new  FreeStyleGnatmetricType.DescriptorImpl();
    }

    @DataBoundConstructor
    public FreeStyleGnatmetricType(String gnatName,
                                   String switches,
                                   String filename,
                                   String gcc_switches) {
        super(gnatName);
        this.switches = switches;
        this.filename = filename;
        this.gcc_switches = gcc_switches;
    }

    @Extension(ordinal=9)
    public static class DescriptorImpl extends GnatmetricTypeDescriptor {
        public DescriptorImpl() {
            super(FreeStyleGnatmetricType.class);
        }

        public FreeStyleGnatmetricType newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(FreeStyleGnatmetricType.class, formData);
        }

        public String getDisplayName() {
            return "general gnatmetric";
        }

        public String getHelpFile() {
            return "/plugin/gnat/gnatmetric/generalCommand.html";
        }
    }

}
