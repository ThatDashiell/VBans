package de.strifel.vbans.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.Ban;
import de.strifel.vbans.database.DatabaseConnection;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static de.strifel.vbans.Util.COLOR_RED;
import static de.strifel.vbans.Util.COLOR_YELLOW;
import static de.strifel.vbans.commands.CommandTempBan.getBanDuration;

public class CommandReduce extends VBanCommand {

    public CommandReduce(VBans vBans) {
        super(vBans);
    }

    @Override
    public void execute(Invocation commandInvocation) {
        String[] strings = commandInvocation.arguments();
        CommandSource commandSource = commandInvocation.source();
        if (strings.length >= 1 && strings.length <= 2) {
            String uuid = database.getUUID(strings[0]);
            Ban ban = database.getBan(uuid);
            if (uuid == null || ban == null) {
                commandSource.sendMessage(Component.text("This Player is not banned!").color(COLOR_RED));
                return;
            }
            long duration = strings.length == 2 ? getBanDuration(strings[1]) : -1;
            if (duration == 0) {
                commandSource.sendMessage(Component.text("Invalid duration! Us d, m, h or s as suffix for time!").color(COLOR_RED));
                return;
            }
            database.reduceBanTo(uuid, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), duration == -1 ? (System.currentTimeMillis() / 1000) : ban.getBannedAt() + duration);
            commandSource.sendMessage(Component.text("All active bans for " + strings[0] + " are reduced!").color(COLOR_YELLOW));
        } else {
            commandSource.sendMessage(Component.text("Usage: /reduce <username> [duration]").color(COLOR_RED));
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation commandInvocation) {
        return CompletableFuture.supplyAsync(() -> database.getUsernamesByQuery(DatabaseConnection.BANNED_CRITERIA));
    }

    @Override
    public boolean hasPermission(Invocation commandInvocation) {
        return commandInvocation.source().hasPermission("VBans.reduce");
    }
}
