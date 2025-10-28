package com.dumbster.smtp;

public enum Stateless implements Action {
    /**
     * Stateless RSET action.
     */
    RSET,
    /**
     * Stateless VRFY action.
     */
    VRFY,
    /**
     * Stateless EXPN action.
     */
    EXPN,
    /**
     * Stateless HELP action.
     */
    HELP,
    /**
     * Stateless NOOP action.
     */
    NOOP;
}
