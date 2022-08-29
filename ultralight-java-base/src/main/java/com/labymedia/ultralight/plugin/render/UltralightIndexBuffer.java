/*
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 - 2022 LabyMedia and contributors
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

package com.labymedia.ultralight.plugin.render;

import com.labymedia.ultralight.annotation.NativeCall;
import com.labymedia.ultralight.annotation.NativeType;

import java.nio.ByteBuffer;

/**
 * Vertex index buffer.
 *
 * @see UltralightGPUDriver#createGeometry(long, UltralightVertexBuffer, UltralightIndexBuffer)
 */
@NativeType("ultralight::IndexBuffer")
public class UltralightIndexBuffer {
    private final @NativeType("uint8_t[]")
    ByteBuffer data;

    /**
     * Constructs a new index buffer wrapping an existing byte buffer.
     *
     * @param data The data of the index buffer
     */
    @NativeCall
    public UltralightIndexBuffer(ByteBuffer data) {
        this.data = data;
    }

    /**
     * Retrieves the data of the index buffer.
     *
     * @return The data of the index buffer
     */
    public ByteBuffer getData() {
        return data;
    }
}
