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

package net.labymedia.ultralight.javascript.interop;

import net.labymedia.ultralight.annotation.NativeCall;
import net.labymedia.ultralight.annotation.NativeType;
import net.labymedia.ultralight.javascript.JavascriptContext;
import net.labymedia.ultralight.javascript.JavascriptObject;

/**
 * Callback for testing if a Javascript object has a certain property.
 */
@NativeType("JSObjectHasPropertyCallback")
public interface JavascriptObjectHasPropertyTester {
    /**
     * If this function returns false, the hasProperty request forwards to object's statically declared properties, then
     * its parent class chain (which includes the default object class), then its prototype chain.
     * <p>
     * This callback enables optimization in cases where only a property's existence needs to be known, not its value,
     * and computing its value would be expensive.
     *
     * @param context The execution context to use
     * @param object The object to search for the property
     * @param propertyName A string containing the name of the property look up
     * @return {@code true} if object has the property, otherwise {@code false}
     */
    @NativeCall
    boolean hasJavascriptProperty(JavascriptContext context, JavascriptObject object, String propertyName);
}
