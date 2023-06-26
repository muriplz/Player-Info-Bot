package org.pipeman.player_info_bot;

import org.bukkit.Statistic;
import org.pipeman.player_info_bot.grief_defender.GriefDefenderImpl;

import java.util.Optional;
import java.util.function.Function;

public record PlayerInformation(int rank, long playtime, long lastSeen, boolean online,
                                Optional<Integer> totalClaimBlocks, Function<Statistic, Integer> statisticSupplier) {
    public static Optional<PlayerInformation> of(String playerName) {
        return Utils.getOfflinePlayer(playerName).map(player -> new PlayerInformation(
                Leaderboard.getRank(playerName),
                player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L,
                player.getLastPlayed(),
                player.isOnline(),
                GriefDefenderImpl.isAvailable() ? Optional.of(GriefDefenderImpl.getClaimBlocks(player.getUniqueId())) : Optional.empty(),
                player::getStatistic
        ));
    }
}