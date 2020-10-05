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

package net.labymedia.ultralight.util;

/**
 * General purpose utility class containing methods which don't fit anywhere else.
 */
public class Util {
    // Static class
    private Util() {}

    /**
     * Method to simply force cast any type to any type. Use with caution,
     * absolutely no checks are done. This is often useful when generic signatures
     * begin to clash and you can ensure that it is safe to erase them.
     *
     * @param o The object to cast
     * @param <T> The target type
     * @return The casted object
     */
    @SuppressWarnings("unchecked")
    public static <T> T forceCast(Object o) {
        return (T) o;
    }
}