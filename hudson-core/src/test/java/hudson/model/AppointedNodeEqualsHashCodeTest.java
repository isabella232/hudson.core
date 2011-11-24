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
 *    Anton Kozak
 *
 *******************************************************************************/
package hudson.model;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static junit.framework.Assert.assertEquals;

/**
 * Test for equals() and hashCode methods of {@link AppointedNode}.
 *
 * Date: 11/2/11
 */
@RunWith(Parameterized.class)
public class AppointedNodeEqualsHashCodeTest {

    private boolean expectedResult;
    private AppointedNode node1;
    private AppointedNode node2;

    public AppointedNodeEqualsHashCodeTest(boolean expectedResult, AppointedNode node1, AppointedNode node2) {
        this.expectedResult = expectedResult;
        this.node1 = node1;
        this.node2 = node2;
    }

    @Parameterized.Parameters
    public static Collection generateData() {
        return Arrays.asList(new Object[][] {
            {true, new AppointedNode("node", true), new AppointedNode("node", true)},
            {false, new AppointedNode("node", true), new AppointedNode("node2", true)},
            {false, new AppointedNode("node", true), new AppointedNode(null, true)},
            {false, new AppointedNode("node", false), new AppointedNode("node", true)},
            {false, new AppointedNode("node", true), new AppointedNode("node", null)},
            {true, new AppointedNode(null, false), new AppointedNode(null, false)},
            {true, new AppointedNode("node", null), new AppointedNode("node", null)}
        });
    }

    @Test
    public void testEquals() {
        assertEquals(expectedResult, node1.equals(node2));
    }

    @Test
    public void testHashCode() {
        assertEquals(expectedResult, node1.hashCode() == node2.hashCode());
    }
}
