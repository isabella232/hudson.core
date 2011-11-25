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

import antlr.ANTLRException;
import hudson.model.FreeStyleProjectMock;
import hudson.triggers.TimerTrigger;
import hudson.triggers.Trigger;
import org.eclipse.hudson.model.project.property.BaseProjectProperty;
import org.eclipse.hudson.model.project.property.TriggerProjectProperty;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Contains test-cases for {@link TriggerProjectProperty}.
 * <p/>
 * Date: 11/17/11
 *
 * @author Nikita Levyankov
 */
public class TriggerProjectPropertyTest {

    private TriggerProjectProperty property;

    @Before
    public void setUp() {
        final String propertyKey = "propertyKey";
        FreeStyleProjectMock project = new FreeStyleProjectMock("project");
        property = new TriggerProjectProperty(project);
        property.setKey(propertyKey);
    }

    @Test
    public void testConstructor() {
        try {
            new TriggerProjectProperty(null);
            fail("Null should be handled by TriggerProjectProperty constructor.");
        } catch (Exception e) {
            assertEquals(BaseProjectProperty.INVALID_JOB_EXCEPTION, e.getMessage());
        }
    }

    /**
     * Verify {@link org.eclipse.hudson.model.project.property.TriggerProjectProperty#clearOriginalValue(hudson.triggers.Trigger)} method.
     *
     * @throws antlr.ANTLRException if any
     */
    @Test
    public void testClearOriginalValue() throws ANTLRException {
        //Overridden flag should be cleared to false. Pre-set true value
        property.setOverridden(true);
        assertTrue(property.isOverridden());
        Trigger trigger = new TimerTrigger("* * * * *");
        property.clearOriginalValue(trigger);
        //Original value should be set with overridden flag == false
        assertFalse(property.isOverridden());
        assertTrue(trigger == property.getOriginalValue());
    }


    /**
     * Test updateOriginalValue method for TriggerProjectProperty.
     *
     * @throws antlr.ANTLRException if any
     */
    @Test
    public void testUpdateOriginalValue() throws ANTLRException {
        Trigger originalTrigger = new TimerTrigger("* * * * *");
        Trigger cascadingTrigger = new TimerTrigger("* * * * *");
        property.updateOriginalValue(originalTrigger, cascadingTrigger);
        //Property isn't overridden because of values equal.
        assertFalse(property.isOverridden());
        //If trigger property value equals to cascading be sure that sets original value instead of cascading.
        assertEquals(property.getOriginalValue(), originalTrigger);

        cascadingTrigger = new TimerTrigger("*/2 * * * *");
        property.updateOriginalValue(originalTrigger, cascadingTrigger);
        assertTrue(property.isOverridden());
        assertEquals(property.getOriginalValue(), originalTrigger);
    }

    /**
     * Verify {@link org.eclipse.hudson.model.project.property.TriggerProjectProperty#onCascadingProjectRemoved()} method.
     *
     * @throws antlr.ANTLRException if any
     */
    @Test
    public void testOnCascadingProjectRemoved() throws ANTLRException {
        Trigger trigger = new TimerTrigger("* * * * *");
        property.setOriginalValue(trigger, false);
        assertTrue(trigger == property.getOriginalValue());
        assertFalse(property.isOverridden());
        property.onCascadingProjectRemoved();
        assertFalse(property.isOverridden());
        assertTrue(trigger == property.getOriginalValue());
    }

}
