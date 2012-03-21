/*******************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
*
*    Kohsuke Kawaguchi, id:cactusman, Anton Kozak, Nikita Levyankov
 *     
 *
 *******************************************************************************/ 

package hudson.model;

import hudson.Extension;

import hudson.util.CascadingUtil;
import java.io.IOException;

import org.hudsonci.api.model.IFreeStyleProject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;

/**
 * Free-style software project.
 * 
 * @author Kohsuke Kawaguchi
 */
public class FreeStyleProject extends Project<FreeStyleProject,FreeStyleBuild> implements TopLevelItem,
    IFreeStyleProject {

    /**
     * See {@link #setCustomWorkspace(String)}.
     *
     * @since 1.216
     * @deprecated as of 2.2.0
     *             don't use this field directly, logic was moved to {@link org.hudsonci.api.model.IProjectProperty}.
     *             Use getter/setter for accessing to this field.
     */
    @Deprecated
    private String customWorkspace;

    /**
     * @deprecated as of 1.390
     */
    public FreeStyleProject(Hudson parent, String name) {
        super(parent, name);
    }

    public FreeStyleProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    protected Class<FreeStyleBuild> getBuildClass() {
        return FreeStyleBuild.class;
    }

    public String getCustomWorkspace() throws IOException {
        return CascadingUtil.getStringProjectProperty(this, CUSTOM_WORKSPACE_PROPERTY_NAME).getValue();
    }

    /**
     * {@inheritDoc}
     */
    public void setCustomWorkspace(String customWorkspace) throws IOException {
        CascadingUtil.getStringProjectProperty(this, CUSTOM_WORKSPACE_PROPERTY_NAME).setValue(customWorkspace);
        save();
    }

    @Override
    protected void submit(StaplerRequest req, StaplerResponse rsp)
        throws IOException, ServletException, Descriptor.FormException {
        super.submit(req, rsp);
        setCustomWorkspace(
            req.hasParameter("customWorkspace") ? req.getParameter("customWorkspace.directory") : null);
    }

    @Override
    protected void buildProjectProperties() throws IOException {
        super.buildProjectProperties();
        convertCustomWorkspaceProperty();
    }

    /**
     * Converts customWorkspace property to ProjectProperty.
     *
     * @throws IOException if any.
     */
    void convertCustomWorkspaceProperty() throws IOException {
        if (null != customWorkspace && null == getProperty(CUSTOM_WORKSPACE_PROPERTY_NAME)) {
            setCustomWorkspace(customWorkspace);
            customWorkspace = null;//Reset to null. No longer needed.
        }
    }

    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension(ordinal=1000)
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends AbstractProjectDescriptor {
        public String getDisplayName() {
            return Messages.FreeStyleProject_DisplayName();
        }

        public FreeStyleProject newInstance(ItemGroup parent, String name) {
            return new FreeStyleProject(parent,name);
        }
    }
}
