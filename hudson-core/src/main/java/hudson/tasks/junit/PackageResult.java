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
 *    Kohsuke Kawaguchi, Daniel Dyer, id:cactusman, Tom Huybrechts, Yahoo!, Inc.
 *
 *
 *******************************************************************************/ 

package hudson.tasks.junit;

import hudson.model.AbstractBuild;
import hudson.tasks.test.MetaTabulatedResult;
import hudson.tasks.test.TestResult;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import java.util.*;

/**
 * Cumulative test result for a package.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PackageResult extends MetaTabulatedResult implements Comparable<PackageResult> {

    private final String packageName;
    /**
     * All {@link ClassResult}s keyed by their short name.
     */
    private final Map<String, ClassResult> classes = new TreeMap<String, ClassResult>();
    private int passCount, failCount, skipCount;
    private final hudson.tasks.junit.TestResult parent;
    private float duration;
    
    private transient String uniqueSafeName;

    PackageResult(hudson.tasks.junit.TestResult parent, String packageName) {
        this.packageName = packageName;
        this.parent = parent;
    }

    @Override
    public AbstractBuild<?, ?> getOwner() {
        return (parent == null ? null : parent.getOwner());
    }

    public hudson.tasks.junit.TestResult getParent() {
        return parent;
    }

    @Exported(visibility = 999)
    public String getName() {
        return packageName;
    }

    @Override
    public synchronized String getSafeName() {
        if (uniqueSafeName != null){
            return uniqueSafeName;
        }
        Collection<PackageResult> siblings = (parent == null ? Collections.EMPTY_LIST : parent.getChildren());
        uniqueSafeName = uniquifyName(siblings, safe(getName()));
        return  uniqueSafeName;
    }

    @Override
    public TestResult findCorrespondingResult(String id) {
        String myID = safe(getName());
        int base = id.indexOf(myID);
        String className;
        String subId = null;
        if (base > 0) {
            int classNameStart = base + myID.length() + 1;
            className = id.substring(classNameStart);
        } else {
            className = id;
        }
        int classNameEnd = className.indexOf('/');
        if (classNameEnd > 0) {
            subId = className.substring(classNameEnd + 1);
            if (subId.length() == 0) {
                subId = null;
            }
            className = className.substring(0, classNameEnd);
        }

        ClassResult child = getClassResult(className);
        if (child != null) {
            if (subId != null) {
                return child.findCorrespondingResult(subId);
            } else {
                return child;
            }
        }

        return null;
    }

    @Override
    public String getTitle() {
        return Messages.PackageResult_getTitle(getName());
    }

    @Override
    public String getChildTitle() {
        return Messages.PackageResult_getChildTitle();
    }

    // TODO: wait until stapler 1.60 to do this @Exported
    @Override
    public float getDuration() {
        return duration;
    }

    @Exported
    @Override
    public int getPassCount() {
        return passCount;
    }

    @Exported
    @Override
    public int getFailCount() {
        return failCount;
    }

    @Exported
    @Override
    public int getSkipCount() {
        return skipCount;
    }

    @Override
    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        ClassResult result = getClassResult(name);
        if (result != null) {
            return result;
        } else {
            return super.getDynamic(name, req, rsp);
        }
    }

    public ClassResult getClassResult(String name) {
        return classes.get(name);
    }

    @Exported(name = "child")
    public Collection<ClassResult> getChildren() {
        return classes.values();
    }

    /**
     * Whether this test result has children.
     */
    @Override
    public boolean hasChildren() {
        int totalTests = passCount + failCount + skipCount;
        return (totalTests != 0);
    }

    /**
     * Returns a list of the failed cases, in no particular sort order
     *
     * @return
     */
    public List<CaseResult> getFailedTests() {
        List<CaseResult> r = new ArrayList<CaseResult>();
        for (ClassResult clr : classes.values()) {
            for (CaseResult cr : clr.getChildren()) {
                if (!cr.isPassed() && !cr.isSkipped()) {
                    r.add(cr);
                }
            }
        }
        return r;
    }

    /**
     * Returns a list of the failed cases, sorted by age.
     *
     * @return
     */
    public List<CaseResult> getFailedTestsSortedByAge() {
        List<CaseResult> failedTests = getFailedTests();
        Collections.sort(failedTests, CaseResult.BY_AGE);
        return failedTests;
    }

    /**
     * Gets the "children" of this test result that passed
     *
     * @return the children of this test result, if any, or an empty collection
     */
    @Override
    public Collection<? extends hudson.tasks.test.TestResult> getPassedTests() {
        List<CaseResult> r = new ArrayList<CaseResult>();
        for (ClassResult clr : classes.values()) {
            for (CaseResult cr : clr.getChildren()) {
                if (cr.isPassed()) {
                    r.add(cr);
                }
            }
        }
        Collections.sort(r, CaseResult.BY_AGE);
        return r;
    }

    /**
     * Gets the "children" of this test result that were skipped
     *
     * @return the children of this test result, if any, or an empty list
     */
    @Override
    public Collection<? extends TestResult> getSkippedTests() {
        List<CaseResult> r = new ArrayList<CaseResult>();
        for (ClassResult clr : classes.values()) {
            for (CaseResult cr : clr.getChildren()) {
                if (cr.isSkipped()) {
                    r.add(cr);
                }
            }
        }
        Collections.sort(r, CaseResult.BY_AGE);
        return r;
    }

//    /**
//     * If this test failed, then return the build number
//     * when this test started failing.
//     */
//    @Override
//    TODO: implement! public int getFailedSince() {
//        return 0;  // (FIXME: generated)
//    }
//    /**
//     * If this test failed, then return the run
//     * when this test started failing.
//     */
//    TODO: implement! @Override
//    public Run<?, ?> getFailedSinceRun() {
//        return null;  // (FIXME: generated)
//    }
    /**
     * @return true if every test was not skipped and every test did not fail,
     * false otherwise.
     */
    @Override
    public boolean isPassed() {
        return (failCount == 0 && skipCount == 0);
    }

    void add(CaseResult r) {
        String n = r.getSimpleName(), sn = safe(n);
        ClassResult c = getClassResult(sn);
        if (c == null) {
            classes.put(sn, c = new ClassResult(this, n));
        }
        c.add(r);
        duration += r.getDuration();
    }

    /**
     * Recount my children
     */
    @Override
    public void tally() {
        passCount = 0;
        failCount = 0;
        skipCount = 0;
        duration = 0;

        for (ClassResult cr : classes.values()) {
            cr.tally();
            passCount += cr.getPassCount();
            failCount += cr.getFailCount();
            skipCount += cr.getSkipCount();
            duration += cr.getDuration();
        }
    }

    void freeze() {
        passCount = failCount = skipCount = 0;
        for (ClassResult cr : classes.values()) {
            cr.freeze();
            passCount += cr.getPassCount();
            failCount += cr.getFailCount();
            skipCount += cr.getSkipCount();
        }
    }

    public int compareTo(PackageResult that) {
        return this.packageName.compareTo(that.packageName);
    }

    public String getDisplayName() {
        return packageName;
    }
}
