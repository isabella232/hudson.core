/*******************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: 
 *
 *    Tom Huybrechts, Yahoo!, Inc., Seiji Sogabe, Winston Prakash
 *     
 *
 *******************************************************************************/
package hudson.tasks.junit;

import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;
import hudson.util.ColorPalette;
import hudson.util.graph.ChartLabel;
import hudson.util.graph.DataSet;
import hudson.util.graph.Graph;

import hudson.util.graph.GraphSeries;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.Stapler;

/**
 * History of {@link hudson.tasks.test.TestObject} over time.
 *
 * @since 1.320
 */
public class History {

    private final TestObject testObject;

    public History(TestObject testObject) {
        this.testObject = testObject;
    }

    public TestObject getTestObject() {
        return testObject;
    }

    public boolean historyAvailable() {
        if (testObject.getOwner().getParent().getBuilds().size() > 1) {
            return true;
        } else {
            return false;
        }
    }

    public List<TestResult> getList(int start, int end) {
        List<TestResult> list = new ArrayList<TestResult>();
        end = Math.min(end, testObject.getOwner().getParent().getBuilds().size());
        for (AbstractBuild<?, ?> b : testObject.getOwner().getParent().getBuilds().subList(start, end)) {
            if (b.isBuilding()) {
                continue;
            }
            TestResult o = testObject.getResultInBuild(b);
            if (o != null) {
                list.add(o);
            }
        }
        return list;
    }

    public List<TestResult> getList() {
        return getList(0, testObject.getOwner().getParent().getBuilds().size());
    }

    /**
     * Graph of duration of tests over time.
     */
    public Graph getDurationGraph() {
        Graph graph = new Graph(-1, 600, 300);
        graph.setYAxisLabel("Mins");
        graph.setData(getDurationGraphDataSet());
        return graph;
    }

    private DataSet<String, HistoryChartLabel> getDurationGraphDataSet() {
        DataSet<String, HistoryChartLabel> data = new DataSet<String, HistoryChartLabel>();

        GraphSeries<String> xSeries = new GraphSeries<String>("Build No.");
        data.setXSeries(xSeries);

        GraphSeries<Number> ySeriesFailed = new GraphSeries<Number>(GraphSeries.TYPE_BAR, "Some Failed", ColorPalette.RED, true, false);
        //ySeriesFailed.setBaseURL(getRelPath(req));
        data.addYSeries(ySeriesFailed);

        GraphSeries<Number> ySeriesSkipped = new GraphSeries<Number>(GraphSeries.TYPE_BAR, "Some Skipped", ColorPalette.YELLOW, true, false);
        //ySeriesSkipped.setBaseURL(getRelPath(req));
        data.addYSeries(ySeriesSkipped);

        GraphSeries<Number> ySeriesPassed = new GraphSeries<Number>(GraphSeries.TYPE_BAR, "Passed", ColorPalette.BLUE, true, false);
        //ySeriesPassed.setBaseURL(getRelPath(req));
        data.addYSeries(ySeriesPassed);

        List<TestResult> list;
        try {
            list = getList(
                    Integer.parseInt(Stapler.getCurrentRequest().getParameter("start")),
                    Integer.parseInt(Stapler.getCurrentRequest().getParameter("end")));
        } catch (NumberFormatException e) {
            list = getList();
        }

        for (hudson.tasks.test.TestResult o : list) {
            xSeries.add(o.getOwner().getDisplayName());
            
            double duration = o.getDuration() / 60;
            if (o.getFailCount() > 0) {
                ySeriesFailed.add(duration);
                ySeriesSkipped.add(0.);
                ySeriesPassed.add(0.);
            } else if (o.getSkipCount() > 0) {
                ySeriesSkipped.add(duration);
                ySeriesFailed.add(0.);
                ySeriesPassed.add(0.);
            } else {
                ySeriesPassed.add(duration);
                ySeriesSkipped.add(0.);
                ySeriesFailed.add(0.);
            }

            // For backward compatibility with JFreechart
            data.add(duration, "", new HistoryChartLabel(o) {

                @Override
                public Color getColor(int row, int column) {
                    if (o.getFailCount() > 0) {
                        return ColorPalette.RED;
                    } else if (o.getSkipCount() > 0) {
                        return ColorPalette.YELLOW;
                    } else {
                        return ColorPalette.BLUE;
                    }
                }
            });
        }
        return data;
    }

