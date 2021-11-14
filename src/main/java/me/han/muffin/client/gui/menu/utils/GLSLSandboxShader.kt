package me.han.muffin.client.gui.menu.utils

import org.lwjgl.opengl.GL20
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

class GLSLSandboxShader(private val fragmentShaderLocation: String) {
    private var programId = 0
    private var timeUniform = 0
    private var mouseUniform = 0
    private var resolutionUniform = 0

    init {
        val program = GL20.glCreateProgram()

        GL20.glAttachShader(program, createShader(GLSLSandboxShader::class.java.getResourceAsStream("/shaders/passthrough.vsh"), GL20.GL_VERTEX_SHADER))
        GL20.glAttachShader(program, createShader(GLSLSandboxShader::class.java.getResourceAsStream(fragmentShaderLocation), GL20.GL_FRAGMENT_SHADER))

        GL20.glLinkProgram(program)

        val linked = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS)

        // If linking failed
        if (linked == 0) {
            System.err.println(GL20.glGetProgramInfoLog(program, GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH)));
            throw IllegalStateException("Shader failed to link")
        }

        programId = program

        // Setup uniforms
        GL20.glUseProgram(program)

        timeUniform = GL20.glGetUniformLocation(program, "time")
        mouseUniform = GL20.glGetUniformLocation(program, "mouse")
        resolutionUniform = GL20.glGetUniformLocation(program, "resolution")

        GL20.glUseProgram(0)
    }

    fun useShader(width: Int, height: Int, mouseX: Float, mouseY: Float, time: Float) {
        GL20.glUseProgram(programId)
        GL20.glUniform2f(resolutionUniform, width.toFloat(), height.toFloat())
        GL20.glUniform2f(mouseUniform, mouseX / width, 1.0f - mouseY / height)
        GL20.glUniform1f(timeUniform, time)
    }

    @Throws(IOException::class)
    private fun createShader(inputStream: InputStream, shaderType: Int): Int {
        val shader = GL20.glCreateShader(shaderType)
        GL20.glShaderSource(shader, readStreamToString(inputStream))
        GL20.glCompileShader(shader)
        val compiled = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS)

        // If compilation failed
        if (compiled == 0) {
            System.err.println(GL20.glGetShaderInfoLog(shader, GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH)))
            throw IllegalStateException("Failed to compile shader")
        }

        return shader
    }

    @Throws(IOException::class)
    private fun readStreamToString(inputStream: InputStream): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(512)
        var read: Int
        while (inputStream.read(buffer, 0, buffer.size).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
        return String(out.toByteArray(), StandardCharsets.UTF_8)
    }

}