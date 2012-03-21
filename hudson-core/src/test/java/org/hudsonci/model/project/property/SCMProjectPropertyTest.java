/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov
 *
 *******************************************************************************/
package org.hudsonci.model.project.property;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProjectMock;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.NullSCM;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Contains test-cases for {@link SCMProjectProperty}.
 * <p/>
 * Date: 11/17/11
 *
 * @author Nikita Levyankov
 */
public class SCMProjectPropertyTest {

    private SCMProjectProperty property;
    private FreeStyleProjectMock project;

    @Before
    public void setUp() {
        project = new FreeStyleProjectMock("project");
        final String propertyKey = "propertyKey";
        property = new SCMProjectProperty(project);
        property.setKey(propertyKey);
    }

    /**
     * Verify constructor
     */
    @Test
    public void testConstructor() {
        try {
            new SCMProjectProperty(null);
            fail("Null should be handled by SCMProjectProperty constructor.");
        } catch (Exception e) {
            assertEquals(BaseProjectProperty.INVALID_JOB_EXCEPTION, e.getMessage());
        }
    }

    /**
     * Verify {@link org.hudsonci.model.project.property.SCMProjectProperty#getDefaultValue()} method.
     */
    @Test
    public void testGetDefaultValue() {
        assertEquals(new NullSCM(), property.getDefaultValue());
    }

    /**
     * Verify {@link org.hudsonci.model.project.property.SCMProjectProperty#returnOriginalValue()} method.
     */
    @Test
    public void testReturnOriginalValue() {
        //If property is marked as overridden or original value is not null and not equals to default value,
        //than original should be used.
        property.setOverridden(true);
        assertTrue(property.returnOriginalValue());
        property.setOriginalValue(new FakeSCM(), false);
        assertTrue(property.returnOriginalValue());

        //If property is not marked as overridden and original value is null or equals to default value - use cascading.
        property.setOriginalValue(property.getDefaultValue(), false);
        assertFalse(property.returnOriginalValue());
        property.setOriginalValue(null, false);
        assertFalse(property.returnOriginalValue());

    }

    private class FakeSCM extends SCM {
        @Override
        public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher,
                                                       TaskListener listener)
            throws IOException, InterruptedException {
            return null;
        }

        @Override
        protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher,
                                                          FilePath workspace, TaskListener listener,
                                                          SCMRevisionState baseline)
            throws IOException, InterruptedException {
            return null;
        }

        @Override
        public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace,
                                BuildListener listener,
                                File changelogFile) throws IOException, InterruptedException {
            return false;
        }

        @Override
        public ChangeLogParser createChangeLogParser() {
            return null;
        }
    }

}
