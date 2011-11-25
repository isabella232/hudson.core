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

package org.eclipse.hudson.model.project.property;

import hudson.model.FreeStyleProjectMock;
import org.eclipse.hudson.model.project.property.BaseProjectProperty;
import org.eclipse.hudson.model.project.property.ExternalProjectProperty;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Contains test-cases for {@link ExternalProjectProperty}.
 * <p/>
 * Date: 11/4/11
 *
 * @author Nikita Levyankov
 */
@SuppressWarnings("unchecked")
public class ExternalProjectPropertyTest {

    private ExternalProjectProperty property;
    private FreeStyleProjectMock project;

    @Before
    public void setUp() {
        project = new FreeStyleProjectMock("project");
        final String propertyKey = "propertyKey";
        property = new ExternalProjectProperty(project);
        property.setKey(propertyKey);
    }

    /**
     * Verify constructor
     */
    @Test
    public void testConstructor() {
        try {
            new ExternalProjectProperty(null);
            fail("Null should be handled by ExternalProjectProperty constructor.");
        } catch (Exception e) {
            assertEquals(BaseProjectProperty.INVALID_JOB_EXCEPTION, e.getMessage());
        }
    }

    /**
     * Verify {@link org.eclipse.hudson.model.project.property.ExternalProjectProperty#updateOriginalValue(Object, Object)} method.
     */
    @Test
    public void testUpdateOriginalValue() {
        //If property is not modified, than it shouldn't be updated.
        assertFalse(property.isModified());
        assertFalse(property.updateOriginalValue(new Object(), new Object()));

        //If property is modified, than BaseProjectProperty#updateOriginalValue method will be called.
        //Result will be true, because in this case property value will be updated.
        property.setModified(true);
        assertTrue(property.isModified());
        assertFalse(property.updateOriginalValue(new Object(), new Object()));
        assertTrue(property.updateOriginalValue(new Object(), project));
    }

    /**
     * Verify {@link org.eclipse.hudson.model.project.property.ExternalProjectProperty#onCascadingProjectSet()} method.
     */
    @Test
    public void testOnCascadingProjectSet() {
        assertFalse(property.isModified());
        assertFalse(property.isOverridden());
        //When cascading project was set, isModified should equal to isOverridden
        property.onCascadingProjectSet();
        assertEquals(property.isModified(), property.isOverridden());

        property.setModified(Boolean.TRUE);
        property.onCascadingProjectSet();
        assertTrue(property.isModified());
        assertEquals(property.isModified(), property.isOverridden());
    }
}
