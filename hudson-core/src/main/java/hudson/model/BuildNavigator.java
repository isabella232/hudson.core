/*
 * Copyright (c) 2013 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */
package hudson.model;

import java.util.List;

/**
 * The BuildNavigator supplies the links to the preceding and following builds.
 * 
 * @author roy.varghese@oracle.com
 */
public interface BuildNavigator<T extends Run> {
        T getPreviousBuild();
        T getNextBuild();
        T getPreviousCompletedBuild();
        T getPreviousBuildInProgress();
        T getPreviousBuiltBuild();
        T getPreviousNotFailedBuild();
        T getPreviousFailedBuild();
        T getPreviousSuccessfulBuild();
        List<T> getPreviousBuildsOverThreshold(int numberOfBuilds, Result threshold);
        
        BallColor getIconColor();
        String getBuildStatusUrl();
        Run.Summary getBuildStatusSummary();
        
        boolean isBuilding();
        boolean isLogUpdated();
    
}
