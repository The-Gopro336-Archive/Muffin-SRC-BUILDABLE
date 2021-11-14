package me.han.muffin.client.imixin.render

import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.Shader

interface IShaderGroup {

    val shaders: List<Shader>
    val mainFramebuffer: Framebuffer

}