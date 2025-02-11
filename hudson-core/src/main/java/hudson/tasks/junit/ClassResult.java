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
import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestResult;
import hudson.tasks.test.TestObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cumulative test result of a test class.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ClassResult extends TabulatedResult implements Comparable<ClassResult> {

    private final String className; // simple name
    private final List<CaseResult> cases = new ArrayList<CaseResult>();
    private int passCount, failCount, skipCount;
    private float duration;
    private final PackageResult parent;
    
    private transient String uniqueSafeName;

    ClassResult(PackageResult parent, String className) {
        this.parent = parent;
        this.className = className;
    }

    @Override
    public AbstractBuild<?, ?> getOwner() {
        return (parent == null ? null : parent.getOwner());
    }

    public PackageResult getParent() {
        return parent;
    }

    @Override
    public ClassResult getPreviousResult() {
        if (parent == null) {
            return null;
        }
        TestResult pr = parent.getPreviousResult();
        if (pr == null) {
            return null;
        }
        if (pr instanceof PackageResult) {
            return ((PackageResult) pr).getClassResult(getName());
        }
        return null;
    }

    @Override
    public hudson.tasks.test.TestResult findCorrespondingResult(String id) {
        String myID = safe(getName());
        int base = id.indexOf(myID);
        String caseName;
        if (base > 0) {
            int caseNameStart = base + myID.length() + 1;
            caseName = id.substring(caseNameStart);
        } else {
            caseName = id;
        }

        CaseResult child = getCaseResult(caseName);
        if (child != null) {
            return child;
        }

        return null;
    }

    public String getTitle() {
        return Messages.ClassResult_getTitle(getName());
    }

    @Override
    public String getChildTitle() {
        return "Class Reults";
    }

    @Exported(visibility = 999)
    public String getName() {
        int idx = className.lastIndexOf('.');
        if (idx < 0) {
            return className;
        } else {
            return className.substring(idx + 1);
        }
    }

    @Override
    public synchronized String getSafeName() {
        if (uniqueSafeName != null){
            return uniqueSafeName;
        }
        uniqueSafeName = uniquifyName(parent.getChildren(), safe(getName()));
        return uniqueSafeName;
    }

    public CaseResult getCaseResult(String name) {
        for (CaseResult c : cases) {
            if (c.getSafeName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        CaseResult c = getCaseResult(name);
        if (c != null) {
            return c;
        } else {
            return super.getDynamic(name, req, rsp);
        }
    }

    @Exported(name = "child")
    public List<CaseResult> getChildren() {
        return cases;
    }

    public boolean hasChildren() {
        return ((cases != null) && (cases.size() > 0));
    }

    // TODO: wait for stapler 1.60     @Exported
    public float getDuration() {
        return duration;
    }

    @Exported
    public int getPassCount() {
        return passCount;
    }

    @Exported
    public int getFailCount() {
        return failCount;
    }

    @Exported
    public int getSkipCount() {
        return skipCount;
    }

    public void add(CaseResult r) {
        cases.add(r);
    }

    /**
     * Recount my children.
     */
    @Override
    public void tally() {
        passCount = failCount = skipCount = 0;
        duration = 0;
        for (CaseResult r : cases) {
            r.setClass(this);
            if (r.isSkipped()) {
                skipCount++;
            } else if (r.isPassed()) {
                passCount++;
            } else {
                failCount++;
            }
            duration += r.getDuration();
        }
    }

    void freeze() {
        passCount = failCount = skipCount = 0;
        duration = 0;
        for (CaseResult r : cases) {
            r.setClass(this);
            if (r.isSkipped()) {
                skipCount++;
            } else if (r.isPassed()) {
                passCount++;
            } else {
                failCount++;
            }
            duration += r.getDuration();
        }
        Collections.sort(cases);
    }

    public String getClassName() {
        return className;
    }

    public int compareTo(ClassResult that) {
        return this.className.compareTo(that.className);
    }

    public String getDisplayName() {
        return getName();
    }

    public String getFullName() {
        return getParent().getDisplayName() + "." + className;
    }

    /**
     * Gets the relative path to this test case from the given object.
     */
    @Override
    public String getRelativePathFrom(TestObject it) {
        if (it instanceof CaseResult) {
            return "..";
        } else {
            return super.getRelativePathFrom(it);
        }
    }
}
