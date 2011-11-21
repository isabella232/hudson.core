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
import org.junit.Test;

import static org.junit.Assert.*;

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

    @Test
    public void testGetCombinationFilterChildValue() throws IOException {
        String parentCombinationFilter = "parent_filter";
        String childCombinationFilter = "child_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setCombinationFilter(childCombinationFilter);
        childProject1.setTemplate(parentProject);
        assertEquals(childProject1.getCombinationFilter(), childCombinationFilter);
    }

    @Test
    public void testGetCombinationFilterParentValue() throws IOException {
        String parentCombinationFilter = "parent_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTemplate(parentProject);
        assertEquals(childProject1.getCombinationFilter(), parentCombinationFilter);
    }

    @Test
    public void testSetCombinationFilterDifferentValues() throws IOException {
        String parentCombinationFilter = "parent_filter";
        String childCombinationFilter = "child_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTemplate(parentProject);
        childProject1.setCombinationFilter(childCombinationFilter);
        assertEquals(childProject1.combinationFilter, childCombinationFilter);
    }

    @Test
    public void testSetCombinationFilterTheSameValues() throws IOException {
        String combinationFilter = "filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setCombinationFilter(combinationFilter);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTemplate(parentProject);
        childProject1.setCombinationFilter(combinationFilter);
        assertNull(childProject1.combinationFilter);
        assertEquals(childProject1.getCombinationFilter(), combinationFilter);
    }

    @Test
    public void testSetCombinationFilterParentNull() throws IOException {
        String combinationFilter = "filter";

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setCombinationFilter(combinationFilter);
        assertEquals(childProject1.combinationFilter, combinationFilter);
        assertEquals(childProject1.getCombinationFilter(), combinationFilter);
    }

    @Test
    public void testGetTouchStoneCombinationFilterChildValue() throws IOException {
        String parentCombinationFilter = "parent_filter";
        String childCombinationFilter = "child_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setTouchStoneCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTouchStoneCombinationFilter(childCombinationFilter);
        childProject1.setTemplate(parentProject);
        assertEquals(childProject1.getTouchStoneCombinationFilter(), childCombinationFilter);
    }

    @Test
    public void testGetTouchStoneCombinationFilterParentValue() throws IOException {
        String parentCombinationFilter = "parent_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setTouchStoneCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTemplate(parentProject);
        assertEquals(childProject1.getTouchStoneCombinationFilter(), parentCombinationFilter);
    }

    @Test
    public void testSetTouchStoneCombinationFilterDifferentValues() throws IOException {
        String parentCombinationFilter = "parent_filter";
        String childCombinationFilter = "child_filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setTouchStoneCombinationFilter(parentCombinationFilter);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTemplate(parentProject);
        childProject1.setTouchStoneCombinationFilter(childCombinationFilter);
        assertEquals(childProject1.touchStoneCombinationFilter, childCombinationFilter);
    }

    @Test
    public void testSetTouchStoneCombinationFilterTheSameValues() throws IOException {
        String combinationFilter = "filter";
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setTouchStoneCombinationFilter(combinationFilter);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTemplate(parentProject);
        childProject1.setTouchStoneCombinationFilter(combinationFilter);
        assertNull(childProject1.touchStoneCombinationFilter);
        assertEquals(childProject1.getTouchStoneCombinationFilter(), combinationFilter);
    }

    @Test
    public void testSetTouchStoneCombinationFilterParentNull() throws IOException {
        String combinationFilter = "filter";

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTouchStoneCombinationFilter(combinationFilter);
        assertEquals(childProject1.touchStoneCombinationFilter, combinationFilter);
        assertEquals(childProject1.getTouchStoneCombinationFilter(), combinationFilter);
    }
    //touchStoneResultCondition
    @Test
    public void testGetTouchStoneResultConditionChildValue() throws IOException {
        Result parentResultCondition = Result.SUCCESS;
        Result childResultCondition = Result.FAILURE;
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setTouchStoneResultCondition(parentResultCondition);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTouchStoneResultCondition(childResultCondition);
        childProject1.setTemplate(parentProject);
        assertEquals(childProject1.getTouchStoneResultCondition(), childResultCondition);
    }

    @Test
    public void testGetTouchStoneResultConditionParentValue() throws IOException {
        Result parentResultCondition = Result.SUCCESS;
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setTouchStoneResultCondition(parentResultCondition);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTemplate(parentProject);
        assertEquals(childProject1.getTouchStoneResultCondition(), parentResultCondition);
    }

    @Test
    public void testSetTouchStoneResultConditionDifferentValues() throws IOException {
        Result parentResultCondition = Result.SUCCESS;
        Result childResultCondition = Result.FAILURE;
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setTouchStoneResultCondition(parentResultCondition);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTemplate(parentProject);
        childProject1.setTouchStoneResultCondition(childResultCondition);
        assertEquals(childProject1.touchStoneResultCondition, childResultCondition);
    }

    @Test
    public void testSetTouchStoneResultConditionTheSameValues() throws IOException {
        Result parentResultCondition = Result.SUCCESS;
        MatrixProject parentProject = new MatrixProjectMock("parent");
        parentProject.setAllowSave(false);
        parentProject.setTouchStoneResultCondition(parentResultCondition);

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTemplate(parentProject);
        childProject1.setTouchStoneResultCondition(parentResultCondition);
        assertNull(childProject1.touchStoneResultCondition);
        assertEquals(childProject1.getTouchStoneResultCondition(), parentResultCondition);
    }

    @Test
    public void testSetTouchStoneResultConditionParentNull() throws IOException {
        Result childResultCondition = Result.FAILURE;

        MatrixProject childProject1 = new MatrixProject("child1"){
            @Override
            void rebuildConfigurations() throws IOException {
            }
        };
        childProject1.setAllowSave(false);
        childProject1.setTouchStoneResultCondition(childResultCondition);
        assertEquals(childProject1.touchStoneResultCondition, childResultCondition);
        assertEquals(childProject1.getTouchStoneResultCondition(), childResultCondition);
    }


    private class MatrixProjectMock extends MatrixProject {

        private MatrixProjectMock(String name) {
            super(null, name);
        }

        @Override
        protected void updateTransientActions() {
        }

        @Override
        void rebuildConfigurations() throws IOException {
        }
    }
}

