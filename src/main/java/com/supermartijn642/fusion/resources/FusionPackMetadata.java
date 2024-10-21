package com.supermartijn642.fusion.resources;

import com.supermartijn642.fusion.FusionClient;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public class FusionPackMetadata {

    private final String minimumVersion;
    private final boolean minVersionSatisfied;
    private final String overridesFolder;

    FusionPackMetadata(String minimumVersion, String overridesFolder){
        this.overridesFolder = overridesFolder;
        // Trim minimum version string
        if(minimumVersion.matches("\\d+\\.\\d+\\.\\d+"))
            this.minimumVersion = minimumVersion;
        else
            this.minimumVersion = minimumVersion.substring(0, minimumVersion.length() - minimumVersion.replaceFirst("\\d+\\.\\d+\\.\\d+\\D", "").length() - 1);
        // Check whether the current version satisfies the minimum version
        String[] currentVersionComponents = FusionClient.getFusionVersion().split("\\.");
        String[] minVersionComponents = this.minimumVersion.split("\\.");
        boolean satisfied = true;
        for(int i = 0; i < 3; i++){
            if(Integer.parseInt(currentVersionComponents[i]) > Integer.parseInt(minVersionComponents[i]))
                break;
            if(Integer.parseInt(currentVersionComponents[i]) < Integer.parseInt(minVersionComponents[i])){
                satisfied = false;
                break;
            }
        }
        this.minVersionSatisfied = satisfied;
    }

    public String getMinimumVersion(){
        return this.minimumVersion;
    }

    public boolean isMinVersionSatisfied(){
        return this.minVersionSatisfied;
    }

    public boolean hasOverridesFolder(){
        return this.overridesFolder != null;
    }

    public String getOverridesFolder(){
        return this.overridesFolder;
    }
}
