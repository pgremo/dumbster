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
 * Contains an SMTP client request. Handles state transitions using the following state transition table.
 * <PRE>
 * -----------+-------------------------------------------------------------------------------------------------
 * |                                 State
 * Action    +-------------+-----------+-----------+--------------+---------------+---------------+------------
 * | CONNECT     | GREET     | MAIL      | RCPT         | DATA_HDR      | DATA_BODY     | QUIT
 * -----------+-------------+-----------+-----------+--------------+---------------+---------------+------------
 * connect    | 220/GREET   | 503/GREET | 503/MAIL  | 503/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 503/QUIT
 * ehlo       | 503/CONNECT | 250/MAIL  | 503/MAIL  | 503/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 503/QUIT
 * mail       | 503/CONNECT | 503/GREET | 250/RCPT  | 503/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 250/RCPT
 * rcpt       | 503/CONNECT | 503/GREET | 503/MAIL  | 250/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 503/QUIT
 * data       | 503/CONNECT | 503/GREET | 503/MAIL  | 354/DATA_HDR | 503/DATA_HDR  | 503/DATA_BODY | 503/QUIT
 * data_end   | 503/CONNECT | 503/GREET | 503/MAIL  | 503/RCPT     | 250/QUIT      | 250/QUIT      | 503/QUIT
 * unrecog    | 500/CONNECT | 500/GREET | 500/MAIL  | 500/RCPT     | ---/DATA_HDR  | ---/DATA_BODY | 500/QUIT
 * quit       | 503/CONNECT | 503/GREET | 503/MAIL  | 503/RCPT     | 503/DATA_HDR  | 503/DATA_BODY | 250/CONNECT
 * blank_line | 503/CONNECT | 503/GREET | 503/MAIL  | 503/RCPT     | ---/DATA_BODY | ---/DATA_BODY | 503/QUIT
 * rset       | 250/GREET   | 250/GREET | 250/GREET | 250/GREET    | 250/GREET     | 250/GREET     | 250/GREET
 * vrfy       | 252/CONNECT | 252/GREET | 252/MAIL  | 252/RCPT     | 252/DATA_HDR  | 252/DATA_BODY | 252/QUIT
 * expn       | 252/CONNECT | 252/GREET | 252/MAIL  | 252/RCPT     | 252/DATA_HDR  | 252/DATA_BODY | 252/QUIT
 * help       | 211/CONNECT | 211/GREET | 211/MAIL  | 211/RCPT     | 211/DATA_HDR  | 211/DATA_BODY | 211/QUIT
 * noop       | 250/CONNECT | 250/GREET | 250/MAIL  | 250/RCPT     | 250|DATA_HDR  | 250/DATA_BODY | 250/QUIT
 * </PRE>
 */
