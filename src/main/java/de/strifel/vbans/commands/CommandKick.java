package de.strifel.vbans.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import de.strifel.vbans.Util;
import de.strifel.vbans.VBans;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static de.strifel.vbans.Util.COLOR_RED;
import static de.strifel.vbans.Util.COLOR_YELLOW;

public class CommandKick extends VBanCommand {
  private final String DEFAULT_REASON;
  private final String KICK_LAYOUT;

  public CommandKick(VBans vBans) {
    super(vBans);
    DEFAULT_REASON = vBans.getMessages().getString("StandardKickMessage");
    KICK_LAYOUT = vBans.getMessages().getString("KickLayout");
  }

  @Override
  public void execute(Invocation commandInvocation) {
    String[] strings = commandInvocation.arguments();
    CommandSource commandSource = commandInvocation.source();

    if (strings.length == 0) {
      commandSource.sendMessage(Component.text("Usage: /kick <player> [reason]").color(COLOR_RED));
      return;
    }
    Optional<Player> oPlayer = server.getPlayer(strings[0]);
    if (oPlayer.isEmpty()) {
      commandSource.sendMessage(Component.text("Player not found!").color(COLOR_RED));
      return;
    }
    Player player = oPlayer.get();
    if (!player.hasPermission("VBans.prevent") || commandSource instanceof ConsoleCommandSource) {
      String reason = DEFAULT_REASON;
      if (strings.length > 1 && commandSource.hasPermission("VBans.kick.reason")) {
        reason = String.join(" ", Arrays.copyOfRange(strings, 1, strings.length));
      }
      player.disconnect(Component.text(KICK_LAYOUT.replace("$reason", reason)));

      if (!database.addBan(player.getUniqueId().toString(), System.currentTimeMillis() / 1000, commandSource instanceof ConsoleCommandSource ? "Console" : ((Player) commandSource).getUniqueId().toString(), reason))
        commandSource.sendMessage(Component.text("Your kick can not be registered.").color(COLOR_RED));

      commandSource.sendMessage(Component.text("You kicked " + strings[0]).color(COLOR_YELLOW));
    } else {
      commandSource.sendMessage(Component.text("You are not allowed to kick this player!").color(COLOR_RED));
    }


  }


  @Override
  public CompletableFuture<List<String>> suggestAsync(Invocation commandInvocation) {
    return CompletableFuture.supplyAsync(() -> {
      if (commandInvocation.arguments().length == 1)
        return Util.getAllPlayernames(server);
      return new ArrayList<>();
    });
  }

  @Override
  public boolean hasPermission(Invocation commandInvocation) {
    return commandInvocation.source().hasPermission("VBans.kick");
  }
}
