package com.dumbster.smtp;

public enum Stateful implements Action {
    /**
     * CONNECT action.
     */
    CONNECT,
    /**
     * EHLO action.
     */
    EHLO,
    /**
     * MAIL action.
     */
    MAIL,
    /**
     * RCPT action.
     */
    RCPT,
    /**
     * DATA action.
     */
    DATA,
    /**
     * "." action.
     */
    DATA_END,
    /**
     * Body text action.
     */
    UNRECOG,
    /**
     * QUIT action.
     */
    QUIT,
    /**
     * Header/body separator action.
     */
    BLANK_LINE;
}
