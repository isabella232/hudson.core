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

package hudson.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import org.antlr.runtime.RecognitionException;

/**
 * {@link CronTab} list (logically OR-ed).
 *
 * @author Kohsuke Kawaguchi
 */
public final class CronTabList {

    private final List<CronTab> tabs;

    public CronTabList(Collection<CronTab> tabs) {
        this.tabs = new ArrayList<CronTab>(tabs);
    }

    /**
     * Returns true if the given calendar matches.
     */
    public boolean check(Calendar cal) {
        for (CronTab tab : tabs) {
            if (tab.check(cal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this crontab entry looks reasonable, and if not, return an
     * warning message.
     *
     * <p> The point of this method is to catch syntactically correct but
     * semantically suspicious combinations, like "* 0 * * *"
     */
    public String checkSanity() {
        for (CronTab tab : tabs) {
            String s = tab.checkSanity();
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    public static CronTabList create(String format) throws RecognitionException {
        List<CronTab> r = new ArrayList<CronTab>();
        int lineNumber = 0;
        for (String line : format.split("\\r?\\n")) {
            lineNumber++;
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;   // ignorable line
            }
            try {
                r.add(new CronTab(line, lineNumber));
            } catch (RecognitionException e) {
                throw new BaseParser.SemanticException(Messages.CronTabList_InvalidInput(line, e.toString()), e);
            }
        }
        return new CronTabList(r);
    }
}
