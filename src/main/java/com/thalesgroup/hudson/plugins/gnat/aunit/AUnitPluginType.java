package com.thalesgroup.hudson.plugins.gnat.aunit;


import com.thalesgroup.dtkit.metrics.hudson.api.descriptor.TestTypeDescriptor;
import com.thalesgroup.dtkit.metrics.hudson.api.type.TestType;
import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * CppUnit Type
 *
 * @author Gregory Boissinot
 */

public class AUnitPluginType extends TestType {

    @DataBoundConstructor
    public AUnitPluginType(String pattern, boolean faildedIfNotNew, boolean deleteOutputFiles) {
        super(pattern, faildedIfNotNew, deleteOutputFiles);
    }

    public TestTypeDescriptor<?> getDescriptor() {
        return new AUnitPluginType.DescriptorImpl();
    }

    @Extension
    public static class DescriptorImpl extends TestTypeDescriptor<AUnitPluginType> {

        public DescriptorImpl() {
            super(AUnitPluginType.class, AUnitInputMetric.class);
        }

        public String getId() {
            return AUnitPluginType.class.getCanonicalName();
        }

    }

}
