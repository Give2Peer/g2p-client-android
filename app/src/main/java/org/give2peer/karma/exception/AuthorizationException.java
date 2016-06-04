package org.give2peer.karma.exception;

/**
 * The request was rebuked because the user is not allowed to do that.
 * The user is correctly authenticated when this happens, it's more of a privilege denial.
 */
public class AuthorizationException extends Exception {}
