package org.pipeman.pipo.listener.minecraft;

import net.dv8tion.jda.api.entities.Activity;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.pipeman.pipo.Pipo;
import org.pipeman.pipo.Utils;

public class PlayerLogin implements ServerPlayConnectionEvents.Join {
    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        if (Utils.getOnlinePlayersSize() == 1) {
            Pipo.JDA.getPresence().setActivity(Activity.watching("1 player"));
        } else {
            Pipo.JDA.getPresence().setActivity(Activity.watching((Utils.getOnlinePlayersSize() + 1) + " players"));
        }
    }
}
