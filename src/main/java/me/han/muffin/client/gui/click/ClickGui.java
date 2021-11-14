package me.han.muffin.client.gui.click;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.DescriptionItem;
import me.han.muffin.client.gui.MuffinGuiScreen;
import me.han.muffin.client.gui.Panel;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.gui.click.item.ModuleButton;
import me.han.muffin.client.gui.font.AWTFontRenderer;
import me.han.muffin.client.gui.font.MinecraftFontRenderer;
import me.han.muffin.client.gui.particle.ParticleSystem;
import me.han.muffin.client.manager.managers.ModuleManager;
import me.han.muffin.client.module.Module;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.render.BlurUtil;
import me.han.muffin.client.utils.render.RenderUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClickGui extends MuffinGuiScreen {
    private static ClickGui clickGui;
    private static final ResourceLocation BLUR_LOCATION = new ResourceLocation("shader/blur/blur.json");
    private static final ParticleSystem particleSystem = new ParticleSystem(90);
    public MinecraftFontRenderer guiFont = Muffin.getInstance().getTtfFontManager().getCFont("Roboto 16");
    private final List<Panel> panels = new ArrayList<>();

    public DescriptionItem descriptionItem;

    public ClickGui() {
        load();
    }

    public static ClickGui getClickGui() {
        return clickGui == null ? clickGui = new ClickGui() : clickGui;
    }

    private void load() {
        int x = -84;
        for (Module.Category moduleType : Module.Category.values())
            if (!moduleType.isHidden()) {
                panels.add(new Panel(moduleType.getName(), x += 90 + ClickGUI.INSTANCE.getGuiWidth().getValue() * 2, 4, true) {
                    @Override
                    public void setupItems() {
                        ModuleManager.modules.forEach(module -> {
                            if (module.getCategory() == moduleType) {
                                addButton(new ModuleButton(module));
                            }
                        });
                    }
                });
            }

        panels.forEach(panel -> panel.getItems().sort(Comparator.comparing(Item::getName)));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        AWTFontRenderer.Companion.setAssumeNonVolatile(true);
        if (ClickGUI.INSTANCE.getGuiBackground().getValue() == ClickGUI.BackgroundMode.Black) {
            drawDefaultBackground();
        } else if (ClickGUI.INSTANCE.getGuiBackground().getValue() == ClickGUI.BackgroundMode.Blur) {
            BlurUtil.blurAll(ClickGUI.INSTANCE.getPanelBlurIntensity().getValue());
        }

        if (ClickGUI.INSTANCE.getDrawParticle().getValue()) {
            ScaledResolution sr = new ScaledResolution(mc);
            final int width = sr.getScaledWidth();
            final int height = sr.getScaledHeight();
            particleSystem.draw(mouseX * width / Globals.mc.displayWidth, height - mouseY * height / Globals.mc.displayHeight - 1);
        }

        descriptionItem = null;

        panels.forEach(panel -> panel.updateFade(RenderUtils.deltaTime));
        panels.forEach(panel -> panel.drawScreen(mouseX, mouseY, partialTicks));

        if (ClickGUI.INSTANCE.getDrawDescription().getValue() && descriptionItem != null) descriptionItem.renderDescription();
        AWTFontRenderer.Companion.setAssumeNonVolatile(false);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        panels.forEach(panel -> panel.processMouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        panels.forEach(panel -> panel.processMouseReleased(mouseX, mouseY, state));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            if (mc.currentScreen == null) {
                mc.setIngameFocus();
            }
        } else {
            panels.forEach(panel -> panel.processKeyPressed(typedChar, keyCode));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Integer.signum(Mouse.getEventDWheel());
        if (dWheel != 0) {
            int x = Mouse.getEventX() * this.width / mc.displayWidth;
            int y = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;
            panels.forEach(panel -> panel.handleWheel(x, y, dWheel));
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (Globals.mc.entityRenderer.isShaderActive()) {
            Globals.mc.entityRenderer.stopUseShader();
        }
        panels.forEach(Panel::onGuiClosed);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        panels.forEach(Panel::updateScreen);
    }

    @Override
    public void initGui() {
        super.initGui();
        if (ClickGUI.INSTANCE.getGuiBackground().getValue() == ClickGUI.BackgroundMode.Blur) {
            if (!Globals.mc.entityRenderer.isShaderActive()) {
                Globals.mc.entityRenderer.loadShader(BLUR_LOCATION);
            }
        }
        panels.forEach(Panel::initGui);
    }

    public final List<Panel> getPanels() {
        return panels;
    }

}