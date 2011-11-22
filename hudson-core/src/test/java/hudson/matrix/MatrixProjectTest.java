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

import hudson.model.Result;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link hudson.matrix.MatrixProject}
 */
public class MatrixProjectTest {

    @Test
    public void testIsRunSequentiallyParentTrue() throws IOException {
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setRunSequentially(true);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        assertTrue(childProject1.isRunSequentially());
    }

    @Test
    public void testIsRunSequentiallyParentFalse() throws IOException {
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setRunSequentially(false);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        assertFalse(childProject1.isRunSequentially());
    }

    @Test
    public void testIsRunSequentiallyDefaultValue() throws IOException {
        MatrixProject childProject1 = new MatrixProjectMock("child1");
        assertFalse(childProject1.isRunSequentially());
    }

    @Test
    public void testIsRunSequentiallyParentFalseChildTrue() throws IOException {
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setRunSequentially(false);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setRunSequentially(Boolean.TRUE);
        assertTrue(childProject1.isRunSequentially());
    }

    @Test
    public void testIsRunSequentiallyParentTrueChildFalse() throws IOException {
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setRunSequentially(Boolean.TRUE);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setRunSequentially(false);
        assertFalse(childProject1.isRunSequentially());
    }

    @Test
    public void testSetRunSequentially() throws IOException {
        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setRunSequentially(true);
        assertTrue(childProject1.isRunSequentially());
    }

    @Test
    public void testGetCombinationFilterChildValue() throws IOException {
        String parentCombinationFilter = "parent_filter";
        String childCombinationFilter = "child_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setCombinationFilter(childCombinationFilter);
        assertEquals(childCombinationFilter, childProject1.getCombinationFilter());
    }

    @Test
    public void testGetCombinationFilterParentValue() throws IOException {
        String parentCombinationFilter = "parent_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        assertEquals(childProject1.getCombinationFilter(), parentCombinationFilter);
    }

    @Test
    public void testSetCombinationFilterDifferentValues() throws IOException {
        String parentCombinationFilter = "parent_filter";
        String childCombinationFilter = "child_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setCombinationFilter(childCombinationFilter);
        assertEquals(childProject1.getCombinationFilter(), childCombinationFilter);
    }

    @Test
    public void testSetCombinationFilterTheSameValues() throws IOException {
        String combinationFilter = "filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setCombinationFilter(combinationFilter);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setCombinationFilter(combinationFilter);
        assertEquals(childProject1.getCombinationFilter(), combinationFilter);
    }

    @Test
    public void testSetCombinationFilterParentNull() throws IOException {
        String combinationFilter = "filter";

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCombinationFilter(combinationFilter);
        assertEquals(childProject1.getCombinationFilter(), combinationFilter);
    }

    @Test
    public void testSetCombinationFilterNull() throws IOException {
        MatrixProject childProject1 = new MatrixProjectMock("child1");
        assertNull(childProject1.getCombinationFilter());
    }

    @Test
    public void testGetTouchStoneCombinationFilterChildValue() throws IOException {
        String parentCombinationFilter = "parent_filter";
        String childCombinationFilter = "child_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setTouchStoneCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setTouchStoneCombinationFilter(childCombinationFilter);
        assertEquals(childProject1.getTouchStoneCombinationFilter(), childCombinationFilter);
    }

    @Test
    public void testGetTouchStoneCombinationFilterParentValue() throws IOException {
        String parentCombinationFilter = "parent_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setTouchStoneCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        assertEquals(childProject1.getTouchStoneCombinationFilter(), parentCombinationFilter);
    }

    @Test
    public void testGetTouchStoneCombinationNull() throws IOException {
        MatrixProject childProject1 = new MatrixProjectMock("child1");
        assertNull(childProject1.getTouchStoneCombinationFilter());
    }

    @Test
    public void testSetTouchStoneCombinationFilterDifferentValues() throws IOException {
        String parentCombinationFilter = "parent_filter";
        String childCombinationFilter = "child_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setTouchStoneCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setTouchStoneCombinationFilter(childCombinationFilter);
        assertEquals(childProject1.getTouchStoneCombinationFilter(), childCombinationFilter);
    }

    @Test
    public void testSetTouchStoneCombinationFilterTheSameValues() throws IOException {
        String combinationFilter = "filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setTouchStoneCombinationFilter(combinationFilter);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setTouchStoneCombinationFilter(combinationFilter);
        assertEquals(childProject1.getTouchStoneCombinationFilter(), combinationFilter);
    }

    @Test
    public void testSetTouchStoneCombinationFilterParentNull() throws IOException {
        String combinationFilter = "filter";

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setTouchStoneCombinationFilter(combinationFilter);
        assertEquals(childProject1.getTouchStoneCombinationFilter(), combinationFilter);
    }

    @Test
    public void testGetTouchStoneResultConditionChildValue() throws IOException {
        Result parentResultCondition = Result.SUCCESS;
        Result childResultCondition = Result.FAILURE;
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setTouchStoneResultCondition(parentResultCondition);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setTouchStoneResultCondition(childResultCondition);
        assertEquals(childProject1.getTouchStoneResultCondition(), childResultCondition);
    }

    @Test
    public void testGetCustomWorkspaceChildValue() throws IOException {
        String parentWorkspace = "/tmp";
        String childWorkspace = "/tmp2";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setCustomWorkspace(parentWorkspace);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setCustomWorkspace(childWorkspace);
        assertEquals(childProject1.getCustomWorkspace(), childWorkspace);
    }

    @Test
    public void testGetCustomWorkspaceParentValue() throws IOException {
        String parentWorkspace = "/tmp";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setCustomWorkspace(parentWorkspace);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        assertEquals(childProject1.getCustomWorkspace(), parentWorkspace);
    }

    @Test
    public void testGetCustomWorkspaceNull() throws IOException {
        MatrixProject childProject1 = new MatrixProjectMock("child1");
        assertNull(childProject1.getCustomWorkspace());
    }

    @Test
    public void testSetCustomWorkspaceDifferentValues() throws IOException {
        String parentWorkspace = "/tmp";
        String childWorkspace = "/tmp2";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setTouchStoneCombinationFilter(parentWorkspace);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setCustomWorkspace(childWorkspace);
        assertEquals(childProject1.getCustomWorkspace(), childWorkspace);
    }

    @Test
    public void testSetCustomWorkspaceTheSameValues() throws IOException {
        String parentWorkspace = "/tmp";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setCustomWorkspace(parentWorkspace);

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCascadingProject(parentProject);
        childProject1.setCustomWorkspace(parentWorkspace);
        assertEquals(childProject1.getCustomWorkspace(), parentWorkspace);
    }

    @Test
    public void testSetCustomWorkspaceParentNull() throws IOException {
        String parentWorkspace = "/tmp";

        MatrixProject childProject1 = new MatrixProjectMock("child1");
        childProject1.setCustomWorkspace(parentWorkspace);
        assertEquals(childProject1.getCustomWorkspace(), parentWorkspace);
    }

    private class MatrixProjectMock extends MatrixProject {

        private MatrixProjectMock(String name) {
            super(null, name);
            setAllowSave(false);
        }

        @Override
        protected void updateTransientActions() {
        }

        @Override
        void rebuildConfigurations() throws IOException {
        }
    }
}
