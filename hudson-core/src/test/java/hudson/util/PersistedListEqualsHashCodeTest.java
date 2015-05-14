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

package hudson.util;

import hudson.model.FreeStyleProjectMock;
import hudson.model.Saveable;
import hudson.tasks.Shell;
import java.io.IOException;
import java.util.Arrays;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

/**
 * Verify equals and hashCode methods for PersistedList object.
 * <p/>
 * Date: 10/5/11
 *
 * @author Nikita Levyankov
 */
public class PersistedListEqualsHashCodeTest {

    private Saveable owner1 = new FreeStyleProjectMock("test1");
    private Saveable owner2 = new FreeStyleProjectMock("test1");

    private PersistedList persistedList1;
    private PersistedList persistedList2;
    private PersistedList persistedList3;

    @Before
    public void setUp() {
        persistedList1 = new PersistedList(owner1);
        persistedList2 = new PersistedList(owner2);
        persistedList3 = new PersistedList(owner1);
    }

    @Test
    public void testHashCode() throws IOException {
        assertEquals(new PersistedList().hashCode(), new PersistedList().hashCode());
        assertEquals(persistedList1.hashCode(), new PersistedList(owner1).hashCode());
        assertFalse(persistedList1.hashCode() == persistedList2.hashCode());
        persistedList1.add(new Shell("echo 'test1'"));
        assertFalse(persistedList1.hashCode() == persistedList2.hashCode());
        persistedList2.add(new Shell("echo 'test1'"));
        assertFalse(persistedList1.hashCode() == persistedList2.hashCode());
        persistedList3.add(new Shell("echo 'test1'"));
        assertEquals(persistedList1.hashCode(), persistedList3.hashCode());
        persistedList3.replaceBy(Arrays.asList(new Shell("echo 'test2'")));
        assertFalse(persistedList1.hashCode() == persistedList3.hashCode());
    }

    @Test
    public void testEqual() throws IOException {
        assertEquals(persistedList1, persistedList1);
        assertFalse(persistedList1.equals(null));
        assertFalse(persistedList1.equals(new Object()));
        assertFalse(persistedList1.equals(persistedList2));
        assertEquals(persistedList1, persistedList3);
        persistedList1.add(new Shell("echo 'test1'"));
        persistedList3.add(new Shell("echo 'test1'"));
        assertEquals(persistedList1, persistedList3);
        persistedList1.add(new Shell("echo 'test3'"));
        persistedList1.add(new Shell("echo 'test2'"));
        persistedList3.add(new Shell("echo 'test2'"));
        persistedList3.add(new Shell("echo 'test3'"));
        assertEquals(persistedList1, persistedList3);

        persistedList3.replaceBy(Arrays.asList(new Shell("echo 'test2'")));
        assertFalse(persistedList1.equals(persistedList3));
    }
}
