package net.emc.emce;

import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;
import me.sargunvohra.mcmods.autoconfig1u.annotation.ConfigEntry;
import net.emc.emce.utils.ModUtils;

@Config(name = "emc-essentials")
public class ModConfig implements ConfigData
{
    @ConfigEntry.Gui.CollapsibleObject

    // Calls for each config class needed for this mod
	public General general = new General();
    public Townless townless = new Townless();
    public Nearby nearby = new Nearby();
    public Commands commands = new Commands();
    public API api = new API();

    // Config variables for general tab
    public static class General {
        public boolean enableMod = true;
        public boolean emcOnly = true;
        public boolean disableVoxelMap = true;
    }

    // Config variables for townless tab
    public static class Townless {
        public boolean enabled = true;
        public boolean presetPositions = true;

        public ModUtils.State positionState = ModUtils.State.TOP_LEFT;

        public int xPos = 1;
        public int yPos = 16;
        public int maxLength = 0; // < 1 = No limit

        public String headingTextColour = "BLUE";
        public String playerTextColour = "BLUE";
    }

    // Config variables for nearby tab
    public static class Nearby {
        public boolean enabled = true;
        public boolean showRank = false;
        public boolean presetPositions = true;

        public ModUtils.State positionState = ModUtils.State.TOP_RIGHT;

        public int xPos = 100;
        public int yPos = 16;

        public String headingTextColour = "GOLD";
        public String playerTextColour = "GOLD";

        // Independent scaling - each axis can be same or different.
        public int xBlocks = 500;
        public int zBlocks = 500;
    }

    // Config variables for commands
    public static class Commands {
        public String townlessTextColour = "LIGHT_PURPLE";
        public String townInfoTextColour = "GREEN";
        public String nationInfoTextColour = "AQUA";
    }

    // Config variables for API
    public static class API {
        public int queueInterval = 5;
        public int nearbyInterval = 10;
        public int townlessInterval = 60;
        public int residentInfoInterval = 60;
        public int townNationInfoInterval = 120;
    }
}