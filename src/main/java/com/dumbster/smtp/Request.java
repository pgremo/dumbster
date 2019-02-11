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
class Request {
    /**
     * SMTP action received from client.
     */
    private Action action;
    /**
     * Current state of the SMTP state table.
     */
    private State state;
    /**
     * Additional information passed from the client with the SMTP action.
     */
    String params;

    /**
     * Create a new SMTP client request.
     *
     * @param actionType type of action/command
     * @param params     remainder of command line once command is removed
     * @param state      current SMTP server state
     */
    Request(Action actionType, String params, State state) {
        this.action = actionType;
        this.state = state;
        this.params = params;
    }

    /**
     * Execute the SMTP request returning a response. This method models the state transition table for the SMTP server.
     *
     * @return reponse to the request
     */
    Response execute() {
        Response response;
        if (action.isStateless()) {
            if (Action.EXPN == action) {
                response = new Response(252, "Not supported", this.state);
            } else if (Action.HELP == action) {
                response = new Response(211, "No help available", this.state);
            } else if (Action.NOOP == action) {
                response = new Response(250, "OK", this.state);
            } else if (Action.VRFY == action) {
                response = new Response(252, "Not supported", this.state);
            } else if (Action.RSET == action) {
                response = new Response(250, "OK", State.GREET);
            } else {
                response = new Response(500, "Command not recognized", this.state);
            }
        } else { // Stateful commands
            if (Action.CONNECT == action) {
                if (State.CONNECT == state) {
                    response = new Response(220, "localhost Dumbster SMTP service ready", State.GREET);
                } else {
                    response = new Response(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (Action.EHLO == action) {
                if (State.GREET == state) {
                    response = new Response(250, "OK", State.MAIL);
                } else {
                    response = new Response(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (Action.MAIL == action) {
                if (State.MAIL == state || State.QUIT == state) {
                    response = new Response(250, "OK", State.RCPT);
                } else {
                    response = new Response(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (Action.RCPT == action) {
                if (State.RCPT == state) {
                    response = new Response(250, "OK", this.state);
                } else {
                    response = new Response(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (Action.DATA == action) {
                if (State.RCPT == state) {
                    response = new Response(354, "Start mail input; end with <CRLF>.<CRLF>", State.DATA_HDR);
                } else {
                    response = new Response(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (Action.UNRECOG == action) {
                if (State.DATA_HDR == state || State.DATA_BODY == state) {
                    response = new Response(-1, "", this.state);
                } else {
                    response = new Response(500, "Command not recognized", this.state);
                }
            } else if (Action.DATA_END == action) {
                if (State.DATA_HDR == state || State.DATA_BODY == state) {
                    response = new Response(250, "OK", State.QUIT);
                } else {
                    response = new Response(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (Action.BLANK_LINE == action) {
                if (State.DATA_HDR == state) {
                    response = new Response(-1, "", State.DATA_BODY);
                } else if (State.DATA_BODY == state) {
                    response = new Response(-1, "", this.state);
                } else {
                    response = new Response(503, "Bad sequence of commands: " + action, this.state);
                }
            } else if (Action.QUIT == action) {
                if (State.QUIT == state) {
                    response = new Response(221, "localhost Dumbster service closing transmission channel", State.CONNECT);
                } else {
                    response = new Response(503, "Bad sequence of commands: " + action, this.state);
                }
            } else {
                response = new Response(500, "Command not recognized", this.state);
            }
        }
        return response;
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
                action = Action.DATA_END;
            } else if (s.length() < 1) {
                action = Action.BLANK_LINE;
            } else {
                action = Action.UNRECOG;
                params = s;
            }
        } else if (state == State.DATA_BODY) {
            if (s.equals(".")) {
                action = Action.DATA_END;
            } else {
                action = Action.UNRECOG;
                if (s.length() < 1) {
                    params = "\n";
                } else {
                    params = s;
                }
            }
        } else {
            String su = s.toUpperCase();
            if (su.startsWith("EHLO ") || su.startsWith("HELO")) {
                action = Action.EHLO;
                params = s.substring(5);
            } else if (su.startsWith("MAIL FROM:")) {
                action = Action.MAIL;
                params = s.substring(10);
            } else if (su.startsWith("RCPT TO:")) {
                action = Action.RCPT;
                params = s.substring(8);
            } else if (su.startsWith("DATA")) {
                action = Action.DATA;
            } else if (su.startsWith("QUIT")) {
                action = Action.QUIT;
            } else if (su.startsWith("RSET")) {
                action = Action.RSET;
            } else if (su.startsWith("NOOP")) {
                action = Action.NOOP;
            } else if (su.startsWith("EXPN")) {
                action = Action.EXPN;
            } else if (su.startsWith("VRFY")) {
                action = Action.VRFY;
            } else if (su.startsWith("HELP")) {
                action = Action.HELP;
            } else {
                action = Action.UNRECOG;
            }
        }

        return new Request(action, params, state);
    }
}
