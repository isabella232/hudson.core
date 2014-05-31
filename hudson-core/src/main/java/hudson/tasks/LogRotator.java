/**
 * *****************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi, Martin Eigenbrodt
 *
 *
 ******************************************************************************
 */
package hudson.tasks;

import hudson.model.BuildHistory;
import hudson.model.BuildHistory.Record;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.scm.SCM;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Deletes old log files.
 *
 * TODO: is there any other task that follows the same pattern? try to
 * generalize this just like {@link SCM} or {@link BuildStep}.
 *
 * @author Kohsuke Kawaguchi
 */
public class LogRotator implements Describable<LogRotator> {

    private static final Logger LOGGER = Logger.getLogger(LogRotator.class.getName());
    /**
     * If not -1, history is only kept up to this days.
     */
    private final int daysToKeep;
    /**
     * If not -1, only this number of build logs are kept.
     */
    private final int numToKeep;
    /**
     * If not -1 nor null, artifacts are only kept up to this days. Null
     * handling is necessary to remain data compatible with old versions.
     *
     * @since 1.350
     */
    private final Integer artifactDaysToKeep;
    /**
     * If not -1 nor null, only this number of builds have their artifacts kept.
     * Null handling is necessary to remain data compatible with old versions.
     *
     * @since 1.350
     */
    private final Integer artifactNumToKeep;

    @DataBoundConstructor
    public LogRotator(String logrotate_days, String logrotate_nums, String logrotate_artifact_days, String logrotate_artifact_nums) {
        this(parse(logrotate_days), parse(logrotate_nums),
                parse(logrotate_artifact_days), parse(logrotate_artifact_nums));
    }

