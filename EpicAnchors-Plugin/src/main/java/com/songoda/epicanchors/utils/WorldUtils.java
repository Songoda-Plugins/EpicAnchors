package com.songoda.epicanchors.utils;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("JavaReflectionMemberAccess")
public class WorldUtils {
    private WorldUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final Method addChunkTicketMethod;
    private static final Method removeChunkTicketMethod;

    static {
        Method tmpAdd;
        Method tmpRem;

        try {
            tmpAdd = Chunk.class.getDeclaredMethod("addPluginChunkTicket", Plugin.class);
            tmpRem = Chunk.class.getDeclaredMethod("removePluginChunkTicket", Plugin.class);
        } catch (NoSuchMethodException ignore) {
            tmpAdd = null;
            tmpRem = null;
        }

        addChunkTicketMethod = tmpAdd;
        removeChunkTicketMethod = tmpRem;
    }

    public static int getRandomTickSpeed(World world) {
        try {
            return Integer.parseInt(world.getGameRuleValue("randomTickSpeed"));
        } catch (NumberFormatException ignore) {
            return 3;
        }
    }

    public static boolean loadAnchoredChunk(Chunk chunk, Plugin plugin) {
        if (addChunkTicketMethod != null) {
            try {
                return (boolean) addChunkTicketMethod.invoke(chunk, plugin);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                Utils.logException(plugin, ex);
            }
        }

        return chunk.load();
    }

    public static boolean unloadAnchoredChunk(Chunk chunk, Plugin plugin) {
        if (removeChunkTicketMethod != null) {
            try {
                removeChunkTicketMethod.invoke(chunk, plugin);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                Utils.logException(plugin, ex);
            }
        }

        return chunk.unload();
    }
}
