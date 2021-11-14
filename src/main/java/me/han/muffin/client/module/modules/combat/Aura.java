package me.han.muffin.client.module.modules.combat;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.client.MotionUpdateEvent;
import me.han.muffin.client.event.events.client.UpdateEvent;
import me.han.muffin.client.manager.managers.FriendManager;
import me.han.muffin.client.manager.managers.LocalMotionManager;
import me.han.muffin.client.module.Module;
import me.han.muffin.client.utils.InventoryUtils;
import me.han.muffin.client.utils.entity.EntityUtil;
import me.han.muffin.client.utils.extensions.mc.entity.EntityKt;
import me.han.muffin.client.utils.math.RayTraceUtils;
import me.han.muffin.client.utils.math.rotation.RotationUtils;
import me.han.muffin.client.utils.math.rotation.Vec2f;
import me.han.muffin.client.utils.network.LagCompensator;
import me.han.muffin.client.utils.timer.Timer;
import me.han.muffin.client.value.EnumValue;
import me.han.muffin.client.value.NumberValue;
import me.han.muffin.client.value.Value;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author han
 * TODO: rewrite and kotlin
 */
public class Aura extends Module {

    private final EnumValue<Era> era = new EnumValue<>(Era.New, "Era");
    private final EnumValue<Mode> mode = new EnumValue<>(Mode.Single, "Mode");
    private final EnumValue<Priority> priority = new EnumValue<>(Priority.Distance, "Priority");
    //  private final EnumValue<TpsSync> tpsSync = new EnumValue<>(TpsSync.Minimal, "TPSSync");
    private final Value<Boolean> players = new Value<>(true, "Players");
    private final Value<Boolean> monster = new Value<>(false, "Monster");
    private final Value<Boolean> animals = new Value<>(false, "Animals");
    private final Value<Boolean> boats = new Value<>(false, "Boats");
    private final Value<Boolean> invisibles = new Value<>(true, "Invisibles");
    private final Value<Boolean> team = new Value<>(false, "Teams");
    private final Value<Boolean> throughWalls = new Value<>(true, "ThroughWalls");
    private final NumberValue<Double> range = new NumberValue<>(6.0,0.1,6.0,0.1,"Range");

    private final EnumValue<RotationMode> rotationMode = new EnumValue<>(RotationMode.Normal, "RotationMode");

    private final NumberValue<Integer> yawSpeed = new NumberValue<>(
            v -> era.getValue() == Era.New && rotationMode.getValue() == RotationMode.Smooth, 90, 0, 180, 1,
            "YawSpeed");

    private final NumberValue<Integer> pitchSpeed = new NumberValue<>(
            v -> era.getValue() == Era.New && rotationMode.getValue() == RotationMode.Smooth, 180, 0, 180, 1,
            "PitchSpeed");

    private final NumberValue<Integer> minTurnSpeed = new NumberValue<>(
            v -> era.getValue() == Era.Old && rotationMode.getValue() == RotationMode.Smooth, 90, 0, 180, 1,
            "MinTurnSpeed");
    private final NumberValue<Integer> maxTurnSpeed = new NumberValue<>(
            v -> era.getValue() == Era.Old && rotationMode.getValue() == RotationMode.Smooth, 180, 0, 180, 1,
            "MaxTurnSpeed");
    private final NumberValue<Integer> randomFactor = new NumberValue<>(
            v -> era.getValue() == Era.Old && rotationMode.getValue() == RotationMode.Smooth, 1, 1, 5, 1,
            "RandomFactor");

    private final Value<Boolean> hitDelay = new Value<>(true, "HitDelay");
    private final NumberValue<Integer> attackSpeed = new NumberValue<>(10,0,20,1,"AttackSpeed");
    private final NumberValue<Double> ticksExisted = new NumberValue<>(50.0,0.0,300.0,2.0,"TicksExisted");
    private final NumberValue<Integer> fov = new NumberValue<>(180,30,180,10,"FOV");
    private final Value<Boolean> rayTrace = new Value<>(false, "RayTrace");
    private final Value<Boolean> autoSword = new Value<>(false, "AutoSword");
    private final Value<Boolean> swordOnly = new Value<>(true, "SwordOnly");
    private final Value<Boolean> stopSprint = new Value<>(true, "StopSprint");
    private final Value<Boolean> critConfirm = new Value<>(v -> era.getValue() == Era.New, false, "CritConfirm");

