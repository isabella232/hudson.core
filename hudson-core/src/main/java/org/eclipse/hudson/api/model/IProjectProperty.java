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

package org.hudsonci.api.model;

import java.io.Serializable;

/**
 * Represents Properties for Job,
 * <p/>
 * Date: 9/22/11
 *
 * @author Nikita Levyankov
 */
public interface IProjectProperty<T> extends Serializable {

    /**
     * Sets key for given property.
     *
     * @param key key.
     */
    void setKey(String key);

    /**
     * @return property key.
     */
    String getKey();

    /**
     * Sets the job, which is owner of current property.
     *
     * @param job {@link ICascadingJob}
     */
    void setJob(ICascadingJob job);

    /**
     * Sets property value.
     * If property has cascading value and properties' {@link #allowOverrideValue(Object, Object)} method returns true,
     * than value will be set to current property.<br/>
     * If property doesn't have cascading value, than value will be set directly.
     *
     * @param value value to set.
     */
    void setValue(T value);

    /**
     * Returns original property value.
     *
     * @return T
     */
    T getOriginalValue();

    /**
     * Returns cascading value if any.
     *
     * @return string.
     */
    T getCascadingValue();

    /**
     * @return true if value inherited from cascading project, false - otherwise,
     */
    boolean isOverridden();

    /**
     * Returns property value. If originalValue is not null or value was overridden for this
     * property - call {@link #getOriginalValue()}, otherwise call {@link #getCascadingValue()}.
     *
     * @return string.
     */
    T getValue();

    /**
     * This value will be taken if both cascading project and current project don't have values. Null by default.
     *
     * @return value
     */
    T getDefaultValue();

    /**
     * Resets value for given job. Default implementation sets Null value and resets propertyOverridden flag to false.
     */
    void resetValue();

    /**
     * Returns true, if cascading value should be overridden by candidate value.
     *
     * @param cascadingValue value from cascading project if any.
     * @param candidateValue candidate value.
     * @return true if cascading value should be replaced by candidate value.
     */
    boolean allowOverrideValue(T cascadingValue, T candidateValue);

    /**
     * Sets the overridden flag.
     *
     * @param overridden true - mark property as overridden, false - otherwise.
     */
    void setOverridden(boolean overridden);

    /**
     * Method that is called while changing cascading parent. Update property internal states.l
     */
    void onCascadingProjectChanged();
}
