package net.emc.emce;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.GsonConfigSerializer;
import net.emc.emce.utils.ConfigUtils;
import net.emc.emce.utils.ModUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import org.apache.logging.log4j.LogManager;
import org.lwjgl.glfw.GLFW;

public class EMCE implements ModInitializer
{
    int townlessPlayerOffset, nearbyPlayerOffset;

    public static String[] colors;

    public static Integer queue = null;

    public static String clientName = "";
    public static String clientTownName = "";
    public static String clientNationName = "";

    public static MinecraftClient client;
    public static Screen screen;
    public static ModConfig config;

    public static boolean shouldRender = false;
    public static boolean debugModeEnabled = false;

    public static JsonArray townless, nearby, allNations, allTowns;

    KeyBinding configKeybind;

    @Override
    public void onInitialize() // [Original Dev Comment] Called when Minecraft starts.
    {
        // Send message informing the player that EMCE cannot be used server side.
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) 
        {
            LogManager.getLogger().error("EMC Essentials is not usable as a server side mod; it can only be used on the client.");
            return;
        }

        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

        // This variable is used in the ClientTickEvents.END_CLIENT_TICK.register 20 lines below to trigger opening the config menu when F4 is pressed.
        configKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open Config Menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F4, "EarthMC Essentials"));

        // This displays all of the text colors that the mod uses in string form.
        colors = new String[] { "BLUE", "DARK_BLUE", "GREEN", "DARK_GREEN", "AQUA", "DARK_AQUA", "RED", "DARK_RED",
                                "LIGHT_PURPLE", "DARK_PURPLE", "YELLOW", "GOLD", "GRAY", "DARK_GRAY", "BLACK", "WHITE" };

        // A json array that stores townless players.
        townless = new JsonArray();

        // A json array that stores a list of nations
        allNations = new JsonArray();

        // A json array that stores a list 
        allTowns = new JsonArray();

        // A json array that stores the nearby players
        nearby = new JsonArray(); // [Original Dev Comment] 'new' because the client cant be near anyone yet.

        //  Register client-side commands.
        Commands.registerCommands();

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            // Checked if F4 was pressed
            if (configKeybind.wasPressed())
            {
                // Sets the value for the config screen
                screen = ConfigUtils.getConfigBuilder().build();
                // Call that opens the config screen
                client.openScreen(screen);
		    }
        });

        // [Original Dev Comment] #region HudRenderCallback
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) ->
        {
            // Exits if the mod is not enabled or is set to not render
            if (!config.general.enableMod || !shouldRender) return;
            // Call to render the text on screen
            final TextRenderer renderer = client.textRenderer;

            // Option in the F4 menu to enable showing townless players
            ModUtils.State townlessState = config.townless.positionState;
            // Option in the F4 menu to enable showing nearby players
            ModUtils.State nearbyState = config.nearby.positionState;

            // Call if enabled
            if (config.townless.enabled)
            {
                if (!config.townless.presetPositions)
                {
                    // Position of the first player, who determines where the list will be.
                    townlessPlayerOffset = config.townless.yPos;
                    // Formatting the color of the townless players on screen to the selected color
                    Formatting townlessTextFormatting = Formatting.byName(config.townless.headingTextColour);
                    // This calls the text from the en_us.json file to display as "Townless Players: [(size of the array of townless players)]"
                    MutableText townlessText = new TranslatableText("text_townless_header", townless.size()).formatted(townlessTextFormatting);
                    // [Original Dev Comment] Draw heading.
                    // This means to render the text on screen at the specified position on the screen (but not call to display, this is just render drawing setup)
                    renderer.drawWithShadow(matrixStack, townlessText, config.townless.xPos, config.townless.yPos - 15, 16777215);
                    // If there is 1 or more townless players...
                    if (townless.size() >= 1)
                    {
                        // Call the loop to iterate through the townless array
                        for (int i = 0; i < townless.size(); i++)
                        {
                            // Color config option, again
                            Formatting playerTextFormatting = Formatting.byName(config.townless.playerTextColour);

                            if (config.townless.maxLength >= 1)
                            {
                                if (i >= config.townless.maxLength)
                                {
                                    /**
                                     * If it weren't for the use of townless.size()-i here, I think using a different type of loop (especially ranged based)
                                     * would simply things a lot. I believe there still might be a more organized way to do this loop, but I am unaware of how it 
                                     * would affect performance. I doubt it would cause issues since this loop isn't expected to be huge, but if there's anything I've
                                     * learned from playing Minecraft for 10 years and EMC for 2 years, it's that good performance should never, ever be
                                     * a given. Minecraft's source code, after all, is very clunky.
                                     */

                                     // Anyways, this displays "And %d more..." if the number of townless players exceeds the max length of players configed to display.
                                    MutableText remainingText = new TranslatableText("text_townless_remaining", townless.size()-i).formatted(playerTextFormatting);
                                    
                                    // Render the text that lists the names of townless players
                                    renderer.drawWithShadow(matrixStack, remainingText, config.townless.xPos, townlessPlayerOffset, 16777215);
                                    break;
                                }
                            }
                            // The name of the townless player (or each townless player via the loop)
                            final JsonObject currentPlayer = (JsonObject) townless.get(i);

                            // To simplify, this text "translates" from player name to 
                            MutableText playerName = new TranslatableText(currentPlayer.get("name").getAsString()).formatted(playerTextFormatting);

                            // Render draw the player
                            renderer.drawWithShadow(matrixStack, playerName, config.townless.xPos, townlessPlayerOffset, 16777215);

                            // Add offset for the next player.
                            townlessPlayerOffset += 10;
                        }

                    }
                }
                else // [Original Dev Comment] No advanced positioning, use preset states.
                {
                    int townlessLongest, nearbyLongest;

                    // Find longest-length element currently in the townless and nearby arrays
                    townlessLongest = Math.max(ModUtils.getLongestElement(townless), ModUtils.getTextWidth(new TranslatableText("text_townless_header", townless.size())));
                    nearbyLongest = Math.max(ModUtils.getNearbyLongestElement(nearby), ModUtils.getTextWidth(new TranslatableText("text_nearby_header", nearby.size())));

                    // This switch statement handles positioning of the text on screen for townless and nearby players depending on the option chosen in the F4 menu.
                    switch(townlessState)
                    {
                        case TOP_MIDDLE:
                        {
                            if (nearbyState.equals(ModUtils.State.TOP_MIDDLE))
                                townlessState.setX(ModUtils.getWindowWidth() / 2 - (townlessLongest + nearbyLongest) / 2 );
                            else
                                townlessState.setX(ModUtils.getWindowWidth() / 2 - townlessLongest / 2);

                            townlessState.setY(16);
                            break;
                        }
                        case TOP_RIGHT:
                        {
                            townlessState.setX(ModUtils.getWindowWidth() - townlessLongest - 5);
                            if (client.player != null) townlessState.setY(ModUtils.getStatusEffectOffset(client.player.getStatusEffects()));
                            break;
                        }
                        case LEFT:
                        {
                            townlessState.setX(5);
                            townlessState.setY(ModUtils.getWindowHeight() / 2 - ModUtils.getTownlessArrayHeight(townless, config.townless.maxLength) / 2);
                            break;
                        }
                        case RIGHT:
                        {
                            townlessState.setX(ModUtils.getWindowWidth() - townlessLongest - 5);
                            townlessState.setY(ModUtils.getWindowHeight() / 2 - ModUtils.getTownlessArrayHeight(townless, config.townless.maxLength) / 2);
                            break;
                        }
                        case BOTTOM_RIGHT:
                        {
                            townlessState.setX(ModUtils.getWindowWidth() - townlessLongest - 5);
                            townlessState.setY(ModUtils.getWindowHeight() - ModUtils.getTownlessArrayHeight(townless, config.townless.maxLength) - 22);
                            break;
                        }
                        case BOTTOM_LEFT:
                        {
                            townlessState.setX(5);
                            townlessState.setY(ModUtils.getWindowHeight() - ModUtils.getTownlessArrayHeight(townless, config.townless.maxLength) - 22);
                            break;
                        }
                        default: // [Original Dev Comment] Defaults to top left
                        {
                            townlessState.setX(5);
                            townlessState.setY(16);
                            break;
                        }
                    }

                    /**
                     * I think the exact same values are called on lines 117 and 119 above and don't change from any other calls. 
                     * As far as I know, these are equivalent values that do not change, so it would probably be more practical to 
                     * just use one variable for both on line 111, right below the if(config.townless.enabled) statement.  
                     * I could be wrong about this (though I'm not risking testing it on EMC, I'm not risking getting banned
                     * for having a questionable mod detected).
                     **/
                    Formatting townlessTextFormatting = Formatting.byName(config.townless.headingTextColour);
                    MutableText townlessText = new TranslatableText("text_townless_header", townless.size()).formatted(townlessTextFormatting);

                    // Draw heading.
                    renderer.drawWithShadow(matrixStack, townlessText, townlessState.getX(), townlessState.getY() - 10, 16777215);

                    /**
                     * This is extremely similar, if not the exact same code as lines 125-167, but by using the townlessState variable it
                     * accounts for positioning.
                     */
                    if (townless.size() >= 1)
                    {
                        for (int i = 0; i < townless.size(); i++)
                        {
                            Formatting playerTextFormatting = Formatting.byName(config.townless.playerTextColour);

                            if (config.townless.maxLength >= 1)
                            {
                                if (i >= config.townless.maxLength)
                                {
                                    MutableText remainingText = new TranslatableText("text_townless_remaining", townless.size()-i).formatted(playerTextFormatting);
                                    renderer.drawWithShadow(matrixStack, remainingText, townlessState.getX(), townlessState.getY() + i*10, 16777215);
                                    break;
                                }
                            }

                            final JsonObject currentPlayer = (JsonObject) townless.get(i);
                            MutableText playerName = new TranslatableText(currentPlayer.get("name").getAsString()).formatted(playerTextFormatting);

                            renderer.drawWithShadow(matrixStack, playerName, townlessState.getX(), townlessState.getY() + i*10, 16777215);
                        }
                    }
                }
            }

            /**
             * This next section deals with nearby players. I will only cover the functionality behind locating these players and position mechanics,
             * not rendering the text after getting the player names, as it uses the same functionality as the townless rendering.
             */
            if (config.nearby.enabled)
            {
                if (!config.nearby.presetPositions) // [Original Dev Comment] Not using preset positions
                {
                    // [Original Dev Comment] Position of the first player, who determines where the list will be.
                    nearbyPlayerOffset = config.nearby.yPos;

                    Formatting nearbyTextFormatting = Formatting.byName(config.nearby.headingTextColour);

                    // From en_us.json: "text_nearby_header": "Nearby Players [number of nearby players]"
                    MutableText nearbyText = new TranslatableText("text_nearby_header", nearby.size()).formatted(nearbyTextFormatting);

                    // [Original Dev Comment] Draw heading.
                    renderer.drawWithShadow(matrixStack, nearbyText, config.nearby.xPos, config.nearby.yPos - 15, 16777215);

                    if (nearby.size() >= 1)
                    {
                        // Accounts for null variables?. Not exactly sure why, but I'm sure this does something to stop a big issue.
                        if (client.player == null) return;

                        for (int i = 0; i < nearby.size(); i++)
                        {
                            JsonObject currentPlayer = (JsonObject) nearby.get(i);
                            /**
                             * I'm not sure Manhattan distance is the best avaliable way to calculate distance between players, but it works
                             * in this situation
                             */
                            int distance = Math.abs(currentPlayer.get("x").getAsInt() - (int) client.player.getX()) +
                                           Math.abs(currentPlayer.get("z").getAsInt() - (int) client.player.getZ());

                            if (currentPlayer.get("name").getAsString().equals(clientName)) continue;

                            Formatting playerTextFormatting = Formatting.byName(config.nearby.playerTextColour);
                            MutableText playerText = new TranslatableText(currentPlayer.get("name").getAsString(), distance).formatted(playerTextFormatting);

                            renderer.drawWithShadow(matrixStack, playerText, config.nearby.xPos, nearbyPlayerOffset, 16777215);

                            // Add offset for the next player.
                            nearbyPlayerOffset += 10;
                        }
                    }
                }
                // Same concept as townless positioning
                else
                {
                    int nearbyLongest, townlessLongest;

                    nearbyLongest = Math.max(ModUtils.getNearbyLongestElement(nearby), ModUtils.getTextWidth(new TranslatableText("text_nearby_header", nearby.size())));
                    townlessLongest = Math.max(ModUtils.getLongestElement(townless), ModUtils.getTextWidth(new TranslatableText("text_townless_header", townless.size())));

                    switch(nearbyState)
                    {
                        case TOP_MIDDLE:
                        {
                            if (townlessState.equals(ModUtils.State.TOP_MIDDLE)) {
                                nearbyState.setX(ModUtils.getWindowWidth() / 2 - (townlessLongest + nearbyLongest) / 2 + townlessLongest + 5);
                                nearbyState.setY(townlessState.getY());
                            }
                            else {
                                nearbyState.setX(ModUtils.getWindowWidth() / 2 - nearbyLongest / 2);
                                nearbyState.setY(16);
                            }

                            break;
                        }
                        case TOP_RIGHT:
                        {
                            if (townlessState.equals(ModUtils.State.TOP_RIGHT))
                                nearbyState.setX(ModUtils.getWindowWidth() - townlessLongest - nearbyLongest - 15);
                            else
                                nearbyState.setX(ModUtils.getWindowWidth() - nearbyLongest - 5);

                            if (client.player != null) nearbyState.setY(ModUtils.getStatusEffectOffset(client.player.getStatusEffects()));

                            break;
                        }
                        case LEFT:
                        {
                            if (townlessState.equals(ModUtils.State.LEFT)) {
                                nearbyState.setX(townlessLongest + 10);
                                nearbyState.setY(townlessState.getY());
                            }
                            else {
                                nearbyState.setX(5);
                                nearbyState.setY(ModUtils.getWindowHeight() / 2 - ModUtils.getArrayHeight(nearby) / 2);
                            }

                            break;
                        }
                        case RIGHT:
                        {
                            if (townlessState.equals(ModUtils.State.RIGHT)) {
                                nearbyState.setX(ModUtils.getWindowWidth() - townlessLongest - nearbyLongest - 15);
                                nearbyState.setY(townlessState.getY());
                            }
                            else {
                                nearbyState.setX(ModUtils.getWindowWidth() - nearbyLongest - 5);
                                nearbyState.setY(ModUtils.getWindowHeight() / 2 - ModUtils.getArrayHeight(nearby) / 2);
                            }

                            break;
                        }
                        case BOTTOM_RIGHT:
                        {
                            if (townlessState.equals(ModUtils.State.BOTTOM_RIGHT))
                            {
                                nearbyState.setX(ModUtils.getWindowWidth() - townlessLongest - nearbyLongest - 15);
                                nearbyState.setY(townlessState.getY());
                            }
                            else {
                                nearbyState.setX(ModUtils.getWindowWidth() - nearbyLongest - 15);
                                nearbyState.setY(ModUtils.getWindowHeight() - ModUtils.getArrayHeight(nearby) - 10);
                            }

                            break;
                        }
                        case BOTTOM_LEFT:
                        {
                            if (townlessState.equals(ModUtils.State.BOTTOM_LEFT)) {
                                nearbyState.setX(townlessLongest + 15);
                                nearbyState.setY(townlessState.getY());
                            }
                            else {
                                nearbyState.setX(5);
                                nearbyState.setY(ModUtils.getWindowHeight() - ModUtils.getArrayHeight(nearby) - 10);
                            }

                            break;
                        }
                        default: // Defaults to top left
                        {
                            if (townlessState.equals(ModUtils.State.TOP_LEFT))
                                nearbyState.setX(townlessLongest + 15);
                            else
                                nearbyState.setX(5);

                            nearbyState.setY(16);

                            break;
                        }
                    }

                    Formatting nearbyTextFormatting = Formatting.byName(config.nearby.headingTextColour);
                    MutableText nearbyText = new TranslatableText("text_nearby_header", nearby.size()).formatted(nearbyTextFormatting);

                    // Draw heading.
                    renderer.drawWithShadow(matrixStack, nearbyText, nearbyState.getX(), nearbyState.getY() - 10, 16777215);

                    if (nearby.size() >= 1)
                    {
                        if (client.player == null) return;

                        for (int i = 0; i < nearby.size(); i++)
                        {
                            JsonObject currentPlayer = (JsonObject) nearby.get(i);
                            int distance = Math.abs(currentPlayer.get("x").getAsInt() - (int) client.player.getX()) +
                                           Math.abs(currentPlayer.get("z").getAsInt() - (int) client.player.getZ());

                            if (currentPlayer.get("name").getAsString().equals(clientName)) continue;

                            String prefix = "";

                            if (config.nearby.showRank)
                            {
                                if (!currentPlayer.has("town")) prefix = "(Townless) ";
                                else prefix = "(" + currentPlayer.get("rank").getAsString() + ") ";
                            }

                            Formatting playerTextFormatting = Formatting.byName(config.nearby.playerTextColour);
                            MutableText playerText = new TranslatableText(prefix + currentPlayer.get("name").getAsString() + ": " + distance + "m").formatted(playerTextFormatting);

                            renderer.drawWithShadow(matrixStack, playerText, nearbyState.getX(), nearbyState.getY() + 10*i, 16777215);
                        }
                    }
                }
            }
        });
        //#endregion
    }
}