package me.han.muffin.client.macro;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.Globals;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MacroManager {

    private final HashMap<String, Macro> macros = new HashMap<>();

    public void initialize() {
        try {
            File files = new File(Muffin.getInstance().getDirectory() + "/macros/");
            for (File file : files.listFiles()) {
                List<String> lines = FileUtils.readLines(file, "UTF-8");
                Macro mac = new Macro();
                lines.forEach(mac::addText);
                macros.put(file.getName().replace(".txt", ""), mac);
            }
        } catch (Exception ignored) {
        }
    }

    private void save() {
        macros.forEach((K, V) -> {
            try {
                final File mac = new File(Muffin.getInstance().getDirectory() + "/macros/" + K + ".txt");
                mac.createNewFile();
                FileWriter writer = new FileWriter(Muffin.getInstance().getDirectory() + "macros/" + K + ".txt");

                V.texts.forEach(s -> {
                    try {
                        writer.write(s + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                writer.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void onKeyPress(String key) {
        macros.forEach((K, V) -> {
            if (K.equals(key)) {
                V.process();
            }
        });
    }

    public class Macro {
        private ArrayList<String> texts = new ArrayList<>();

        public Macro() {
        }

        public Macro(String string2) {
            texts.add(string2);
        }

        public void process() {
            texts.forEach(t -> Globals.mc.player.sendChatMessage(t));
        }

        public void removeText(String text) {
            if (texts.contains(text)) texts.remove(text);
        }

        public void addText(String text) {
            texts.add(text);
        }
    }

    public void addMacro(String string, String string2) {
        if (macros.containsKey(string)) {
            final Macro mac = macros.get(string);
            mac.addText(string2);
        } else
            macros.put(string, new Macro(string2));

        save();
    }

    public void removeMacro(String string) {
        if (macros.containsKey(string))
            macros.remove(string);

        save();
    }

}