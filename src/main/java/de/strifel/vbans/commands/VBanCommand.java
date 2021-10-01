package de.strifel.vbans.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.VBans;
import de.strifel.vbans.database.DatabaseConnection;

public abstract class VBanCommand implements SimpleCommand {
  protected final ProxyServer server;
  protected final DatabaseConnection database;
  protected final VBans vBans;

  protected VBanCommand(VBans vBans) {
    this.vBans = vBans;
    this.database = this.vBans.getDatabaseConnection();
    this.server = vBans.getServer();
  }
}
