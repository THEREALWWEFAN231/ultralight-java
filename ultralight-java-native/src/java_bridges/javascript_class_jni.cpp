/*
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2022 LabyMedia and contributors
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

#include "ultralight_java/java_bridges/javascript_class_jni.hpp"

#include "JavaScriptCore/JavaScript.h"

namespace ultralight_java {
    void JavascriptClassJNI::release(JNIEnv *, jclass, jlong handle) {
        // TODO: Actually release when callback is implemented in Javascript engine
    }
} // namespace ultralight_java