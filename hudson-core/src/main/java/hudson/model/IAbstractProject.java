/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov
 *
 *******************************************************************************/
package hudson.model;

import hudson.scm.SCM;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import java.util.Map;

/**
 * Interface that reflects common methods for AbstractProject model
 * <p/>
 * Date: 9/15/11
 *
 * @author Nikita Levyankov
 */
public interface IAbstractProject extends IJob {

    /**
     * Returns configured SCM for project,
     *
     * @return {@link SCM} instance
     */
    SCM getScm();

    /**
     * Returns map of triggers.
     *
     * @return {@link Map}.
     */
    Map<TriggerDescriptor, Trigger> getTriggers();

    /**
     * Gets the specific trigger, should be null if the property is not configured for this job.
     *
     * @param clazz class of trigger
     * @return T
     */
    <T extends Trigger> T getTrigger(Class<T> clazz);

    /**
     * Checks whether workspace should be cleaned before build
     *
     * @return boolean value
     */
    boolean isCleanWorkspaceRequired();

    /**
     * Indicates whether build should be blocked while downstream project is building.
     *
     * @return true if yes, false - otherwise.
     */
    boolean blockBuildWhenDownstreamBuilding();

    /**
     * Indicates whether build should be blocked while upstream project is building.
     *
     * @return true if yes, false - otherwise.
     */
    boolean blockBuildWhenUpstreamBuilding();

    /**
     * Checks whether scmRetryCount is configured
     *
     * @return true if yes, false - otherwise.
     */
    boolean hasCustomScmCheckoutRetryCount();

    /**
     * Returns scm checkout retry count.
     *
     * @return int value.
     */
    int getScmCheckoutRetryCount();

    /**
     * Returns project quiet period.
     *
     * @return int value.
     */
    int getQuietPeriod();

    /**
     * If this project is configured to be always built on this node,
     * return that {@link Node}. Otherwise null.
     *
     * @return {@link Label} instance.
     */
    Label getAssignedLabel();

    /**
     * Gets the textual representation of the assigned label as it was entered by the user.
     *
     * @return string
     */
    String getAssignedLabelString();

    /**
     * Gets whether this project is using the advanced affinity chooser UI.
     *
     * @return true - advanced chooser, false - simple textfield.
     */
    //TODO this method is UI only. Investigate how-to remove it from model.
    boolean isAdvancedAffinityChooser();
}
