/*******************************************************************************
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *
 *
 *
 *******************************************************************************/ 

package hudson.model.queue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.model.JobProperty;
import hudson.model.Label;
import hudson.model.LoadBalancer;
import hudson.model.Node;
import hudson.model.Queue.BuildableItem;
import hudson.model.Queue.Executable;
import hudson.model.Queue.JobOffer;
import hudson.model.Queue.Task;
import hudson.model.labels.LabelAssignmentAction;
import java.io.IOException;


import static java.lang.Math.*;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.hudson.security.team.Team;
import org.eclipse.hudson.security.team.TeamManager;

/**
 * Defines a mapping problem for answering "where do we execute this task?"
 *
 * <p> The heart of the placement problem is a mapping problem. We are given a
 * {@link Task}, (which in the general case consists of a set of
 * {@link SubTask}s), and we are also given a number of idle {@link Executor}s,
 * and our goal is to find a mapping from the former to the latter, which
 * determines where each {@link SubTask} gets executed.
 *
 * <p> This mapping is done under two constraints:
 *
 * <ul> <li> "Same node" constraint. Some of the subtasks need to be co-located
 * on the same node. See {@link SubTask#getSameNodeConstraint()} <li> Label
 * constraint. {@link SubTask}s can specify that it can be only run on nodes
 * that has the label. </ul>
 *
 * <p> We first fold the former constraint into the problem definition. That is,
 * we now consider a set of {@link SubTask}s that need to be co-located as a
 * single {@link WorkChunk}. Similarly, we consider a set of all
 * {@link Executor}s from the same node as {@link ExecutorChunk}. Now, the
 * problem becomes the weighted matching problem from {@link WorkChunk} to
 * {@link ExecutorChunk}.
 *
 * <p> An instance of {@link MappingWorksheet} captures a problem definition,
 * plus which {@link ExecutorChunk} and {@link WorkChunk} are compatible. The
 * purpose of this class (and {@link ExecutorChunk} and {@link WorkChunk}) are
 * to expose a lot of convenience methods to assist various algorithms that
 * produce the solution of this mapping problem, which is represented as
 * {@link Mapping}.
 *
 * @see LoadBalancer#map(Task, MappingWorksheet)
 * @author Kohsuke Kawaguchi
 */
public class MappingWorksheet {
    //TODO: review and check whether we can do it private

    public final List<ExecutorChunk> executors;
    public final List<WorkChunk> works;
    /**
     * {@link BuildableItem} for which we are trying to figure out the execution
     * plan. Never null.
     */
    public final BuildableItem item;

    public List<ExecutorChunk> getExecutors() {
        return executors;
    }

    public List<WorkChunk> getWorks() {
        return works;
    }

    public BuildableItem getItem() {
        return item;
    }

    private static class ReadOnlyList<E> extends AbstractList<E> {

        protected final List<E> base;

        ReadOnlyList(List<E> base) {
            this.base = base;
        }

        public E get(int index) {
            return base.get(index);
        }

        public int size() {
            return base.size();
        }
    }

    public final class ExecutorChunk extends ReadOnlyList<ExecutorSlot> {
        //TODO: review and check whether we can do it private

        public final int index;
        public final Computer computer;
        public final Node node;

        private ExecutorChunk(List<ExecutorSlot> base, int index) {
            super(base);
            this.index = index;
            assert !base.isEmpty();
            computer = base.get(0).getExecutor().getOwner();
            node = computer.getNode();
        }

        public int getIndex() {
            return index;
        }

        public Computer getComputer() {
            return computer;
        }

        public Node getNode() {
            return node;
        }

        /**
         * Is this executor chunk and the given work chunk compatible? Can the
         * latter be run on the former?
         */
        public boolean canAccept(WorkChunk c) {
            return this.size() >= c.size()
                    && (c.assignedLabel == null || c.assignedLabel.contains(node));
        }

        /**
         * Node name.
         */
        public String getName() {
            return node.getNodeName();
        }

        /**
         * Number of executors in this chunk. Alias for size but more readable.
         */
        public int capacity() {
            return size();
        }

        private void execute(WorkChunk wc, WorkUnitContext wuc) {
            assert capacity() >= wc.size();
            int e = 0;
            for (SubTask s : wc) {
                while (!get(e).isAvailable()) {
                    e++;
                }
                get(e++).set(wuc.createWorkUnit(s));
            }
        }
    }

    public class WorkChunk extends ReadOnlyList<SubTask> {

