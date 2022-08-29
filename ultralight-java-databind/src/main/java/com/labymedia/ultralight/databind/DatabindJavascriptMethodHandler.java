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

package com.labymedia.ultralight.databind;

import com.labymedia.ultralight.databind.call.CallData;
import com.labymedia.ultralight.databind.call.MethodChooser;
import com.labymedia.ultralight.databind.call.property.PropertyCaller;
import com.labymedia.ultralight.databind.utils.JavascriptConversionUtils;
import com.labymedia.ultralight.javascript.JavascriptClass;
import com.labymedia.ultralight.javascript.JavascriptClassAttributes;
import com.labymedia.ultralight.javascript.JavascriptClassDefinition;
import com.labymedia.ultralight.javascript.JavascriptContext;
import com.labymedia.ultralight.javascript.JavascriptObject;
import com.labymedia.ultralight.javascript.JavascriptValue;
import com.labymedia.ultralight.javascript.interop.JavascriptInteropException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Method handler for calls inbound from Javascript.
 */
public final class DatabindJavascriptMethodHandler {
    private final JavascriptClassDefinition definition;
    private final String name;

    private final MethodChooser methodChooser;
    private final JavascriptConversionUtils conversionUtils;

    private final PropertyCaller propertyCaller;

    private final Set<Method> methodSet;

    /**
     * Constructs a new {@link DatabindJavascriptMethodHandler}.
     *
     * @param configuration   The configuration to use
     * @param conversionUtils The conversion utilities to use for converting objects
     * @param propertyCaller  The property caller used for calling properties on java objects and classes
     * @param methodSet       The methods which can be invoked by this handler
     * @param name            The name of this handler class
     */
    private DatabindJavascriptMethodHandler(
            DatabindConfiguration configuration,
            JavascriptConversionUtils conversionUtils,
            PropertyCaller propertyCaller,
            Set<Method> methodSet,
            String name
    ) {
        this.definition = new JavascriptClassDefinition()
                .name(name)
                .attributes(JavascriptClassAttributes.NO_AUTOMATIC_PROTOTYPE);

        this.name = name;
        this.methodChooser = configuration.methodChooser();
        this.conversionUtils = conversionUtils;
        this.propertyCaller = propertyCaller;
        this.methodSet = methodSet;
    }

    /**
     * Registers the callbacks on the definition.
     */
    private void registerCallbacks() {
        definition.onCallAsFunction(this::onCallAsFunction);
        definition.onGetProperty(this::onGetProperty);
    }

    /**
     * Called by Javascript when a request is issued to invoke a function matching this handler.
     *
     * @param context    The Javascript context the function is being invoked int
     * @param function   The function being called
     * @param thisObject The this parameter of the call
     * @param arguments  The arguments of the call
     * @return The result of the call
     * @throws JavascriptInteropException If invoking the method fails
     */
    private JavascriptValue onCallAsFunction(
            JavascriptContext context,
            JavascriptObject function,
            JavascriptObject thisObject,
            JavascriptValue[] arguments
    ) throws JavascriptInteropException {
        Data privateData = (Data) function.getPrivate();
        CallData<Method> callData;

        if (privateData.parameterTypes() == null) {
            // Implicit types
            callData = methodChooser.choose(methodSet, arguments);
        } else {
            // Explicit types
            callData = methodChooser.choose(methodSet, privateData.parameterTypes(), arguments);
        }

        // Prepare the call
        Method method = callData.getTarget();
        List<Object> parameters = callData.constructArguments(
                context,
                conversionUtils,
                arguments
        );

        // Invoke method with constructed arguments
        Object ret = propertyCaller.callMethod(privateData.instance(), method, parameters.toArray());
        Class<?> suggestedReturnType = method.getReturnType();

        if (ret != null) {
            suggestedReturnType = ret.getClass();
        }

        return conversionUtils.toJavascript(context, ret, suggestedReturnType);
    }

    /**
     * Called by Javascript when a property is requested on the function.
     *
     * @param context      The context the property is requested in
     * @param object       The object the property is requested from
     * @param propertyName The name of the requested property
     * @return The found property, or {@code null}, if the property could not be found
     */
    private JavascriptValue onGetProperty(JavascriptContext context, JavascriptObject object, String propertyName) {
        if (!propertyName.equals("signature")) {
            return context.makeUndefined();
        }

        Data privateData = (Data) object.getPrivate();

        // Create the explicit API interface
        return context.makeObject(DatabindJavascriptExplicitAPI.create(conversionUtils, name).bake(),
                new DatabindJavascriptExplicitAPI.Data(privateData.instance, bake()));
    }

    /**
     * Bakes the class definition.
     *
     * @return The baked definition
     */
    public JavascriptClass bake() {
        return definition.bake();
    }

    /**
     * Creates a new {@link DatabindJavascriptMethodHandler}.
     *
     * @param configuration   The configuration to use
     * @param conversionUtils The conversion utilities to user for converting objects
     * @param propertyCaller  The property caller used for calling properties on java objects and classes
     * @param methodSet       The sets of methods invocable by this handler
     * @param name            The name of this handler class
     * @return The created handler
     */
    static DatabindJavascriptMethodHandler create(
            DatabindConfiguration configuration,
            JavascriptConversionUtils conversionUtils,
            PropertyCaller propertyCaller,
            Set<Method> methodSet,
            String name
    ) {
        DatabindJavascriptMethodHandler javascriptClass = new DatabindJavascriptMethodHandler(configuration, conversionUtils, propertyCaller, methodSet, name);
        javascriptClass.registerCallbacks();
        return javascriptClass;
    }

    /**
     * Data of a not yet parameter bound call.
     */
    public static class Data {
        private final Object instance;
        private final Class<?>[] parameterTypes;

        /**
         * Constructs a new {@link Data} instance for a not yet bound call.
         *
         * @param instance       The instance this call will be invoked on
         * @param parameterTypes The requested parameter types, or {@code null}, if not known
         */
        public Data(Object instance, Class<?>[] parameterTypes) {
            this.instance = instance;
            this.parameterTypes = parameterTypes;
        }

        /**
         * Retrieves the instance this call will be invoked on.
         *
         * @return The instance this call will be invoked on
         */
        public Object instance() {
            return instance;
        }

        /**
         * Retrieves the type of the parameters this call will be invoked with.
         *
         * @return The type of the parameters this will be invoked with, or {@code null}, if not known
         */
        public Class<?>[] parameterTypes() {
            return parameterTypes;
        }
    }
}
