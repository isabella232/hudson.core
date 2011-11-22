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

import java.io.IOException;
import java.io.Serializable;

/**
 * Represents Properties for Job,
 * <p/>
 * Date: 9/22/11
 *
 * @author Nikita Levyankov
 */
public interface IProperty<T> extends Serializable {

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
     * @throws IOException if any.
     */
    void setValue(T value) throws IOException;

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
     * @throws IOException if any.
     */
    T getCascadingValue() throws IOException;

    /**
     * @return true if value inherited from cascading project, false - otherwise,
     */
    boolean isPropertyOverridden();

    /**
     * Returns property value. If originalValue is not null or value was overridden for this
     * property - call {@link #getOriginalValue()}, otherwise call {@link #getCascadingValue()}.
     *
     * @return string.
     * @throws IOException if any.
     */
    T getValue() throws IOException;

}
