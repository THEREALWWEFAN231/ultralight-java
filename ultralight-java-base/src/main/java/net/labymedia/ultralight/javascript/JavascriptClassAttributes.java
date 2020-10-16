/*
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 LabyMedia and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.labymedia.ultralight.javascript;

import net.labymedia.ultralight.annotation.NativeType;

/**
 * Attributes of a Javascript class. Defines how the Javascript class is created.
 */
public final class JavascriptClassAttributes {
    /**
     * Specifies that a class has no special attributes.
     */
    @NativeType("<unnamed enum>")
    public static final int NONE = 0;

    /**
     * Specifies that a class should not automatically generate a shared prototype for its instance objects. Use
     * this in combination with {@link JavascriptObject#setPrototype(JavascriptValue)} to manage prototypes manually.
     */
    @NativeType("<unnamed enum>")
    public static final int NO_AUTOMATIC_PROTOTYPE = 1 << 1;
}
