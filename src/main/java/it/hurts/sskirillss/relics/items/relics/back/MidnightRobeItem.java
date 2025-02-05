package it.hurts.sskirillss.relics.items.relics.back;

import com.google.common.collect.Lists;
import it.hurts.sskirillss.relics.init.EffectRegistry;
import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.items.relics.base.IRenderableCurio;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.items.relics.base.data.RelicData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.CastData;
import it.hurts.sskirillss.relics.items.relics.base.data.cast.misc.CastType;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilitiesData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.AbilityData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.StatData;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.misc.UpgradeOperation;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootData;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.misc.LootCollections;
import it.hurts.sskirillss.relics.items.relics.base.data.style.StyleData;
import it.hurts.sskirillss.relics.items.relics.base.data.style.misc.Backgrounds;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.MathUtils;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.ParticleUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.SlotContext;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.UUID;

public class MidnightRobeItem extends RelicItem implements IRenderableCurio {
    private static final String TAG_TARGET = "target";

    @Override
    public RelicData constructDefaultRelicData() {
        return RelicData.builder()
                .abilities(AbilitiesData.builder()
                        .ability(AbilityData.builder("vanish")
                                .requiredPoints(2)
                                .active(CastData.builder()
                                        .type(CastType.TOGGLEABLE)
                                        .build())
                                .stat(StatData.builder("light")
                                        .initialValue(1D, 2D)
                                        .upgradeModifier(UpgradeOperation.ADD, 1D)
                                        .formatValue(value -> (int) MathUtils.round(value, 0))
                                        .build())
                                .stat(StatData.builder("speed")
                                        .initialValue(0.1D, 0.35D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.35D)
                                        .formatValue(value -> (int) (MathUtils.round(value, 3) * 100))
                                        .build())
                                .build())
                        .ability(AbilityData.builder("backstab")
                                .stat(StatData.builder("damage")
                                        .initialValue(1.25D, 1.75D)
                                        .upgradeModifier(UpgradeOperation.MULTIPLY_BASE, 0.1D)
                                        .formatValue(value -> (int) (100 * MathUtils.round(value - 1, 1)))
                                        .build())
                                .stat(StatData.builder("distance")
                                        .initialValue(15D, 20D)
                                        .upgradeModifier(UpgradeOperation.ADD, -0.5D)
                                        .formatValue(value -> MathUtils.round(value, 1))
                                        .build())
                                .build())
                        .build())
                .leveling(new LevelingData(100, 10, 100))
                .style(StyleData.builder()
                        .background(Backgrounds.CAVE)
                        .build())
                .loot(LootData.builder()
                        .entry(LootCollections.ANTHROPOGENIC)
                        .build())
                .build();
    }

