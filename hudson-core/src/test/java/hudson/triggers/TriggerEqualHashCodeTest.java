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
package hudson.triggers;

import java.util.Arrays;
import java.util.Collection;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Test for equals and hashCode methods of {@link Trigger} objects
 * </p>
 * Date: 10/28/2011
 *
 * @author
 */
@RunWith(Parameterized.class)
public class TriggerEqualHashCodeTest {

    private boolean expectedResult;
    private Trigger trigger1;
    private Trigger trigger2;

    public TriggerEqualHashCodeTest(boolean expectedResult, Trigger trigger1, Trigger trigger2) {
        this.expectedResult = expectedResult;
        this.trigger1 = trigger1;
        this.trigger2 = trigger2;
    }

    @Parameterized.Parameters
    public static Collection generateData() throws RecognitionException {
        return Arrays.asList(new Object[][] {
            {true, new TimerTrigger(""), new TimerTrigger("")},
            {true, new TimerTrigger("* * * * *"), new TimerTrigger("* * * * *")},
            {false, new TimerTrigger("* * * * *"), new TimerTrigger("*/2 * * * *")},
            {true, new SCMTrigger(""), new SCMTrigger("")},
            {true, new SCMTrigger("* * * * *"), new SCMTrigger("* * * * *")},
            {false, new SCMTrigger("* * * * *"), new SCMTrigger("*/2 * * * *")}
        });
    }

    @Test
    public void testEquals() throws RecognitionException {
        assertEquals(expectedResult, trigger1.equals(trigger2));
    }

    @Test
    public void testHashCode() {
        assertEquals(expectedResult, trigger1.hashCode() == trigger2.hashCode());
    }
}
