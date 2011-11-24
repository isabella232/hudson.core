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
 * Equals and hashCode test for {@link BooleanParameterDefinition} object
 * <p/>
 * Date: 11/2/11
 *
 * @author Nikita Levyankov
 */
public class BooleanParameterDefinitionEqualsHashCodeTest {

    @Test
    public void testEquals() {
        BooleanParameterDefinition o1 = new BooleanParameterDefinition("name1", false, "description");
        assertEquals(o1, o1);
        assertFalse(o1.equals(null));
        assertFalse(o1.equals(new StringParameterDefinition("test1", "value", "description")));
        assertFalse(new BooleanParameterDefinition("name1", false, null).equals(
            new BooleanParameterDefinition(null, false, null)));
        assertFalse(
            new BooleanParameterDefinition(null, true, null).equals(new BooleanParameterDefinition(null, false, null)));
        assertFalse(o1.equals(new BooleanParameterDefinition(null, true, null)));
        assertFalse(o1.equals(new BooleanParameterDefinition("name1", true, "description")));

        assertEquals(o1, new BooleanParameterDefinition("name1", false, "description"));
        assertEquals(o1, new BooleanParameterDefinition("name1", false, "description1"));
        assertEquals(new BooleanParameterDefinition(null, false, "d1"),
            new BooleanParameterDefinition(null, false, "d1"));
        assertEquals(new BooleanParameterDefinition(null, false, null),
            new BooleanParameterDefinition(null, false, null));
    }

    @Test
    public void testHashCode() {
        BooleanParameterDefinition o1 = new BooleanParameterDefinition("name1", false, "description");
        assertEquals(o1, o1);
        assertFalse(o1.hashCode() == new StringParameterDefinition("test1", "value", "description").hashCode());
        assertFalse(new BooleanParameterDefinition("name1", false, null).hashCode() ==
            new BooleanParameterDefinition(null, false, null).hashCode());
        assertFalse(new BooleanParameterDefinition(null, true, null).hashCode() ==
            new BooleanParameterDefinition(null, false, null).hashCode());
        assertFalse(o1.hashCode() == new BooleanParameterDefinition(null, true, null).hashCode());
        assertFalse(o1.hashCode() == new BooleanParameterDefinition("name1", true, "description").hashCode());

        assertTrue(o1.hashCode() == new BooleanParameterDefinition("name1", false, "description").hashCode());
        assertTrue(o1.hashCode() == new BooleanParameterDefinition("name1", false, "description1").hashCode());
        assertTrue(new BooleanParameterDefinition(null, false, "d1").hashCode() ==
            new BooleanParameterDefinition(null, false, "d1").hashCode());
        assertTrue(new BooleanParameterDefinition(null, false, null).hashCode() ==
            new BooleanParameterDefinition(null, false, null).hashCode());
    }
}
