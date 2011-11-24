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
 * Equals and hashCode test for {@link ChoiceParameterDefinition} object
 * <p/>
 * Date: 11/2/11
 *
 * @author Nikita Levyankov
 */
public class ChoiceParameterDefinitionEqualsHashCodeTest {

    @Test
    public void testEquals() {
        ChoiceParameterDefinition o1 = new ChoiceParameterDefinition("name1", "value\nvalue2", "description");
        assertEquals(o1, o1);
        assertFalse(o1.equals(null));
        assertFalse(o1.equals(new StringParameterDefinition("test1", "value\nvalue2", "description")));
        assertFalse(new ChoiceParameterDefinition("name1", "value\nvalue2", null).equals(
            new ChoiceParameterDefinition(null, "value\nvalue2", null)));
        assertFalse(new ChoiceParameterDefinition(null, "value1", null).equals(
            new ChoiceParameterDefinition(null, "value\nvalue2", null)));
        assertFalse(o1.equals(new ChoiceParameterDefinition(null, "value1", null)));
        assertFalse(o1.equals(new ChoiceParameterDefinition("name1", "value1", "description")));

        assertEquals(o1, new ChoiceParameterDefinition("name1", "value\nvalue2", "description"));
        assertEquals(o1, new ChoiceParameterDefinition("name1", "value\nvalue2", "description1"));
        assertEquals(new ChoiceParameterDefinition(null, "value\nvalue2", "d1"),
            new ChoiceParameterDefinition(null, "value\nvalue2", "d1"));
        assertEquals(new ChoiceParameterDefinition(null, "value\nvalue2", null),
            new ChoiceParameterDefinition(null, "value\nvalue2", null));
    }

    @Test
    public void testHashCode() {
        ChoiceParameterDefinition o1 = new ChoiceParameterDefinition("name1", "value\nvalue2", "description");
        assertTrue(o1.hashCode() == o1.hashCode());
        assertFalse(o1.hashCode() == new StringParameterDefinition("test1", "value\nvalue2", "description").hashCode());
        assertFalse(new ChoiceParameterDefinition("name1", "value\nvalue2", null).hashCode() ==
            new ChoiceParameterDefinition(null, "value\nvalue2", null).hashCode());
        assertFalse(new ChoiceParameterDefinition(null, "value1", null).hashCode() ==
            new ChoiceParameterDefinition(null, "value\nvalue2", null).hashCode());
        assertFalse(o1.hashCode() == new ChoiceParameterDefinition(null, "value1", null).hashCode());
        assertFalse(o1.hashCode() == new ChoiceParameterDefinition("name1", "value1", "description").hashCode());

        assertTrue(o1.hashCode() == new ChoiceParameterDefinition("name1", "value\nvalue2", "description").hashCode());
        assertTrue(o1.hashCode() == new ChoiceParameterDefinition("name1", "value\nvalue2", "description1").hashCode());
        assertTrue(new ChoiceParameterDefinition(null, "value\nvalue2", "d1").hashCode() ==
            new ChoiceParameterDefinition(null, "value\nvalue2", "d1").hashCode());
        assertTrue(new ChoiceParameterDefinition(null, "value\nvalue2", null).hashCode() ==
            new ChoiceParameterDefinition(null, "value\nvalue2", null).hashCode());
    }
}
