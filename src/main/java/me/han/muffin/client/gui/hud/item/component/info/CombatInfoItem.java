package me.han.muffin.client.gui.hud.item.component.info;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.events.client.KeyPressedEvent;
import me.han.muffin.client.event.events.client.MouseEvent;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.manager.managers.HoleManager;
import me.han.muffin.client.manager.managers.ItemManager;
import me.han.muffin.client.module.modules.combat.AutoCrystalModule;
import me.han.muffin.client.module.modules.combat.AutoTrapModule;
import me.han.muffin.client.module.modules.player.SelfShootModule;
import me.han.muffin.client.utils.block.HoleType;
import me.han.muffin.client.utils.client.BindUtils;
import me.han.muffin.client.utils.entity.EntityUtil;
import me.han.muffin.client.utils.extensions.mc.entity.EntityKt;
import me.han.muffin.client.utils.extensions.mc.item.OperationKt;
import me.han.muffin.client.value.BindValue;
import me.han.muffin.client.value.Value;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTippedArrow;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import org.lwjgl.input.Keyboard;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CombatInfoItem extends HudItem {
    private final Value<Boolean> hole = new Value<>(true, "Hole");
    private final Value<Boolean> selfHealth = new Value<>(false, "Health");
    private final Value<Boolean> totem = new Value<>(true, "Totems");
    private final Value<Boolean> obbyCounter = new Value<>(false, "Obby");
    private final Value<Boolean> crystalCounter = new Value<>(true, "CrystalAmount");
    private final Value<Boolean> EXP = new Value<>(true, "EXPCounter");
    private final Value<Boolean> Crystal = new Value<>(true, "Crystal");
    private final Value<Boolean> Surround = new Value<>(false, "Surround");
    private final Value<Boolean> holeFiller = new Value<>(true, "HoleFiller");
    private final Value<Boolean> Trap = new Value<>(false, "Trap");
    private final Value<Boolean> STRENGTH = new Value<>(true, "Strength");
    private final Value<Boolean> WEAKNESS = new Value<>(true, "WKS");
    private final Value<Boolean> arrowStatusDetector = new Value<>(true, "ArrowStatus");
    private final BindValue<Integer> arrowSwitchBind = new BindValue<>((Integer) Keyboard.KEY_NONE, "ArrowSwitch");

    public CombatInfoItem() {
        super("CombatInfo", HudCategory.Info, 2, 150);
        addSettings(hole, selfHealth, totem, EXP, crystalCounter, Crystal, Trap, STRENGTH, WEAKNESS, arrowStatusDetector, arrowSwitchBind);
        Muffin.getInstance().getEventManager().addEventListener(this);
    }

    String totalTotem;
    String totalEBottle;
    String totalCrystal;

    String selfHP;

    String caStatus;
    String trapStatus;

    String strengthStatus;
    String weaknessStatus;

    String holeStatus;

    String arrowStatus;

    private final DecimalFormat dfHealth = new DecimalFormat("#.#");

    private String getHoleType() {
        HoleType holeTypes = HoleManager.INSTANCE.getHoleInfo(Globals.mc.player).getType();

        if (holeTypes == HoleType.None) return ChatFormatting.AQUA + "None";
        if (holeTypes == HoleType.Obsidian) return ChatFormatting.DARK_RED + "Unsafe";
        if (holeTypes == HoleType.Bedrock) return ChatFormatting.DARK_GREEN + "Safe";

        return ChatFormatting.DARK_RED + "Unsafe";
    }

    private String getSelfHP() {
        dfHealth.setRoundingMode(RoundingMode.HALF_UP);
        float health = EntityKt.getRealHealth(Globals.mc.player);
        String hp = dfHealth.format(health);
        if (health <= 8) return ChatFormatting.RED + hp;
        return ChatFormatting.GREEN + hp;
    }

    @Listener
    private void onMouseClicked(MouseEvent event) {
        if (EntityUtil.fullNullCheck()) return;
        if (BindUtils.INSTANCE.checkIsClickedToggle(arrowSwitchBind.getValue())) doArrowSwitch();
    }

    @Listener
    private void onKeyPressed(KeyPressedEvent event) {
        if (EntityUtil.fullNullCheck()) return;
        if (BindUtils.INSTANCE.checkIsClickedToggle(arrowSwitchBind.getValue())) doArrowSwitch();
    }

    private final List<Integer> arrowsSlot = new ArrayList<>();

    /*
    private static boolean checkShootArrowIsBad() {
        int minValue = Integer.MAX_VALUE;
        Pair<Integer, Boolean> pairStatus = new Pair<>(-1, false);

        for (int i = 0; i < 45; i++) {
            ItemStack stack = Globals.mc.player.inventoryContainer.getInventory().get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemTippedArrow)) continue;
            for (PotionEffect effect : PotionUtils.getEffectsFromStack(stack)) {
                pairStatus = new Pair<>(i, effect.getPotion().isBadEffect() || SelfShootModule.INSTANCE.getBadEffects().contains(effect.getPotion()));
            }
        }

        int slot = pairStatus.getFirst();
        if (slot == -1) return false;

        return pairStatus.getSecond();
    }
     */

    private static boolean checkShootArrowIsBad() {
        for (int i = 0; i < 45; i++) {
            ItemStack stack = Globals.mc.player.inventoryContainer.getInventory().get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemTippedArrow)) continue;
            for (PotionEffect effect : PotionUtils.getEffectsFromStack(stack)) {
                return effect.getPotion().isBadEffect() || SelfShootModule.INSTANCE.getBadEffects().contains(effect.getPotion());
            }
        }
        return false;
    }

    private void doArrowSwitch() {
        arrowsSlot.clear();
        for (int i = 0; i < 45; i++) {
            ItemStack stack = Globals.mc.player.inventoryContainer.getInventory().get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemTippedArrow)) continue;
            arrowsSlot.add(i);
        }

        if (arrowsSlot.isEmpty()) return;

        int minSlot = Integer.MAX_VALUE;
        int maxSlot = Integer.MAX_VALUE;

        int priorityArrow = -1;

        for (int arrowSlot : arrowsSlot) {
            if (arrowSlot < maxSlot) {
                priorityArrow = arrowSlot;
                maxSlot = arrowSlot;
            }
        }

        if (priorityArrow == -1) return;

        int closest = priorityArrow;

        for (int arrowSlot : arrowsSlot) {
            final int diff = Math.abs(arrowSlot - priorityArrow);
            if (diff < minSlot) {
                minSlot = arrowSlot;
                closest = arrowSlot;
            }
        }

        if (closest == -1) return;

        OperationKt.moveToSlot(closest, priorityArrow);
    }

    @Override
    public void updateTicking() {
        super.updateTicking();

        if (EntityUtil.fullNullCheck()) return;

        if (hole.getValue()) {
            holeStatus = getHoleType();
        }

        if (selfHealth.getValue()) {
            selfHP = getSelfHP();
        }

        if (totem.getValue()) {
            int totems = ItemManager.totemStack.getCount();
            totalTotem = ((totems > 3) ? ChatFormatting.DARK_GREEN.toString() : ChatFormatting.DARK_RED.toString()) + totems;
        }

        if (crystalCounter.getValue()) {
            int crystals = ItemManager.crystalStack.getCount();
            totalCrystal = ((crystals > 64) ? ChatFormatting.DARK_PURPLE.toString() : ChatFormatting.DARK_RED.toString()) + crystals;
        }

        if (EXP.getValue()) {
            int boe = ItemManager.expStack.getCount();
            totalEBottle = ((boe > 64) ? ChatFormatting.YELLOW.toString() : ChatFormatting.DARK_RED.toString()) + boe;
        }

        if (Crystal.getValue()) {
            caStatus = (AutoCrystalModule.INSTANCE.isEnabled() ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED) + "CA";
        }

        if (Trap.getValue()) {
            trapStatus = (AutoTrapModule.INSTANCE.isEnabled() ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED) + "Trap";
        }

        if (STRENGTH.getValue()) {
            strengthStatus = (Globals.mc.player.isPotionActive(MobEffects.STRENGTH) ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED) + "STR";
        }

        if (WEAKNESS.getValue()) {
            weaknessStatus = (Globals.mc.player.isPotionActive(MobEffects.WEAKNESS) ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED) + "WKS";
        }

        if (arrowStatusDetector.getValue()) {
            arrowStatus =  (checkShootArrowIsBad() ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED) + "AST";
        }

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        int y = getY();
        float height = Muffin.getInstance().getFontManager().getStringHeight();

        if (hole.getValue() && holeStatus != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(holeStatus, getX(), y);
            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();
        }

        if (totem.getValue() && totalTotem != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(totalTotem, getX(), y);
            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();
        }

        if (selfHealth.getValue() && selfHP != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(selfHP, getX(), y);
            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();
        }

        if (crystalCounter.getValue() && totalCrystal != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(totalCrystal, getX(), y);
            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();
        }

        if (EXP.getValue() && totalEBottle != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(totalEBottle, getX(), y);
            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();
        }

        if (Crystal.getValue() && caStatus != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(caStatus, getX(), y);
            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();
        }

        if (Trap.getValue() && trapStatus != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(trapStatus, getX(), y);
            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();
        }

        if (STRENGTH.getValue() && strengthStatus != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(strengthStatus, getX(), y);
            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();
        }

        if (WEAKNESS.getValue() && weaknessStatus != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(weaknessStatus, getX(), y);
            y += 12;
            height += Muffin.getInstance().getFontManager().getStringHeight();
        }

        if (arrowStatusDetector.getValue() && arrowStatus != null) {
            Muffin.getInstance().getFontManager().drawStringWithShadow(arrowStatus, getX(), y);
        }

        setWidth(Muffin.getInstance().getFontManager().getStringWidth(holeStatus));
        setHeight(height * 1.5f);
    }


}