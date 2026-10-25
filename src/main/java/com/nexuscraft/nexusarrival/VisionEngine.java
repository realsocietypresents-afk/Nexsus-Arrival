package com.nexuscraft.nexusarrival;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Plays a {@link VisionDefinition} to one specific player -- every beat targets that player
 *  alone (Player#spawnParticle / Player#playSound are per-client; nothing here is visible or
 *  audible to anyone standing next to them, and nothing here is a real entity, so it can safely
 *  play in a 3-block spawn room just as well as an open field). Each beat is scheduled as its
 *  own {@code runTaskLater} relative to when the vision starts, and every beat re-checks that
 *  the player is still online before touching them, since a vision can span several seconds and
 *  a player is always free to log out mid-illusion. */
final class VisionEngine {

    private final JavaPlugin plugin;
    /** Particles we have already warned about, so a bad config entry logs once, not every beat. */
    private final java.util.Set<Particle> warned = java.util.EnumSet.noneOf(Particle.class);

    VisionEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void play(Player player, VisionDefinition vision) {
        for (VisionBeat beat : vision.beats) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> runBeat(player, beat), beat.delayTicks);
        }
    }

    void runBeat(Player player, VisionBeat beat) {
        if (!player.isOnline()) {
            return;
        }
        Location at = player.getLocation();
        switch (beat.type) {
            case PARTICLE -> spawn(player, beat.particle, at, beat.count, beat.offsetX, beat.offsetY, beat.offsetZ);
            case SOUND -> player.playSound(at, beat.sound, beat.volume, beat.pitch);
            case LIGHTNING -> {
                // Deliberately NOT World#strikeLightningEffect: that's a real-world visual/sound
                // effect broadcast to every nearby player, which would break this whole system's
                // "private, only-this-player" promise. A per-player flash (Player#spawnParticle /
                // Player#playSound both send to one client only) fakes the same beat honestly.
                spawn(player, Particle.FLASH, at.clone().add(0, 2.0, 0), 1, 0, 0, 0);
                spawn(player, Particle.ELECTRIC_SPARK, at, 40, 1.0, 1.5, 1.0);
                player.playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
            }
            case MESSAGE -> player.sendMessage(beat.text);
        }
    }

    /** Spawns a particle for one player, supplying the extra data modern Paper demands.
     *  Some particles carry a data payload (FLASH and ENTITY_EFFECT take a {@link Color}, DUST takes
     *  dust options, ...); newer servers throw "missing required data class" if it is absent. We look
     *  at {@link Particle#getDataType()} and fill in a harmless default; anything we cannot fill is
     *  skipped with a single warning instead of throwing out of the scheduler task. */
    private void spawn(Player player, Particle particle, Location at, int count,
                       double ox, double oy, double oz) {
        try {
            Class<?> type = particle.getDataType();
            if (type == null || type == Void.class) {
                player.spawnParticle(particle, at, count, ox, oy, oz);
            } else if (type == Color.class) {
                player.spawnParticle(particle, at, count, ox, oy, oz, 0.0, Color.WHITE);
            } else if (type == Float.class) {
                player.spawnParticle(particle, at, count, ox, oy, oz, 0.0, 0.0f);
            } else if (type == Integer.class) {
                player.spawnParticle(particle, at, count, ox, oy, oz, 0.0, 0);
            } else {
                warnOnce(particle, "needs " + type.getSimpleName() + " data, which NexusArrival cannot supply");
            }
        } catch (IllegalArgumentException e) {
            warnOnce(particle, e.getMessage());
        }
    }

    private void warnOnce(Particle particle, String why) {
        if (warned.add(particle)) {
            plugin.getLogger().warning("nexusarrival: skipping particle " + particle.name() + " (" + why
                    + "). Pick another particle in config.yml; this message is shown once.");
        }
    }
}
