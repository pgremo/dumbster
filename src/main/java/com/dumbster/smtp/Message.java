/*
 * Dumbster - a dummy SMTP server
 * Copyright 2016 Joachim Nicolay
 * Copyright 2004 Jason Paul Kitchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dumbster.smtp;

import java.util.*;

import static com.dumbster.smtp.State.*;
import static java.util.Collections.*;

/**
 * Container for a complete SMTP message - headers and message body.
 */
public class Message {
    /**
     * Headers: Map of List of String hashed on header name.
     */
    private final Map<String, List<String>> headers;
    /**
     * Message body.
     */
    private final StringBuilder body;

    /**
     * Constructor. Initializes headers Map and body buffer.
     */
    public Message() {
        headers = new LinkedHashMap<>(10);
        body = new StringBuilder();
    }

    /**
     * Update the headers or body depending on the Response object and line of input.
     *
     * @param response Response object
     * @param params   remainder of input line after SMTP command has been removed
     */
    public void store(Response response, String params) {
        if (params == null) return;
        if (response.nextState() == DATA_HDR) {
            var headerNameEnd = params.indexOf(':');
            if (headerNameEnd >= 0) {
                var name = params.substring(0, headerNameEnd).trim();
                var value = params.substring(headerNameEnd + 1).trim();
                addHeader(name, value);
            }
        } else if (response.nextState() == DATA_BODY) {
            body.append(params);
        }
    }

    /**
     * Get an Iterator over the header names.
     *
     * @return an Iterator over the set of header names (String)
     */
    public Set<String> getHeaderNames() {
        return unmodifiableSet(new LinkedHashSet<>(headers.keySet()));
    }

    /**
     * Get the value(s) associated with the given header name.
     *
     * @param name header name
     * @return value(s) associated with the header name
     */
    public List<String> getHeaderValues(String name) {
        var values = headers.get(name);
        return values == null ? emptyList() : unmodifiableList(values);
    }

    /**
     * Get the first values associated with a given header name.
     *
     * @param name header name
     * @return first value associated with the header name
     */
    public String getHeaderValue(String name) {
        var values = headers.get(name);
        return values == null ? null : values.getFirst();
    }

    /**
     * Get the message body.
     *
     * @return message body
     */
    public String getBody() {
        return body.toString();
    }

    /**
     * Adds a header to the Map.
     *
     * @param name  header name
     * @param value header value
     */
    private void addHeader(String name, String value) {
        headers.computeIfAbsent(name, _ -> new LinkedList<>()).add(value);
    }

    /**
     * String representation of the Message.
     *
     * @return a String
     */
    @Override
    public String toString() {
        var msg = new StringBuilder();
        for (var entry : headers.entrySet()) {
            for (var value : entry.getValue()) {
                msg.append(entry.getKey());
                msg.append(": ");
                msg.append(value);
                msg.append('\n');
            }
        }
        msg.append('\n');
        msg.append(body);
        msg.append('\n');
        return msg.toString();
    }
}
