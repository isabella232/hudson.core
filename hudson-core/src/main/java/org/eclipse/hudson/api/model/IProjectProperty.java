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

package org.eclipse.hudson.api.model;

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
     * Sets the job, which is owner of current property.
     *
     * @param job {@link IJob}
     */
    void setJob(IJob job);

    /**
     * Sets property value.
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
    boolean isPropertyOverridden();

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
}
