package org.give2peer.karma.response;

import org.give2peer.karma.entity.User;


/**
 * An element of the ErrorResponse below.
 *
 * Unsure how we should handle redundant information with the HTTP status codes.
 */
class Error
{
    String code;
    String message;

    /** @return the custom (not http) error code that the server sent back, one of below */
    public String getCode()                { return code;                                          }
    public void setCode(String code)       {   this.code = code;                                   }
    /** @return a message (in english) the server may have provided */
    public String getMessage()             { return message;                                       }
    public void setMessage(String message) {   this.message = message;                             }
}

/**
 * Populated by the responses sent back when the HTTP status code is >= 400.
 *
 *
 * The typical JSON body sent back by the webserver when there's an error looks like this :
 * ```
 * {
 *     "error": {
 *         "code": "api.error.item.picture.thumbnail",
 *         "message": "The thumbnail creation failed."
 *     }
 * }
 * ```
 * Note that the error message will already be localized according to the 'Accept-Language' header.
 *
 */
public class ErrorResponse
{

    static String UNKNOWN           = "api.error.unknown";

//    static String BAD_USERNAME      = "api.error.";

//    static int UNAVAILABLE_USERNAME =  1; // we use this
//    static int BANNED_FOR_ABUSE     =  2;
//    static int UNSUPPORTED_FILE     =  3;
//    static int NOT_AUTHORIZED       =  4; // we catch the 401 HTTP status code so this is never used
//    static int SYSTEM_ERROR         =  5;
//    static int BAD_LOCATION         =  6;
//    static int UNAVAILABLE_EMAIL    =  7; // use use this
//    static int EXCEEDED_QUOTA       =  8; // we catch the 429 HTTP status code so this is never used
//    static int BAD_USER_ID          = 10;
//    static int BAD_ITEM_TYPE        = 11;
//    static int LEVEL_TOO_LOW        = 12;
//    static int ALREADY_DONE         = 13;

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

    public String getErrorCode() {
        if (null != error) { return error.getCode(); } else { return UNKNOWN; }
    }

    public String getMessage() {
        if (hasError()) {
            return error.getMessage();
        } else {
            // todo: localize (low priority)
            return "We don't know what happened. Something is amiss!";
        }
    }

//    public boolean isBadUsername() {
//        return hasError() &&
//                (getErrorCode().equals(BAD_USERNAME) || getErrorCode() == UNAVAILABLE_USERNAME);
//    }
//
//    public boolean isBadEmail() {
//        return hasError() && getErrorCode() == UNAVAILABLE_EMAIL;
//    }
//
//    public boolean isLevelTooLow() {
//        return hasError() && getErrorCode() == LEVEL_TOO_LOW;
//    }
//
//    public boolean isAlreadyDone() {
//        return hasError() && getErrorCode() == ALREADY_DONE;
//    }


}