    private final List<EntityLivingBase> validTargets = new CopyOnWriteArrayList<>();
    private EntityLivingBase target = null;
    private final Timer timer = new Timer();

    private Vec2f playerRotation = new Vec2f(0.0F, 0.0F);
    Entity finalEntity = null;

    private int inactiveTicks = 0;

    public Aura() {
        super("Aura", Category.COMBAT, "Automatically attack enemies.", 15);
        addSettings(
                era,
                mode,
                priority,
                //        tpsSync,
                players,
                monster,
                animals,
                boats,
                invisibles,
                team,
                throughWalls,
                range,
                rotationMode,
                yawSpeed,
                pitchSpeed,
                minTurnSpeed,
                maxTurnSpeed,
                randomFactor,
                hitDelay,
                attackSpeed,
                ticksExisted,
                fov,
                rayTrace,
                autoSword,
                swordOnly,
                stopSprint
        );

        minTurnSpeed.setListeners(value -> {
            int minSpeed = minTurnSpeed.getValue();
            if (minSpeed > maxTurnSpeed.getValue()) {
                minTurnSpeed.setValue(maxTurnSpeed.getValue());
            }
        });

        maxTurnSpeed.setListeners(value -> {
            int maxSpeed = maxTurnSpeed.getValue();
            if (maxSpeed < minTurnSpeed.getValue()) {
                maxTurnSpeed.setValue(minTurnSpeed.getValue());
            }
        });

    }

    private enum TpsSync {
        Minimal, Average, Last
    }

    private enum Era {
        Old, New
    }

    private enum StrafeMode {
        Off,
        Strict,
        Silent
    }

    private enum Mode {
        Single,
        Switch
    }

    private enum Priority {
        Distance,
        Health,
        Armor
    }

    private enum RotationMode {
        None,
        Normal,
        Smooth
    }

    @Override
    public void onEnable() {
        if (fullNullCheck()) return;

        playerRotation = RotationUtils.INSTANCE.getPlayerRotation(1F);
        validTargets.clear();
        target = null;
        finalEntity = null;
    }

    @Override
    public String getHudInfo() {
        return mode.getFixedValue();
    }

    @Listener
    private void onPlayerUpdate(UpdateEvent event) {
        if (event.getStage() != EventStageable.EventStage.PRE) return;

        if (fullNullCheck()) return;

        if (era.getValue() != Era.New) return;

        if (inactiveTicks > 20) resetRotation();

        if (validTargets.isEmpty()) {
            if (priority.getValue().equals(Priority.Distance)) {
                EntityLivingBase target = Globals.mc.world.loadedEntityList
                        .parallelStream()
                        .filter(this::isValidTarget)
                        .map(entity -> (EntityLivingBase) entity)
                        .min((e1, e2) -> (int) (Globals.mc.player.getDistance(e1) - Globals.mc.player.getDistance(e2)))
                        .orElse(null);
                validTargets.add(target);
            } else if (priority.getValue().equals(Priority.Health)) {
                EntityLivingBase target = Globals.mc.world.loadedEntityList
                        .parallelStream()
                        .filter(this::isValidTarget)
                        .map(entity -> (EntityLivingBase) entity)
                        .min((e1, e2) -> (int) ((e1.getHealth() + e1.getAbsorptionAmount()) - (e2.getHealth() + e2.getAbsorptionAmount())))
                        .orElse(null);
                validTargets.add(target);
            } else if (priority.getValue().equals(Priority.Armor)) {
                EntityLivingBase target = Globals.mc.world.loadedEntityList
                        .parallelStream()
                        .filter(this::isValidTarget)
                        .map(entity -> (EntityLivingBase) entity)
                        .min(Comparator.comparingInt(EntityLivingBase::getTotalArmorValue))
                        .orElse(null);
                validTargets.add(target);
            }
        } else {
            validTargets.forEach(target -> {
                if (!isValidTarget(target)) {
                    validTargets.remove(target);
                }
            });
        }

        if (validTargets.isEmpty()) {
            finalEntity = null;
            resetRotation();
            return;
        }

        if (finalEntity == null) {
            finalEntity = Globals.mc.world.loadedEntityList
                    .parallelStream()
                    .filter(this::isValidTarget)
                    .filter(entity -> RotationUtils.INSTANCE.isEntityInFov(entity, fov.getValue()))
                    .map(entity -> (EntityLivingBase) entity)
                    .min((e1, e2) -> (int) (Globals.mc.player.getDistance(e1) - Globals.mc.player.getDistance(e2)))
                    .orElse(null);
        } else {
            if (!isValidTarget(finalEntity) || !RotationUtils.INSTANCE.isEntityInFov(finalEntity, fov.getValue())) {
                finalEntity = null;
            }
        }


        if (stopSprint.getValue()) slowdownMovement();
    }

