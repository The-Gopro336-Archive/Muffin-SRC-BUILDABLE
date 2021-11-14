package me.han.muffin.client.utils

object OperatingSystemHelper {

    /**
     * @return current OperatingSystem
     */
    fun getOS(): OperatingSystem {
        return when {
            System.getProperty("os.name").toLowerCase().contains("nux") -> {
                OperatingSystem.UNIX
            }
            System.getProperty("os.name").toLowerCase().contains("darwin") || System.getProperty("os.name").toLowerCase().contains("mac") -> {
                OperatingSystem.OSX
            }
            System.getProperty("os.name").toLowerCase().contains("win") -> {
                OperatingSystem.WINDOWS
            }
            else -> {
                throw RuntimeException("Operating system couldn't be detected! Report this to the developers")
            }
        }
    }

    /**
     * @return the separator used in filepaths for the current operating system
     */
    fun getFolderSeparator(os: OperatingSystem): Char {
        return when (os) {
            OperatingSystem.UNIX -> '/'
            OperatingSystem.OSX -> '/'
            OperatingSystem.WINDOWS -> '\\'
        }
    }

    enum class OperatingSystem {
        UNIX, OSX, WINDOWS
    }

}