    @Override
    public void curioTick(String identifier, int index, LivingEntity entity, ItemStack stack) {
        Level level = entity.getCommandSenderWorld();

        if (level.isClientSide())
            return;

        ServerLevel serverLevel = (ServerLevel) level;
        LivingEntity target = getTarget(serverLevel, stack);

        if (target != null) {
            double radius = getAbilityValue(stack, "backstab", "distance");
            double step = 0.15D;
            int offset = 16;

            double len = (float) (2 * Math.PI * radius);
            int num = (int) (len / step);

            for (int i = 0; i < num; i++) {
                double angle = Math.toRadians(((360F / num) * i) + (360F * ((((len / step) - num) / num) / len)));

                double extraX = (radius * Math.sin(angle)) + target.getX();
                double extraZ = (radius * Math.cos(angle)) + target.getZ();
                double extraY = target.getY() + target.getBbHeight() * 0.5F;

                boolean foundPos = false;

                int tries;

                for (tries = 0; tries < offset * 2; tries++) {
                    Vec3 vec = new Vec3(extraX, extraY, extraZ);
                    BlockPos pos = new BlockPos((int) vec.x, (int) vec.y, (int) vec.z);

                    BlockState state = serverLevel.getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(serverLevel, pos);

                    if (shape.isEmpty()) {
                        if (!foundPos) {
                            extraY -= 1;

                            continue;
                        }
                    } else
                        foundPos = true;

                    if (shape.isEmpty())
                        break;

                    AABB aabb = shape.bounds();

                    if (!aabb.move(pos).contains(vec)) {
                        if (aabb.maxY >= 1F) {
                            extraY += 1;

                            continue;
                        }

                        break;
                    }

                    extraY += step;
                }

                if (tries < offset * 2)
                    serverLevel.sendParticles(ParticleUtils.constructSimpleSpark(new Color(50 + serverLevel.getRandom().nextInt(50), 0, 255), 0.25F, 3, 0.75F),
                            extraX, extraY + 0.2F, extraZ, 1, 0.05, 0.05, 0.05, 0.025);
            }
        }

        if (!canHide(entity)) {
            EntityUtils.removeAttribute(entity, stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);

            if (target != null && (target.isDeadOrDying() || target.position().distanceTo(entity.position()) >= getAbilityValue(stack, "backstab", "distance")))
                NBTUtils.clearTag(stack, TAG_TARGET);
        } else {
            entity.addEffect(new MobEffectInstance(EffectRegistry.VANISHING.get(), 5, 0, false, false));

            EntityUtils.applyAttribute(entity, stack, Attributes.MOVEMENT_SPEED, (float) getAbilityValue(stack, "vanish", "speed"), AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }

    @Nullable
    private static LivingEntity getTarget(Level level, ItemStack stack) {
        if (level.isClientSide())
            return null;

        ServerLevel serverLevel = (ServerLevel) level;

        String string = NBTUtils.getString(stack, TAG_TARGET, "");

        if (string.isEmpty())
            return null;

        if (!(serverLevel.getEntity(UUID.fromString(string)) instanceof LivingEntity target) || target.isDeadOrDying()) {
            NBTUtils.clearTag(stack, TAG_TARGET);

            return null;
        }

        return target;
    }

    private static boolean canHide(LivingEntity entity) {
        ItemStack stack = EntityUtils.findEquippedCurio(entity, ItemRegistry.MIDNIGHT_ROBE.get());

        if (!(stack.getItem() instanceof IRelicItem relic))
            return false;

        Level world = entity.getCommandSenderWorld();
        BlockPos position = entity.blockPosition().above();

        double light = relic.getAbilityValue(stack, "vanish", "light");

        return relic.isAbilityTicking(stack, "vanish") && NBTUtils.getString(stack, TAG_TARGET, "").isEmpty()
                && world.getBrightness(LightLayer.BLOCK, position) + world.getBrightness(LightLayer.SKY, position) / 2D <= (world.isNight() ? light * 1.5D : light);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        EntityUtils.removeAttribute(slotContext.entity(), stack, Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public LayerDefinition constructLayerDefinition() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.4F), 0.0F);

        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 25).addBox(-5.0F, -1.275F, -1.75F, 10.0F, 3.0F, 6.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(0.0F, 0.0F, 1.5F, 0.3927F, 0.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -0.5F, -3.0F, 10.0F, 7.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition cube_r2 = body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(32, 8).addBox(-5.0F, -6.6F, -1.6F, 10.0F, 8.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(32, 0).addBox(-5.0F, -6.6F, 4.4F, 10.0F, 8.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 28).mirror().addBox(-4.99F, -6.6F, -1.6F, 0.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(0, 28).addBox(4.99F, -6.6F, -1.6F, 0.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 17.9046F, 1.5671F, 0.3491F, 0.0F, 0.0F));

        PartDefinition cube_r3 = body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 13).addBox(-5.0F, -4.65F, -3.9F, 10.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 10.5F, 1.5F, 0.1309F, 0.0F, 0.0F));

        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 43).addBox(-1.0F, -2.25F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(6.0F, 2.0F, 0.0F));

        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(-3.0F, -2.25F, -2.0F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.25F)).mirror(false), PartPose.offset(-6.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public List<String> headParts() {
        return Lists.newArrayList("head");
    }

    @Override
    public List<String> bodyParts() {
        return Lists.newArrayList("right_arm", "left_arm", "body");
    }

    @Mod.EventBusSubscriber
    public static class ServerEvents {
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            LivingEntity target = event.getEntity();
            Level level = target.getCommandSenderWorld();

            if (!(event.getSource().getEntity() instanceof Player player)
                    || level.isClientSide() || target.getStringUUID().equals(player.getStringUUID()))
                return;

            ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.MIDNIGHT_ROBE.get());

            if (!(stack.getItem() instanceof IRelicItem relic) || !canHide(player) || player.position().distanceTo(new Vec3(target.getX(),
                    player.getY(), target.getZ())) > relic.getAbilityValue(stack, "backstab", "distance"))
                return;

            relic.spreadExperience(player, stack, Math.round(event.getAmount() * 0.5F));

            event.setAmount((float) (event.getAmount() * relic.getAbilityValue(stack, "backstab", "damage")));

            NBTUtils.setString(stack, TAG_TARGET, target.getStringUUID());
        }
    }
}