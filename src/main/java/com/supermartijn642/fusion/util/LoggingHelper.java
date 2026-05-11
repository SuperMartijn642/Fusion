package com.supermartijn642.fusion.util;

import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.util.UserErrorException;

import java.util.List;

/**
 * Created 11/05/2026 by SuperMartijn642
 */
public class LoggingHelper {

    public static void logUserError(Throwable error, String header, Object... headerArgs){
        // Format header
        header = header.formatted(headerArgs);
        // Create error message
        int depth = 0;
        StringBuilder message = new StringBuilder(header);
        while(error instanceof UserErrorException || error instanceof JsonParseException){
            message.append('\n').append(" ".repeat(depth++)).append(" |-> ").append(error.getMessage());
            error = error.getCause();
        }
        // Log message
        if(error == null)
            FusionClient.LOGGER.error(message.toString());
        else
            FusionClient.LOGGER.error(message.toString(), error);
    }

    public static void logUserWarnings(List<String> warnings, String header, Object... headerArgs){
        // Format header
        header = header.formatted(headerArgs);
        // Create warning message
        StringBuilder message = new StringBuilder(header);
        for(int i = 0; i < warnings.size(); i++){
            message.append('\n')
                .append(i == warnings.size() - 1 ? " └-> " : " |-> ")
                .append(warnings.get(i));
        }
        // Log message
        FusionClient.LOGGER.warn(message.toString());
    }
}
