package de.strifel.vbans.database;

import de.strifel.vbans.VBans;

public class Ban {
  protected final long until;
  protected final long bannedAt;
  protected final long reducedUntil;
  protected final long id;
  private final String player, by, reason;

  public Ban(long id, String player, String by, String reason, long until, long bannedAt, long reducedUntil) {
    this.player = player;
    this.by = by;
    this.reason = reason;
    this.until = until;
    this.bannedAt = bannedAt;
    this.reducedUntil = reducedUntil;
    this.id = id;
  }


  public String getUsername(VBans vbans) {
    return vbans.getDatabaseConnection().getUsername(player);
  }

  public String getBannedByUsername(VBans vbans) {
    if (by.equals("Console")) return by;
    return vbans.getDatabaseConnection().getUsername(by);
  }

  public String getPlayer() {
    return player;
  }

  public String getBannedBy() {
    return by;
  }

  public String getReason() {
    return reason;
  }

  public long getUntil() {
    if (reducedUntil != 0) return reducedUntil;
    return until;
  }

  public long getId() {
    return id;
  }

  public long getBannedAt() {
    return bannedAt;
  }
}
