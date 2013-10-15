/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 *    Kohsuke Kawaguchi, Tom Huybrechts
 *
 *
 *******************************************************************************/ 

package hudson.model;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import hudson.Extension;

public class RunParameterDefinition extends SimpleParameterDefinition {

    private final String projectName;

    @DataBoundConstructor
    public RunParameterDefinition(String name, String projectName, String description) {
        super(name, description);
        this.projectName = projectName;
    }

    @Exported
    public String getProjectName() {
        return projectName;
    }

    public Job getProject() {
        return (Job) Hudson.getInstance().getItem(projectName);
    }
    
    @Override
    public ParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof RunParameterValue) {
            RunParameterValue value = (RunParameterValue) defaultValue;
            return new RunParameterDefinition(getName(), value.getRunId(), getDescription());
        } else {
            return this;
        }
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.RunParameterDefinition_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/help/parameter/run.html";
        }

        @Override
        public ParameterDefinition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(RunParameterDefinition.class, formData);
        }
    }

    @Override
    public ParameterValue getDefaultParameterValue() {
        Run<?, ?> lastBuild = getProject().getLastBuild();
        if (lastBuild != null) {
            return createValue(lastBuild.getExternalizableId());
        } else {
            return null;
        }
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        RunParameterValue value = req.bindJSON(RunParameterValue.class, jo);
        value.setDescription(getDescription());
        return value;
    }

    public RunParameterValue createValue(String value) {
        return new RunParameterValue(getName(), value, getDescription());
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && new EqualsBuilder()
                .append(getProjectName(), ((RunParameterDefinition) o).getProjectName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(getProjectName())
                .toHashCode();
    }
}
