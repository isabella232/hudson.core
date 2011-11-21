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
package hudson.matrix;

import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link hudson.matrix.MatrixProject}
 */
public class MatrixProjectTest {

    @Test
    public void testIsRunSequentiallyParentTrue() throws IOException {
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setRunSequentially(Boolean.TRUE);

        MatrixProject childProject1 = new MatrixProject("child1");
        childProject1.setTemplate(parentProject);
        childProject1.setAllowSave(false);
        assertTrue(childProject1.isRunSequentially());
    }

    @Test
    public void testIsRunSequentiallyParentFalse() throws IOException {
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setRunSequentially(Boolean.FALSE);

        MatrixProject childProject1 = new MatrixProject("child1");
        childProject1.setTemplate(parentProject);
        childProject1.setAllowSave(false);
        assertFalse(childProject1.isRunSequentially());
    }

    @Test
    public void testIsRunSequentiallyDefaultValue() throws IOException {
        MatrixProject childProject1 = new MatrixProject("child1");
        childProject1.setAllowSave(false);
        assertFalse(childProject1.isRunSequentially());
    }

    @Test
    public void testIsRunSequentiallyParentFalseChildTrue() throws IOException {
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setRunSequentially(Boolean.FALSE);

        MatrixProject childProject1 = new MatrixProject("child1");
        childProject1.setTemplate(parentProject);
        childProject1.runSequentially = Boolean.TRUE;
        childProject1.setAllowSave(false);
        assertTrue(childProject1.isRunSequentially());
    }

    @Test
    public void testIsRunSequentiallyParentTrueChildFalse() throws IOException {
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setRunSequentially(Boolean.TRUE);

        MatrixProject childProject1 = new MatrixProject("child1");
        childProject1.setTemplate(parentProject);
        childProject1.runSequentially = Boolean.FALSE;
        childProject1.setAllowSave(false);
        assertFalse(childProject1.isRunSequentially());
    }

    @Test
    public void testIsRunSequentiallyParentNullChildTrue() throws IOException {
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setRunSequentially(null);

        MatrixProject childProject1 = new MatrixProject("child1");
        childProject1.setTemplate(parentProject);
        childProject1.runSequentially = Boolean.TRUE;
        childProject1.setAllowSave(false);
        assertTrue(childProject1.isRunSequentially());
    }

    private class MatrixProjectMock extends MatrixProject {

        private MatrixProjectMock(String name) {
            super(null, name);
        }

        @Override
        protected void updateTransientActions() {
        }
    }
}
