/*
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 LabyMedia and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.labymedia.ultralight.lwjgl3.opengl;

import net.labymedia.ultralight.UltralightJava;
import net.labymedia.ultralight.UltralightLoadException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Entry pointer and controller for the test application.
 */
public class TestRunner {
    public static void main(String[] args) throws UltralightLoadException, InterruptedException {
        // Get a directory to put natives into
        Path nativesDir = Paths.get(".");

        // Get the existing native library path
        String libraryPath = System.getProperty("java.library.path");
        if(libraryPath != null) {
            // There is a path set already, append our natives dir
            libraryPath += File.pathSeparator + nativesDir.toAbsolutePath().toString();
        } else {
            // There is no path set, make our natives dir the current path
            libraryPath = nativesDir.toAbsolutePath().toString();
        }

        // Set the path back
        System.setProperty("java.library.path", libraryPath);

        // Extract and load the natives
        UltralightJava.extractNativeLibrary(nativesDir);
        UltralightJava.load(nativesDir);

        CountDownLatch shutdownLatch = new CountDownLatch(1);

        // Create and run a simple test application
        TestApplication application = new TestApplication();
        application.centerWindow();
        application.run();

        // The user has requested the application to stop
        application.stop();

        shutdownLatch.countDown();

        shutdownLatch.await();
    }
}