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
 *   Nikita Levyankov
 *
 *
 *******************************************************************************/
package hudson.tasks;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import org.junit.Test;

/**
 * Verify equals and hashCode methods for Ant object.
 * <p/>
 * Date: 10/5/11
 *
 * @author Nikita Levyankov
 */
public class AntEqualsHashCodeTest {
    private Ant ant = new Ant("targets", "antName", "antOpts", "buildFile", "properties", false);

    @Test
    public void testHashCode() {
        assertEquals(ant.hashCode(), new Ant("targets", "antName", "antOpts", "buildFile", "properties", false).hashCode());
        assertFalse(ant.hashCode() == new Ant("targets", "antName", "antOpts", "buildFile", "properties1", false).hashCode());
        assertFalse(ant.hashCode() == new Ant("targets", "antName", "antOpts", "buildFile1", "properties", false).hashCode());
        assertFalse(ant.hashCode() == new Ant("targets", "antName", "antOpts1", "buildFile", "properties", false).hashCode());
        assertFalse(ant.hashCode() == new Ant("targets", "antName1", "antOpts", "buildFile", "properties", false).hashCode());
        assertFalse(ant.hashCode() == new Ant("targets1", "antName", "antOpts", "buildFile", "properties", false).hashCode());
        assertFalse(
            ant.hashCode() == new Ant("targets1", "antName1", "antOpts1", "buildFile1", "properties1", false).hashCode());
    }

    @Test
    public void testEqual() {
        assertEquals(ant, ant);
        assertFalse(ant.equals(null));
        assertFalse(ant.equals(new Object()));
        assertEquals(ant, new Ant("targets", "antName", "antOpts", "buildFile", "properties", false));
        assertFalse(ant.equals(new Ant("targets", "antName", "antOpts", "buildFile", "properties1", false)));
        assertFalse(ant.equals(new Ant("targets", "antName", "antOpts", "buildFile1", "properties", false)));
        assertFalse(ant.equals(new Ant("targets", "antName", "antOpts1", "buildFile", "properties", false)));
        assertFalse(ant.equals(new Ant("targets", "antName1", "antOpts", "buildFile", "properties", false)));
        assertFalse(ant.equals(new Ant("targets1", "antName", "antOpts", "buildFile", "properties", false)));
        assertFalse(ant.equals(new Ant("targets1", "antName1", "antOpts1", "buildFile1", "properties1", false)));
    }
}
