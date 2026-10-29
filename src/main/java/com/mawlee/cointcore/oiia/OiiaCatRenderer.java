package com.mawlee.cointcore.oiia;

import com.mawlee.cointcore.CointCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws the OIIA cat model ({@link OiiaCatMesh}) doing its dance from {@link OiiaCatClientHooks#dance}.
 * Falls back to the vanilla cat model if the mesh can't be loaded. Not drawn beyond {@link #MAX_DISTANCE} blocks:
 * the mesh is ~16k triangles.
 */
public class OiiaCatRenderer extends EntityRenderer<OiiaCatEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "textures/entity/oiia_cat.png");
    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/entity/cat/british_shorthair.png");
    private static final double MAX_DISTANCE = 48.0D;
    /** The mesh is exported about one block long, tail included; at 1.4 the body fills one block and the tail hangs over. */
    private static final float SCALE = 1.4F;
    /** The mesh is centred on its bounding box, tail included; shift it so the body sits over the pivot. */
    private static final float BODY_OFFSET_Z = -0.13F;

    private final CatModel<Cat> fallback;

    public OiiaCatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.fallback = new CatModel<>(context.bakeLayer(ModelLayers.CAT));
        this.shadowRadius = 0.5F;
    }

    @Override
    public boolean shouldRender(OiiaCatEntity cat, Frustum frustum, double camX, double camY, double camZ) {
        return cat.distanceToSqr(camX, camY, camZ) < MAX_DISTANCE * MAX_DISTANCE
                && super.shouldRender(cat, frustum, camX, camY, camZ);
    }

    @Override
    public void render(OiiaCatEntity cat, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        OiiaCatMesh mesh = OiiaCatMesh.get();
        OiiaCatClientHooks.Dance dance = OiiaCatClientHooks.dance(cat, partialTick, mesh);
        pose.pushPose();
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(0.0F, Math.max(0.0F, dance.bob()), 0.0F);
        if (mesh != null) {
            // The mesh faces +Z, which is where an entity with yaw 0 looks.
            pose.mulPose(Axis.YP.rotationDegrees(-cat.getYRot() + dance.spin()));
            pose.translate(0.0F, 0.0F, BODY_OFFSET_Z);
            mesh.render(pose.last(), buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, dance.pose());
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - cat.getYRot() + dance.spin()));
            // Living-entity model space: flipped, origin 1.5 blocks up; vanilla cats are drawn at 0.8 scale.
            pose.scale(-0.8F, -0.8F, 0.8F);
            pose.translate(0.0F, -1.501F, 0.0F);
            fallback.renderToBuffer(pose, buffers.getBuffer(fallback.renderType(FALLBACK)), packedLight, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
        super.render(cat, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(OiiaCatEntity cat) {
        return OiiaCatMesh.get() != null ? TEXTURE : FALLBACK;
    }

    @EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(OiiaCat.ENTITY.get(), OiiaCatRenderer::new);
        }
    }
}