    /**
     * Graph of # of tests over time.
     */
    public Graph getCountGraph() {
        Graph graph = new Graph(-1, 600, 300);
        graph.setXAxisLabel("");
        graph.setData(getCountGraphDataSet());
        return graph;
    }

    private DataSet<String, HistoryChartLabel> getCountGraphDataSet() {
        DataSet<String, HistoryChartLabel> data = new DataSet<String, HistoryChartLabel>();
        
        GraphSeries<String> xSeries = new GraphSeries<String>("Build No.");
        data.setXSeries(xSeries);
        
        GraphSeries<Number> ySeriesFailed = new GraphSeries<Number>(GraphSeries.TYPE_BAR, "Failed", ColorPalette.RED);
        //ySeriesFailed.setBaseURL(getRelPath(req)); 
        data.addYSeries(ySeriesFailed);
        
        GraphSeries<Number> ySeriesSkipped = new GraphSeries<Number>(GraphSeries.TYPE_BAR, "Skipped", ColorPalette.YELLOW);
        //ySeriesSkipped.setBaseURL(getRelPath(req));
        data.addYSeries(ySeriesSkipped);
        
        GraphSeries<Number> ySeriesPassed = new GraphSeries<Number>(GraphSeries.TYPE_BAR, "Passed", ColorPalette.BLUE);
        //ySeriesPassed.setBaseURL(getRelPath(req));
        data.addYSeries(ySeriesPassed);

        List<TestResult> list;
        try {
            list = getList(
                    Integer.parseInt(Stapler.getCurrentRequest().getParameter("start")),
                    Integer.parseInt(Stapler.getCurrentRequest().getParameter("end")));
        } catch (NumberFormatException e) {
            list = getList();
        }

        for (TestResult o : list) {
            xSeries.add(o.getOwner().getDisplayName());
            ySeriesFailed.add((double)o.getFailCount());
            ySeriesSkipped.add((double)o.getSkipCount());
            ySeriesPassed.add((double)(o.getTotalCount() - o.getFailCount() - o.getSkipCount()));
            
            // For backward compatibility with JFreechart
            data.add(o.getPassCount(), "2Passed", new HistoryChartLabel(o));
            data.add(o.getFailCount(), "1Failed", new HistoryChartLabel(o));
            data.add(o.getSkipCount(), "0Skipped", new HistoryChartLabel(o));
        }
        return data;
    }

    // For backward compatibility with JFreechart
    class HistoryChartLabel extends ChartLabel {

        TestResult o;
        String url;

        public HistoryChartLabel(TestResult o) {
            this.o = o;
            this.url = null;
        }

        private void generateUrl() {
            AbstractBuild<?, ?> build = o.getOwner();
            String buildLink = build.getUrl();
            String actionUrl = o.getTestResultAction().getUrlName();
            this.url = Hudson.getInstance().getRootUrl() + buildLink + actionUrl + o.getUrl();
        }

        public int compareTo(ChartLabel that) {
            return this.o.getOwner().number - ((HistoryChartLabel) that).o.getOwner().number;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof HistoryChartLabel)) {
                return false;
            }
            HistoryChartLabel that = (HistoryChartLabel) o;
            return this.o == that.o;
        }

        @Override
        public int hashCode() {
            return o.hashCode();
        }

        @Override
        public String toString() {
            String l = o.getOwner().getDisplayName();
            String s = o.getOwner().getBuiltOnStr();
            if (s != null) {
                l += ' ' + s;
            }
            return l;
//            return o.getDisplayName() + " " + o.getOwner().getDisplayName();
        }

        @Override
        public Color getColor(int row, int column) {
            return  ColorPalette.BLUE;
        }

        @Override
        public String getLink(int row, int column) {
            if (this.url == null) {
                generateUrl();
            }
            return url;
        }

        @Override
        public String getToolTip(int row, int column) {
            return o.getOwner().getDisplayName() + " : " + o.getDurationString();
        }
    }
}
