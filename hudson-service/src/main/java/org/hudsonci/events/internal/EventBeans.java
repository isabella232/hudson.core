/*
 * Copyright (c) 2015 Hudson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hudson - initial API and implementation and/or initial documentation
 */

package org.hudsonci.events.internal;

import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.hudsonci.events.EventConsumer;

/**
 * Just satisfies dependencies required by CDI.
 * This class <strong>should not</strong> be used.
 *
 * @author Kaz Nishimura
 */
@ApplicationScoped
public class EventBeans {

    @Produces
    private List<EventConsumer> getEventConsumers() {
        throw new RuntimeException("CDI is not supported");
    }
}
