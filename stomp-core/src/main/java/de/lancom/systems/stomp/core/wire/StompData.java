package de.lancom.systems.stomp.core.wire;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Base class for stomp frame data.
 */
public class StompData {

    @Getter
    private final Map<String, String> headers = new HashMap<>();

    @Getter
    private byte[] body;

    /**
     * Get body content type.
     *
     * @return content type
     */
    public String getContentType() {
        return this.getHeaders().get(StompHeader.CONTENT_TYPE.value());
    }

    /**
     * Set body content type.
     *
     * @param contentType content type
     */
    public void setContentType(final String contentType) {
        this.getHeaders().put(StompHeader.CONTENT_TYPE.value(), contentType);
    }

    /**
     * Get body content length.
     *
     * @return content length
     */
    public Integer getContentLength() {
        final String header = this.getHeaders().get(StompHeader.CONTENT_LENGTH.value());
        if (header != null) {
            return Integer.valueOf(header);
        } else {
            return null;
        }
    }

    /**
     * Set body content length.
     *
     * @param contentLength content length
     */
    public void setContentLength(final Integer contentLength) {
        if (contentLength != null) {
            this.getHeaders().put(StompHeader.CONTENT_LENGTH.value(), contentLength.toString());
        } else {
            this.getHeaders().remove(StompHeader.CONTENT_LENGTH.value());
        }
    }

    /**
     * Set body as byte array and set content length header.
     *
     * @param body body
     */
    public void setBody(final byte[] body) {
        this.body = body;
        if (body != null) {
            this.setContentLength(body.length);
        } else {
            this.setContentLength(null);
        }
    }

    /**
     * Get body as string using UTF-8 encoding.
     *
     * @return body
     */
    public String getBodyAsString() {
        if (body == null) {
            return null;
        } else {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    /**
     * Set body as string using UTF-8 encoding.
     *
     * @param string body
     */
    public void setBodyAsString(final String string) {
        if (string == null) {
            this.body = null;
        } else {
            this.body = string.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Get header value using {@link StompHeader}.
     *
     * @param header header
     * @return value
     */
    public String getHeader(final StompHeader header) {
        return getHeader(header.value());
    }

    /**
     * Set header using {@link StompHeader}.
     *
     * @param header header
     * @param value value
     */
    public void setHeader(final StompHeader header, final String value) {
        this.setHeader(header.value(), value);
    }

    /**
     * Check wether this frame has the given header.
     *
     * @param header header
     * @return header available
     */
    public boolean hasHeader(final StompHeader header) {
        return this.getHeaders().containsKey(header.value());
    }

    /**
     * Get header value using name.
     *
     * @param header header name
     * @return value
     */
    public String getHeader(final String header) {
        return getHeaders().get(header);
    }

    /**
     * Set header value using name.
     *
     * @param header header name
     * @param value value
     */
    public void setHeader(final String header, final String value) {
        this.getHeaders().put(header, value);
    }

    /**
     * Check wether this frame has the given header.
     *
     * @param header header
     * @return header available
     */
    public boolean hasHeader(final String header) {
        return this.getHeaders().containsKey(header);
    }

    /**
     * Copy current instance values to another frame.
     *
     * @param target target frame
     * @param <T> target type
     * @return target
     */
    public <T extends StompData> T copy(final T target) {
        target.getHeaders().clear();
        target.getHeaders().putAll(this.getHeaders());
        target.setBody(this.getBody());
        return target;
    }

    /**
     * Copy current instance values to another frame.
     *
     * @param targetClass target frame class
     * @param <T> target type
     * @return target
     */
    public <T extends StompData> T copy(final Class<T> targetClass) {

        try {
            final T target = targetClass.newInstance();
            return this.copy(target);
        } catch (final Exception ex) {
            throw new RuntimeException("Could not copy stomp values", ex);
        }

    }
}
