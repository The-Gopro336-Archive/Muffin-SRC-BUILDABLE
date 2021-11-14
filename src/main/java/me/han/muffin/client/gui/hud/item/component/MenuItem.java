package me.han.muffin.client.gui.hud.item.component;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.gui.Panel;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.gui.hud.item.HudButton;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.manager.managers.HudManager;
import me.han.muffin.client.utils.render.RenderUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MenuItem extends HudItem {

    private final List<Panel> panels = new ArrayList<>();

    public MenuItem() {
        super("Menu", 80, 30);
        setHidden(false);
        addFlag(HudItem.onlyVisibleInHudEditor);
        if (getPanels().isEmpty()) load();
    }


    private void load() {

        int posX = -84;
/*
        for (HudItem.HudCategory hudType : HudItem.HudCategory.values())
            panels.add(new Panel(hudType.getName(), posX += 90, 4, true) {
                @Override
                public void setupItems() {
                    VerifyKt.verify();
                    LoaderKt.checkIdentity();
                    ConfirmationKt.confirmIsValid();
                    Muffin.getInstance().getHudManager().items.forEach(hudItem -> {
                        if (hudItem.getCategory() == hudType) {
                            addButton(new HudButton(hudItem));
                        }
                    });
                }
            });

 */



        panels.add(new Panel("Menu", getX(), getY(), true) {
            @Override
            public void setupItems() {
                HudManager.getHudManager().items.forEach(item -> addButton(new HudButton(item)));
            }
        });


        panels.forEach(panel -> panel.getItems().sort(Comparator.comparing(Item::getName)));
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        panels.forEach(Panel::updateScreen);
        panels.forEach(panel -> panel.getItems().forEach(item -> {
            item.red = Muffin.getInstance().getFontManager().getPublicRed();
            item.green = Muffin.getInstance().getFontManager().getPublicGreen();
            item.blue = Muffin.getInstance().getFontManager().getPublicBlue();
        }));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        panels.forEach(panel -> panel.updateFade(RenderUtils.deltaTime));
        panels.forEach(panel -> panel.drawScreen(mouseX, mouseY, partialTicks));
        panels.forEach(panel -> panel.setX(getX()));
        panels.forEach(panel -> panel.setY(getY()));

        for (Panel panel : getPanels()) {
            setWidth(panel.getWidth());
            setHeight(panel.getHeight() - 6);
        }

    }

    @Override
    public void processKeyPressed(char character, int key) {
        super.processKeyPressed(character, key);
        panels.forEach(panel -> panel.processKeyPressed(character, key));
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int mouseButton) {
        super.onMouseClick(mouseX, mouseY, mouseButton);
        for (Panel panel : getPanels())
            return panel.processMouseClicked(mouseX, mouseY, mouseButton);

        return super.onMouseClick(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onMouseRelease(int mouseX, int mouseY, int mouseButton) {
        super.onMouseRelease(mouseX, mouseY, mouseButton);
        panels.forEach(panel -> panel.processMouseReleased(mouseX, mouseY, mouseButton));
    }

    public List<Panel> getPanels() {
        return panels;
    }

}