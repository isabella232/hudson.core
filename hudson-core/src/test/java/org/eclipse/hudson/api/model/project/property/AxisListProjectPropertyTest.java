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

package org.eclipse.hudson.api.model.project.property;

import hudson.matrix.AxisList;
import hudson.model.FreeStyleProjectMock;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Contains test-cases for {@link AxisListProjectProperty}.
 * <p/>
 * Date: 11/4/11
 *
 * @author Nikita Levyankov
 */
public class AxisListProjectPropertyTest {

    private AxisListProjectProperty property;

    @Before
    public void setUp() {
        final String propertyKey = "propertyKey";
        FreeStyleProjectMock project = new FreeStyleProjectMock("project");
        property = new AxisListProjectProperty(project);
        property.setKey(propertyKey);
    }

    /**
     * Verify constructor
     */
    @Test
    public void testConstructor() {
        try {
            new AxisListProjectProperty(null);
            fail("Null should be handled by AxisListProjectProperty constructor.");
        } catch (Exception e) {
            assertEquals(BaseProjectProperty.INVALID_JOB_EXCEPTION, e.getMessage());
        }
    }

    /**
     * Verify {@link CopyOnWriteListProjectProperty#getDefaultValue()} method.
     */
    @Test
    public void testAxisListProjectPropertyGetDefaultValue() {
        AxisList defaultValue = property.getDefaultValue();
        assertNotNull(defaultValue);
        assertTrue(defaultValue.isEmpty());
    }
}
