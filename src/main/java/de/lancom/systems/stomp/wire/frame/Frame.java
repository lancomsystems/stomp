package de.lancom.systems.stomp.wire.frame;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import de.lancom.systems.stomp.wire.StompHeader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Frame {

    private final Map<String, String> headers = new HashMap<String, String>();
    private final String action;
    private byte[] body;

    public String getContentType() {
        return this.getHeaders().get(StompHeader.CONTENT_TYPE.value());
    }

    public void setContentType(final String contentType) {
        this.getHeaders().put(StompHeader.CONTENT_TYPE.value(), contentType);
    }

    public Integer getContentLength() {
        final String header = this.getHeaders().get(StompHeader.CONTENT_LENGTH.value());
        if (header != null) {
            return Integer.valueOf(header);
        } else {
            return null;
        }
    }

    public void setContentLength(final Integer contentLength) {
        if (contentLength != null) {
            this.getHeaders().put(StompHeader.CONTENT_LENGTH.value(), contentLength.toString());
        } else {
            this.getHeaders().remove(StompHeader.CONTENT_LENGTH.value());
        }
    }

    public String getBodyAsString() {
        if (body == null) {
            return null;
        } else {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    public void setBodyAsString(final String body) {
        if (body == null) {
            this.body = null;
        } else {
            this.body = body.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String getHeader(final StompHeader header) {
        return getHeader(header.value());
    }

    public String getHeader(final String header) {
        return getHeaders().get(header);
    }

    public void setHeader(final StompHeader header, final String value) {
        this.setHeader(header.value(), value);
    }

    public void setHeader(final String header, final String value) {
        this.getHeaders().put(header, value);
    }

    public <T extends Frame> T copy(final T target) {
        target.getHeaders().clear();
        target.getHeaders().putAll(this.headers);
        target.setBody(this.body);
        return target;
    }

    @Override
    public String toString() {
        return String.format(
                "%s(action=%s, body=%s, headers=%s)",
                this.getClass().getSimpleName(),
                action,
                this.getBodyAsString(),
                headers
        );
    }
}
