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

#include "ultralight_java/java_bridges/ultralight_view_jni.hpp"

#include "ultralight_java/java_bridges/javascript_context_lock_jni.hpp"
#include "ultralight_java/java_bridges/ultralight_ref_ptr_jni.hpp"
#include "ultralight_java/util/util.hpp"

namespace ultralight_java {
    std::unordered_map<ultralight::View *, BridgedViewListener *> UltralightViewJNI::existing_view_listeners;
    std::unordered_map<ultralight::View *, BridgedLoadListener *> UltralightViewJNI::existing_load_listeners;

    void UltralightViewJNI::clean_up() {
        for(const auto [_, listener] : existing_view_listeners) {
            delete listener;
        }

        for(const auto [_, listener] : existing_load_listeners) {
            delete listener;
        }
    }

    jstring UltralightViewJNI::url(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return nullptr;
        }

        return Util::create_jstring_from_utf16(env, view->url().utf16());
    }

    jstring UltralightViewJNI::title(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return nullptr;
        }

        return Util::create_jstring_from_utf16(env, view->title().utf16());
    }

    jlong UltralightViewJNI::width(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return 0;
        }

        return view->width();
    }

    jlong UltralightViewJNI::height(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return 0;
        }

        return view->height();
    }

    jboolean UltralightViewJNI::is_loading(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return 0;
        }

        return view->is_loading();
    }

    jobject UltralightViewJNI::surface(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return nullptr;
        }

        auto *surface = view->surface();
        if(!surface) {
            return nullptr;
        }

        // Special cases: Detect if bitmap surface
        if(auto *bitmap_surface = dynamic_cast<ultralight::BitmapSurface *>(surface); bitmap_surface) {
            return env->NewObject(
                runtime.ultralight_bitmap_surface.clazz,
                runtime.ultralight_bitmap_surface.constructor,
                instance,
                reinterpret_cast<jlong>(bitmap_surface));
        }

        return env->NewObject(
            runtime.ultralight_surface.clazz,
            runtime.ultralight_surface.constructor,
            instance,
            reinterpret_cast<jlong>(surface));
    }

    void UltralightViewJNI::load_html(
        JNIEnv *env, jobject instance, jstring html, jstring url, jboolean add_to_history) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        } else if(!html) {
            env->ThrowNew(runtime.null_pointer_exception.clazz, "html can't be null");
            return;
        }

        // Extract the real strings, convert null to an empty string
        ultralight::String16 real_html = Util::create_utf16_from_jstring(env, html);
        ultralight::String16 real_url = url ? Util::create_utf16_from_jstring(env, url) : "";

        view->LoadHTML(real_html, real_url, add_to_history);
    }

    void UltralightViewJNI::load_url(JNIEnv *env, jobject instance, jstring url) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        } else if(!url) {
            env->ThrowNew(runtime.null_pointer_exception.clazz, "url can't be null");
            return;
        }

        ultralight::String16 real_url = Util::create_utf16_from_jstring(env, url);
        view->LoadURL(real_url);
    }

    void UltralightViewJNI::resize(JNIEnv *env, jobject instance, jlong width, jlong height) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->Resize(width, height);
    }

    jobject UltralightViewJNI::lock_javascript_context(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return nullptr;
        }

        return JavascriptContextLockJNI::create(env, std::move(view->LockJSContext()));
    }

    jstring UltralightViewJNI::evaluate_script(JNIEnv *env, jobject instance, jstring script) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return nullptr;
        } else if(!script) {
            env->ThrowNew(runtime.null_pointer_exception.clazz, "script can't be null");
            return nullptr;
        }

        ultralight::String16 real_script = Util::create_utf16_from_jstring(env, script);

        // Evaluate the script and catch exceptions
        ultralight::String exception;
        ultralight::String return_value = view->EvaluateScript(real_script, &exception);

        if(!exception.empty()) {
            // A javascript exception occurred
            env->ThrowNew(runtime.javascript_evaluation_exception.clazz, exception.utf8().data());
            return nullptr;
        } else {
            return Util::create_jstring_from_utf16(env, return_value.utf16());
        }
    }

    jboolean UltralightViewJNI::can_go_back(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return false;
        }

        return view->CanGoBack();
    }

    jboolean UltralightViewJNI::can_go_forward(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return false;
        }

        return view->CanGoForward();
    }

    void UltralightViewJNI::go_back(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->GoBack();
    }

    void UltralightViewJNI::go_forward(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->GoForward();
    }

    void UltralightViewJNI::go_to_history_offset(JNIEnv *env, jobject instance, jint offset) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->GoToHistoryOffset(offset);
    }

    void UltralightViewJNI::reload(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->Reload();
    }

    void UltralightViewJNI::stop(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->Stop();
    }

    void UltralightViewJNI::focus(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->Focus();
    }

    void UltralightViewJNI::unfocus(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->Unfocus();
    }

    jboolean UltralightViewJNI::has_focus(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return false;
        }

        return view->HasFocus();
    }

    jboolean UltralightViewJNI::has_input_focus(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return false;
        }

        return view->HasInputFocus();
    }

    void UltralightViewJNI::fire_key_event(JNIEnv *env, jobject instance, jobject event) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        } else if(!event) {
            env->ThrowNew(runtime.null_pointer_exception.clazz, "event can't be null");
            return;
        }

        view->FireKeyEvent(Util::create_key_event_from_jobject(env, event));
    }

    void UltralightViewJNI::fire_mouse_event(JNIEnv *env, jobject instance, jobject event) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        } else if(!event) {
            env->ThrowNew(runtime.null_pointer_exception.clazz, "event can't be null");
            return;
        }

        view->FireMouseEvent(Util::create_mouse_event_from_jobject(env, event));
    }

    void UltralightViewJNI::fire_scroll_event(JNIEnv *env, jobject instance, jobject event) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        } else if(!event) {
            env->ThrowNew(runtime.null_pointer_exception.clazz, "event can't be null");
            return;
        }

        view->FireScrollEvent(Util::create_scroll_event_from_jobject(env, event));
    }

    void UltralightViewJNI::set_view_listener(JNIEnv *env, jobject instance, jobject listener) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        auto *view_key = view.get();

        if(auto it = existing_view_listeners.find(view_key); it != existing_view_listeners.end()) {
            view->set_load_listener(nullptr);
            delete it->second;
            existing_view_listeners.erase(it);
        }

        if(listener) {
            auto [it, _] = existing_view_listeners.insert(
                std::make_pair(view_key, new BridgedViewListener(env, listener)));
            view->set_view_listener(it->second);
        } else {
            view->set_view_listener(nullptr);
        }
    }

    void UltralightViewJNI::set_load_listener(JNIEnv *env, jobject instance, jobject listener) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        auto *view_key = view.get();

        if(auto it = existing_load_listeners.find(view_key); it != existing_load_listeners.end()) {
            view->set_load_listener(nullptr);
            delete it->second;
            existing_load_listeners.erase(it);
        }

        if(listener) {
            auto [it, _] = existing_load_listeners.insert(
                std::make_pair(view_key, new BridgedLoadListener(env, listener)));
            view->set_load_listener(it->second);
        } else {
            view->set_load_listener(nullptr);
        }
    }

    void UltralightViewJNI::set_needs_paint(JNIEnv *env, jobject instance, jboolean needs_paint) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->set_needs_paint(needs_paint);
    }

    void UltralightViewJNI::set_device_scale(JNIEnv *env, jobject instance, jdouble device_scale) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return;
        }

        view->set_device_scale(device_scale);
    }

    jdouble UltralightViewJNI::device_scale(JNIEnv *env, jobject instance){
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return 0;
        }

        return view->device_scale();
    }

    jboolean UltralightViewJNI::needs_paint(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return false;
        }

        return view->needs_paint();
    }

    jobject UltralightViewJNI::inspector(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);
        if(env->ExceptionCheck()) {
            return nullptr;
        }

        auto inspector = view->inspector();
        auto pointer = UltralightRefPtrJNI::
            create(env, std::move(ultralight::RefPtr<ultralight::View>(std::move(inspector))));
        return env->NewObject(runtime.ultralight_view.clazz, runtime.ultralight_view.constructor, pointer);
    }

    jobject UltralightViewJNI::render_target(JNIEnv *env, jobject instance) {
        auto view = UltralightRefPtrJNI::unwrap_ref_ptr<ultralight::View>(env, instance);

        const auto &render_target = view->render_target();
        auto uv_coords = Util::create_float_array(env, 4, render_target.uv_coords.value);

        auto target = env->NewObject(
            runtime.ultralight_render_target.clazz,
            runtime.ultralight_render_target.constructor,
            static_cast<jboolean>(render_target.is_empty),
            static_cast<jlong>(render_target.width),
            static_cast<jlong>(render_target.height),
            static_cast<jlong>(render_target.texture_id),
            static_cast<jlong>(render_target.texture_width),
            static_cast<jlong>(render_target.texture_height),
            runtime.ultralight_bitmap_format.constants.to_java(env, render_target.texture_format),
            uv_coords,
            static_cast<jlong>(render_target.render_buffer_id));

        return target;
    }
} // namespace ultralight_java