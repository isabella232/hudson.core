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

package org.eclipse.hudson.api.model.project.property;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.eclipse.hudson.api.model.IJob;
import org.eclipse.hudson.api.model.IProjectProperty;

/**
 * Base property implementation. Contains common methods for setting and getting cascading and overridden properties.
 * <p/>
 * Date: 9/22/11
 *
 * @author Nikita Levyankov
 */
public class BaseProjectProperty<T> implements IProjectProperty<T> {
    static final String INVALID_JOB_EXCEPTION = "Project property should have not null job";
    static final String INVALID_PROPERTY_KEY_EXCEPTION = "Project property should have not null propertyKey";

    private transient String propertyKey;
    private transient IJob job;
    private T originalValue;
    private boolean propertyOverridden;

    /**
     * Instantiate new property.
     *
     * @param job owner of current property.
     */
    public BaseProjectProperty(IJob job) {
        setJob(job);
    }

    /**
     * {@inheritDoc}
     */
    public void setKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    /**
     * {@inheritDoc}
     */
    public void setJob(IJob job) {
        if (null == job) {
            throw new IllegalArgumentException(INVALID_JOB_EXCEPTION);
        }
        this.job = job;
    }

    /**
     * @return job that property belongs to.
     */
    final IJob getJob() {
        return job;
    }

    /**
     * {@inheritDoc}
     */
    public void setOverridden(boolean overridden) {
        propertyOverridden = overridden;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T getCascadingValue() {
        if (null == propertyKey) {
            throw new IllegalArgumentException(INVALID_PROPERTY_KEY_EXCEPTION);
        }
        return getJob().hasCascadingProject() ?
            (T) getJob().getCascadingProject().getProperty(propertyKey, this.getClass()).getValue() : getDefaultValue();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOverridden() {
        return propertyOverridden;
    }

    /**
     * {@inheritDoc}
     */
    public T getDefaultValue() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public T getValue() {
        if (isOverridden() || null != originalValue) {
            return getOriginalValue();
        }
        return getCascadingValue();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setValue(T value) {
        if (null == propertyKey) {
            throw new IllegalArgumentException(INVALID_PROPERTY_KEY_EXCEPTION);
        }
        value = prepareValue(value);
        if (!getJob().hasCascadingProject()) {
            setOriginalValue(value, false);
        } else {
            T cascadingValue = getCascadingValue();
            T candidateValue = null == value ? getDefaultValue() : value;
            if (allowOverrideValue(cascadingValue, candidateValue)) {
                setOriginalValue(value, true);
            } else {
                resetValue();
            }
        }
    }

    /**
     * Method that sets original value and mark it as overridden if needed. It was created to provide better flexibility
     * in subclasses.
     *
     * @param originalValue value to set
     * @param overridden true - to mark as overridden.
     */
    protected void setOriginalValue(T originalValue, boolean overridden) {
        this.originalValue = originalValue;
        setOverridden(overridden);
    }

    /**
     * {@inheritDoc}
     */
    public void resetValue() {
        this.originalValue = null;
        setOverridden(false);
    }

    /**
     * {@inheritDoc}
     */
    public boolean allowOverrideValue(T cascadingValue, T candidateValue) {
        return ObjectUtils.notEqual(cascadingValue, candidateValue)
            && !EqualsBuilder.reflectionEquals(cascadingValue, candidateValue);
    }

    /**
     * Pre-process candidate value.
     *
     * @param candidateValue candidateValue.
     * @return candidateValue by default.
     */
    protected T prepareValue(T candidateValue) {
        return candidateValue;
    }

    /**
     * {@inheritDoc}
     */
    public T getOriginalValue() {
        return originalValue;
    }
}
