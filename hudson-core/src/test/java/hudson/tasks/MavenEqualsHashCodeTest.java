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
 * Verify equals and hashCode methods for Maven object.
 * <p/>
 * Date: 10/5/11
 *
 * @author Nikita Levyankov
 */
public class MavenEqualsHashCodeTest {

    private Maven maven = new Maven("targets", "name", "pom", "properties", "jvmOptions", false, false);

    @Test
    public void testHashCode() {
        assertEquals(maven.hashCode(),
            new Maven("targets", "name", "pom", "properties", "jvmOptions", false, false).hashCode());
        assertFalse(
            maven.hashCode() == new Maven("targets", "name", "pom", "properties", "jvmOptions", true, false).hashCode());
        assertFalse(
            maven.hashCode() == new Maven("targets", "name", "pom", "properties", "jvmOptions1", false, false).hashCode());
        assertFalse(
            maven.hashCode() == new Maven("targets", "name", "pom", "properties1", "jvmOptions", false, false).hashCode());
        assertFalse(
            maven.hashCode() == new Maven("targets", "name", "pom1", "properties", "jvmOptions", false, false).hashCode());
        assertFalse(
            maven.hashCode() == new Maven("targets", "name1", "pom", "properties", "jvmOptions", false, false).hashCode());
        assertFalse(
            maven.hashCode() == new Maven("targets1", "name", "pom", "properties", "jvmOptions", false, false).hashCode());
    }

    @Test
    public void testEqual() {
        assertEquals(maven, maven);
        assertFalse(maven.equals(null));
        assertFalse(maven.equals(new Object()));
        assertEquals(maven, new Maven("targets", "name", "pom", "properties", "jvmOptions", false, false));
        assertFalse(maven.equals(new Maven("targets", "name", "pom", "properties", "jvmOptions", true, false)));
        assertFalse(maven.equals(new Maven("targets", "name", "pom", "properties", "jvmOptions1", false, false)));
        assertFalse(maven.equals(new Maven("targets", "name", "pom1", "properties", "jvmOptions", false, false)));
        assertFalse(maven.equals(new Maven("targets", "name1", "pom", "properties", "jvmOptions", false, false)));
        assertFalse(maven.equals(new Maven("targets1", "name", "pom", "properties", "jvmOptions", false, false)));
        assertFalse(maven.equals(new Maven("targets1", "name1", "pom1", "properties1", "jvmOptions1", true, false)));
    }
}
