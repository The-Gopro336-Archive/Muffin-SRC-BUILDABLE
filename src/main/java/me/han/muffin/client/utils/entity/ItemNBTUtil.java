package me.han.muffin.client.utils.entity;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;


public class ItemNBTUtil {
    public static boolean detectNBT(ItemStack stack) {
        return stack.hasTagCompound();
    }

    public static void initNBT(ItemStack stack) {
        if (!ItemNBTUtil.detectNBT(stack)) {
            ItemNBTUtil.injectNBT(stack, new NBTTagCompound());
        }
    }

    public static void injectNBT(ItemStack stack, NBTTagCompound nbt) {
        stack.setTagCompound(nbt);
    }

    public static NBTTagCompound getNBT(ItemStack stack) {
        ItemNBTUtil.initNBT(stack);
        return stack.getTagCompound();
    }

    public static void setBoolean(ItemStack stack, String tag, boolean b) {
        ItemNBTUtil.getNBT(stack).setBoolean(tag, b);
    }

    public static void setByte(ItemStack stack, String tag, byte b) {
        ItemNBTUtil.getNBT(stack).setByte(tag, b);
    }

    public static void setShort(ItemStack stack, String tag, short s) {
        ItemNBTUtil.getNBT(stack).setShort(tag, s);
    }

    public static void setInt(ItemStack stack, String tag, int i) {
        ItemNBTUtil.getNBT(stack).setInteger(tag, i);
    }

    public static void setLong(ItemStack stack, String tag, long l) {
        ItemNBTUtil.getNBT(stack).setLong(tag, l);
    }

    public static void setFloat(ItemStack stack, String tag, float f) {
        ItemNBTUtil.getNBT(stack).setFloat(tag, f);
    }

    public static void setDouble(ItemStack stack, String tag, double d) {
        ItemNBTUtil.getNBT(stack).setDouble(tag, d);
    }

    public static void setCompound(ItemStack stack, String tag, NBTTagCompound cmp) {
        if (!tag.equalsIgnoreCase("ench")) {
            ItemNBTUtil.getNBT(stack).setTag(tag, cmp);
        }
    }

    public static void setString(ItemStack stack, String tag, String s) {
        ItemNBTUtil.getNBT(stack).setString(tag, s);
    }

    public static void setList(ItemStack stack, String tag, NBTTagList list) {
        ItemNBTUtil.getNBT(stack).setTag(tag, list);
    }

    public static boolean verifyExistence(ItemStack stack, String tag) {
        return !stack.isEmpty() && ItemNBTUtil.detectNBT(stack) && ItemNBTUtil.getNBT(stack).hasKey(tag);
    }

    @Deprecated
    public static boolean verifyExistance(ItemStack stack, String tag) {
        return ItemNBTUtil.verifyExistence(stack, tag);
    }

    public static boolean getBoolean(ItemStack stack, String tag, boolean defaultExpected) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getBoolean(tag) : defaultExpected;
    }

    public static byte getByte(ItemStack stack, String tag, byte defaultExpected) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getByte(tag) : defaultExpected;
    }

    public static short getShort(ItemStack stack, String tag, short defaultExpected) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getShort(tag) : defaultExpected;
    }

    public static int getInt(ItemStack stack, String tag, int defaultExpected) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getInteger(tag) : defaultExpected;
    }

    public static long getLong(ItemStack stack, String tag, long defaultExpected) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getLong(tag) : defaultExpected;
    }

    public static float getFloat(ItemStack stack, String tag, float defaultExpected) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getFloat(tag) : defaultExpected;
    }

    public static double getDouble(ItemStack stack, String tag, double defaultExpected) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getDouble(tag) : defaultExpected;
    }

    public static NBTTagCompound getCompound(ItemStack stack, String tag, boolean nullifyOnFail) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getCompoundTag(tag) : (nullifyOnFail ? null : new NBTTagCompound());
    }

    public static String getString(ItemStack stack, String tag, String defaultExpected) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getString(tag) : defaultExpected;
    }

    public static NBTTagList getList(ItemStack stack, String tag, int objtype, boolean nullifyOnFail) {
        return ItemNBTUtil.verifyExistence(stack, tag) ? ItemNBTUtil.getNBT(stack).getTagList(tag, objtype) : (nullifyOnFail ? null : new NBTTagList());
    }
}

