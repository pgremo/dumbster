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


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class RequestTest {

    @Test
    public void testUnrecognizedCommandConnectState() {
        var request = new Request(Action.UNRECOG, null, State.CONNECT);
        var response = request.execute();
        assertEquals(500, response.code());
    }

    @Test
    public void testUnrecognizedCommandGreetState() {
        var request = new Request(Action.UNRECOG, null, State.GREET);
        var response = request.execute();
        assertEquals(500, response.code());
    }

    @Test
    public void testUnrecognizedCommandMailState() {
        var request = new Request(Action.UNRECOG, null, State.MAIL);
        var response = request.execute();
        assertEquals(500, response.code());
    }

    @Test
    public void testUnrecognizedCommandQuitState() {
        var request = new Request(Action.UNRECOG, null, State.QUIT);
        var response = request.execute();
        assertEquals(500, response.code());
    }

    @Test
    public void testUnrecognizedCommandRcptState() {
        var request = new Request(Action.UNRECOG, null, State.RCPT);
        var response = request.execute();
        assertEquals(500, response.code());
    }

    @Test
    public void testUnrecognizedCommandDataBodyState() {
        var request = new Request(Action.UNRECOG, null, State.DATA_BODY);
        var response = request.execute();
        assertEquals(-1, response.code());
    }

    @Test
    public void testUnrecognizedCommandDataHdrState() {
        var request = new Request(Action.UNRECOG, null, State.DATA_HDR);
        var response = request.execute();
        assertEquals(-1, response.code());
    }


}
