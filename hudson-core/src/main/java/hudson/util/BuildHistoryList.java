/*******************************************************************************
 *
 * Copyright (c) 2004-2013 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Roy Varghese
 *
 *
 *******************************************************************************/
package hudson.util;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import hudson.model.BuildHistory;
import hudson.model.BuildHistory.Record;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.View;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

/**
 * The equivalent of {@link RunList} that is based on {@link BuildHistory.Record}s
 * instead of the more heavy-weight {@link Run} object.
 * 
 * @author Roy Varghese
 */
public class BuildHistoryList<J extends Job<J,R>, R extends Run<J,R>> 
    extends AbstractRunList<BuildHistory.Record<J,R>> {

    private static class DateComparator<JobT extends Job<JobT,RunT>,
                                 RunT extends Run<JobT,RunT>> implements Comparator<Record<JobT,RunT>> {
        
        public int compare(Record<JobT,RunT> lhs, Record<JobT,RunT> rhs) {
            long lt = lhs.getTimeInMillis();
            long rt = rhs.getTimeInMillis();
            if (lt > rt) {
                return -1;
            }
            if (lt < rt) {
                return 1;
            }
            return 0;
        }
    };
    
    private BuildHistoryList(List<BuildHistory.Record<J,R>> records) {
        super(records);
    }
    
    /**
     * Creates a {@code BuildHistoryList} from a single Job.
     */
    public static <JobT extends Job<JobT,RunT>,RunT extends Run<JobT,RunT>> 
            BuildHistoryList<JobT,RunT> newBuildHistoryList(BuildHistory<JobT,RunT> history) {
      return new BuildHistoryList(history.allRecords());
    }
    
    /**
     * Create a {@code BuildHistoryList} for a collection of Jobs.
     */
    public static <JobT extends Job<JobT,RunT>,RunT extends Run<JobT,RunT>> 
            BuildHistoryList<JobT,RunT> newBuildHistoryList(Collection<JobT> jobs) {
      ArrayList<Record<JobT,RunT>> list = new ArrayList<Record<JobT,RunT>>();
      for (JobT job: jobs) {
          BuildHistory<JobT,RunT> bh = job.getBuildHistory();
          list.addAll(bh.allRecords());
      }
      Collections.sort(list, new DateComparator<JobT,RunT>());
      return new BuildHistoryList(list);
    }
    
    /**
     * Create a {@code BuildHistoryList} for a view.
     */
    public static BuildHistoryList newBuildHistoryList(View view) {
        ArrayList list = new ArrayList();
        for (Item item : view.getItems()) {
            for (Job<?, ?> j : item.getAllJobs()) {
                list.addAll(j.getBuildHistory().allRecords());
            }
        }
        Collections.sort(list, new DateComparator());
        return new BuildHistoryList(list);
          

    }

    @Override
    public BuildHistory.Record<J,R> getFirstBuild() {
        return size() > 0? get(0): null;
    }

    @Override
    public BuildHistory.Record<J,R> getLastBuild() {
        return size() > 0? get(size()-1): null;
    }

    /**
     * Filter the list to non-successful builds only.
     */
    public BuildHistoryList<J,R> failureOnly() {
        Iterator<BuildHistory.Record<J,R>> iter = new Iterators.FilterIterator<Record<J,R>>(this.iterator()) {

            @Override
            protected boolean filter(Record<J, R> record) {
                return !Result.SUCCESS.equals(record.getResult());
            }
            
        };
        
        return new BuildHistoryList<J,R>( ImmutableList.copyOf(iter));
    }

    /**
     * Filter the list to regression builds only.
     */
    @Override
    public BuildHistoryList<J,R> regressionOnly() {
        Iterator<BuildHistory.Record<J,R>> iter = new Iterators.FilterIterator<Record<J,R>>(this.iterator()) {

            @Override
            protected boolean filter(Record<J, R> record) {
                return record.getBuildStatusSummary().isWorse;
            }
            
        };
        
        return new BuildHistoryList<J,R>( ImmutableList.copyOf(iter));
    }

    /**
     * Filter the list by timestamp.
     *
     * {@code s&lt=;e}.
     */
    @Override
    public BuildHistoryList<J,R> byTimestamp(long start, long end) {
        
        Comparator<Long> DESCENDING_ORDER = new Comparator<Long>() {
            public int compare(Long o1, Long o2) {
                if (o1 > o2) {
                    return -1;
                }
                
                if (o1 < o2) {
                    return +1;
                }
                return 0;
            }
        };
        
        Function<Record<J,R>,Long> TRANSFORMER = new Function<Record<J,R>,Long>() {

            @Override
            public Long apply(Record<J, R> input) {
                return input.getTimeInMillis();
            }
            
        };

        int s = Collections.binarySearch(Lists.transform(this, TRANSFORMER), start, DESCENDING_ORDER);
        if (s < 0) {
            s = -(s + 1);   // min is inclusive
        }
        int e = Collections.binarySearch(Lists.transform(this, TRANSFORMER), end, DESCENDING_ORDER);
        if (e < 0) {
            e = -(e + 1);   
        }
        else {
            e++;// max is exclusive
        }
        
        return new BuildHistoryList<J,R>(subList(e,s));
    }

    /**
     * Reduce the size of the list by only leaving relatively new ones. This
     * also removes on-going builds, as RSS cannot be used to publish
     * information if it changes.
     */
    @Override
    public BuildHistoryList<J,R> newBuilds() {
        GregorianCalendar threshold = new GregorianCalendar();
        threshold.add(Calendar.DAY_OF_YEAR, -7);
        final long timeInMillis = threshold.getTimeInMillis();

        
        Iterator<Record<J,R>> iter = new Iterators.FilterIterator<Record<J,R>>(this.iterator()) {
            int count = 0;
            
            @Override
            protected boolean filter(Record<J, R> record) {
                boolean result = ( !record.isBuilding() && 
                                   (count < 10 || record.getTimeInMillis() > timeInMillis));
                if ( result ) {
                    count++;
                }
                return result;
                        
            }
        
        };
        
        return new BuildHistoryList<J,R>(Lists.newArrayList(iter));

    }

    @Override
    public BuildHistoryList<J,R> node(final Node node) {
        Iterator<Record<J,R>> iter = new Iterators.FilterIterator<Record<J,R>>(this.iterator()) {
            int count = 0;
            
            @Override
            protected boolean filter(Record<J, R> record) {
              String nodeName = record.getBuiltOnNodeName();
              
              return (nodeName == null && node == Hudson.getInstance()) ||
                      node.getNodeName().equals(nodeName);
            }
        
        };
        
        return new BuildHistoryList(Lists.newArrayList(iter));
    }
}