    @Listener
    private void onMotionUpdate(MotionUpdateEvent event) {
        if (fullNullCheck()) return;
        inactiveTicks++;

        if (swordOnly.getValue() && !(Globals.mc.player.getHeldItemMainhand().getItem() instanceof ItemSword)) {
            resetRotation();
            return;
        }

        if (event.getStage() == EventStageable.EventStage.PRE) {
            Entity currentEntity = (era.getValue() == Era.Old) ? target : (era.getValue() == Era.New) ? finalEntity : null;
            if (currentEntity != null) LocalMotionManager.INSTANCE.addMotionBase(this, new LocalMotionManager.Motion(null, null, playerRotation));
        }

        if (era.getValue() == Era.New) {
            if (finalEntity == null) {
                return;
            }

            setRotations();
            inactiveTicks = 0;

            Entity rTraceTarget = RayTraceUtils.INSTANCE.getRaytraceEntity(range.getValue(), new Vec2f(event.getRotation().getX(), event.getRotation().getY()));
            boolean checkRTrace;

            if (rayTrace.getValue()) {
                checkRTrace = rTraceTarget != null;
            } else {
                checkRTrace = true;
            }

            float ticks = LagCompensator.INSTANCE.getAdjustTicks();

            boolean canAttack = hitDelay.getValue() ? (Globals.mc.player.getCooledAttackStrength(0.5f + ticks) >= 1.0f) : timer.passedAPS(attackSpeed.getValue());

            switch (mode.getValue()) {
                case Single:
                    if (isValidTarget(finalEntity) && canAttack && checkRTrace) {
                        attack();
                        finalEntity = null;
                        timer.reset();
                    }
                    break;
                case Switch:
                    if (isValidTarget(finalEntity) && canAttack && checkRTrace) {
                        attack();
                        validTargets.remove(finalEntity);
                        finalEntity = null;
                    }
                    break;
            }

            validTargets.forEach(target -> {
                if (!isValidTarget(target)) {
                    validTargets.remove(target);
                }
            });


        } else if (era.getValue() == Era.Old) {
            if (event.getStage() == EventStageable.EventStage.PRE) {

                if (swordOnly.getValue() && !(Globals.mc.player.getHeldItemMainhand().getItem() instanceof ItemSword)) {
                    resetRotation();
                    return;
                }

                if (validTargets.isEmpty()) {
                    if (priority.getValue().equals(Priority.Distance)) {
                        //   validTargets.addAll(getTargetsByDistance());

                        EntityLivingBase target = Globals.mc.world.loadedEntityList
                                .stream()
                                .filter(this::isValidTarget)
                                .map(entity -> (EntityLivingBase) entity)
                                .min((e1, e2) -> (int) (Globals.mc.player.getDistance(e1) - Globals.mc.player.getDistance(e2)))
                                .orElse(null);

                        validTargets.add(target);

                    } else if (priority.getValue().equals(Priority.Health)) {
                        //      validTargets.addAll(getTargetsByHealth());
                        EntityLivingBase target = Globals.mc.world.loadedEntityList
                                .stream()
                                .filter(this::isValidTarget)
                                .map(entity -> (EntityLivingBase) entity)
                                .min((e1, e2) -> (int) ((e1.getHealth() + e1.getAbsorptionAmount()) - (e2.getHealth() + e2.getAbsorptionAmount())))
                                .orElse(null);

                        validTargets.add(target);
                    } else if (priority.getValue().equals(Priority.Armor)) {
                        //       validTargets.addAll(getTargetsByArmor());
                        EntityLivingBase target = Globals.mc.world.loadedEntityList
                                .stream()
                                .filter(this::isValidTarget)
                                .map(entity -> (EntityLivingBase) entity)
                                .min(Comparator.comparingInt(EntityLivingBase::getTotalArmorValue))
                                .orElse(null);

                        validTargets.add(target);
                    }
                } else {
                    validTargets.forEach(target -> {
                        if (!isValidTarget(target)) {
                            validTargets.remove(target);
                        }
                    });
                }

                if (validTargets.isEmpty()) {
                    target = null;
                    resetRotation();
                    return;
                }

                if (target == null) {
                    target = Globals.mc.world.loadedEntityList
                            .stream()
                            .filter(this::isValidTarget)
                            .filter(entity -> RotationUtils.INSTANCE.isEntityInFov(entity, fov.getValue()))
                            .map(entity -> (EntityLivingBase) entity)
                            .min((e1, e2) -> (int) (Globals.mc.player.getDistance(e1) - Globals.mc.player.getDistance(e2)))
                            .orElse(null);
                /*
                Optional<EntityLivingBase> entity =
                        validTargets
                                .stream()
                                .filter(ent -> PlayerHelper.isAiming(PlayerRotation.getRotations(ent)[0], PlayerRotation.getRotations(ent)[1], fov.getValue()))
                                .filter(this::isValidTarget)
                                .min((entity1, entity2) -> {
                                    float entityFOV = PlayerHelper.getFOV(PlayerRotation.getRotations(entity1));
                                    float entity2FOV = PlayerHelper.getFOV(PlayerRotation.getRotations(entity2));
                                    return Float.compare(entityFOV, entity2FOV);
                                });

                entity.ifPresent(value -> target = value);
                 */
                }


                if (isValidTarget(target)) {

                    Vec2f rotation = RotationUtils.INSTANCE.getRotationToEntityClosestStrict(target);

                    if (rotationMode.getValue().equals(RotationMode.Normal)) {
                        rotation = Globals.mc.player.getDistance(target) <= 0.5 ? new Vec2f(rotation.getX(), 90) : rotation;
                        playerRotation = rotation;
                    } else if (rotationMode.getValue() == RotationMode.Smooth) {
                        final Vec2f rotationsCurrent = RotationUtils.INSTANCE.getPlayerRotation(1F);
                        final Vec2f rotationsSmooth = RotationUtils.INSTANCE.smoothRotation(
                                rotationsCurrent, new Vec2f(rotation.getX(), rotation.getY()),
                                calculateRotationSpeed() + (ThreadLocalRandom.current().nextInt(5) * randomFactor.getValue()));

                        final int randomFactor = this.randomFactor.getValue();
                        final float randomYaw = (float) ThreadLocalRandom.current().nextDouble(-randomFactor, randomFactor);
                        final float randomPitch = (float) ThreadLocalRandom.current().nextDouble(-randomFactor, randomFactor);
                        playerRotation = Globals.mc.player.getDistance(target) <= 0.5 ? new Vec2f(rotationsSmooth.plus(randomYaw, randomPitch).getX(), 90) : rotationsSmooth.plus(randomYaw, randomPitch);
                    }

                /*
                if (rotationMode.getValue().equals(RotationMode.Normal)) {
                    float[] rotations = RotationUtils.faceEntityAtLocation(target, bone.getValue());
                    event.getRotation().setYaw(rotations[0]);
                    event.getRotation().setPitch(Globals.mc.player.getDistance(target) <= 0.5 ? 90 : rotations[1]);

                } else if (rotationMode.getValue().equals(RotationMode.Basic)) {
                    float[] rotations = RotationUtils.getRotations(target, bone.getValue());
                    event.getRotation().setYaw(rotations[0]);
                    event.getRotation().setPitch(Globals.mc.player.getDistance(target) <= 0.5 ? 90 : rotations[1]);
                }
                 */

                } else {
                    validTargets.remove(target);
                    target = null;
                    resetRotation();
                }

            } else if (event.getStage() == EventStageable.EventStage.POST) {

                if (swordOnly.getValue() && !(Globals.mc.player.getHeldItemMainhand().getItem() instanceof ItemSword)) {
                    resetRotation();
                    return;
                }

                Entity rTraceTarget = RayTraceUtils.INSTANCE.getRaytraceEntity(range.getValue(), new Vec2f(event.getRotation().getX(), event.getRotation().getY()));
                boolean checkRTrace;

                if (rayTrace.getValue()) {
                    checkRTrace = rTraceTarget != null;
                } else {
                    checkRTrace = true;
                }

                //    float ticks = (float) Math.floor(-(20.0f - LagCompensator.INSTANCE.getTickRate()));
                float ticks = LagCompensator.INSTANCE.getAdjustTicks();

                boolean canAttack = hitDelay.getValue() ? (Globals.mc.player.getCooledAttackStrength(0.5f + ticks) >= 1.0f) : timer.passedAPS(attackSpeed.getValue());
                inactiveTicks = 0;

                switch (mode.getValue()) {
                    case Single:
                        if (isValidTarget(target) && canAttack && checkRTrace) {
                            attackTarget(target);
                            target = null;
                            timer.reset();
                        }
                        break;
                    case Switch:
                        if (isValidTarget(target) && canAttack && checkRTrace) {
                            attackTarget(target);
                            validTargets.remove(target);
                            target = null;
                        }
                        break;
                }

                validTargets.forEach(target -> {
                    if (!isValidTarget(target)) {
                        validTargets.remove(target);
                    }
                });

            }
        }
    }

