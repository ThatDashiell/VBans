package de.strifel.vbans.commands;

import com.velocitypowered.api.command.CommandSource;
import de.strifel.vbans.Util;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.HistoricalBan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static de.strifel.vbans.Util.*;

public class CommandBanHistory extends VBanCommand {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    public CommandBanHistory(VBans vBans) {
        super(vBans);
    }

    @Override
    public void execute(Invocation commandInvocation) {
        String[] strings = commandInvocation.arguments();
        CommandSource commandSource = commandInvocation.source();

        if (strings.length == 1) {
            List<HistoricalBan> bans = database.getBanHistory(server.getPlayer(strings[0]).isPresent() ? server.getPlayer(strings[0]).get().getUniqueId().toString() : database.getUUID(strings[0]), commandSource.hasPermission("VBans.history.seeDeleted"));
            commandSource.sendMessage(Component.text("Ban history of " + strings[0]).color(COLOR_YELLOW));
            commandSource.sendMessage(Component.text("----------------------------------------").color(COLOR_YELLOW));
            for (HistoricalBan ban : bans) {
                commandSource.sendMessage(generateBanText(ban));
            }
            commandSource.sendMessage(Component.text("----------------------------------------").color(COLOR_YELLOW));
        } else {
            commandSource.sendMessage(Component.text("Usage: /banhistory <username>").color(COLOR_RED));
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation commandInvocation) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> users = database.getUsernamesByQuery("");
            users.addAll(Util.getAllPlayernames(server));
            return users;
        });
    }

    @Override
    public boolean hasPermission(Invocation commandInvocation) {
        return commandInvocation.source().hasPermission("VBans.history");
    }

    private Component generateBanText(HistoricalBan ban) {
        Component banText =
                Component.text("#" + ban.getId() + " " + DATE_FORMAT.format(ban.getBannedAt() * 1000) + ": ")
                        .append(Component.text("\"" + ban.getReason() + "\"").decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                        .append(Component.text(" by "))
                        .append(Component.text(ban.getBannedByUsername(vBans)))
                        .append(Component.text(" "));
        banText = banText.color(COLOR_YELLOW);
        Component length = getBanLength(ban.getOriginalBanEnd(), ban.getBannedAt());
        if (ban.isReduced()) {
            length = length.decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.TRUE);
            banText = banText.append(length).append(Component.text(" "));
            banText = banText.append(getBanLength(ban.getUntil(), ban.getBannedAt()));
            banText = banText.append(Component.text("(Reduced by " + ban.getReducedByUsername(vBans) + ")").color(COLOR_DARK_GREEN));
        } else {
            banText = banText.append(length);
        }
        if (ban.isPurged()) {
            banText = banText.decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.TRUE);
            banText = Component.text("").append(banText).append(Component.text("(Deleted by " + ban.getPurgedByUsername(vBans) + ")"));
        }

        return banText;
    }


    private Component getBanLength(long end, long start) {
        if (end == -1) {
            return Component.text("(permanent)").color(COLOR_RED);
        } else if (start - end >= 0) {
            return Component.text("(kick)").color(COLOR_DARK_GREEN);
        } else {
            long duration = end - start;
            if (duration / (60 * 60 * 24) > 0) {
                return Component.text("§c(" + ((int) duration / (60 * 60 * 24)) + "d)").color(COLOR_RED);
            } else if (duration / (60 * 60) > 0) {
                return Component.text("§c(" + ((int) duration / (60 * 60)) + "h)").color(COLOR_RED);
            } else if (duration / 60 > 0) {
                return Component.text("§c(" + ((int) duration / 60) + "m)").color(COLOR_RED);
            } else {
                return Component.text("§c(" + duration + "s)");
            }
        }

    }
}
