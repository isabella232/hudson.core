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
import hudson.tasks.Shell;
import hudson.util.DescribableList;
import java.util.Arrays;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * Contains test-cases for {@link DescribableListProjectProperty}.
 * <p/>
 * Date: 11/4/11
 *
 * @author Nikita Levyankov
 */
@SuppressWarnings("unchecked")
public class DescribableListProjectPropertyTest {

    private FreeStyleProjectMock project;
    private FreeStyleProjectMock parent;
    private DescribableListProjectProperty property;

    @Before
    public void setUp() {
        project = new FreeStyleProjectMock("project");
        parent = new FreeStyleProjectMock("parent");

        final String propertyKey = "propertyKey";
        property = new DescribableListProjectProperty(project);
        property.setKey(propertyKey);
    }

    /**
     * Verify constructor
     */
    @Test
    public void testConstructor() {
        try {
            new DescribableListProjectProperty(null);
            fail("Null should be handled by DescribableListProjectProperty constructor.");
        } catch (Exception e) {
            assertEquals(BaseProjectProperty.INVALID_JOB_EXCEPTION, e.getMessage());
        }
    }

    /**
     * Verify {@link org.eclipse.hudson.model.project.property.DescribableListProjectProperty#getDefaultValue()} method.
     */
    @Test
    public void testGetDefaultValue() {
        DescribableList defaultValue = property.getDefaultValue();
        assertNotNull(defaultValue);
        //Default value should be initialized and stored as original value
        assertTrue(property.getOriginalValue() == defaultValue);
        assertFalse(property.isOverridden());
    }

    /**
     * Verify {@link org.eclipse.hudson.model.project.property.CopyOnWriteListProjectProperty#getOriginalValue()} method.
     */
    @Test
    public void testGetOriginalValue() {
        //Original value is not initialized. Default value will be used instead. Shouldn't be null
        DescribableList originalValue = property.getOriginalValue();
        assertNotNull(originalValue);
        //Value was set, so return it without modification
        assertTrue(originalValue == property.getOriginalValue());
    }

    /**
     * Verify {@link org.eclipse.hudson.model.project.property.CopyOnWriteListProjectProperty#returnOriginalValue()} method.
     */
    @Test
    public void testReturnOriginalValue() {
        //Return cascading is property is not overridden and is empty.
        assertFalse(property.returnOriginalValue());

        //Return properties' originalValue if it was overridden
        property.setOverridden(true);
        assertTrue(property.returnOriginalValue());
        //If property has not empty value - return it (basically for non-cascadable projects)
        property.setOriginalValue(new DescribableList(project, Arrays.asList(new Object())), false);
        assertTrue(property.returnOriginalValue());
        //If property has not empty value and was overridden - return it
        property.setOriginalValue(new DescribableList(project, Arrays.asList(new Object())), true);
        assertTrue(property.returnOriginalValue());
    }

    /**
     * Verify {@link org.eclipse.hudson.model.project.property.CopyOnWriteListProjectProperty#allowOverrideValue(Object, Object)} method.
     */
    @Test
    public void testAllowOverrideValue() {
        //Don't need to override null values and equal lists
        assertFalse(property.allowOverrideValue(null, null));
        assertFalse(property.allowOverrideValue(new DescribableList(project), new DescribableList(project)));
        //Don't need to override Describable lists which has same Describable#data values, even if owners are not equal.
        assertFalse(property.allowOverrideValue(new DescribableList(project), new DescribableList(project)));
        assertFalse(property.allowOverrideValue(new DescribableList(project), new DescribableList(parent)));
        DescribableList describableList1 = new DescribableList(project,
            Arrays.asList(new Shell("echo 'test3'"), new Shell("echo 'test2'")));
        DescribableList describableList2 = new DescribableList(project,
            Arrays.asList(new Shell("echo 'test2'"), new Shell("echo 'test3'")));
        assertFalse(property.allowOverrideValue(describableList1, describableList2));

        DescribableList describableList3 = new DescribableList(parent, describableList2.toList());
        assertFalse(property.allowOverrideValue(describableList1, describableList3));

        describableList1 = new DescribableList(project, Arrays.asList(new Object()));
        describableList2 = new DescribableList(project, Arrays.asList(new Object()));
        assertFalse(property.allowOverrideValue(describableList1, describableList2));

        //Allow override if cascading or candidate are null
        assertTrue(property.allowOverrideValue(null, new DescribableList(project)));
        assertTrue(property.allowOverrideValue(new DescribableList(project), null));

        assertTrue(property.allowOverrideValue(new DescribableList(project),
            new DescribableList(project, Arrays.asList(new Shell("echo 'test1'")))));
        assertTrue(property.allowOverrideValue(new DescribableList(project, Arrays.asList(new Shell("echo 'test1'"))),
            new DescribableList(project)));
    }

}
