package com.nexuscraft.nexusarrival;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Plain-main test: the lightning beat must not throw for particles that need data (the v0.1.3
 *  field failure: "missing required data class org.bukkit.Color"). */
public class VisionEngineTest {

    private static int failures;

    private static void check(boolean ok, String what) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }

    private static final class TestPlugin extends JavaPlugin { }

    /** The shared World stub recurses forever when constructed, so use a world-less location. */
    private static Player newPlayer() {
        return new Player() {
            @Override
            public org.bukkit.Location getLocation() {
                return new org.bukkit.Location(null, 0, 64, 0);
            }
        };
    }

    public static void main(String[] args) {
        VisionEngine engine = new VisionEngine(new TestPlugin());
        Player p = newPlayer();

        // The stub now behaves like modern Paper: a data-less FLASH spawn throws.
        boolean threw = false;
        try {
            p.spawnParticle(Particle.FLASH, p.getLocation(), 1);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check(threw, "stub reproduces the field bug for FLASH without data");

        // The lightning beat now works, and still plays everything else.
        engine.runBeat(p, VisionBeat.lightning(0));
        check(p.spawned.contains(Particle.FLASH), "FLASH spawned with colour data");
        check(p.spawned.contains(Particle.ELECTRIC_SPARK), "sparks spawned");

        // A configured particle beat for a data-needing particle also works.
        Player q = newPlayer();
        engine.runBeat(q, VisionBeat.particle(0, Particle.FLASH, 3, 0, 0, 0));
        check(q.spawned.contains(Particle.FLASH), "configured FLASH beat works");

        // Plain particles keep working.
        Player r = newPlayer();
        engine.runBeat(r, VisionBeat.particle(0, Particle.SOUL, 5, 0.1, 0.1, 0.1));
        check(r.spawned.contains(Particle.SOUL), "plain particle unchanged");

        // Sounds and messages are untouched by the change.
        engine.runBeat(r, VisionBeat.sound(0, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f));
        engine.runBeat(r, VisionBeat.message(0, "hi"));

        if (failures > 0) {
            System.out.println(failures + " failure(s)");
            System.exit(1);
        }
        System.out.println("VisionEngineTest OK");
    }
}
