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
 *
 *******************************************************************************/
package hudson.model;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Equals and hashCode test for {@link RunParameterDefinition} object
 * <p/>
 * Date: 11/2/11
 *
 * @author Nikita Levyankov
 */
public class RunParameterDefinitionEqualsHashCodeTest {

    @Test
    public void testEquals() {
        RunParameterDefinition o1 = new RunParameterDefinition("name1", "value", "description");
        assertEquals(o1, o1);
        assertFalse(o1.equals(null));
        assertFalse(o1.equals(new StringParameterDefinition("test1", "value", "description")));
        assertFalse(new RunParameterDefinition("name1", "value", null).equals(
            new RunParameterDefinition(null, "value", null)));
        assertFalse(new RunParameterDefinition(null, "value1", null).equals(
            new RunParameterDefinition(null, "value", null)));
        assertFalse(o1.equals(new RunParameterDefinition(null, "value1", null)));
        assertFalse(o1.equals(new RunParameterDefinition("name1", "value1", "description")));

        assertEquals(o1, new RunParameterDefinition("name1", "value", "description"));
        assertEquals(o1, new RunParameterDefinition("name1", "value", "description1"));
        assertEquals(new RunParameterDefinition(null, "value", "d1"),
            new RunParameterDefinition(null, "value", "d1"));
        assertEquals(new RunParameterDefinition(null, "value", null),
            new RunParameterDefinition(null, "value", null));
    }

    @Test
    public void testHashCode() {
        RunParameterDefinition o1 = new RunParameterDefinition("name1", "value", "description");
        assertTrue(o1.hashCode() == o1.hashCode());
        assertFalse(o1.equals(new StringParameterDefinition("test1", "value", "description")));
        assertFalse(new RunParameterDefinition("name1", "value", null).hashCode() ==
            new RunParameterDefinition(null, "value", null).hashCode());
        assertFalse(new RunParameterDefinition(null, "value1", null).hashCode() ==
            new RunParameterDefinition(null, "value", null).hashCode());
        assertFalse(o1.hashCode() == new RunParameterDefinition(null, "value1", null).hashCode());
        assertFalse(o1.hashCode() == new RunParameterDefinition("name1", "value1", "description").hashCode());

        assertTrue(o1.hashCode() == new RunParameterDefinition("name1", "value", "description").hashCode());
        assertTrue(o1.hashCode() == new RunParameterDefinition("name1", "value", "description1").hashCode());
        assertTrue(new RunParameterDefinition(null, "value", "d1").hashCode() ==
            new RunParameterDefinition(null, "value", "d1").hashCode());
        assertTrue(new RunParameterDefinition(null, "value", null).hashCode() ==
            new RunParameterDefinition(null, "value", null).hashCode());
    }
}