    public static int parse(String p) {
        if (p == null) {
            return -1;
        }
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * @param daysToKeep
     * @param numToKeep
     * @deprecated since 1.350. Use {@link #LogRotator(int, int, int, int)}
     */
    public LogRotator(int daysToKeep, int numToKeep) {
        this(daysToKeep, numToKeep, -1, -1);
    }

    public LogRotator(int daysToKeep, int numToKeep, int artifactDaysToKeep, int artifactNumToKeep) {
        this.daysToKeep = daysToKeep;
        this.numToKeep = numToKeep;
        this.artifactDaysToKeep = artifactDaysToKeep;
        this.artifactNumToKeep = artifactNumToKeep;

    }

    public void perform(Job<?, ?> job) throws IOException, InterruptedException {

        LOGGER.log(FINE, "Running the log rotation for {0}", job.getFullDisplayName());

        BuildHistory bh = job.getBuildHistoryData();
        List<Record> allRecords = bh.allRecords();

        Record lsb = bh.getLastSuccessful();
        Record lstb = bh.getLastStable();

        // keep the last successful build regardless of the status
        List<Record> subrecords = new ArrayList(allRecords);
        Calendar cal = null;
        //Delete builds
        if (-1 != numToKeep || -1 != daysToKeep) {
            if (-1 != daysToKeep) {
                cal = new GregorianCalendar();
                cal.add(Calendar.DAY_OF_YEAR, -daysToKeep);
            }
            if (-1 != numToKeep) {
                subrecords = allRecords.subList(Math.min(allRecords.size(), numToKeep), allRecords.size());
            }
            //Delete builds based on configured values. See http://issues.hudson-ci.org/browse/HUDSON-3650
            deleteBuilds(subrecords, lsb, lstb, cal);
        }

        cal = null;
        //Delete build artifacts
        if (-1 != artifactNumToKeep || -1 != artifactDaysToKeep) {
            if (-1 != artifactDaysToKeep) {
                cal = new GregorianCalendar();
                cal.add(Calendar.DAY_OF_YEAR, -artifactDaysToKeep);
            }
            if (-1 != artifactNumToKeep) {
                subrecords = allRecords.subList(Math.min(allRecords.size(), artifactNumToKeep), allRecords.size());
            }
            //Delete build artifacts based on configured values. See http://issues.hudson-ci.org/browse/HUDSON-3650
            deleteBuildArtifacts(subrecords, lsb, lstb, cal);
        }
    }

    /**
     * Performs builds deletion
     *
     * @param builds list of builds
     * @param lastSuccessBuild last success build
     * @param lastStableBuild last stable build
     * @param cal calendar if configured
     * @throws IOException if configured
     */
    private void deleteBuilds(List<Record> subrecords, Record lastSuccessBuild, Record lastStableBuild, Calendar cal)
            throws IOException {
        for (Record currentBuild : subrecords) {
            if (allowDeleteBuild(lastSuccessBuild, lastStableBuild, currentBuild, cal)) {
                Run currentRun = currentBuild.getBuild();
                if (currentRun.isKeepLog()) {
                    LOGGER.log(FINER, "{0} is not GC-ed because it''s marked as a keeper", currentBuild.getFullDisplayName());
                } else {
                    LOGGER.log(FINER, "{0} is to be removed", currentBuild.getFullDisplayName());
                    currentRun.delete();
                }
            }
        }
    }

    /**
     * Checks whether current build could be deleted. If current build equals to
     * last Success Build or last Stable Build or currentBuild is configured to
     * keep logs or currentBuild timestamp is before configured calendar value -
     * return false, otherwise return true.
     *
     * @param lastSuccessBuild {@link Run}
     * @param lastStableBuild {@link Run}
     * @param currentBuild {@link Run}
     * @param cal {@link Calendar}
     * @return true - if deletion is allowed, false - otherwise.
     */
    private boolean allowDeleteBuild(Record lastSuccessBuild, Record lastStableBuild, Record currentBuild, Calendar cal) {
        if (currentBuild == lastSuccessBuild) {
            LOGGER.log(FINER, "{0} is not GC-ed because it''s the last successful build", currentBuild.getFullDisplayName());
            return false;
        }
        if (currentBuild == lastStableBuild) {
            LOGGER.log(FINER, "{0} is not GC-ed because it''s the last stable build", currentBuild.getFullDisplayName());
            return false;
        }
        if (null != cal && !currentBuild.getTimestamp().before(cal)) {
            LOGGER.log(FINER, "{0} is not GC-ed because it''s still new", currentBuild.getFullDisplayName());
            return false;
        }
        return true;
    }

    /**
     * Performs build artifacts deletion
     *
     * @param builds list of builds
     * @param lastSuccessBuild last success build
     * @param lastStableBuild last stable build
     * @param cal calendar if configured
     * @throws IOException if configured
     */
    private void deleteBuildArtifacts(List<Record> subrecords, Record lastSuccessBuild, Record lastStableBuild,
            Calendar cal) throws IOException {
        for (Record currentBuild : subrecords) {
            if (allowDeleteArtifact(lastSuccessBuild, lastStableBuild, currentBuild, cal)) {
                Run currentRun = currentBuild.getBuild();
                if (currentRun.isKeepLog()) {
                    LOGGER.log(FINER, "{0} is not purged of artifacts because it''s marked as a keeper", currentBuild.getFullDisplayName());
                } else {
                    LOGGER.log(FINER, "Artifacts of {0} to be removed", currentBuild.getFullDisplayName());
                    currentRun.deleteArtifacts();
                }
            }
        }
    }

    /**
     * Checks whether artifacts from build could be deleted. If current build
     * equals to last Success Build or last Stable Build or currentBuild is
     * configured to keep logs or currentBuild timestamp is before configured
     * calendar value - return false, otherwise return true.
     *
     * @param lastSuccessBuild {@link Run}
     * @param lastStableBuild {@link Run}
     * @param currentBuild {@link Run}
     * @param cal {@link Calendar}
     * @return true - if deletion is allowed, false - otherwise.
     */
    private boolean allowDeleteArtifact(Record lastSuccessBuild, Record lastStableBuild, Record currentBuild, Calendar cal) {

        if (currentBuild == lastSuccessBuild) {
            LOGGER.log(FINER, "{0} is not purged of artifacts because it''s the last successful build", currentBuild.getFullDisplayName());
            return false;
        }
        if (currentBuild == lastStableBuild) {
            LOGGER.log(FINER, "{0} is not purged of artifacts because it''s the last stable build", currentBuild.getFullDisplayName());
            return false;
        }
        if (null != cal && !currentBuild.getTimestamp().before(cal)) {
            LOGGER.log(FINER, "{0} is not purged of artifacts because it''s still new", currentBuild.getFullDisplayName());
            return false;
        }
        return true;
    }

    public int getDaysToKeep() {
        return daysToKeep;
    }

    public int getNumToKeep() {
        return numToKeep;
    }

    public int getArtifactDaysToKeep() {
        return unbox(artifactDaysToKeep);
    }

    public int getArtifactNumToKeep() {
        return unbox(artifactNumToKeep);
    }

    public String getDaysToKeepStr() {
        return toString(daysToKeep);
    }

    public String getNumToKeepStr() {
        return toString(numToKeep);
    }

    public String getArtifactDaysToKeepStr() {
        return toString(artifactDaysToKeep);
    }

    public String getArtifactNumToKeepStr() {
        return toString(artifactNumToKeep);
    }

    private int unbox(Integer i) {
        return i == null ? -1 : i;
    }

    private String toString(Integer i) {
        if (i == null || i == -1) {
            return "";
        }
        return String.valueOf(i);
    }

    @Override
    public LRDescriptor getDescriptor() {
        return DESCRIPTOR;
    }
    public static final LRDescriptor DESCRIPTOR = new LRDescriptor();

    public static final class LRDescriptor extends Descriptor<LogRotator> {

        @Override
        public String getDisplayName() {
            return "Log Rotation";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LogRotator that = (LogRotator) o;

        if (daysToKeep != that.daysToKeep) {
            return false;
        }
        if (numToKeep != that.numToKeep) {
            return false;
        }
        if (artifactDaysToKeep != null ? !artifactDaysToKeep.equals(that.artifactDaysToKeep)
                : that.artifactDaysToKeep != null) {
            return false;
        }
        return !(artifactNumToKeep != null ? !artifactNumToKeep.equals(that.artifactNumToKeep)
                : that.artifactNumToKeep != null);
    }

    @Override
    public int hashCode() {
        int result = daysToKeep;
        result = 31 * result + numToKeep;
        result = 31 * result + (artifactDaysToKeep != null ? artifactDaysToKeep.hashCode() : 0);
        result = 31 * result + (artifactNumToKeep != null ? artifactNumToKeep.hashCode() : 0);
        return result;
    }
}
