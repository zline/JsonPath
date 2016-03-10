package com.jayway.jsonpath.internal.regex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regexp that works predictable amount of time.
 *
 * @author Vasiliy Voloshin (v.voloshin@corp.mail.ru).
 * @author Alexey Makeyev (makeev@corp.mail.ru).
 *
 * NOTE: catching StackOverflowError because during some string (in which unicode character length measured in chars
 * changes frequently) processing Pattern.Curly recurses a lot and even for moderate length strings (~ 75k) entire
 * computation could be interrupted with a stack overflow, which is a greater evil (in our case). After stack unwinding
 * up to a catch computation could be restarted.
 *
 * See also https://swtch.com/~rsc/regexp/regexp1.html .
 */
public class InterruptablePattern {

    public static final int DEFAULT_MAX_ERRORS_NUM = 200;

    private static final Set<Character> REPETITION_META_SYMBOL = new HashSet<Character>(Arrays.asList(new Character[]{
            '?', '*', '+', '{'
    }));

    private final String re;
    private final Pattern pattern;
    private int errorCnt;
    private final int maxErrorsNum;
    private int repetitionMetaSymbolCnt = -1;
    // NOTE interruptable behaviour can be turned on based on input data (in addition to isInterruptable flag)
    private boolean isInterruptable = false;
    private final InterruptableCharSequence sequence;


    public InterruptablePattern(String re) {
        this(re, 0);
    }

    public InterruptablePattern(String re, int flags) {
        this(re, flags, DEFAULT_MAX_ERRORS_NUM);
    }

    /**
     * Ctor
     * @param re regex string
     * @param flags regex compilation flags
     * @param maxErrorsNum after maxErrorsNum interrupts this regex is disabled entirely
     */
    public InterruptablePattern(String re, int flags, int maxErrorsNum) {
        this(Pattern.compile(re, flags), maxErrorsNum);
    }

    public InterruptablePattern(Pattern re) {
        this(re, DEFAULT_MAX_ERRORS_NUM);
    }

    public InterruptablePattern(Pattern re, int maxErrorsNum) {
        this.re = re.pattern();
        this.pattern = re;
        this.maxErrorsNum = maxErrorsNum;
        this.errorCnt = 0;
        this.sequence = new InterruptableCharSequence("");

        this.checkIsInterruptable();
    }

    public boolean matches(CharSequence input) throws TooManyRegexLookupsException {
        try {
            Matcher m = getMatcher(input, false);
            return m != null && m.matches();
        } catch (TooManyRegexLookupsException e) {
            if (++this.errorCnt <= this.maxErrorsNum)
                throw e;
        } catch (StackOverflowError exc) {
            return false;
        }
        assert false : "Unreachable statement";
        return false;
    }

    public String get(CharSequence input) throws TooManyRegexLookupsException {
        try {
            Matcher m = getMatcher(input, true);
            if (m == null || !m.find() || m.groupCount() < 1)
                return "";

            String group = m.group(1);
            return group != null ? group : "";

        } catch (TooManyRegexLookupsException e) {
            if (++this.errorCnt <= this.maxErrorsNum)
                throw e;
        } catch (StackOverflowError exc) {
            return "";
        }
        assert false : "Unreachable statement";
        return "";
    }

    public Long count(CharSequence input) throws TooManyRegexLookupsException {
        try {
            Matcher m = getMatcher(input, true);
            if (m == null)
                return 0L;

            long group = 0;
            while (m.find())
                group++;

            return group;

        } catch (TooManyRegexLookupsException e) {
            if (++this.errorCnt <= this.maxErrorsNum)
                throw e;
        } catch (StackOverflowError exc) {
            return 0L;
        }
        assert false : "Unreachable statement";
        return 0L;
    }

    public boolean isThresholdValue() {
        assert this.errorCnt <= this.maxErrorsNum :
                "errorCnt( " + this.errorCnt + " ) > maxErrorsNum( " + this.maxErrorsNum + " )";

        return this.errorCnt == this.maxErrorsNum;
    }

    private Matcher getMatcher(CharSequence input, boolean isSearch) {
        // in case of isInterruptable regex complexity could be exponential
        boolean useInterrupts = this.isInterruptable;

        // in case of repetition meta characters regex complexity could be at least O(n^2) and during long string
        // processing it could be harmful (and isSearch mode itself is like repetition)
        if (! useInterrupts && input.length() > 0 && getRepetitionMetaSymbolCnt() > 0
                && Math.pow(input.length(), getRepetitionMetaSymbolCnt() + (isSearch ? 1 : 0)) >= (double)(input.length()) * InterruptableCharSequence.DEFAULT_RATIO)
            useInterrupts = true;

        if (useInterrupts) {
            if (this.isThresholdValue())
                return null;

            return this.pattern.matcher(this.sequence.setInner(input));
        }

        return this.pattern.matcher(input);
    }


    /**
     * Trying to guess if this.re is not safe and could take a lot of time.
     */
    private void checkIsInterruptable() {
        if (re.contains(")*") || re.contains(")+") || re.contains("){")) {
            this.isInterruptable = true;
            return;
        }

        if (getRepetitionMetaSymbolCnt() >= 10)
            this.isInterruptable = true;
    }

    private int getRepetitionMetaSymbolCnt() {
        if (-1 == repetitionMetaSymbolCnt) {
            repetitionMetaSymbolCnt = 0;

            boolean _class = false;
            boolean escaped = false;
            for (char symbol : re.toCharArray()) {

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (symbol == '\\') {
                    escaped = true;
                    continue;
                }

                if (_class && symbol != ']')
                    continue;

                if (symbol == '[')
                    _class = true;
                else if (symbol == ']')
                    _class = false;

                if (!REPETITION_META_SYMBOL.contains(symbol))
                    continue;

                repetitionMetaSymbolCnt++;
            }
        }

        return repetitionMetaSymbolCnt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InterruptablePattern that = (InterruptablePattern) o;

        if (maxErrorsNum != that.maxErrorsNum) return false;
        if (!pattern.equals(that.pattern)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = pattern.hashCode();
        result = 31 * result + maxErrorsNum;
        return result;
    }

    // Getters and Setters
    public Pattern getPattern() {
        return pattern;
    }

    public int getErrorCnt() {
        return errorCnt;
    }

    public int getMaxErrorsNum() {
        return maxErrorsNum;
    }

    public boolean isInterruptable() {
        return isInterruptable;
    }
}
