package org.give2peer.karma.response;

import org.give2peer.karma.entity.User;


/**
 * An element of the ErrorResponse below.
 *
 * Unsure how we should handle redundant information with the HTTP status codes.
 *
 */
class Error
{

    int code;
    String message;

    /** @return the custom (not http) error code that the server sent back, one of `self::` */
    public int getCode()                   { return code;                                          }
    public void setCode(int code)          { this.code = code;                                     }
    /** @return a message (in english) the server may have provided */
    public String getMessage()             { return message;                                       }
    public void setMessage(String message) { this.message = message;                               }
}

/**
 * Populated by the responses sent back when the HTTP status code is >= 400.
 *
 * /!\
 *     Documentation server-side about this is non-existent.
 */
public class ErrorResponse
{

    static int UNAVAILABLE_USERNAME = 1; // we use this
    static int BANNED_FOR_ABUSE     = 2;
    static int UNSUPPORTED_FILE     = 3;
    static int NOT_AUTHORIZED       = 4; // we catch the 401 HTTP status code so this is never used
    static int SYSTEM_ERROR         = 5;
    static int BAD_LOCATION         = 6;
    static int UNAVAILABLE_EMAIL    = 7; // use use this
    static int EXCEEDED_QUOTA       = 8; // we catch the 429 HTTP status code so this is never used
    static int BAD_USERNAME         = 9; // we use this

    Error error;

    /**
     * An Error object (defined above).
     * Has a code and a message.
     * May not be present. ('cause of legacy code)
     * paper fix, maybe we can ensure that this MUST be here or we fail hard
     * => server work, that
     */
    public Error getError() { return error; }

    public boolean hasError() { return null != error; }

    public int getErrorCode()
    {
        if (null != error) {
            return error.getCode();
        } else {
            return 0;
        }
    }

    public String getMessage()
    {
        if (hasError()) {
            return error.getMessage();
        } else {
            return "No error was attached to this error response. Something is amiss !";
        }
    }

    public boolean isBadUsername()
    {
        if (hasError()) {
            return getErrorCode() == BAD_USERNAME ||
                   getErrorCode() == UNAVAILABLE_USERNAME;
        } else {
            return false;
        }
    }

    public boolean isBadEmail()
    {
        if (hasError()) {
            return getErrorCode() == UNAVAILABLE_EMAIL;
        } else {
            return false;
        }
    }

}
