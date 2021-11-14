package me.han.muffin.client.utils.render.shader.shaders;

import me.han.muffin.client.core.Globals;
import me.han.muffin.client.utils.render.shader.FramebufferShader;
import org.lwjgl.opengl.GL20;

public class OutlineShader extends FramebufferShader {

    public static final OutlineShader OUTLINE_SHADER = new OutlineShader();

    public OutlineShader() {
        super("outline.frag");
    }

    @Override
    public void setupUniforms() {
        setupUniform("texture");
        setupUniform("texelSize");
        setupUniform("color");
        setupUniform("divider");
        setupUniform("radius");
        setupUniform("maxSample");
    }

    @Override
    public void updateUniforms() {
        GL20.glUniform1i(getUniform("texture"), 0);
        GL20.glUniform2f(getUniform("texelSize"), 1F / Globals.mc.displayWidth * (radius * quality), 1F / Globals.mc.displayHeight * (radius * quality));
        GL20.glUniform4f(getUniform("color"), red, green, blue, alpha);
        GL20.glUniform1f(getUniform("radius"), radius);
    }

}