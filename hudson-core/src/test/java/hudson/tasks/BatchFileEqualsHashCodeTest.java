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

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

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
        assertEquals(new BatchFile(null).hashCode(), new BatchFile(null).hashCode());
        assertEquals(new BatchFile("").hashCode(), new BatchFile("").hashCode());
        assertEquals(new BatchFile("echo").hashCode(), new BatchFile("echo").hashCode());
        assertFalse(new BatchFile("echo 'test'").hashCode() == new BatchFile("echo '123'").hashCode());
        assertFalse(new BatchFile(null).hashCode() == new BatchFile("echo '123'").hashCode());
    }

    @Test
    public void testEqual() {
        BatchFile echo = new BatchFile("echo");
        assertEquals(echo, echo);
        assertFalse(new BatchFile("echo").equals(null));
        assertFalse(echo.equals(new Object()));
        assertEquals(echo, new BatchFile("echo"));
        assertEquals(new BatchFile(null), new BatchFile(null));
        assertEquals(new BatchFile(""), new BatchFile(""));
        assertFalse(new BatchFile("echo 'test'").equals(new BatchFile("echo '123'")));
        assertFalse(new BatchFile(null).equals(new BatchFile("echo '123'")));
    }
}