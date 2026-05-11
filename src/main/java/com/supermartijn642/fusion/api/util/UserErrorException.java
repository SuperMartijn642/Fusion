package com.supermartijn642.fusion.api.util;

/**
 * Error type for expected problems or warnings that should be logged to the user.
 * <p>
 * Created 07/05/2026 by SuperMartijn642
 */
public final class UserErrorException extends Exception {

    public UserErrorException(String message, Throwable cause){
        super(message, cause);
    }

    public UserErrorException(String message){
        super(message);
    }
}
