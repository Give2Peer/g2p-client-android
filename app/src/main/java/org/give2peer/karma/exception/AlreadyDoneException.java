package org.give2peer.karma.exception;

/**
 * The request was rebuked because the user can only do it once.
 * This is BAD design. It should be some form of QuotaException.
 */
public class AlreadyDoneException extends Exception {}
