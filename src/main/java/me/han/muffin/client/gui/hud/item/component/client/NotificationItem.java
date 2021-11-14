package me.han.muffin.client.gui.hud.item.component.client;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.gui.hud.item.HudItem;
import me.han.muffin.client.manager.managers.NotificationManager;
import me.han.muffin.client.utils.color.ColourUtils;
import me.han.muffin.client.utils.render.RenderUtils;

public class NotificationItem extends HudItem {

    public NotificationItem() {
        super("Notifications", HudCategory.Client, 100, 2);
    }

    @Override
    public void updateTicking() {
        super.updateTicking();

        synchronized (NotificationManager.notifications) {
            NotificationManager.notifications.removeIf(NotificationManager.Notification::isDecayed);
        }

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        float x = getX();
        float y = getY();
        float maxWidth = 0f;

        synchronized (NotificationManager.notifications) {
            for (NotificationManager.Notification notification : NotificationManager.notifications) {
                float width = Muffin.getInstance().getFontManager().getStringWidth(notification.getDescription()) + 1.5f;
                int color = 255;

                switch (notification.getType()) {
                    case Info:
                        color = Muffin.getInstance().getFontManager().getColor();
                        break;
                    case Success:
                        color = ColourUtils.Colors.GREEN;
                        break;
                    case Warning:
                        color = ColourUtils.Colors.YELLOW;
                        break;
                    case Error:
                        color = ColourUtils.Colors.RED;
                        break;
                }

                RenderUtils.rectangle(x - 4.0f, y, x - 1.5f, y + 13, color);
                RenderUtils.drawRect(x - 1.5f, y, x + width, y + 13, 0x75101010);
                Muffin.getInstance().getFontManager().drawStringWithShadow(notification.getDescription(), x, y + 5, color);

                if (width >= maxWidth) maxWidth = width;
                y -= 13;
            }
        }

        setHeight(10f);
        setWidth(maxWidth);
    }

    public enum FadeState {
        IN, STAY, OUT, END
    }

}