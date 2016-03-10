package com.jayway.jsonpath.internal.filter;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ValueNodeTest {

    @Test
    public void StringNode_couldBeANumber_test() {
        assertThat(ValueNode.StringNode.couldBeANumber("1")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("-11233")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("+1")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("1.100")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("1.1")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("1.1e5")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("1.1e+5")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber(".1e-5")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("0.1e-5")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("2e5")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("100e+5")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("100e-5")).isTrue();
        assertThat(ValueNode.StringNode.couldBeANumber("100e5")).isTrue();

        assertThat(ValueNode.StringNode.couldBeANumber("")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("-")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("+")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("x1.100")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("1. 1")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("1 .1e5")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("1.1e")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber(".1e-")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("0.1e-a")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("2m5")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("10x0e+5")).isFalse();
        assertThat(ValueNode.StringNode.couldBeANumber("100 e-5")).isFalse();
    }
}
