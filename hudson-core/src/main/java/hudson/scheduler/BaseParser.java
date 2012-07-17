/*******************************************************************************
 *
 * Copyright (c) 2004-2012 Oracle Corporation.
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
 *
 *******************************************************************************/ 

package hudson.scheduler;

import org.antlr.runtime.Parser;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;

/**
 * @author Kohsuke Kawaguchi
 */
abstract class BaseParser extends Parser {

    private static final int[] LOWER_BOUNDS = new int[]{0, 0, 1, 0, 0};
    private static final int[] UPPER_BOUNDS = new int[]{59, 23, 31, 12, 7};

    protected BaseParser(TokenStream tokenStream) {
        super(tokenStream);
    }

    protected BaseParser(TokenStream stream, RecognizerSharedState sharedState) {
        super(stream, sharedState);
    }

    protected long doRange(int start, int end, int step, int field) throws RecognitionException {
        rangeCheck(start, field);
        rangeCheck(end, field);
        if (step <= 0) {
            error(Messages.BaseParser_MustBePositive(step));
        }
        if (start > end) {
            error(Messages.BaseParser_StartEndReversed(end, start));
        }

        long bits = 0;
        for (int i = start; i <= end; i += step) {
            bits |= 1L << i;
        }
        return bits;
    }

    protected long doRange(int step, int field) throws RecognitionException {
        return doRange(LOWER_BOUNDS[field], UPPER_BOUNDS[field], step, field);
    }

    protected void rangeCheck(int value, int field) throws RecognitionException {
        if (value < LOWER_BOUNDS[field] || UPPER_BOUNDS[field] < value) {
            error(Messages.BaseParser_OutOfRange(value, LOWER_BOUNDS[field], UPPER_BOUNDS[field]));
        }
    }

    private void error(String msg) throws RecognitionException {
        Token token = getTokenStream().LT(0);
        throw new SemanticException(
                msg,
                token.getLine(),
                token.getCharPositionInLine());
    }

    public static class SemanticException extends RecognitionException {

        String msg;
        Throwable throwable;

        public SemanticException(String msg, int line, int charPositionInLine) {
            super();
            this.msg = msg;
            this.line = line;
            this.charPositionInLine = charPositionInLine;
        }

        public SemanticException(String msg, Throwable e) {
            super();
            this.msg = msg;
            this.throwable = e;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("SemanticException: ");
            sb.append(msg);
            if (line > 0 || charPositionInLine > 0) {
                sb.append(" at [").append(line).append(',').append(charPositionInLine);
            }
            if (throwable != null) {
                sb.append(" from ").append(throwable.toString());
            }
            return sb.toString();
        }

        @Override
        public String getMessage() {
            return msg;
        }
    }
}
