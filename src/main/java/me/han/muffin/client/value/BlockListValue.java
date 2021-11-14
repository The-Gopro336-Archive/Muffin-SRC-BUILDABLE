package me.han.muffin.client.value;

import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockListValue extends Value<List<String>> {

    public BlockListValue(List<String> list, String name) {
        super(new ArrayList<>(list), name);
    }

    public void addBlock(Block block)
    {
        getValue().add(getStringFromBlock(block));
    }

    public void removeBlock(Block block)
    {
        getValue().remove(getStringFromBlock(block));
    }

    public boolean containsBlock(Block block)
    {
        return getValue().contains(getStringFromBlock(block));
    }

    private String getStringFromBlock(Block block)
    {
        return Block.REGISTRY.getNameForObject(block).toString();
    }

}