record Request(Action action, String params, State state) {
    /**
     * Execute the SMTP request returning a response. This method models the state transition table for the SMTP server.
     *
     * @return response to the request
     */
    Context execute(Context context) {
        Response response;

        if (action instanceof Stateless) {
            response = switch (action) {
                case Stateless.EXPN, Stateless.VRFY -> new Response(252, "Not supported", this.state);
                case Stateless.HELP -> new Response(211, "No help available", this.state);
                case Stateless.NOOP -> new Response(250, "OK", this.state);
                case Stateless.RSET -> new Response(250, "OK", State.GREET);
                default -> new Response(500, "Command not recognized", this.state);
            };
        } else {
            switch (action) {
                case Stateful.CONNECT -> {
                    if (State.CONNECT == state) {
                        response = new Response(220, "localhost Dumbster SMTP service ready", State.GREET);
                    } else {
                        response = new Response(503, "Bad sequence of commands: %s".formatted(action), this.state);
                    }
                }
                case Stateful.EHLO -> {
                    if (State.GREET == state) {
                        response = new Response(250, "OK", State.MAIL);
                    } else {
                        response = new Response(503, "Bad sequence of commands: %s".formatted(action), this.state);
                    }
                }
                case Stateful.MAIL -> {
                    if (State.MAIL == state || State.DATA_END == state) {
                        response = new Response(250, "OK", State.RCPT);
                    } else {
                        response = new Response(503, "Bad sequence of commands: %s".formatted(action), this.state);
                    }
                }
                case Stateful.RCPT -> {
                    if (State.RCPT == state) {
                        response = new Response(250, "OK", this.state);
                    } else {
                        response = new Response(503, "Bad sequence of commands: %s".formatted(action), this.state);
                    }
                }
                case Stateful.DATA -> {
                    if (State.RCPT == state) {
                        response = new Response(354, "Start mail input; end with <CRLF>.<CRLF>", State.DATA_HDR);
                    } else {
                        response = new Response(503, "Bad sequence of commands: %s".formatted(action), this.state);
                    }
                }
                case Stateful.UNRECOG -> {
                    if (State.DATA_HDR == state || State.DATA_BODY == state) {
                        response = new Response(-1, "", this.state);
                    } else {
                        response = new Response(500, "Command not recognized", this.state);
                    }
                }
                case Stateful.DATA_END -> {
                    if (State.DATA_HDR == state || State.DATA_BODY == state) {
                        context.messages().add(context.message());
                        response = new Response(250, "OK", State.DATA_END);
                        context = new Context(context.messages(), new Message(), response);
                    } else {
                        response = new Response(503, "Bad sequence of commands: %s".formatted(action), this.state);
                    }
                }
                case Stateful.BLANK_LINE -> {
                    if (State.DATA_HDR == state) {
                        response = new Response(-1, "", State.DATA_BODY);
                    } else if (State.DATA_BODY == state) {
                        response = new Response(-1, "", this.state);
                    } else {
                        response = new Response(503, "Bad sequence of commands: %s".formatted(action), this.state);
                    }
                }
                case Stateful.QUIT -> {
                    response = new Response(221, "localhost Dumbster service closing transmission channel", State.CONNECT);
                }
                default -> response = new Response(500, "Command not recognized", this.state);
            }
        }
        context.message().store(response.nextState(), params());
        return new Context(context.messages(), context.message(), response);
    }

    /**
     * Create an SMTP request object given a line of the input stream from the client and the current internal state.
     *
     * @param s     line of input
     * @param state current state
     * @return a populated Request object
     */
    static Request createRequest(String s, State state) {
        Action action;
        String params = null;

        if (state == State.DATA_HDR) {
            if (s.equals(".")) {
                action = Stateful.DATA_END;
            } else if (s.isEmpty()) {
                action = Stateful.BLANK_LINE;
            } else {
                action = Stateful.UNRECOG;
                params = s;
            }
        } else if (state == State.DATA_BODY) {
            if (s.equals(".")) {
                action = Stateful.DATA_END;
            } else {
                action = Stateful.UNRECOG;
                params = s.isEmpty() ? "\n" : s;
            }
        } else {
            if (matches(s, "EHLO") || matches(s, "HELO")) {
                action = Stateful.EHLO;
                params = s.substring(5);
            } else if (matches(s, "MAIL FROM:")) {
                action = Stateful.MAIL;
                params = s.substring(10);
            } else if (matches(s, "RCPT TO:")) {
                action = Stateful.RCPT;
                params = s.substring(8);
            } else if (matches(s, "DATA")) {
                action = Stateful.DATA;
            } else if (matches(s, "QUIT")) {
                action = Stateful.QUIT;
            } else if (matches(s, "RSET")) {
                action = Stateless.RSET;
            } else if (matches(s, "NOOP")) {
                action = Stateless.NOOP;
            } else if (matches(s, "EXPN")) {
                action = Stateless.EXPN;
            } else if (matches(s, "VRFY")) {
                action = Stateless.VRFY;
            } else if (matches(s, "HELP")) {
                action = Stateless.HELP;
            } else if (matches(s, "CONNECT")) {
                action = Stateful.CONNECT;
            } else {
                action = Stateful.UNRECOG;
            }
        }

        return new Request(action, params, state);
    }

    private static boolean matches(String target, String pattern) {
        return target.regionMatches(true, 0, pattern, 0, pattern.length());
    }
}
