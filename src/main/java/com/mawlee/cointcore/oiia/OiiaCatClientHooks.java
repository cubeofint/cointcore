package com.mawlee.cointcore.oiia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client side of {@link OiiaCatEntity}: keeps one looping song per playing cat and works out the dance in step with
 * playback. While the song is voiced the cat takes the OIIA pose, spins and bobs; in the pauses it stands still.
 * Called only on the client.
 */
final class OiiaCatClientHooks {
    /** Length of {@code sounds/oiia_cat.ogg} in ticks (5.72 s). */
    private static final float LOOP_TICKS = 114.42F;
    /** Voiced parts of the clip in ticks ("oiia oiia" twice). */
    private static final float[][] VOICED = {{9.28F, 39.36F}, {66.12F, 106.07F}};
    /** Spin speed of the model's own animation: about four turns a second. */
    private static final float SPIN_PER_TICK = 73.0F;
    /** Ticks to blend into / out of the OIIA pose at the edges of a voiced part. */
    private static final float POSE_BLEND_TICKS = 2.0F;

    private static final Map<OiiaCatEntity, Song> SONGS = new WeakHashMap<>();

    /** Extra yaw (degrees), height offset (blocks) and OIIA pose weight (0..1) for one frame. */
    record Dance(float spin, float bob, float pose) {
        static final Dance STILL = new Dance(0.0F, 0.0F, 0.0F);
    }

    private OiiaCatClientHooks() {
    }

    static void tick(OiiaCatEntity cat) {
        if (!cat.isPlaying()) {
            return;
        }
        Song song = SONGS.get(cat);
        if (song == null || song.isStopped()) {
            song = new Song(cat);
            SONGS.put(cat, song);
            Minecraft.getInstance().getSoundManager().play(song);
        }
    }

    static Dance dance(OiiaCatEntity cat, float partialTick, OiiaCatMesh mesh) {
        Song song = SONGS.get(cat);
        if (song == null) {
            return Dance.STILL;
        }
        if (song.isStopped() || !cat.isPlaying() || !Minecraft.getInstance().getSoundManager().isActive(song)) {
            return new Dance(song.lastSpin, 0.0F, 0.0F);
        }
        float elapsed = cat.tickCount - song.startTick + partialTick;
        int loops = (int) (elapsed / LOOP_TICKS);
        float position = elapsed - loops * LOOP_TICKS;
        float voiced = loops * voicedBefore(LOOP_TICKS) + voicedBefore(position);
        float spin = voiced * SPIN_PER_TICK;
        song.lastSpin = spin;
        float pose = poseWeight(position);
        float bob = 0.0F;
        if (mesh != null && pose > 0.0F) {
            float span = OiiaCatMesh.ANIM_SPIN_END - OiiaCatMesh.ANIM_SPIN_START;
            bob = mesh.bobAt(OiiaCatMesh.ANIM_SPIN_START + (voiced / 20.0F) % span) * pose;
        }
        return new Dance(spin, bob, pose);
    }

    /** Voiced ticks between the start of the clip and {@code position}. */
    private static float voicedBefore(float position) {
        float total = 0.0F;
        for (float[] part : VOICED) {
            total += Math.max(0.0F, Math.min(position, part[1]) - part[0]);
        }
        return total;
    }

    private static float poseWeight(float position) {
        for (float[] part : VOICED) {
            if (position >= part[0] && position <= part[1]) {
                float edge = Math.min(position - part[0], part[1] - position);
                return Math.min(1.0F, edge / POSE_BLEND_TICKS);
            }
        }
        return 0.0F;
    }

    /** Follows the cat and stops itself when the cat is removed, unloaded or switched off. */
    private static final class Song extends AbstractTickableSoundInstance {
        private final OiiaCatEntity cat;
        private final int startTick;
        private float lastSpin;

        Song(OiiaCatEntity cat) {
            super(OiiaCat.SOUND.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.cat = cat;
            this.startTick = cat.tickCount;
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.x = cat.getX();
            this.y = cat.getY();
            this.z = cat.getZ();
        }

        @Override
        public void tick() {
            if (cat.isRemoved() || !cat.isPlaying()) {
                stop();
                return;
            }
            x = cat.getX();
            y = cat.getY();
            z = cat.getZ();
        }
    }
}