    private void attackTarget(Entity entity) {
        boolean wasSprinting = Globals.mc.player.isSprinting();
        boolean wasSneaking = Globals.mc.player.isSneaking();
        boolean wasBlocking = Globals.mc.player.isActiveItemStackBlocking();

        if (autoSword.getValue()) {
            int swordSlot = InventoryUtils.findSwordAndAxe();
            if (swordSlot != -1) {
                if (Globals.mc.player.inventory.currentItem != swordSlot) {
                    InventoryUtils.swapSlot(swordSlot);
                }
            }
        }

        if (wasSneaking) {
            Globals.mc.player.connection.sendPacket(new CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
        }

        if (wasSprinting) {
            Globals.mc.player.connection.sendPacket(new CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.STOP_SPRINTING));
        }

        if (wasBlocking) {
            if (Globals.mc.player.getHeldItemOffhand().getItem() instanceof ItemShield) {
                /*
                Globals.mc.player.connection.sendPacket(
                        new CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM,
                                new BlockPos(Globals.mc.player),
                                EnumFacing.getFacingFromVector((float) Globals.mc.player.posX, (float) Globals.mc.player.posY, (float) Globals.mc.player.posZ)));
                 */
                Globals.mc.player.connection.sendPacket(
                        new CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM,
                                BlockPos.ORIGIN, EnumFacing.DOWN));
            }
        }

