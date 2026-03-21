package com.supermartijn642.fusion.util;

import com.supermartijn642.fusion.Fusion;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public class IdentifierUtil {

    /**
     * Checks whether the given namespace contains illegal characters
     * @param namespace namespace to be checked
     * @return {@code true} if the namespace is valid
     */
    public static boolean isValidNamespace(String namespace){
        return namespace != null && namespace.length() > 0 && namespace.matches("[a-z0-9_.-]*");
    }

    /**
     * Checks whether the given path contains illegal characters
     * @param path identifier path to be checked
     * @return {@code true} if the path is valid
     */
    public static boolean isValidPath(String path){
        return path != null && path.length() > 0 && path.matches("[a-z0-9_./-]*");
    }

    /**
     * Checks whether given identifier contains illegal characters
     * @param namespace identifier namespace to be checked
     * @param path      identifier path to be checked
     * @return {@code true} if the identifier is valid
     */
    public static boolean isValidIdentifier(String namespace, String path){
        return isValidNamespace(namespace) && isValidPath(path);
    }

    /**
     * Checks whether given identifier contains illegal characters
     * @param identifier identifier to be checked
     * @return {@code true} if the identifier is valid
     */
    public static boolean isValidIdentifier(String identifier){
        String[] parts = identifier.split(":");
        return (parts.length == 1 && isValidPath(parts[0])) || (parts.length == 2 && isValidIdentifier(parts[0], parts[1]));
    }

    /**
     * Converts the given string into a {@link ResourceLocation} instance. If no namespace is specified, the 'fusion' namespace will be used
     */
    public static ResourceLocation withFusionNamespace(String identifier){
        String[] parts = identifier.split(":");
        return parts.length == 1 ?
            Fusion.identifier(parts[0]) :
            new ResourceLocation(identifier);
    }

    /**
     * Creates a new identifier with the same namespace and the same path with the given suffix appended.
     */
    public static ResourceLocation withSuffix(ResourceLocation identifier, String suffix){
        return new ResourceLocation(identifier.getNamespace(), identifier.getPath() + suffix);
    }
}
