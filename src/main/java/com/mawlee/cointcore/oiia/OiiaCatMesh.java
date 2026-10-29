package com.mawlee.cointcore.oiia;

import com.mawlee.cointcore.CointCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * The OIIA cat mesh ("Oiiaioooooiai Cat" by Zhuier, Sketchfab, CC BY 4.0), converted from GLB into
 * {@code models/entity/oiia_cat.mesh}: per vertex position (blocks, feet at y=0, facing +Z), normal, uv and the
 * "OIIA pose" morph delta (crouched, head up). {@code oiia_cat.anim} holds the model's own bob curve sampled at 60 Hz.
 * Loaded once on first render; {@link #get()} returns null when the files are missing or broken.
 */
final class OiiaCatMesh {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MESH = ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "models/entity/oiia_cat.mesh");
    private static final ResourceLocation ANIM = ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "models/entity/oiia_cat.anim");
    private static final int MAGIC = 0x4F494941;
    /** Seconds into the model's animation where the spin starts; the bob curve is read from here on. */
    static final float ANIM_SPIN_START = 1.1F;
    static final float ANIM_SPIN_END = 5.9F;

    private static OiiaCatMesh instance;
    private static boolean loaded;

    private final float[] position;
    private final float[] normal;
    private final float[] uv;
    private final float[] morph;
    private final int[] indices;
    private final float[] bob;

    private OiiaCatMesh(float[] position, float[] normal, float[] uv, float[] morph, int[] indices, float[] bob) {
        this.position = position;
        this.normal = normal;
        this.uv = uv;
        this.morph = morph;
        this.indices = indices;
        this.bob = bob;
    }

    static OiiaCatMesh get() {
        if (!loaded) {
            loaded = true;
            try {
                instance = load();
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Failed to load OIIA cat mesh", exception);
            }
        }
        return instance;
    }

    private static OiiaCatMesh load() throws IOException {
        Optional<Resource> meshResource = Minecraft.getInstance().getResourceManager().getResource(MESH);
        Optional<Resource> animResource = Minecraft.getInstance().getResourceManager().getResource(ANIM);
        if (meshResource.isEmpty() || animResource.isEmpty()) {
            return null;
        }
        float[] position;
        float[] normal;
        float[] uv;
        float[] morph;
        int[] indices;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(meshResource.get().open()))) {
            if (in.readInt() != MAGIC) {
                throw new IOException("bad mesh header");
            }
            int vertices = in.readInt();
            position = new float[vertices * 3];
            normal = new float[vertices * 3];
            uv = new float[vertices * 2];
            morph = new float[vertices * 3];
            for (int i = 0; i < vertices; i++) {
                for (int c = 0; c < 3; c++) {
                    position[i * 3 + c] = in.readFloat();
                }
                for (int c = 0; c < 3; c++) {
                    normal[i * 3 + c] = in.readFloat();
                }
                uv[i * 2] = in.readFloat();
                uv[i * 2 + 1] = in.readFloat();
                for (int c = 0; c < 3; c++) {
                    morph[i * 3 + c] = in.readFloat();
                }
            }
            indices = new int[in.readInt()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = in.readInt();
            }
        }
        float[] bob;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(animResource.get().open()))) {
            bob = new float[in.readInt()];
            for (int i = 0; i < bob.length; i++) {
                in.readFloat(); // time: keys are at 60 Hz
                bob[i] = in.readFloat();
                in.readFloat(); // yaw: the spin is driven by the song instead
                in.readFloat(); // morph weight: likewise
            }
        }
        return new OiiaCatMesh(position, normal, uv, morph, indices, bob);
    }

    /** The model's bob (blocks) at {@code seconds} into its animation, linearly interpolated. */
    float bobAt(float seconds) {
        float key = Math.max(0.0F, seconds * 60.0F);
        int index = Math.min((int) key, bob.length - 2);
        float fraction = Math.min(1.0F, key - index);
        return bob[index] + (bob[index + 1] - bob[index]) * fraction;
    }

    /**
     * Emits every triangle as a quad with its last vertex repeated (entity render types draw quads).
     * {@code pose} weighs the morph toward the OIIA pose (0 = standing, 1 = crouched with head up).
     */
    void render(PoseStack.Pose matrix, VertexConsumer buffer, int light, float pose) {
        for (int i = 0; i < indices.length; i += 3) {
            int a = indices[i];
            vertex(matrix, buffer, a, light, pose);
            vertex(matrix, buffer, indices[i + 1], light, pose);
            int c = indices[i + 2];
            vertex(matrix, buffer, c, light, pose);
            vertex(matrix, buffer, c, light, pose);
        }
    }

    private void vertex(PoseStack.Pose matrix, VertexConsumer buffer, int v, int light, float pose) {
        int p = v * 3;
        buffer.addVertex(matrix,
                        position[p] + morph[p] * pose,
                        position[p + 1] + morph[p + 1] * pose,
                        position[p + 2] + morph[p + 2] * pose)
                .setColor(-1)
                .setUv(uv[v * 2], uv[v * 2 + 1])
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(matrix, normal[p], normal[p + 1], normal[p + 2]);
    }
}