        public final int index;
        // the main should be always at position 0
//        /**
//         * This chunk includes {@linkplain WorkUnit#isMainWork() the main work unit}.
//         */
//        public final boolean isMain;
        /**
         * If this task needs to be run on a node with a particular label,
         * return that {@link Label}. Otherwise null, indicating it can run on
         * anywhere.
         */
        public final Label assignedLabel;
        /**
         * If the previous execution of this task run on a certain node and this
         * task prefers to run on the same node, return that. Otherwise null.
         */
        public final ExecutorChunk lastBuiltOn;

        private WorkChunk(List<SubTask> base, int index) {
            super(base);
            assert !base.isEmpty();
            this.index = index;
            this.assignedLabel = getAssignedLabel(base.get(0));

            Node lbo = base.get(0).getLastBuiltOn();
            for (ExecutorChunk ec : executors) {
                if (ec.node == lbo) {
                    lastBuiltOn = ec;
                    return;
                }
            }
            lastBuiltOn = null;
        }
        
        private Label getAssignedLabel(SubTask task) {
            for (LabelAssignmentAction laa : item.getActions(LabelAssignmentAction.class)) {
                Label l = laa.getAssignedLabel(task);
                if (l!=null)    return l;
            }
            return task.getAssignedLabel();
        }

        public List<ExecutorChunk> applicableExecutorChunks() {
            List<ExecutorChunk> r = new ArrayList<ExecutorChunk>(executors.size());
            for (ExecutorChunk e : executors) {
                if (e.canAccept(this)) {
                    r.add(e);
                }
            }
            return r;
        }
    }

    /**
     * Represents the solution to the mapping problem. It's a mapping from every
     * {@link WorkChunk} to {@link ExecutorChunk} that satisfies the
     * constraints.
     */
    public final class Mapping {
        // for each WorkChunk, identify ExecutorChunk where it is assigned to.

        private final ExecutorChunk[] mapping = new ExecutorChunk[works.size()];

        /**
         * {@link ExecutorChunk} assigned to the n-th work chunk.
         */
        public ExecutorChunk assigned(int n) {
            return mapping[n];
        }

        /**
         * n-th {@link WorkChunk}.
         */
        public WorkChunk get(int n) {
            return works.get(n);
        }

        /**
         * Update the mapping to execute n-th {@link WorkChunk} on the specified
         * {@link ExecutorChunk}.
         */
        public ExecutorChunk assign(int index, ExecutorChunk element) {
            ExecutorChunk o = mapping[index];
            mapping[index] = element;
            return o;
        }

        /**
         * Number of {@link WorkUnit}s that require assignments.
         */
        public int size() {
            return mapping.length;
        }

        /**
         * Returns the assignment as a map.
         */
        public Map<WorkChunk, ExecutorChunk> toMap() {
            Map<WorkChunk, ExecutorChunk> r = new HashMap<WorkChunk, ExecutorChunk>();
            for (int i = 0; i < size(); i++) {
                r.put(get(i), assigned(i));
            }
            return r;
        }

