package pp

import org.gradle.internal.os.OperatingSystem

/**
 * Provides static methods for automatically setting JVM option
 * -XstartOnFirstThread on MacOS when using library jme3-lwjgl3.
 */
class JMEStart {
    /**
     * Check whether jme3-lwjgl (i.e., version 2) should be used instead of jme3-lwjgl3.
     * This is controlled by existence of the file 'use/lwjgl2'
     */
    def static use_lwjgl2() {
        return new File('use/lwjgl2').exists()
    }

    /**
     * Returns the LWJGL-specific JVM args when starting a jme program.
     */
    def static lwjglJvmArgs() {
        if (mustStartOnFirstThread())
            return ['-XstartOnFirstThread']
        else
            return []
    }

    /**
     * Check whether whether JVM option -XstartOnFirstThread is needed.
     */
    def static mustStartOnFirstThread() {
        return OperatingSystem.current().isMacOsX() && !use_lwjgl2()
    }
}