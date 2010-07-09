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
import hudson.Extension;

public class FreeStyleGnatcheckType extends GnatcheckType {

    public final String switches;

    public final String filename;

    public final String gcc_switches;

    public final String arg_list_filename;

    public DescriptorImpl getDescriptor() {
        return new FreeStyleGnatcheckType.DescriptorImpl();
    }

    @DataBoundConstructor
    public FreeStyleGnatcheckType(String gnatName,
                                  String switches,
                                  String filename,
                                  String arg_list_filename,
                                  String gcc_switches,
                                  String rule_options) {
        super(gnatName, rule_options);
        this.switches = switches;
        this.filename = filename;
        this.arg_list_filename = arg_list_filename;
        this.gcc_switches = gcc_switches;
    }


    @Extension
    public static class DescriptorImpl extends GnatcheckTypeDescriptor {
        public DescriptorImpl() {
            super(FreeStyleGnatcheckType.class);
        }

        public FreeStyleGnatcheckType newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(FreeStyleGnatcheckType.class, formData);
        }

        public String getHelpFile() {
            return "/plugin/gnat/gnatcheck/generalCommand.html";
        }


        public String getDisplayName() {
            return "general gnatcheck";
        }

    }

}
