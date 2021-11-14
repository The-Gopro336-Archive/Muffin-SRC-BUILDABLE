package me.han.muffin.client.utils.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.event.EventStageable;
import me.han.muffin.client.event.events.client.UpdateEvent;
import team.stiff.pomelo.impl.annotated.handler.annotation.Listener;

import java.util.function.Predicate;

public class HotbarSelectionService {
    private static HotbarSelectionService instance = null;

    public static HotbarSelectionService getInstance() {
        return instance;
    }

    private int originalIndex = -1;
    private long ticksElapsed = -1;

    private int lastSetIndex = -1;
    private Predicate<Long> resetCondition = it -> true;

    public HotbarSelectionService() {
        instance = this;
        Muffin.getInstance().getEventManager().addEventListener(this);
    }

    public ResetFunction setSelected(final int index, boolean reset, Predicate<Long> condition) {
        if (index < 0 || index > LocalPlayerInventory.getHotbarSize() - 1) {
            throw new IllegalArgumentException(
                    "index must be between 0 and " + (LocalPlayerInventory.getHotbarSize() - 1));
        }

        final int current = selected();

        if (!reset) {
            select(index);
            if (originalIndex != -1) {
                originalIndex = index;
            }

            return () -> select(index);
        } else {
            if (current != index) {
                if (originalIndex == -1) {
                    originalIndex = current;
                }

                lastSetIndex = index;
                resetCondition = MoreObjects.firstNonNull(condition, it -> true);

                select(index);
            }
            ticksElapsed = 0;

            return () -> {
                if (index == selected() && lastSetIndex == index) {
                    select(current);
                    reset();
                }
            };
        }
    }

    public void resetSelected() {
        if (originalIndex != -1 && selected() == lastSetIndex) {
            select(originalIndex);
        }
        reset();
    }

    private void reset() {
        originalIndex = -1;
        ticksElapsed = -1;
        lastSetIndex = -1;
        resetCondition = it -> true;
    }

    @Listener
    public void onPlayerUpdate(UpdateEvent event) {
        if (event.getStage().equals(EventStageable.EventStage.PRE)) {
            if (Globals.mc.world == null || Globals.mc.player == null) {
                reset();
                return;
            }

            if (originalIndex != -1 && resetCondition.test(ticksElapsed)) {
                resetSelected();
            }
            if (ticksElapsed != -1) {
                ++ticksElapsed;
            }
        }
    }

    private static void select(int index) {
        if (Globals.mc.player == null) {
            return;
        }
        LocalPlayerInventory.getInventory().currentItem = index;
    }

    private static int selected() {
        return Globals.mc.player == null ? -1 : LocalPlayerInventory.getSelected().getIndex();
    }

    public interface ResetFunction {

        void revert();
    }

}