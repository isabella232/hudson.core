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
package hudson.model;

import java.io.File;
import java.net.URISyntaxException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Test for legacy
 * <p/>
 * Date: 9/23/11
 *
 * @author Nikita Levyankov
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class})
public class LegacyProjectTest {

    private File config;
    @Before
    public void setUp() throws URISyntaxException {
        config = new File(FreeStyleProject.class.getResource("/hudson/model/freestyle").toURI());
    }

    /**
     * Tests unmarshalls FreeStyleProject configuration and checks whether properties are configured based
     * on legacy values,
     *
     * @throws Exception if any.
     */
    @Test
    public void testLoadLegacyFreeStyleProject() throws Exception {
        FreeStyleProject project = (FreeStyleProject) Items.getConfigFile(config).read();
        project.setAllowSave(false);
        project.initProjectProperties();
        //Checks customWorkspace value
        assertNull(project.getProperty(FreeStyleProject.CUSTOM_WORKSPACE_PROPERTY_NAME));
        project.buildProjectProperties();
        assertNotNull(project.getProperty(FreeStyleProject.CUSTOM_WORKSPACE_PROPERTY_NAME));
    }

    /**
     * Tests unmarshalls FreeStyleProject configuration and checks whether properties
     * from AbstractProject are configured
     *
     * @throws Exception if any.
     */
    @Test
    public void testLoadLegacyAbstractProject() throws Exception {
        AbstractProject project = (AbstractProject) Items.getConfigFile(config).read();
        project.setAllowSave(false);
        project.initProjectProperties();
        assertNull(project.getProperty(AbstractProject.BLOCK_BUILD_WHEN_UPSTREAM_BUILDING_PROPERTY_NAME));
        assertNull(project.getProperty(AbstractProject.BLOCK_BUILD_WHEN_DOWNSTREAM_BUILDING_PROPERTY_NAME));
        assertNull(project.getProperty(AbstractProject.CONCURRENT_BUILD_PROPERTY_NAME));
        assertNull(project.getProperty(AbstractProject.CLEAN_WORKSPACE_REQUIRED_PROPERTY_NAME));
        assertNull(project.getProperty(AbstractProject.QUIET_PERIOD_PROPERTY_NAME));
        assertNull(project.getProperty(AbstractProject.SCM_CHECKOUT_RETRY_COUNT_PROPERTY_NAME));
        assertNull(project.getProperty(AbstractProject.JDK_PROPERTY_NAME));
        project.buildProjectProperties();
        assertNotNull(project.getProperty(AbstractProject.BLOCK_BUILD_WHEN_UPSTREAM_BUILDING_PROPERTY_NAME));
        assertNotNull(project.getProperty(AbstractProject.BLOCK_BUILD_WHEN_DOWNSTREAM_BUILDING_PROPERTY_NAME));
        assertNotNull(project.getProperty(AbstractProject.CONCURRENT_BUILD_PROPERTY_NAME));
        assertNotNull(project.getProperty(AbstractProject.CLEAN_WORKSPACE_REQUIRED_PROPERTY_NAME));
        assertNotNull(project.getProperty(AbstractProject.QUIET_PERIOD_PROPERTY_NAME));
        assertNotNull(project.getProperty(AbstractProject.SCM_CHECKOUT_RETRY_COUNT_PROPERTY_NAME));
        assertNotNull(project.getProperty(AbstractProject.JDK_PROPERTY_NAME));
    }

    /**
     * Tests unmarshalls FreeStyleProject configuration and checks whether properties
     * from Job are configured
     *
     * @throws Exception if any.
     */
    @Test
    public void testLoadLegacyJob() throws Exception {
        Job project = (Job) Items.getConfigFile(config).read();
        project.setAllowSave(false);
        project.initProjectProperties();
        assertNull(project.getProperty(Job.LOG_ROTATOR_PROPERTY_NAME));
        project.buildProjectProperties();
        assertNotNull(project.getProperty(Job.LOG_ROTATOR_PROPERTY_NAME));
    }
}
