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

package hudson.model.project.property;

import org.apache.commons.lang3.ObjectUtils;
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
     * Sets the overridden flag.
     *
     * @param overridden true - mark property as overridden, false - otherwise.
     */
    final void setPropertyOverridden(boolean overridden) {
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
    public boolean isPropertyOverridden() {
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
        if (isPropertyOverridden() || null != originalValue) {
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
            originalValue = value;
        } else if (allowOverrideValue(
            (T) getJob().getCascadingProject().getProperty(propertyKey, this.getClass()).getValue(), value)) {
            originalValue = value;
            setPropertyOverridden(true);
        } else {
            resetValue();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void resetValue() {
        this.originalValue = null;
        setPropertyOverridden(false);
    }

    /**
     * Returns true, if cascading value should be overridden by candidate value.
     *
     * @param cascadingValue value from cascading project if any.
     * @param candidateValue candidate value.
     * @return true if cascading value should be replaced by candidate value.
     */
    protected boolean allowOverrideValue(T cascadingValue, T candidateValue) {
        return !ObjectUtils.equals(cascadingValue, candidateValue);
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