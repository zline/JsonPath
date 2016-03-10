package com.jayway.jsonpath.internal.regex;

/**
 * @author Vasiliy Voloshin (v.voloshin@corp.mail.ru).
 */
public class InterruptableCharSequence implements CharSequence {

    public static final int DEFAULT_RATIO = 1000;

    private CharSequence inner;
    private int ratio = 1000;
    private int cnt = 0;
    private int maxLookUp = 0;

    public InterruptableCharSequence(CharSequence inner) {
        this(inner, DEFAULT_RATIO);
    }

    public InterruptableCharSequence(CharSequence inner, int ratio) {
        super();
        this.ratio = ratio;
        this.setInner(inner);
    }

    @Override
    public char charAt(int index) throws TooManyRegexLookupsException {
        if (++cnt > maxLookUp)
            throw new TooManyRegexLookupsException("String length: " + inner.length()
                    + "; Threshold value: " + maxLookUp);
        return inner.charAt(index);
    }

    @Override
    public int length() {
        return inner.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        // FIXME TODO this.cnt should be shared with subSequence
        return new InterruptableCharSequence(inner.subSequence(start, end), ratio);
    }

    @Override
    public String toString() {
        return inner.toString();
    }

    public InterruptableCharSequence setInner(CharSequence inner) {
        this.inner = inner;
        this.cnt = 0;
        this.maxLookUp = inner.length() * ratio;
        return this;
    }
}