        if (CriticalsModule.INSTANCE.isEnabled()) {
            CriticalsModule.INSTANCE.doCritical(entity);
        }

        if (hitDelay.getValue()) {
            Globals.mc.playerController.attackEntity(Globals.mc.player, entity);
        } else {
            Globals.mc.player.connection.sendPacket(new CPacketUseEntity(entity));
        }

        Globals.mc.player.swingArm(EnumHand.MAIN_HAND);

        final float enchantmentLevel = (entity instanceof EntityLivingBase) ?
                EnchantmentHelper.getModifierForCreature(Globals.mc.player.getHeldItem(EnumHand.MAIN_HAND),
                        ((EntityLivingBase) entity).getCreatureAttribute()) : 0.0f;

        final boolean critAble =
                Globals.mc.player.fallDistance > 0.0f &&
                        !Globals.mc.player.onGround &&
                        !Globals.mc.player.isOnLadder() &&
                        !Globals.mc.player.isInWater() &&
                        !Globals.mc.player.isPotionActive(MobEffects.BLINDNESS) &&
                        Globals.mc.player.getRidingEntity() == null;

        if (CriticalsModule.INSTANCE.isEnabled() || critAble) {
            Globals.mc.player.onCriticalHit(entity);
        }

        if (enchantmentLevel > 0.0f) {
            Globals.mc.player.onEnchantmentCritical(entity);
        }