        /**
         * Checks if the assignments made thus far are valid an within the
         * constraints.
         */
        public boolean isPartiallyValid() {
            int[] used = new int[executors.size()];
            for (int i = 0; i < mapping.length; i++) {
                ExecutorChunk ec = mapping[i];
                if (ec == null) {
                    continue;
                }
                if (!ec.canAccept(works(i))) {
                    return false;   // invalid assignment
                }
                if ((used[ec.index] += works(i).size()) > ec.capacity()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Makes sure that all the assignments are made and it is within the
         * constraints.
         */
        public boolean isCompletelyValid() {
            for (ExecutorChunk ec : mapping) {
                if (ec == null) {
                    return false;   // unassigned
                }
            }
            return isPartiallyValid();
        }

        /**
         * Executes this mapping by handing over {@link Executable}s to
         * {@link JobOffer} as defined by the mapping.
         */
        public void execute(WorkUnitContext wuc) {
            if (!isCompletelyValid()) {
                throw new IllegalStateException();
            }

            for (int i = 0; i < size(); i++) {
                assigned(i).execute(get(i), wuc);
            }
        }
    }

    public MappingWorksheet(BuildableItem item, List<? extends ExecutorSlot> offers) {
        this(item, offers, LoadPredictor.all());
    }

    public MappingWorksheet(BuildableItem item, List<? extends ExecutorSlot> offers, Collection<? extends LoadPredictor> loadPredictors) {
        this.item = item;

        // group executors by their computers
        Map<Computer, List<ExecutorSlot>> j = new HashMap<Computer, List<ExecutorSlot>>();
        for (ExecutorSlot o : offers) {
            Computer c = o.getExecutor().getOwner();
            boolean canBuildInNode = true;
            TeamManager teamManager = Hudson.getInstance().getTeamManager();
            if (teamManager.isTeamManagementEnabled()) {
                String name = c.getName();
                if (c instanceof Hudson.MasterComputer) {
                    name = "Master";
                }
                String jobName = item.task.getName();
                //All jobs must be part of a team, if not this task item may be a sub-job.
                //Try to find the parent job that belongs to the team
                Team jobTeam = teamManager.findJobOwnerTeam(jobName);
                if (jobTeam == null) {
                    AbstractProject jobTask = null;
                    if (item.task instanceof AbstractProject) {
                        jobTask = (AbstractProject) item.task;
                    }
                    if (jobTask != null) {
                        do {
                            // Check if the parent of this sub-job is a team job
                            if (jobTask.getParent() instanceof AbstractProject) {
                                jobTask = (AbstractProject) jobTask.getParent();
                                jobName = jobTask.getName();
                                jobTeam = teamManager.findJobOwnerTeam(jobName);
                            } else if (jobTask.getParent() instanceof ItemGroup) {
                                ItemGroup itemGroup = (ItemGroup) jobTask.getParent();
                                // This item group may be added as Job property to parent
                                if (itemGroup instanceof JobProperty) {
                                    JobProperty property = (JobProperty) itemGroup;
                                    if (property.getOwner() instanceof AbstractProject) {
                                        jobTask = (AbstractProject) property.getOwner();
                                        jobName = jobTask.getName();
                                        jobTeam = teamManager.findJobOwnerTeam(jobName);
                                    }
                                }
                            } else {
                                break;
                            }
                        } while (jobTeam == null);
                    }
                }
                // If job team is still null do a last ditch test
                if (jobTeam == null) {
                    if (item.task instanceof MatrixConfiguration) {
                        MatrixConfiguration matrixConfiguration = (MatrixConfiguration) item.task;
                        jobName = matrixConfiguration.getParent().getName();
                    }
                }
                canBuildInNode = teamManager.canNodeExecuteJob(name, jobName);
            }
            List<ExecutorSlot> l = j.get(c);
            if (l == null) {
                j.put(c, l = new ArrayList<ExecutorSlot>());
            }
            if (canBuildInNode) {
                l.add(o);
            }
        }

        {// take load prediction into account and reduce the available executor pool size accordingly
            long duration = item.task.getEstimatedDuration();
            if (duration > 0) {
                long now = System.currentTimeMillis();
                for (Entry<Computer, List<ExecutorSlot>> e : j.entrySet()) {
                    final List<ExecutorSlot> list = e.getValue();
                    final int max = e.getKey().countExecutors();

                    // build up the prediction model. cut the chase if we hit the max.
                    Timeline timeline = new Timeline();
                    int peak = 0;
                    OUTER:
                    for (LoadPredictor lp : loadPredictors) {
                        for (FutureLoad fl : Iterables.limit(lp.predict(this, e.getKey(), now, now + duration), 100)) {
                            peak = max(peak, timeline.insert(fl.startTime, fl.startTime + fl.duration, fl.numExecutors));
                            if (peak >= max) {
                                break OUTER;
                            }
                        }
                    }

                    int minIdle = max - peak; // minimum number of idle nodes during this time period
                    if (minIdle < list.size()) {
                        e.setValue(list.subList(0, minIdle));
                    }
                }
            }
        }

        // build into the final shape
        List<ExecutorChunk> executors = new ArrayList<ExecutorChunk>();
        for (List<ExecutorSlot> group : j.values()) {
            if (group.isEmpty()) {
                continue;   // evict empty group
            }
            ExecutorChunk ec = new ExecutorChunk(group, executors.size());
            if (ec.node == null) {
                continue;   // evict out of sync node
            }
            executors.add(ec);
        }
        this.executors = ImmutableList.copyOf(executors);

        // group execution units into chunks. use of LinkedHashMap ensures that the main work comes at the top
        Map<Object, List<SubTask>> m = new LinkedHashMap<Object, List<SubTask>>();
        for (SubTask meu : Tasks.getSubTasksOf(item.task)) {
            Object c = Tasks.getSameNodeConstraintOf(meu);
            if (c == null) {
                c = new Object();
            }

            List<SubTask> l = m.get(c);
            if (l == null) {
                m.put(c, l = new ArrayList<SubTask>());
            }
            l.add(meu);
        }

        // build into the final shape
        List<WorkChunk> works = new ArrayList<WorkChunk>();
        for (List<SubTask> group : m.values()) {
            works.add(new WorkChunk(group, works.size()));
        }
        this.works = ImmutableList.copyOf(works);
    }

    public WorkChunk works(int index) {
        return works.get(index);
    }

    public ExecutorChunk executors(int index) {
        return executors.get(index);
    }

    public static abstract class ExecutorSlot {

        public abstract Executor getExecutor();

        public abstract boolean isAvailable();

        protected abstract void set(WorkUnit p) throws UnsupportedOperationException;
    }
}
