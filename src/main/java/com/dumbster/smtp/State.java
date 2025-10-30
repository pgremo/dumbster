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

/**
 * SMTP server state.
 */
public enum State {
    /**
     * CONNECT state: waiting for a client connection.
     */
    CONNECT,
    /**
     * GREET state: waiting for a ELHO message.
     */
    GREET,
    /**
     * MAIL state: waiting for the MAIL FROM: command.
     */
    MAIL,
    /**
     * RCPT state: waiting for a RCPT &lt;email address&gt; command.
     */
    RCPT,
    /**
     * Waiting for headers.
     */
    DATA_HDR,
    /**
     * Processing body text.
     */
    DATA_BODY,
    /**
     * End of a message.
     */
    DATA_END,
    /**
     * End of client transmission.
     */
    QUIT;
}
