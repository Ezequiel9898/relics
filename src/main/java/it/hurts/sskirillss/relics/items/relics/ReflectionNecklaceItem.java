package it.hurts.sskirillss.relics.items.relics;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import it.hurts.sskirillss.relics.configs.variables.stats.RelicStats;
import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.RelicItem;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.PacketPlayerMotion;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.Reference;
import it.hurts.sskirillss.relics.utils.RelicUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.DamagingProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.*;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.ArrayUtils;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class ReflectionNecklaceItem extends RelicItem<ReflectionNecklaceItem.Stats> implements ICurioItem {
    public static final String TAG_CHARGE_AMOUNT = "charges";
    public static final String TAG_UPDATE_TIME = "time";

    public static ReflectionNecklaceItem INSTANCE;

    public ReflectionNecklaceItem() {
        super(Rarity.EPIC);

        INSTANCE = this;
    }

    @Override
    public List<ITextComponent> getShiftTooltip(ItemStack stack) {
        List<ITextComponent> tooltip = Lists.newArrayList();
        tooltip.add(new TranslationTextComponent("tooltip.relics.reflection_necklace.shift_1"));
        return tooltip;
    }

    @Override
    public void curioTick(String identifier, int index, LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity.tickCount % 20 == 0) {
            int time = NBTUtils.getInt(stack, TAG_UPDATE_TIME, 0);
            int charges = NBTUtils.getInt(stack, TAG_CHARGE_AMOUNT, 0);
            if (charges < config.maxCharges) {
                if (time < (charges > 0 ? config.timePerCharge * charges : config.timePerCharge)) {
                    NBTUtils.setInt(stack, TAG_UPDATE_TIME, time + 1);
                } else {
                    NBTUtils.setInt(stack, TAG_UPDATE_TIME, 0);
                    NBTUtils.setInt(stack, TAG_CHARGE_AMOUNT, charges + 1);
                }
            }
        }
    }

    @Override
    public List<ResourceLocation> getLootChests() {
        return RelicUtils.Worldgen.NETHER;
    }

    @Override
    public Class<Stats> getConfigClass() {
        return Stats.class;
    }

    public static final ModelResourceLocation RL = new ModelResourceLocation(new ResourceLocation(Reference.MODID, "rn_shield"), "inventory");
    private static final Direction[] DIR = ArrayUtils.add(Direction.values(), null);

    @Override
    public void render(String identifier, int index, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int light, LivingEntity livingEntity, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, ItemStack stack) {
        ICurio.RenderHelper.translateIfSneaking(matrixStack, livingEntity);
        ICurio.RenderHelper.rotateIfSneaking(matrixStack, livingEntity);
        matrixStack.scale(0.35F, 0.35F, 0.35F);
        matrixStack.translate(0.0F, 0.3F, -0.4F);
        matrixStack.mulPose(Direction.DOWN.getRotation());
        Minecraft.getInstance().getItemRenderer()
                .renderStatic(new ItemStack(ItemRegistry.REFLECTION_NECKLACE.get()), ItemCameraTransforms.TransformType.NONE, light, OverlayTexture.NO_OVERLAY,
                        matrixStack, renderTypeBuffer);
    }

    @Override
    public boolean canRender(String identifier, int index, LivingEntity livingEntity, ItemStack stack) {
        return true;
    }

    @Mod.EventBusSubscriber(modid = Reference.MODID)
    public static class ReflectionNecklaceServerEvents {
        @SubscribeEvent
        public static void onEntityHurt(LivingHurtEvent event) {
            Stats config = INSTANCE.config;
            if (event.getEntityLiving() instanceof PlayerEntity
                    && (CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.REFLECTION_NECKLACE.get(), event.getEntityLiving()).isPresent())) {
                PlayerEntity player = (PlayerEntity) event.getEntityLiving();
                ItemStack stack = CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.REFLECTION_NECKLACE.get(), event.getEntityLiving()).get().getRight();
                int charges = NBTUtils.getInt(stack, TAG_CHARGE_AMOUNT, 0);
                if (charges > 0 && event.getSource().getEntity() instanceof LivingEntity) {
                    LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
                    if (attacker == null) return;
                    if (player.position().distanceTo(attacker.position()) < config.minDistanceForKnockback) {
                        Vector3d motion = attacker.position().subtract(player.position()).normalize().multiply(2F, 1.5F, 2F);
                        if (attacker instanceof PlayerEntity) {
                            NetworkHandler.sendToClient(new PacketPlayerMotion(motion.x, motion.y, motion.z), (ServerPlayerEntity) attacker);
                        } else {
                            attacker.setDeltaMovement(motion);
                        }
                        player.getCommandSenderWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                        event.setCanceled(true);
                    }
                    if (attacker != player) {
                        NBTUtils.setInt(stack, TAG_CHARGE_AMOUNT, charges - 1);
                        attacker.hurt(DamageSource.playerAttack(player), event.getAmount() * config.reflectedDamageMultiplier);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onProjectileImpact(ProjectileImpactEvent event) {
            if (!(event.getRayTraceResult() instanceof EntityRayTraceResult)) return;
            Entity undefinedProjectile = event.getEntity();
            Entity target = ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
            if (!(target instanceof PlayerEntity)) return;
            PlayerEntity player = (PlayerEntity) target;
            if (CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.REFLECTION_NECKLACE.get(), player).isPresent()) {
                ItemStack stack = CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.REFLECTION_NECKLACE.get(), player).get().getRight();
                if (NBTUtils.getInt(stack, TAG_CHARGE_AMOUNT, 0) > 0) {
                    undefinedProjectile.setDeltaMovement(undefinedProjectile.getDeltaMovement().reverse());
                    if (undefinedProjectile instanceof DamagingProjectileEntity) {
                        DamagingProjectileEntity projectile = (DamagingProjectileEntity) undefinedProjectile;
                        projectile.setOwner(player);
                        projectile.xPower *= -1;
                        projectile.yPower *= -1;
                        projectile.zPower *= -1;
                    }
                    event.setCanceled(true);
                    undefinedProjectile.hurtMarked = true;
                    NBTUtils.setInt(stack, TAG_CHARGE_AMOUNT, NBTUtils.getInt(stack, TAG_CHARGE_AMOUNT, 0) - 1);
                    player.getCommandSenderWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = Reference.MODID, value = Dist.CLIENT)
    public static class ReflectionNecklaceClientEvents {
        @SubscribeEvent
        public static void onPlayerRender(RenderPlayerEvent event) {
            if (!event.getPlayer().isSpectator() && !event.getPlayer().isInvisible()
                    && CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.REFLECTION_NECKLACE.get(), event.getPlayer()).isPresent()) {
                int charges = NBTUtils.getInt(CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.REFLECTION_NECKLACE.get(),
                        event.getPlayer()).get().getRight(), TAG_CHARGE_AMOUNT, 0);
                PlayerEntity player = event.getPlayer();
                MatrixStack matrixStack = event.getMatrixStack();
                IBakedModel model = Minecraft.getInstance().getModelManager().getModel(RL);
                if (charges > 0) {
                    for (int i = 0; i < charges; i++) {
                        matrixStack.pushPose();
                        matrixStack.scale(2F, 2F, 2F);
                        float f = player.getSwimAmount(player.tickCount);
                        if (player.isFallFlying()) {
                            matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - player.yRot));
                            float f1 = (float) player.getFallFlyingTicks() + player.tickCount;
                            float f2 = MathHelper.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
                            if (!player.isAutoSpinAttack()) {
                                matrixStack.mulPose(Vector3f.XP.rotationDegrees(f2 * (-90.0F - player.xRot)));
                            }

                            Vector3d vector3d = player.getViewVector(player.tickCount);
                            Vector3d vector3d1 = player.getDeltaMovement();
                            double d0 = Entity.getHorizontalDistanceSqr(vector3d1);
                            double d1 = Entity.getHorizontalDistanceSqr(vector3d);
                            if (d0 > 0.0D && d1 > 0.0D) {
                                double d2 = (vector3d1.x * vector3d.x + vector3d1.z * vector3d.z) / Math.sqrt(d0 * d1);
                                double d3 = vector3d1.x * vector3d.z - vector3d1.z * vector3d.x;
                                matrixStack.mulPose(Vector3f.YP.rotation((float) (Math.signum(d3) * Math.acos(d2))));
                            }
                        } else if (f > 0.0F) {
                            float f3 = player.isInWater() ? -90.0F - player.xRot : -90.0F;
                            float f4 = MathHelper.lerp(f, 0.0F, f3);
                            matrixStack.mulPose(Vector3f.XP.rotationDegrees(f4));
                            if (player.isVisuallySwimming()) {
                                matrixStack.translate(0.0D, -1.0D, (double) 0.3F);
                            }
                        }
                        matrixStack.translate(0, 0.75, 0);
                        matrixStack.mulPose(Vector3f.ZP.rotationDegrees((MathHelper.cos(player.tickCount / 10.0F) / 7.0F) * (180F / (float) Math.PI)));
                        matrixStack.mulPose(Vector3f.YP.rotationDegrees((player.tickCount / 10.0F) * (180F / (float) Math.PI) + (i * (360F / charges))));
                        matrixStack.mulPose(Vector3f.XP.rotationDegrees((MathHelper.sin(player.tickCount / 10.0F) / 7.0F) * (180F / (float) Math.PI)));
                        matrixStack.translate(-0.5, -0.75, -1);
                        for (Direction dir : DIR) {
                            Minecraft.getInstance().getItemRenderer().renderQuadList(
                                    matrixStack, event.getBuffers().getBuffer(Atlases.cutoutBlockSheet()),
                                    model.getQuads(null, dir, player.getCommandSenderWorld().getRandom(), EmptyModelData.INSTANCE),
                                    ItemStack.EMPTY, event.getLight(), OverlayTexture.NO_OVERLAY);
                        }
                        matrixStack.popPose();
                    }
                }
            }
        }
    }

    public static class Stats extends RelicStats {
        public int maxCharges = 3;
        public int timePerCharge = 60;
        public int minDistanceForKnockback = 10;
        public float reflectedDamageMultiplier = 2.0F;
    }
}