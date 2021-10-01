package de.strifel.vbans.database;

import de.strifel.vbans.VBans;

public class HistoryBan extends Ban {
  private final String purgedBy, reducedBy;

  HistoryBan(long id, String player, String by, String reason, long until, long bannedAt, long reducedUntil, String purgedBy, String reducedBy) {
    super(id, player, by, reason, until, bannedAt, reducedUntil);
    this.purgedBy = purgedBy;
    this.reducedBy = reducedBy;
  }

  public boolean isPurged() {
    return purgedBy != null;
  }

  public String getPurgedByUsername(VBans vBans) {
    if (purgedBy.equals("Console")) return purgedBy;
    return vBans.getDatabaseConnection().getUsername(purgedBy);

  }

  public String getReducedByUsername(VBans vBans) {
    if (reducedBy.equals("Console")) return reducedBy;
    return vBans.getDatabaseConnection().getUsername(reducedBy);

  }

  public boolean isReduced() {
    return reducedUntil != 0;
  }

  public long getOriginalBanEnd() {
    return until;
  }
}
