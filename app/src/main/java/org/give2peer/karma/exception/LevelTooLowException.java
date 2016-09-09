package org.give2peer.karma.exception;

/**
 * The request was rebuked because the user was too low level.
 */
public class LevelTooLowException extends Exception {

    // constructor never used, we need to refactor to allow error responses to provide data
//    public LevelTooLowException(int levelRequired) {
//        super();
//        this.levelRequired = levelRequired;
//    }
//    int levelRequired;

}
