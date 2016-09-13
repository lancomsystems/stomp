package de.lancom.systems.stomp.core.wire;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SompEncodingTest {

    @Test
    public void encode() {
        final String value = ":\n\r\\:\n\r\\";
        final String actual = StompEncoding.encodeHeaderValue(value);
        final String expected = "\\c\\n\\r\\\\\\c\\n\\r\\\\";
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void decode() {
        final String value = "\\c\\n\\r\\\\\\c\\n\\r\\\\";
        final String actual = StompEncoding.decodeHeaderValue(value);
        final String expected = ":\n\r\\:\n\r\\";
        assertThat(actual, is(equalTo(expected)));
    }
}
