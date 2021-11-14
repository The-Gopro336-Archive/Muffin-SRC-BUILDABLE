package me.han.muffin.client.gui;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import me.han.muffin.client.gui.click.ClickGui;
import me.han.muffin.client.gui.click.item.Button;
import me.han.muffin.client.gui.click.item.Item;
import me.han.muffin.client.module.modules.other.ClickGUI;
import me.han.muffin.client.utils.color.Colour;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.BlurUtil;
import me.han.muffin.client.utils.render.RenderUtils;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public abstract class Panel {

    private final List<Item> items = new ArrayList<>();
    private final String label;

    private int x;
    private int y;

    private int x2;
    private int y2;

    private int width;
    private final int height;

    private boolean open;
    public boolean drag;

    public int red;
    public int green;
    public int blue;

    private int animatedAlpha;
    private int totalHeight;
    private float fade;
    private boolean hidden = false;

    private int clickedMouseX = 0;
    private int clickedMouseY = 0;

    // animated colours
    private int topInnerAlpha;
    private int topOuterAlpha;

    private int textAlpha;

    private int innerAlpha;
    private int outerAlpha;

    private int blurAlpha;

    private int animatedCount;
  //  public TimeAnimation linear;
    public Colour panelBackgroundColour = new Colour(0,0, 0,0);
    public Colour panelTextColour = new Colour(0,0, 0,0);

    public Panel(String label, int x, int y, boolean open) {
      //  linear = (TimeAnimation) new TimeAnimation().setDuration(20).setTransition(Transition.INVERSE_STEEP_CURVE);
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = 83;
        this.height = 16;
        this.open = open;
        setupItems();
    }

    /**
     * dont remove, actually has a use (ClickGui.java)
     */
    public abstract void setupItems();

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
//        if (open) linear.play();
//        else linear.reverse().play();

        width = 83 + ClickGUI.INSTANCE.getGuiWidth().getValue() * 2;
        drag(mouseX, mouseY);
        float totalItemHeight = open ? getTotalItemHeight() - 2F : 0F;
        totalHeight = (int) totalItemHeight;

            if (topInnerAlpha < 155)
                topInnerAlpha += 15 * partialTicks;
            if (topInnerAlpha > 155)
                topInnerAlpha = 155;

            if (topOuterAlpha < 205)
                topOuterAlpha += 15 * partialTicks;
            if (topOuterAlpha > 205)
                topOuterAlpha = 205;

            if (textAlpha < panelTextColour.getA())
                textAlpha += 15 * partialTicks;
            if (textAlpha > panelTextColour.getA())
                textAlpha = panelTextColour.getA();

            if (innerAlpha < panelBackgroundColour.getA())
                innerAlpha += 15 * partialTicks;
            if (innerAlpha > panelBackgroundColour.getA())
                innerAlpha = panelBackgroundColour.getA();

            if (outerAlpha < 225)
                outerAlpha += 15 * partialTicks;
            if (outerAlpha > 225)
                outerAlpha = 225;

            if (blurAlpha < 10)
                blurAlpha += 3 * partialTicks;
            if (blurAlpha > 10)
                blurAlpha = 10;

        if (animatedCount < 90)
            animatedCount += 10 * partialTicks;
        if (animatedCount > 90)
            animatedCount = 90;

 /*
            if (topInnerAlpha > 155)
                topInnerAlpha -= 15 * partialTicks;
            if (topInnerAlpha < 0)
                topInnerAlpha = 0;

            if (topOuterAlpha > 205)
                topOuterAlpha -= 15 * partialTicks;
            if (topOuterAlpha < 0)
                topOuterAlpha = 0;

            if (textAlpha > 255)
                textAlpha -= 15 * partialTicks;
            if (textAlpha < 0)
                textAlpha = 0;

            if (innerAlpha > 95)
                innerAlpha -= 15 * partialTicks;
            if (innerAlpha < 0)
                innerAlpha = 0;

            if (outerAlpha > 225)
                outerAlpha -= 15 * partialTicks;
            if (outerAlpha < 0)
                outerAlpha = 0;

            if (blurAlpha > 30)
                blurAlpha -= 2 * partialTicks;
            if (blurAlpha < 0)
                blurAlpha = 0;

  */

        RenderUtils.drawGradientRect(x - 3, y - 1.5F, (x + width) + 3, (y + height) - 6.7F, ColourUtils.toRGBA(this.red, this.green, this.blue, topInnerAlpha), ColourUtils.toRGBA(this.red, this.green, this.blue, topOuterAlpha));
        //  RenderUtils.drawRect(x - 3, y - 1.5F, (x + width) + 3, y + height - 6, ColourUtils.toRGBA(this.red, this.green, this.blue, 235));
        //    RenderUtils.drawGradientRect(x, y - 1.5F, (x + width), y + height - 6, ColourUtils.toRGBA(this.red, this.green, this.blue, 120), ColourUtils.toRGBA(this.red, this.green, this.blue, 140));

        if (ClickGui.getClickGui().guiFont != null)
            ClickGui.getClickGui().guiFont.drawCenteredStringWithShadow(getName(), x - 3 + width / 2f, y + 1, ColourUtils.toRGBA(panelTextColour.getR(), panelTextColour.getG(), panelTextColour.getB(), textAlpha));
        else
            Globals.mc.fontRenderer.drawStringWithShadow(getName(), x + width / 4F, y + 1, ColourUtils.toRGBA(panelTextColour.getR(), panelTextColour.getG(), panelTextColour.getB(), textAlpha));

        //   RenderUtils.drawRect(x, y + 10.5F, (x + width), y + height + totalItemHeight, ColourUtils.toRGBA(15,15,15,120));

  //      RenderUtils.drawBorderedRectReliant(x, y + 9.4F, x + width, (y + getFade()) - 1.5F, 0.5f, ColourUtils.toRGBA(90, 90, 90, 95), ColourUtils.toRGBA(this.red, this.green, this.blue, 225));

        if (ClickGUI.INSTANCE.getPanelLineThickness().getValue() > 0.0) {
            if (ClickGUI.INSTANCE.getPanelBackground().getValue()) {
                RenderUtils.drawBorderedRectReliant(x, y + 9.4F, x + width, (y + fade) - 1.5F, ClickGUI.INSTANCE.getPanelLineThickness().getValue(), ColourUtils.toRGBA(panelBackgroundColour.getR(), panelBackgroundColour.getG(), panelBackgroundColour.getB(), innerAlpha), ColourUtils.toRGBA(this.red, this.green, this.blue, outerAlpha));
            } else {
                RenderUtils.drawOutlineRect(x, y + 9.4F, x + width, (y + fade) - 1.5F, ClickGUI.INSTANCE.getPanelLineThickness().getValue(), ColourUtils.toRGBA(this.red, this.green, this.blue, outerAlpha));
            }
        } else {
            RenderUtils.drawRect(x, y + 9.4F, x + width, (y + fade) - 1.5F, ColourUtils.toRGBA(panelBackgroundColour.getR(), panelBackgroundColour.getG(), panelBackgroundColour.getB(), innerAlpha));
        }

        if (open) {
            //    BlurUtil.blurAreaBoarder(x, y + 10, width - 1, getFade() - 12, 30);
            //    BlurUtil.blurAreaBoarder(x, y + 10, width - 1, height + getTotalHeight() - 12, blurAlpha, 1, 1);
            if (ClickGUI.INSTANCE.getPanelBlur().getValue()) {
                BlurUtil.blurArea(x, y + 4, width - 1, height + getTotalHeight() - 6, ClickGUI.INSTANCE.getPanelBlurIntensity().getValue(), 1, 1);
            }
        }

        float y = getY() + getHeight() - 3.5F;
        for (Item item : getItems()) {
            if (item.isHidden()) continue;

            item.setLocation(x + 2F, y);
            item.setWidth(getWidth() - 4);
            item.setHeight(15 + ClickGUI.INSTANCE.getGuiHeight().getValue());
            if (y <= getY() + fade) item.drawScreen(mouseX, mouseY, partialTicks);
            y += item.getHeight() - 1.5F;
        }


    }

    private void drag(int mouseX, int mouseY) {
        if (!drag) {
            return;
        }

        ScaledResolution resolution = new ScaledResolution(Globals.mc);

        x = x2 + mouseX;
        y = y2 + mouseY;

        this.setX(Math.min(Math.max(0, x), resolution.getScaledWidth() - getWidth()));
        this.setY(Math.min(Math.max(0, y), resolution.getScaledHeight() - getHeight()));

    }

    public boolean processMouseClicked(int mouseX, int mouseY, int mouseButton) {
        clickedMouseX = mouseX;
        clickedMouseY = mouseY;

        if (mouseButton == 0 && isHovering(mouseX, mouseY)) {
            x2 = x - mouseX;
            y2 = y - mouseY;

            if (drag) {
                drag = false;
            }

            drag = true;
            return true;
        }

        if (mouseButton == 1 && isHovering(mouseX, mouseY)) {
            open = !open;
            Globals.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (open) {
            for (Item items : getItems()) {

                if (items.isHidden())
                    continue;

                if (items.getY() <= getY() + fade) {
                    items.processMouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }

        return false;
    }

    public void addButton(Button button) {
        items.add(button);
    }

    public void processMouseReleased(int mouseX, int mouseY, int releaseButton) {
        if (releaseButton == 0) {
            drag = false;
        }

        if (!open) {
            return;
        }

        getItems().forEach(item -> item.processMouseReleased(mouseX, mouseY, releaseButton));
    }

    public void processKeyPressed(char character, int key) {
        if (!open) {
            return;
        }

        getItems().forEach(item -> item.processKeyPressed(character, key));
    }

    public void onGuiClosed() {
        if (ClickGUI.INSTANCE.getPanelAnimation().getValue()) {
            fade = 0;
            animatedAlpha = 0;
            topInnerAlpha = 0;
            topOuterAlpha = 0;

            textAlpha = 1;
            innerAlpha = 0;
            outerAlpha = 0;

            blurAlpha = 0;
        }

        clickedMouseX = 0;
        clickedMouseY = 0;

      //  fade = 0;
        getItems().forEach(Item::onGuiClosed);
    }

    public void handleWheel(int x, int y, int step) {
        if (!open) {
            return;
        }

        ScaledResolution resolution = new ScaledResolution(Globals.mc);

        this.y += (step * 12);
        //this.y2 += (step * 12);

    //    this.setY(Math.min(Math.max(0, this.y), resolution.getScaledHeight() - getHeight()));


    }


    public void updateScreen() {
        if (!open) {
            return;
        }

        red = Muffin.getInstance().getFontManager().getPublicRed();
        green = Muffin.getInstance().getFontManager().getPublicGreen();
        blue = Muffin.getInstance().getFontManager().getPublicBlue();

        panelBackgroundColour = ClickGUI.INSTANCE.getPanelBackgroundColour();
        panelTextColour = ClickGUI.INSTANCE.getPanelBackgroundTextColour();

        ClickGui.getClickGui().getPanels().forEach(panel -> panel.getItems().forEach(item -> {
            item.red = red;
            item.green = green;
            item.blue = blue;
            item.textAlpha = textAlpha;
        }));

        if (Muffin.getInstance().getFontManager().getGuiFont() != null) ClickGui.getClickGui().guiFont = Muffin.getInstance().getFontManager().getGuiFont();
        else ClickGui.getClickGui().guiFont = null;

        getItems().forEach(Item::onUpdateScreen);

    }

    public void initGui() {
    //    blurAlpha = 0;
        getItems().forEach(Item::onInitGui);
    }

    public void updateFade(final int delta) {
        /*
        if (open) {
            if (fade < height + getTotalHeight()) {
                linear.play();
                fade = (float) (5.0F * linear.get());
            }
            if (fade > height + getTotalHeight()) {
                linear.cancel();
                fade = height + getTotalHeight();
            }
        } else {
            if (fade > 10) {
                linear.play();
                fade = (float) (5.0F * linear.get());
            }
            if (fade < 10) {
                linear.cancel();
                fade = 10;
            }
        }
         */


        if (open) {
            if (fade < height + getTotalHeight()) fade += 0.6F * delta;
            if (fade > height + getTotalHeight()) fade = height + getTotalHeight();
        } else {
            if (fade > 10) fade -= 0.6F * delta;
            if (fade < 10) fade = 10;
        }

    }

    public String getName() {
        return label;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean getOpen() {
        return open;
    }

    public final List<Item> getItems() {
        return items;
    }

    private boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight() - (open ? 6 : 5);
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    private float getTotalItemHeight() {
        float height = 0;
        for (Item item : getItems()) {
            height += item.getHeight() - 1.5F;
        }
        return height;
    }

    public void setX(int dragX) {
        this.x = dragX;
    }

    public void setY(int dragY) {
        this.y = dragY;
    }

    public int getTotalHeight() {
        return totalHeight;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isHidden() {
        return this.hidden;
    }

}