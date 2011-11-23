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

import hudson.tasks.Builder;
import hudson.tasks.Shell;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 * Verify equals and hashCode methods for CopyOnWriteList object.
 * <p/>
 * Date: 10/5/11
 *
 * @author Nikita Levyankov
 */
public class CopyOnWriteListEqualsHashCodeTest {
    private List<Builder> data1;
    private List<Builder> data2;

    @Before
    public void setUp() {
        data1 = new ArrayList<Builder>();
        data1.add(new Shell("echo 'test'"));
        data1.add(new Shell("echo 'test1'"));
        data1.add(new Shell("echo 'test2'"));

        data2 = new ArrayList<Builder>();
        data2.add(new Shell("echo 'test1'"));
        data2.add(new Shell("echo 'test'"));
    }

    @Test
    public void testHashCode() {
        assertEquals(new CopyOnWriteList(data1).hashCode(), new CopyOnWriteList(data1).hashCode());

        assertFalse(new CopyOnWriteList(data1).hashCode() == new CopyOnWriteList(data2).hashCode());
        data2.add(new Shell("echo 'test2'"));
        assertFalse(new CopyOnWriteList(data1).hashCode() == new CopyOnWriteList(data2).hashCode());
    }

    @Test
    public void testEqual() {
        CopyOnWriteList list = new CopyOnWriteList();
        assertEquals(list, list);
        assertFalse(list.equals(null));
        assertFalse(list.equals(new Object()));
        assertEquals(new CopyOnWriteList(new ArrayList()), new CopyOnWriteList(new ArrayList()));

        assertFalse(new CopyOnWriteList(data1).equals(new CopyOnWriteList(data2)));
        assertEquals(new CopyOnWriteList(data1), new CopyOnWriteList(data1));
        data2.add(new Shell("echo 'test2'"));
        assertEquals(new CopyOnWriteList(data1), new CopyOnWriteList(data2));
    }
}
