package me.han.muffin.client.module;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.han.muffin.client.Muffin;
import me.han.muffin.client.event.events.client.ModuleEvent;
import me.han.muffin.client.preset.Preset;
import me.han.muffin.client.utils.client.BindUtils;
import me.han.muffin.client.utils.entity.EntityUtil;
import me.han.muffin.client.value.*;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class Module {

    boolean enabled;
    boolean drawn;
    int bind;
    public int modulePriority;
    String name;
    Category category;
    String description;
    boolean alwaysListening;
    boolean isToggleable;
    public boolean isStartup;

    private final List<Value> settings = new ArrayList<>();
    private final List<Preset> presets = new ArrayList<>();

    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.bind = Keyboard.KEY_NONE;
        this.description = null;
        this.enabled = false;
        this.drawn = false;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = false;
        alwaysListening = false;
    }

    public Module(String name, Category category, boolean shouldDrawn) {
        this.name = name;
        this.category = category;
        description = null;
        this.bind = Keyboard.KEY_NONE;
        this.enabled = false;
        this.drawn = shouldDrawn;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = false;
        alwaysListening = false;
    }

    public Module(String name, Category category, boolean shouldDrawn, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.bind = Keyboard.KEY_NONE;
        this.enabled = false;
        this.drawn = shouldDrawn;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = false;
        alwaysListening = false;
    }

    public Module(String name, Category category, boolean shouldDrawn, boolean shouldEnable) {
        this.name = name;
        this.category = category;
        description = null;
        this.bind = Keyboard.KEY_NONE;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = shouldEnable;
        alwaysListening = false;
    }

    public Module(String name, Category category, boolean shouldDrawn, boolean shouldEnable, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.bind = Keyboard.KEY_NONE;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = shouldEnable;
        alwaysListening = false;
    }

    public Module(String name, Category category, boolean shouldDrawn, boolean shouldEnable, String description, int priority) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.bind = Keyboard.KEY_NONE;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.modulePriority = priority;
        this.isToggleable = true;
        this.isStartup = shouldEnable;
        alwaysListening = false;
    }

    public Module(String name, Category category, boolean shouldDrawn, boolean shouldEnable, boolean alwaysListening, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.bind = Keyboard.KEY_NONE;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = shouldEnable;
        this.alwaysListening = alwaysListening;
    }

    public Module(String name, Category category, boolean shouldDrawn, boolean shouldEnable, int bind) {
        this.name = name;
        this.category = category;
        description = null;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.bind = bind;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = shouldEnable;
        alwaysListening = false;
    }

    public Module(String name, Category category, boolean shouldDrawn, boolean shouldEnable, int bind, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.bind = bind;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = shouldEnable;
        alwaysListening = false;
    }

    public Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.bind = Keyboard.KEY_NONE;
        this.description = description;
        this.enabled = false;
        this.drawn = false;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = false;
        alwaysListening = false;
    }

    public Module(String name, Category category, String description, boolean shouldDrawn) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.bind = Keyboard.KEY_NONE;
        this.enabled = false;
        this.drawn = shouldDrawn;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = false;
        alwaysListening = false;
    }

    public Module(String name, Category category, String description, int priority) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.bind = Keyboard.KEY_NONE;
        this.enabled = false;
        this.drawn = false;
        this.modulePriority = priority;
        this.isToggleable = true;
        this.isStartup = false;
        alwaysListening = false;
    }

    public Module(String name, Category category, String description, boolean shouldDrawn, boolean shouldEnable) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.bind = Keyboard.KEY_NONE;
        this.modulePriority = -1;
        this.isToggleable = true;
        this.isStartup = shouldEnable;
        alwaysListening = false;
    }

    public Module(String name, Category category, String description, boolean shouldDrawn, boolean shouldEnable, boolean isToggleable) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.bind = Keyboard.KEY_NONE;
        this.modulePriority = -1;
        this.isToggleable = isToggleable;
        this.isStartup = shouldEnable;
        alwaysListening = false;
    }

    public Module(String name, Category category, String description, boolean shouldDrawn, boolean shouldEnable, boolean isToggleable, int priority) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.bind = Keyboard.KEY_NONE;
        this.modulePriority = priority;
        this.isToggleable = isToggleable;
        this.isStartup = shouldEnable;
        alwaysListening = false;
    }

    public Module(String name, Category category, String description, boolean shouldDrawn, boolean shouldEnable, int priority) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = shouldEnable;
        this.drawn = shouldDrawn;
        this.bind = Keyboard.KEY_NONE;
        this.modulePriority = priority;
        this.isToggleable = true;
        this.isStartup = shouldEnable;
        alwaysListening = false;
    }

    public boolean fullNullCheck() {
        return EntityUtil.fullNullCheck();
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category c) {
        category = c;
    }

    public int getBind() {
        return bind;
    }

    public void setBind(int b) {
        bind = b;
    }

    public String getChatName() {
        return "[" + name + "] ";
    }

    public boolean isDrawn() {
        return this.drawn;
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onToggle() {}

    public void enable() {
        enabled = true;

        onToggle();
        onEnable();
        Muffin.getInstance().getEventManager().dispatchEvent(new ModuleEvent(ModuleEvent.Type.Enable, this));
        if (!alwaysListening) Muffin.getInstance().getEventManager().addEventListener(this);
    }

    public void disable() {
        if (!isToggleable) return;
        enabled = false;

        onToggle();
        onDisable();
        Muffin.getInstance().getEventManager().dispatchEvent(new ModuleEvent(ModuleEvent.Type.Disable, this));
        if (!alwaysListening) Muffin.getInstance().getEventManager().removeEventListener(this);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (isEnabled() != enabled) if (enabled) enable(); else disable();
    }

    public void toggle() {
        setEnabled(!isEnabled());
        onToggle();
    }

    public void draw() {
        drawn = true;
    }

    public void dontDraw() {
        drawn = false;
    }

    public void setDrawn(boolean drawn) {
        if (isDrawn() != drawn) if (drawn) draw(); else dontDraw();
    }

    public boolean isDisabled() {
        return !isEnabled();
    }

    public String getHudInfo() {
        return null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    public enum Category {
        COMBAT("Combat", false),
        EXPLOITS("Exploits", false),
        RENDER("Render", false),
        MISC("Misc", false),
        PLAYER("Player", false),
        MOVEMENT("Movement", false),
        OTHERS("Others", false),
        HIDDEN("Hidden", true);

        boolean hidden;
        String name;

        Category(String name, boolean hidden) {
            this.name = name;
            this.hidden = hidden;
        }

        public boolean isHidden() {
            return hidden;
        }

        public String getName() {
            return name;
        }
    }


    public Value getSettingByName(String alias) {
        for (Value property : settings) {
            for (String propertyAlias : property.getAliases()) {
                if (alias.equalsIgnoreCase(propertyAlias)) {
                    return property;
                }
            }
        }
        return null;
    }

    public List<Value> getSettings() {
        return settings;
    }

    protected void addSettings(Value... properties) {
        Arrays.stream(properties).forEach(p -> p.setModule(this));

        Collections.addAll(settings, properties);
 //       this.settingList.sort(Comparator.comparing(p -> p.getAliases()[0]));
    }

    protected void offsetPresets(Preset... presets) {
        this.presets.addAll(Arrays.asList(presets));
        this.presets.sort(Comparator.comparing(Preset::getName));
    }

    public Preset getPresetByLabel(String label) {
        for (Preset preset : presets) {
            if (label.equalsIgnoreCase(preset.getName())) {
                return preset;
            }
        }
        return null;
    }

    public List<Preset> getPresets() {
        return presets;
    }

    public void loadSettingConfig(File folder, JsonObject node) {
        if (!folder.exists()) return;

        File modsFolder = new File(folder, "modules");
        if (!modsFolder.exists()) modsFolder.mkdir();

        node.entrySet().forEach(entry -> {
            for (Value value : getSettings()) {
                if (value.getAliases()[0].equalsIgnoreCase(entry.getKey())) {
                    if (value.getValue() instanceof Number && !(value.getValue() instanceof Enum)) {

                        if (value.getValue() instanceof Float)
                            ((NumberValue) value).setValueConfig(entry.getValue().getAsJsonPrimitive().getAsFloat());
                        else if (value.getValue() instanceof Double)
                            ((NumberValue) value).setValueConfig(entry.getValue().getAsJsonPrimitive().getAsDouble());
                        else if (value.getValue() instanceof Long)
                            ((NumberValue) value).setValueConfig(entry.getValue().getAsJsonPrimitive().getAsLong());
                        else if (value.getValue() instanceof Integer) {
                            if (value instanceof BindValue) {
                                value.setValue(BindUtils.INSTANCE.getConvertedKeyBind(entry.getValue().getAsJsonPrimitive().getAsString()));
                                //((BindValue) value).setKeyValue(entry.getValue().getAsJsonPrimitive().getAsString());
                            } else {
                                ((NumberValue) value).setValueConfig(entry.getValue().getAsJsonPrimitive().getAsInt());
                            }
                        }
                    } else if (value.getValue() instanceof Boolean) {
                        value.setValue(entry.getValue().getAsJsonPrimitive().getAsBoolean());
                    } else if (value.getValue() instanceof Enum) {
                        ((EnumValue) value).setEnumValue(entry.getValue().getAsJsonPrimitive().getAsString());
                    } else if (value.getValue() instanceof String) {
                        value.setValue(entry.getValue().getAsJsonPrimitive().getAsString());
                    }
                }
            }
        });

    }

    public void saveSettingConfig(File folder) {
        if (!folder.exists()) folder.mkdir();

        File modsFolder = new File(folder, "modules");
        if (!modsFolder.exists()) modsFolder.mkdir();

        if (settings.isEmpty()) return;

        File jsonFile = new File(modsFolder, getName().toLowerCase().replace(" ", "") + ".json");

        if (jsonFile.exists()) {
            jsonFile.delete();
        } else {
            try {
                jsonFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        JsonObject node = new JsonObject();

        Collection<Value> settings1 = Collections.unmodifiableCollection(this.getSettings());
        settings1.forEach(setting -> {

            if (setting instanceof Value) {
                if (setting.getValue() instanceof Boolean) {
                    node.addProperty(setting.getAliases()[0], (Boolean) setting.getValue());
                }
            }

            if (setting instanceof NumberValue) {
                ((NumberValue<?>) setting).setClamp(false);

                if (setting.getValue() instanceof Number && !(setting.getValue() instanceof Enum)) {
                    if (setting.getValue() instanceof Integer) {
                        node.addProperty(setting.getAliases()[0], (Integer) setting.getValue());
                    } else if (setting.getValue() instanceof Double) {
                        node.addProperty(setting.getAliases()[0], (Double) setting.getValue());
                    } else if (setting.getValue() instanceof Float) {
                        node.addProperty(setting.getAliases()[0], (Float) setting.getValue());
                    } else if (setting.getValue() instanceof Long) {
                        node.addProperty(setting.getAliases()[0], (Long) setting.getValue());
                    }
                }
            }

            if (setting instanceof EnumValue) {
                if (setting.getValue() instanceof Enum) {
                    node.addProperty(setting.getAliases()[0], ((Enum) setting.getValue()).name());
                }
            }

            if (setting instanceof StringValue) {
                if (setting.getValue() instanceof String) {
                    node.addProperty(setting.getAliases()[0], (String) setting.getValue());
                }
            }

            if (setting instanceof ColourValue) {
                if (setting.getValue() instanceof Color) {
                    node.addProperty(setting.getAliases()[0], ((Color) setting.getValue()).getRGB());
                }
            }
/*
            if (setting.getValue() instanceof BindValue) {
                if (setting.getValue() instanceof Integer) {
                    int value = (Integer) setting.getValue();
                    if (value < 0) {
                        String name = "MOUSE" + +value + 1;
                        node.addProperty(setting.getAliases()[0], name);
                    } else {
                        node.addProperty(setting.getAliases()[0], Keyboard.getKeyName(value));
                    }
                }
            }
 */
            if (setting instanceof BindValue) {
                if (setting.getValue() instanceof Integer) {
                    node.addProperty(setting.getAliases()[0], BindUtils.INSTANCE.getFormattedKeyBind((Integer) setting.getValue()));
              //      node.addProperty(setting.getAliases()[0], Keyboard.getKeyName((Integer) setting.getValue()));
                }
            }

        });

        if (node.entrySet().isEmpty()) {
            return;
        }

        try {
            jsonFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            FileWriter writer = new FileWriter(jsonFile);
            Throwable throwable = null;
            try {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(node));
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                if (throwable != null) {
                    try {
                        writer.close();
                    } catch (Throwable e) {
                        throwable.addSuppressed(e);
                    }
                } else {
                    writer.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            jsonFile.delete();
        }
    }

}