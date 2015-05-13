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
 * Verify equals and hashCode methods for Shell object.
 * <p/>
 * Date: 10/5/11
 *
 * @author Nikita Levyankov
 */
public class ShellEqualsHashCodeTest {

    @Test
    public void testHashCode() {
        assertEquals(new Shell(null, false).hashCode(), new Shell(null, false).hashCode());
        assertEquals(new Shell("", false).hashCode(), new Shell("", false).hashCode());
        assertEquals(new Shell("echo", false).hashCode(), new Shell("echo", false).hashCode());
        assertFalse(new Shell("echo 'test'", false).hashCode() == new Shell("echo '123'", false).hashCode());
        assertFalse(new Shell(null, false).hashCode() == new Shell("echo '123'", false).hashCode());
    }

    @Test
    public void testEqual() {
        Shell echo = new Shell("echo", false);
        assertEquals(echo, echo);
        assertFalse(new Shell("echo", false).equals(null));
        assertFalse(echo.equals(new Object()));
        assertEquals(echo, new Shell("echo", false));
        assertEquals(new Shell(null, false), new Shell(null, false));
        assertEquals(new Shell("", false), new Shell("", false));
        assertFalse(new Shell("echo 'test'", false).equals(new Shell("echo '123'", false)));
        assertFalse(new Shell(null, false).equals(new Shell("echo '123'", false)));
    }
}
