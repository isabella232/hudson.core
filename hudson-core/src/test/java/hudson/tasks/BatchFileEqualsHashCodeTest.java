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
 * Verify equals and hashCode methods for BatchFile object.
 * <p/>
 * Date: 10/5/11
 *
 * @author Nikita Levyankov
 */
public class BatchFileEqualsHashCodeTest {
    @Test
    public void testHashCode() {
        assertEquals(new BatchFile(null, false).hashCode(), new BatchFile(null, false).hashCode());
        assertEquals(new BatchFile("", false).hashCode(), new BatchFile("", false).hashCode());
        assertEquals(new BatchFile("echo", false).hashCode(), new BatchFile("echo", false).hashCode());
        assertFalse(new BatchFile("echo 'test'", false).hashCode() == new BatchFile("echo '123'", false).hashCode());
        assertFalse(new BatchFile(null, false).hashCode() == new BatchFile("echo '123'", false).hashCode());
    }

    @Test
    public void testEqual() {
        BatchFile echo = new BatchFile("echo", false);
        assertEquals(echo, echo);
        assertFalse(new BatchFile("echo", false).equals(null));
        assertFalse(echo.equals(new Object()));
        assertEquals(echo, new BatchFile("echo", false));
        assertEquals(new BatchFile(null, false), new BatchFile(null, false));
        assertEquals(new BatchFile("", false), new BatchFile("", false));
        assertFalse(new BatchFile("echo 'test'", false).equals(new BatchFile("echo '123'", false)));
        assertFalse(new BatchFile(null, false).equals(new BatchFile("echo '123'", false)));
    }
}