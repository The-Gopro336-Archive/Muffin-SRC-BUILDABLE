package me.han.muffin.client.config;

import me.han.muffin.client.Muffin;
import me.han.muffin.client.core.EncryptUtils;
import me.han.muffin.client.gui.altmanager.Account;
import me.han.muffin.client.manager.AccountManager;

import java.io.*;

public class AltManagerConfig {

    public AltManagerConfig() {
        loadAccounts();
    }

    public void saveAccounts() {
        try {
            File file = new File(Muffin.getInstance().getDirectory(), "accounts.SECURE");
            if (!file.exists()) {
                file.createNewFile();
            }

            if (!file.exists()) {
                return;
            }

            File accountFile = new File(file.getAbsolutePath());

            if (accountFile.exists()) accountFile.delete();

            if (AccountManager.altList.isEmpty()) return;

            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (Account account : AccountManager.altList) {
                if (!account.isPremium()) {
                    String encrypted = EncryptUtils.encrypt(account.getLabel(), "carbonara");
                    bw.write(encrypted);
                    bw.newLine();
                    continue;
                }
                String accountSave = account.getLabel() + ":" + account.getUuid() + ":" + account.getPassword();
                String encrypted = EncryptUtils.encrypt(accountSave, "carbonara");
                bw.write(encrypted);
                bw.newLine();
            }

            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadAccounts() {
        try {
            File file = new File(Muffin.getInstance().getDirectory(), "accounts.SECURE");

            if (!file.exists()) {
                file.createNewFile();
            }

            BufferedReader br = new BufferedReader(new FileReader(file));
            AccountManager.altList.clear();
            String readLine;

            while ((readLine = br.readLine()) != null) {
                try {
                    String decrypted = EncryptUtils.decrypt(readLine,"carbonara");
                    if (!decrypted.contains(":")) AccountManager.altList.add(new Account(decrypted));
                    else {
                        String[] split = decrypted.split(":");
                        if (split.length > 1) AccountManager.altList.add(new Account(split[1], split[0], split[2]));
                        else AccountManager.altList.add(new Account(split[0]));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}