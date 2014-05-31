/**
 * *****************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 ******************************************************************************
 */
package hudson.model;

import hudson.model.RunMap.RunValue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for {@link Job}.
 */
public class SimpleJobTest extends TestCase {

    public void testGetEstimatedDuration() throws IOException {

        final SortedMap<Integer, TestBuild> runs = new TreeMap<Integer, TestBuild>();

        Job project = createMockProject(runs);

        TestBuild previousPreviousBuild = new TestBuild(project, Result.SUCCESS, 20, null);
        runs.put(3, previousPreviousBuild);

        TestBuild previousBuild = new TestBuild(project, Result.SUCCESS, 15, previousPreviousBuild);
        runs.put(2, previousBuild);

        TestBuild lastBuild = new TestBuild(project, Result.SUCCESS, 42, previousBuild);
        runs.put(1, lastBuild);

        // without assuming to know to much about the internal calculation
        // we can only assume that the result is between the maximum and the minimum
        Assert.assertTrue(project.getEstimatedDuration() < 42);
        Assert.assertTrue(project.getEstimatedDuration() > 15);
    }

    public void testGetEstimatedDurationWithOneRun() throws IOException {

        final SortedMap<Integer, TestBuild> runs = new TreeMap<Integer, TestBuild>();

        Job project = createMockProject(runs);

        TestBuild lastBuild = new TestBuild(project, Result.SUCCESS, 42, null);
        runs.put(1, lastBuild);

        Assert.assertEquals(42, project.getEstimatedDuration());
    }

    public void testGetEstimatedDurationWithFailedRun() throws IOException {

        final SortedMap<Integer, TestBuild> runs = new TreeMap<Integer, TestBuild>();

        Job project = createMockProject(runs);

        TestBuild lastBuild = new TestBuild(project, Result.FAILURE, 42, null);
        runs.put(1, lastBuild);

        Assert.assertEquals(-1, project.getEstimatedDuration());
    }

    public void testGetEstimatedDurationWithNoRuns() throws IOException {

        final SortedMap<Integer, TestBuild> runs = new TreeMap<Integer, TestBuild>();

        Job project = createMockProject(runs);

        Assert.assertEquals(-1, project.getEstimatedDuration());
    }

    public void testGetEstimatedDurationIfPrevious3BuildsFailed() throws IOException {

        final SortedMap<Integer, TestBuild> runs = new TreeMap<Integer, TestBuild>();

        Job project = createMockProject(runs);

        TestBuild prev4Build = new TestBuild(project, Result.SUCCESS, 1, null);
        runs.put(5, prev4Build);

        TestBuild prev3Build = new TestBuild(project, Result.SUCCESS, 1, prev4Build);
        runs.put(4, prev3Build);

        TestBuild previous2Build = new TestBuild(project, Result.FAILURE, 50, prev3Build);
        runs.put(3, previous2Build);

        TestBuild previousBuild = new TestBuild(project, Result.FAILURE, 50, previous2Build);
        runs.put(2, previousBuild);

        TestBuild lastBuild = new TestBuild(project, Result.FAILURE, 50, previousBuild);
        runs.put(1, lastBuild);

        // failed builds must not be used. Instead the last successful builds before them
        // must be used
        Assert.assertEquals(project.getEstimatedDuration(), 1);
    }

    private Job createMockProject(final SortedMap<Integer, TestBuild> runs) {
        Job project = new Job(null, "name") {

            int i = 1;

            @Override
            public int assignBuildNumber() throws IOException {
                return i++;
            }

            @Override
            public SortedMap<Integer, ? extends Run> _getRuns() {
                return runs;
            }

            @Override
            public boolean isBuildable() {
                return true;
            }

            @Override
            protected void removeRun(Run run) {
            }

            @Override
            public BuildHistory getBuildHistoryData() {
                return createMockBuildHistory(_getRuns());
            }
            
            public long getEstimatedDuration() {
                List<Run> builds = getLastBuildsOverThreshold(3, Result.UNSTABLE);

                if (builds.isEmpty()) {
                    return -1;
                }

                long totalDuration = 0;
                for (Run b : builds) {
                    totalDuration += b.getDuration();
                }
                if (totalDuration == 0) {
                    return -1;
                }

                return Math.round((double) totalDuration / builds.size());
            }

        };
        return project;
    }

    private BuildHistory createMockBuildHistory(final SortedMap<Integer, ? extends Run> runs) {
        return new BuildHistory() {

            @Override
            public BuildHistory.Record getFirst() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public BuildHistory.Record getLast() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public BuildHistory.Record getLastCompleted() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public BuildHistory.Record getLastFailed() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public BuildHistory.Record getLastStable() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public BuildHistory.Record getLastUnstable() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public BuildHistory.Record getLastSuccessful() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public BuildHistory.Record getLastUnsuccessful() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public List<Record> getLastRecordsOverThreshold(int n, Result threshold) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

            @Override
            public Run getLastBuild() {
                try {
                    return runs.get(runs.lastKey());
                }
                catch (NoSuchElementException e) {
                    return null;
                }
            }

            @Override
            public Run getFirstBuild() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Run getLastSuccessfulBuild() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Run getLastUnsuccessfulBuild() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Run getLastUnstableBuild() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Run getLastStableBuild() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Run getLastFailedBuild() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Run getLastCompletedBuild() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public List getLastBuildsOverThreshold(int n, Result threshold) {
                List<Run> result = new ArrayList<Run>(n);

                Run r = getLastBuild();
                while (r != null && result.size() < n) {

                    if (!r.isBuilding() && 
                        (r.getResult() != null && 
                         r.getResult().isBetterOrEqualTo(threshold))) {

                        result.add(r);
                    }
                    r = r.getPreviousBuild();
                }

                return result;
            }

            @Override
            public Iterator iterator() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public List allRecords() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        };
    }

    private static class TestBuild extends Run {

        public TestBuild(Job project, Result result, long duration, TestBuild previousBuild) throws IOException {
            super(project);
            setResult(result);
            this.duration = duration;
            this.previousBuild = previousBuild;
        }

        @Override
        public int compareTo(Run o) {
            return 0;
        }

        @Override
        public boolean isBuilding() {
            return false;
        }

        @Override
        public String toString() {
            return "TestBuild";
        }

    }
}
