package me.han.muffin.client.utils.render.shader.shaders;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.utils.render.shader.FramebufferShader;
import org.lwjgl.opengl.GL20;

public class CoolShader extends FramebufferShader {
    public static final CoolShader COOL_SHADER = new CoolShader();

    public CoolShader() {
        super("cool.shader");
    }

    @Override
    public void setupUniforms() {
        setupUniform("viewPos");
        setupUniform("tex");
        setupUniform("depth");
    }

    @Override
    public void updateUniforms() {
        GL20.glUniform2f(getUniform("viewPos"), 1F / Globals.mc.displayWidth * (radius * quality), 1F / Globals.mc.displayHeight * (radius * quality));
        GL20.glUniform1i(getUniform("tex"), 0);
        GL20.glUniform1i(getUniform("depth"), 0);
    }

}
