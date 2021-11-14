package me.han.muffin.client.manager.managers

import me.han.muffin.client.Muffin
import me.han.muffin.client.mixin.ClientLoader
import me.han.muffin.client.utils.render.RenderUtils
import me.han.muffin.client.utils.render.texture.MipmapTexture
import net.minecraft.client.renderer.GlStateManager.glTexParameterf
import net.minecraft.client.renderer.GlStateManager.glTexParameteri
import org.apache.commons.io.FileUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URISyntaxException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.concurrent.thread

object TextureManager {
    private const val RESOURCE_FOLDER = "textures/muffin"
    private const val TEXTURES_FOLDER = "assets/minecraft/textures/muffin"
    private val TEXTURE_DIRECTORY = File(Muffin.getInstance().directory, "textures")

    private var deletedDirectory = false

    val fileMap = HashMap<String, File>()
    val streamMap = HashMap<String, InputStream>()
    val textureMap = HashMap<String, MipmapTexture>()

    val loadTextureThread = thread {
        // val classLoader = Muffin::class.java.classLoader

        with(TEXTURE_DIRECTORY) {
            if (!exists()) mkdir()
        }

        // updateLocalTextures()
        TEXTURE_DIRECTORY.listFiles()?.forEach { fileIn ->
            val fileSuffixRemoved = fileIn.name.removeSuffix(".png")
            fileMap[fileSuffixRemoved] = fileIn
        }

//        val URI = try {
//            classLoader.getResource(TEXTURES_FOLDER)?.toURI()
//        } catch (e: URISyntaxException) {
//            ClientLoader.LOGGER.error("Failed to load resource path!", e)
//            null
//        } catch (e: NullPointerException) {
//            ClientLoader.LOGGER.error("Failed to load resource path!", e)
//            null
//        } ?: return@thread
//
//        if (URI.scheme.endsWith("jar")) {
//            try {
//                val jarLocation = Muffin::class.java.protectionDomain.codeSource.location
//                // val jarFile = Paths.get(jarLocation.toURI())
//                // val jarFile = Paths.get(jarLocation.toString()) // .substring("file:".length))
//
//                val jarFile = Paths.get(jarLocation.toURI().toString().substring(4).substringBefore("!").substring(6).replace(File.separatorChar, '/'))
//                // ClientLoader.LOGGER.info(jarFile.toString())
//                val fileSystem = FileSystems.newFileSystem(jarFile, null)
//
//                Files.newDirectoryStream(fileSystem.getPath(TEXTURES_FOLDER)).use { directory ->
//                    directory.forEach { pathIn ->
//                        ClientLoader.LOGGER.info("PathIn: $pathIn")
//                        // val file = pathIn.toFile()
//                        val stream = Muffin::class.java.classLoader?.getResourceAsStream(pathIn.toString()) ?: Muffin::class.java.getResourceAsStream(pathIn.toString())
//                        val fileName = pathIn.toString().removeSuffix(".png")
//                        ClientLoader.LOGGER.info("StreamIn: $stream")
//
//                        // fileMap[fileName] = file
//                        streamMap[fileName] = stream
//                    }
//                    directory.close()
//                }
//
//                println(streamMap.toString())
//
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        } else {
//            val sourcePath = Paths.get(URI)
//            try {
//                Files.newDirectoryStream(sourcePath).use { directory ->
//                    directory.forEach { pathIn ->
//                        val file = pathIn.toFile()
//                        val stream = FileInputStream(file)
//
//                        val fileName = file.name.removeSuffix(".png")
//                        fileMap[fileName] = file
//                        streamMap[fileName] = stream
//                    }
//                    directory.close()
//                }
//
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }

//        val jarFile = Muffin::class.java.protectionDomain.codeSource.location.path.file
//
//        if (jarFile.isFile) {
//            JarFile(jarFile).use { jarFile ->
//                val entries = jarFile.entries()
//                while (entries.hasMoreElements()) {
//                    val element = entries.nextElement()
//                    var name = element.name
//                    if (name.startsWith("$TEXTURES_FOLDER/")) {
//                        name = name.removePrefix("$TEXTURES_FOLDER/")
//                        fileMap[name] = File(element)
//                    }
//                }
//
//                jarFile.close()
//            }
//        } else {
//            Muffin::class.java.getResource("/$TEXTURES_FOLDER")?.let { url ->
//                try {
//                    url.toURI().file.listFiles()?.forEach { app ->
//                    }
//                } catch (e: URISyntaxException) {
//                }
//            }
//        }
//
//        val textureAssets = javaClass.getResourceAsStream("assets/minecraft/textures/muffin") ?: return@thread
    }

    private fun updateLocalTextures() {
        with(TEXTURE_DIRECTORY) {
            if (!deletedDirectory) {
                listFiles()?.forEach { it.delete() }
                deletedDirectory = true
            }
        }

        val latestTextures = getLatestJarTextures()
        if (latestTextures.isEmpty()) return

        val filesIn = TEXTURE_DIRECTORY.listFiles()

        if (filesIn.isNullOrEmpty()) {
            latestTextures.forEach {
                File(TEXTURE_DIRECTORY, it.key).apply {
                    if (!exists()) createNewFile()
                    FileUtils.copyInputStreamToFile(it.value, this)
                }
            }
        }

//        } else {
//            filesIn.forEach { file ->
//                if (file.isFile) {
//                    latestTextures.forEach { latestTexture ->
//                        val latestName = latestTexture.key
//                        if (!file.name.equals(latestName)) {
//
//                        }
//                    }
//                }
//            }
//        }
    }

    private fun getLatestJarTextures(): Map<String, InputStream> {
        val tempHashMap = hashMapOf<String, InputStream>()
        val classLoader = javaClass.classLoader

        val URI = try {
            classLoader.getResource(TEXTURES_FOLDER)?.toURI() ?: javaClass.getResource(TEXTURES_FOLDER)?.toURI()
        } catch (e: URISyntaxException) {
            ClientLoader.LOGGER.error("Failed to load resource path!", e)
            null
        } catch (e: NullPointerException) {
            ClientLoader.LOGGER.error("Failed to load resource path!", e)
            null
        } ?: return emptyMap()

        if (URI.scheme.endsWith("jar")) {
            try {
                val jarLocation = javaClass.protectionDomain.codeSource.location
                // val jarFile = Paths.get(jarLocation.toURI())
                // val jarFile = Paths.get(jarLocation.toString()) // .substring("file:".length))

                val jarFile = Paths.get(jarLocation.toURI().toString().substring(4).substringBefore("!").substring(6).replace(File.separatorChar, '/'))
                // ClientLoader.LOGGER.info(jarFile.toString())
                val fileSystem = FileSystems.newFileSystem(jarFile, null)

                Files.newDirectoryStream(fileSystem.getPath(TEXTURES_FOLDER)).use { directory ->
                    directory.forEach { pathIn ->
                        // ClientLoader.LOGGER.info("PathIn: $pathIn")
                        var fileName = pathIn.toString()// .removeSuffix(".png")
                        val stream = javaClass.classLoader?.getResourceAsStream(fileName) ?: javaClass.getResourceAsStream(fileName)
                        fileName = fileName.substringAfterLast("/")
                        ClientLoader.LOGGER.info("FileName: $fileName")
                        tempHashMap[fileName] = stream
                    }
                    directory.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            val sourcePath = Paths.get(URI)
            try {
                Files.newDirectoryStream(sourcePath).use { directory ->
                    directory.forEach { pathIn ->
                        val file = pathIn.toFile()
                        val inputStream = FileInputStream(file)
                        val fileName = file.name.substringAfterLast("/") // .removeSuffix(".png")
                        tempHashMap[fileName] = inputStream
                    }
                    directory.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return tempHashMap
    }

    fun drawMipmapIcon512(x: Double, y: Double, size: Float) {
        val iconTexture = getTexture("muffin_512") ?: return
        RenderUtils.drawMipMapTexture(iconTexture, x, y, size)
    }

    fun getTexture(name: String?): MipmapTexture? {
        if (name == null) return null
        if (loadTextureThread.isAlive) return null

        if (!textureMap.containsKey(name)) loadTexture(name)

        return textureMap[name]
    }

    private fun loadTexture(name: String) {
        val file = fileMap[name] ?: return
        // val stream = streamMap[name] ?: return

        try {
            val image = ImageIO.read(file)

            val texture = MipmapTexture(image, GL_RGBA, 4)

            texture.bindTexture()
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, -0.5F)
            texture.unbindTexture()

            textureMap[name] = texture
        } catch (e: IOException) {
            ClientLoader.LOGGER.warn("Failed to load texture", e)
        }
    }

}