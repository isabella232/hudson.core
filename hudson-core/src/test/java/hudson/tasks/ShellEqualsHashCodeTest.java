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
        assertEquals(new Shell(null).hashCode(), new Shell(null).hashCode());
        assertEquals(new Shell("").hashCode(), new Shell("").hashCode());
        assertEquals(new Shell("echo").hashCode(), new Shell("echo").hashCode());
        assertFalse(new Shell("echo 'test'").hashCode() == new Shell("echo '123'").hashCode());
        assertFalse(new Shell(null).hashCode() == new Shell("echo '123'").hashCode());
    }

    @Test
    public void testEqual() {
        Shell echo = new Shell("echo");
        assertEquals(echo, echo);
        assertFalse(new Shell("echo").equals(null));
        assertFalse(echo.equals(new Object()));
        assertEquals(echo, new Shell("echo"));
        assertEquals(new Shell(null), new Shell(null));
        assertEquals(new Shell(""), new Shell(""));
        assertFalse(new Shell("echo 'test'").equals(new Shell("echo '123'")));
        assertFalse(new Shell(null).equals(new Shell("echo '123'")));
    }
}
