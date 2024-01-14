package org.pipeman.pipo;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import org.json.JSONObject;
import org.pipeman.pipo.offline.Offlines;
import org.pipeman.pipo.offline.OfflinesStats;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class Utils {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static byte[] getSkin(String name) {
        try {
            String uuid = new JSONObject(
                    CLIENT.send(
                            HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name)).build(),
                            HttpResponse.BodyHandlers.ofString()
                    ).body()
            ).getString("id");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                    .build();

            String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
            JSONObject data = new JSONObject(body);
            byte[] decode = Base64.getDecoder().decode(data.getJSONArray("properties").getJSONObject(0).getString("value"));
            String url = new JSONObject(new String(decode)).getJSONObject("textures").getJSONObject("SKIN").getString("url");

            BufferedImage image = ImageIO.read(new URL(url));
            BufferedImage output = new BufferedImage(8, 8, Image.SCALE_FAST);

            copyRect(image, output, 8, 8, 8, 8, 0, 0);
            copyRect(image, output, 40, 8, 8, 8, 0, 0);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(scaleImage(output, 32), "png", out);

            return out.toByteArray();
        } catch (Exception ignored) {
            return new byte[0];
        }
    }

    private static RenderedImage scaleImage(BufferedImage in, int scale) {
        BufferedImage out = new BufferedImage(in.getWidth() * scale, in.getHeight() * scale,
                BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < in.getWidth(); x++) {
            for (int y = 0; y < in.getHeight(); y++) {
                drawSquare(out, in.getRGB(x, y), x * scale, y * scale, scale);
            }
        }
        return out;
    }

    private static void drawSquare(BufferedImage image, int color, int x, int y, int size) {
        for (int xI = 0; xI < size; xI++) {
            for (int yI = 0; yI < size; yI++) {
                image.setRGB(xI + x, yI + y, color);
            }
        }
    }

    private static void copyRect(BufferedImage in, BufferedImage out, int x, int y, int width, int height, int destX, int destY) {
        for (int xI = 0; xI < width; xI++) {
            for (int yI = 0; yI < height; yI++) {
                int rgb = in.getRGB(xI + x, yI + y);
                if (rgb == 0) continue;
                out.setRGB(xI + destX, yI + destY, rgb);
            }
        }

        // titel, schulfach, datei
    }

    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public static String ordinal(int i) {
        int mod100 = i % 100;
        int mod10 = i % 10;
        if (mod10 == 1 && mod100 != 11) return i + "st";
        else if (mod10 == 2 && mod100 != 12) return i + "nd";
        else if (mod10 == 3 && mod100 != 13) return i + "rd";
        return i + "th";
    }

    public static <T, R> List<T> map(Collection<R> list, Function<R, T> mappingFunction) {
        List<T> out = new ArrayList<>();
        for (R r : list) {
            out.add(mappingFunction.apply(r));
        }
        return out;
    }

    public static MessageEmbed createErrorEmbed(String error) {
        return new EmbedBuilder()
                .setTitle("Error")
                .addField("Description", error, false)
                .setColor(new Color(59, 152, 0))
                .build();
    }

    public static long getPlaytime(String name) {

        UUID id = Offlines.getUUIDbyName(name);
        long playtime = OfflinesStats.getPlayerStat("play_time", id) / (60);
        return Math.max(0, playtime);
    }

    public static long getLastPlayed(String name) {
        return Pipo.getInstance().lastTimePlayed.getElement(Offlines.getUUIDbyName(name)) * 1000;
    }

    public static boolean isOnline(String name) {
        return Arrays.asList(MinecraftServerSupplier.getServer().getPlayerNames()).contains(name);
    }

    public static int getOnlinePlayersSize() {
        return MinecraftServerSupplier.getServer().getCurrentPlayerCount();
    }

    public static UUID getMinecraftId(Member member) {
        AtomicReference<UUID> id = new AtomicReference<>();
        Pipo.getInstance().minecraftToDiscord.getHashMap().forEach(((uuid, memberid) -> {
            if (memberid.equals(member.getId())) id.set(uuid);
        }));
        return id.get();
    }

    public static CompletableFuture<Boolean> isPlayerOnGroup(UUID who, String group) {
        return LuckPermsProvider.get().getUserManager().loadUser(who)
                .thenApplyAsync(user -> {
                    Collection<Group> inheritedGroups = user.getInheritedGroups(user.getQueryOptions());
                    return inheritedGroups.stream().anyMatch(g -> g.getName().equals(group));
                });
    }

    public static void addGroup(UUID id, String permission) {
        LuckPermsProvider.get().getUserManager().modifyUser(id, user -> {
            user.data().add(Node.builder("group." + permission).build());
        });
    }

    public static void removeGroup(UUID id, String permission) {
        LuckPermsProvider.get().getUserManager().modifyUser(id, user -> {
            user.data().remove(Node.builder("group." + permission).build());
        });
    }
}