        Globals.mc.player.resetCooldown();

        if (wasSneaking) {
            Globals.mc.player.connection.sendPacket(new CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SNEAKING));
        }

        if (wasSprinting) {
            Globals.mc.player.connection.sendPacket(new CPacketEntityAction(Globals.mc.player, CPacketEntityAction.Action.START_SPRINTING));
        }

        if (wasBlocking) {
            if (Globals.mc.player.getHeldItemMainhand().getItem() instanceof ItemSword && Globals.mc.player.getHeldItemOffhand().getItem() instanceof ItemShield) {
                Globals.mc.playerController.processRightClick(Globals.mc.player, Globals.mc.world, EnumHand.OFF_HAND);
            }
        }

    }

    private void resetRotation() {
        playerRotation = RotationUtils.INSTANCE.getPlayerRotation(1F);
    }

    private List<EntityLivingBase> getTargetsByHealth() {
        List<EntityLivingBase> targets = new ArrayList<>();
        for (Object o : Globals.mc.world.loadedEntityList) {
            Entity entity = (Entity) o;

            if (!(entity instanceof EntityLivingBase))
                continue;

            EntityLivingBase entityLivingBase = (EntityLivingBase) entity;

            if (!isValidTarget(entityLivingBase))
                continue;

            targets.add(entityLivingBase);
        }
        targets.sort((target1, target2) -> Math.round((target2.getHealth() + target2.getAbsorptionAmount()) - (target1.getHealth() + target1.getAbsorptionAmount())));
        return targets;
    }

    private List<EntityLivingBase> getTargetsByDistance() {
        List<EntityLivingBase> targets = new ArrayList<>();
        for (Object o : Globals.mc.world.loadedEntityList) {
            Entity entity = (Entity) o;

            if (!(entity instanceof EntityLivingBase))
                continue;

            EntityLivingBase entityLivingBase = (EntityLivingBase) entity;

            if (!isValidTarget(entityLivingBase))
                continue;

            targets.add(entityLivingBase);
        }

        targets.sort((e1, e2) -> {
            double d1 = Globals.mc.player.getDistance(e1);
            double d2 = Globals.mc.player.getDistance(e2);
            return (int)(d1 - d2);
        });

        return targets;
    }

    private List<EntityLivingBase> getTargetsByArmor() {
        List<EntityLivingBase> targets = new ArrayList<>();
        for (Object o : Globals.mc.world.loadedEntityList) {
            Entity entity = (Entity) o;

            if (!(entity instanceof EntityLivingBase))
                continue;

            EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
            if (!isValidTarget(entityLivingBase))
                continue;
            targets.add(entityLivingBase);
        }

        targets.sort((e1, e2) -> {
            double d1 = e2.getTotalArmorValue();
            double d2 = e1.getTotalArmorValue();
            return (int) (d1 - d2);
        });

        return targets;
    }

    public boolean isValidTarget(Entity entity) {

        if (entity == null || entity == Globals.mc.player) {
            return false;
        }

        if (boats.getValue() && entity instanceof EntityBoat) {
            if (entity.getPassengers().contains(Globals.mc.player)) {
                return false;
            }

            if (entity.getControllingPassenger() != null &&
                    (entity.getControllingPassenger() == Globals.mc.player || FriendManager.isFriend(entity.getControllingPassenger().getName()))) {
                return false;
            }

            if (entity.getPassengers().isEmpty()) {
                return true;
            }
        }

        if (!(entity instanceof EntityLivingBase))
            return false;

        if (entity.equals(Globals.mc.player.getRidingEntity())) {
            return false;
        }

        if (!EntityKt.isAlive(entity)) {
            return false;
        }

        if (FriendManager.isFriend(entity.getName())) {
            return false;
        }

        if (getDistanceSq(entity) > (range.getValue() * range.getValue())) {
            return false;
        }

        if (Globals.mc.player.getDistance(entity) > range.getValue())
            return false;

        if (!throughWalls.getValue() && !Globals.mc.player.canEntityBeSeen(entity) && !EntityUtil.INSTANCE.canEntityFeetBeSeen(entity)) {
            return false;
        }

        if (entity.ticksExisted < ticksExisted.getValue()) {
            return false;
        }

        if (team.getValue() && entity.isOnSameTeam(Globals.mc.player)) {
            return false;
        }

        if (!invisibles.getValue() && entity.isInvisible()) {
            return false;
        }

        if (((EntityLivingBase) entity).deathTime > 0) return false;

        if (players.getValue() && entity instanceof EntityPlayer) {
            final EntityPlayer player = (EntityPlayer) entity;
            return (!player.getName().contains("Dead") && player.getEntityId() != -1337 && !player.capabilities.isCreativeMode);
        }

        if (animals.getValue() && entity instanceof EntityAnimal) {
            return !(!entity.onGround && entity.fallDistance == 0D);
        }

        return monster.getValue() && !(entity instanceof EntityPlayer) && !(entity instanceof EntityAnimal);
    }

    private float calculateRotationSpeed() {
        if (minTurnSpeed.getValue().equals(maxTurnSpeed.getValue())) {
            return 180;
        }
        return (float) ThreadLocalRandom.current().nextDouble(minTurnSpeed.getValue(), maxTurnSpeed.getValue());
    }

    public static double getDistanceSq(Entity target) {
        double xDiff = target.posX - Globals.mc.player.posX;
        double yDiff = target.posY - Globals.mc.player.posY;
        double zDiff = target.posZ - Globals.mc.player.posZ;

        if (yDiff > -0.2) {
            yDiff = target.posY - (Globals.mc.player.posY + 1.0);
        } else if (yDiff < 0.3) {
            yDiff = target.posY + 1.0 - Globals.mc.player.posY;
        }

        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    }

    public void attack() {
        if (finalEntity == null) return;

        if (rotationMode.getValue() != RotationMode.None && !isOver()) return;

        boolean wasBlocking = Globals.mc.player.isActiveItemStackBlocking();

        if (autoSword.getValue()) {
            int swordSlot = InventoryUtils.findSwordAndAxe();
            if (swordSlot != -1) {
                if (Globals.mc.player.inventory.currentItem != swordSlot) {
                    InventoryUtils.swapSlot(swordSlot);
                }
            }
        }

        if (wasBlocking && Globals.mc.player.getHeldItemOffhand().getItem() instanceof ItemShield) {
            Globals.mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }

        if (critConfirm.getValue() && CriticalsModule.INSTANCE.isEnabled()) {
            CriticalsModule.INSTANCE.doCritical(finalEntity);
        }

        if (hitDelay.getValue()) {
            Globals.mc.playerController.attackEntity(Globals.mc.player, finalEntity);
        } else {
            Globals.mc.player.connection.sendPacket(new CPacketUseEntity(finalEntity));
        }

        Globals.mc.player.swingArm(EnumHand.MAIN_HAND);

        final float n = (finalEntity instanceof EntityLivingBase) ?
                EnchantmentHelper.getModifierForCreature(Globals.mc.player.getHeldItem(EnumHand.MAIN_HAND), ((EntityLivingBase) finalEntity).getCreatureAttribute()) : 0.0f;

        final boolean b2 =
                Globals.mc.player.fallDistance > 0.0f &&
                        !Globals.mc.player.onGround &&
                        !Globals.mc.player.isOnLadder() &&
                        !Globals.mc.player.isInWater() &&
                        !Globals.mc.player.isPotionActive(MobEffects.BLINDNESS) &&
                        Globals.mc.player.getRidingEntity() == null;

        if (CriticalsModule.INSTANCE.isEnabled() || b2) {
            Globals.mc.player.onCriticalHit(finalEntity);
        }

        if (n > 0.0f) {
            Globals.mc.player.onEnchantmentCritical(finalEntity);
        }

        Globals.mc.player.resetCooldown();

        if (wasBlocking) {
            if (Globals.mc.player.getHeldItemMainhand().getItem() instanceof ItemSword && Globals.mc.player.getHeldItemOffhand().getItem() instanceof ItemShield) {
                Globals.mc.playerController.processRightClick(Globals.mc.player, Globals.mc.world, EnumHand.OFF_HAND);
            }
        }

    }

    public void setRotations() {
        if (finalEntity == null) return;

        if (rotationMode.getValue() == RotationMode.Normal) {
            Vec2f rotation = RotationUtils.INSTANCE.getRotationToEntityClosestStrict(finalEntity);
            playerRotation = new Vec2f(RotationUtils.INSTANCE.updateRotation(Globals.mc.player.rotationYaw, rotation.getX(), fov.getValue()), rotation.getY());
        } else if (rotationMode.getValue() == RotationMode.Smooth) {
            Vec2f smoothRotations = RotationUtils.INSTANCE.faceEntitySmooth(
                    playerRotation.getX(), playerRotation.getY(),
                    RotationUtils.INSTANCE.getRotationToSpecialEyeHeight(finalEntity).getX(), RotationUtils.INSTANCE.getRotationToSpecialEyeHeight(finalEntity).getY(),
                    yawSpeed.getValue(), pitchSpeed.getValue());
            playerRotation = new Vec2f(RotationUtils.INSTANCE.updateRotation(Globals.mc.player.rotationYaw, smoothRotations.getX(), fov.getValue()), smoothRotations.getY());
        }

        if (playerRotation.getY() > 90) {
            playerRotation = new Vec2f(playerRotation.getX(), 90);
        } else if (playerRotation.getY() < -90) {
            playerRotation = new Vec2f(playerRotation.getX(), -90);
        }

    }

    private boolean isOver() {
        if (finalEntity == null) return false;

        Vec2f smoothRotations = RotationUtils.INSTANCE.faceEntitySmooth(
                playerRotation.getX(), playerRotation.getY(),
                RotationUtils.INSTANCE.getRotationToSpecialEyeHeight(finalEntity).getX(), RotationUtils.INSTANCE.getRotationToSpecialEyeHeight(finalEntity).getY(),
                360, 360);

        return playerRotation.getX() <= smoothRotations.getX() + 10 && playerRotation.getX() >= smoothRotations.getX() - 10 &&
                playerRotation.getY() <= smoothRotations.getY() + 5 && playerRotation.getY() >= smoothRotations.getY() - 5;
    }

    private void slowdownMovement() {
        if (finalEntity == null) return;

        if (Globals.mc.player.hurtTime < 1) {
            double distance = Math.abs(RotationUtils.INSTANCE.normalizeAngle(Globals.mc.player.rotationYaw) - RotationUtils.INSTANCE.getRotationToSpecialEyeHeight(finalEntity).getX());
            if (distance >= 360F / fov.getValue() * 10.0D) {
                Globals.mc.player.motionX /= 1.25;
                Globals.mc.player.motionZ /= 1.25;
            }
        }
    }


}