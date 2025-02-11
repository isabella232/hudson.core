/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 
 *    Kohsuke Kawaguchi
 *
 *
 *******************************************************************************/ 

package hudson.model;

import hudson.FilePath;
import hudson.Util;
import hudson.Extension;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.eclipse.hudson.security.team.TeamManager;

/**
 * Clean up old left-over workspaces from slaves.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class WorkspaceCleanupThread extends AsyncPeriodicWork {

    private static WorkspaceCleanupThread theInstance;

    public WorkspaceCleanupThread() {
        super("Workspace clean-up");
        theInstance = this;
    }

    public long getRecurrencePeriod() {
        return DAY;
    }

    public static void invoke() {
        theInstance.run();
    }
    // so that this can be easily accessed from sub-routine.
    private TaskListener listener;

    protected void execute(TaskListener listener) throws InterruptedException, IOException {
        try {
            if (disabled) {
                LOGGER.fine("Disabled. Skipping execution");
                return;
            }

            this.listener = listener;

            Hudson h = Hudson.getInstance();
            for (Node n : h.getNodes()) {
                if (n instanceof Slave) {
                    process((Slave) n);
                }
            }

            process(h);
        } finally {
            this.listener = null;
        }
    }

    private void process(Hudson h) throws IOException, InterruptedException {
        File[] jobsRootDirs = Hudson.getInstance().getTeamManager().getJobsRootFolders();
        if (jobsRootDirs == null) {
            return;
        }
        for (File dir : jobsRootDirs) {
            FilePath ws = new FilePath(new File(dir, "workspace"));
            if (shouldBeDeleted(dir.getName(), ws, h)) {
                delete(ws);
            }
        }
    }

    private boolean shouldBeDeleted(String jobName, FilePath dir, Node n) throws IOException, InterruptedException {
        // TODO: the use of remoting is not optimal.
        // One remoting can execute "exists", "lastModified", and "delete" all at once.
        TopLevelItem item = Hudson.getInstance().getItem(jobName);
        if (item == null) {
            //bug fix https://bugs.eclipse.org/bugs/show_bug.cgi?id=434000
            // Workspace directories for concurrent jobs removed before build completion
            // Since _<number> is added to job name, it has to be stripped before checking
            int index = jobName.lastIndexOf("_");
            if (index > 0) {
                jobName = jobName.substring(0, index);
                item = Hudson.getInstance().getItem(jobName);
                if (item == null) {
                    // no such project anymore
                    LOGGER.fine("Directory " + dir + " is not owned by any project");
                    return true;
                }
            } else {
                // no such project anymore
                LOGGER.fine("Directory " + dir + " is not owned by any project");
                return true;
            }
        }

        if (!dir.exists()) {
            return false;
        }

        // if younger than a month, keep it
        long now = new Date().getTime();
        if (dir.lastModified() + 30 * DAY > now) {
            LOGGER.fine("Directory " + dir + " is only " + Util.getTimeSpanString(now - dir.lastModified()) + " old, so not deleting");
            return false;
        }

        if (item instanceof AbstractProject) {
            AbstractProject p = (AbstractProject) item;
            Node lb = p.getLastBuiltOn();
            LOGGER.finer("Directory " + dir + " is last built on " + lb);
            if (lb != null && lb.equals(n)) {
                // this is the active workspace. keep it.
                LOGGER.fine("Directory " + dir + " is the last workspace for " + p);
                return false;
            }

            if (!p.getScm().processWorkspaceBeforeDeletion(p, dir, n)) {
                LOGGER.fine("Directory deletion of " + dir + " is vetoed by SCM");
                return false;
            }
        }

        LOGGER.finer("Going to delete directory " + dir);
        return true;
    }

    private void process(Slave s) throws InterruptedException {
        listener.getLogger().println("Scanning " + s.getNodeName());

        try {
            FilePath path = s.getWorkspaceRoot();
            if (path == null) {
                return;
            }

            List<FilePath> dirs = path.list(DIR_FILTER);
            if (dirs == null) {
                return;
            }
            for (FilePath dir : dirs) {
                if (shouldBeDeleted(dir.getName(), dir, s)) {
                    delete(dir);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed on " + s.getNodeName()));
        }
    }

    private void delete(FilePath dir) throws InterruptedException {
        try {
            listener.getLogger().println("Deleting " + dir);
            dir.deleteRecursive();
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed to delete " + dir));
        }
    }

    private static class DirectoryFilter implements FileFilter, Serializable {

        public boolean accept(File f) {
            return f.isDirectory();
        }
        private static final long serialVersionUID = 1L;
    }
    private static final FileFilter DIR_FILTER = new DirectoryFilter();
    private static final long DAY = 1000 * 60 * 60 * 24;
    private static final Logger LOGGER = Logger.getLogger(WorkspaceCleanupThread.class.getName());
    /**
     * Can be used to disable workspace clean up.
     */
    public static boolean disabled = Boolean.getBoolean(WorkspaceCleanupThread.class.getName() + ".disabled");
}
