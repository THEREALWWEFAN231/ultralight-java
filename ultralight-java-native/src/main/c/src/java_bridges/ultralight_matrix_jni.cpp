#include "ultralight_java/java_bridges/ultralight_matrix_jni.hpp"

#include "ultralight_java/java_bridges/ultralight_matrix4x4_jni.hpp"
#include "ultralight_java/ultralight_java_instance.hpp"

namespace ultralight_java {
    void UltralightMatrixJNI::set1(JNIEnv *env, jobject instance, jobject target) {
        auto *matrix = reinterpret_cast<ultralight::Matrix *>(
            env->CallLongMethod(instance, runtime.object_with_handle.get_handle_method));

        auto *matrix4x4 = reinterpret_cast<ultralight::Matrix4x4 *>(
            env->CallLongMethod(target, runtime.object_with_handle.get_handle_method));

        matrix->Set(*matrix4x4);
    }

    void UltralightMatrixJNI::set(
        JNIEnv *env,
        jobject instance,
        jdouble m11,
        jdouble m12,
        jdouble m13,
        jdouble m14,
        jdouble m21,
        jdouble m22,
        jdouble m23,
        jdouble m24,
        jdouble m31,
        jdouble m32,
        jdouble m33,
        jdouble m34,
        jdouble m41,
        jdouble m42,
        jdouble m43,
        jdouble m44) {

        auto *matrix = reinterpret_cast<ultralight::Matrix *>(
            env->CallLongMethod(instance, runtime.object_with_handle.get_handle_method));
        matrix->Set(m11, m12, m13, m14, m21, m22, m23, m24, m31, m32, m33, m34, m41, m42, m43, m44);
    }

    void UltralightMatrixJNI::setOrthographicProjection(
        JNIEnv *env, jobject instance, jdouble width, jdouble height, jboolean flipY) {
        auto *matrix = reinterpret_cast<ultralight::Matrix *>(
            env->CallLongMethod(instance, runtime.object_with_handle.get_handle_method));

        matrix->SetOrthographicProjection(width, height, flipY);
    }

    jobject UltralightMatrixJNI::getMatrix4x4(JNIEnv *env, jobject instance) {
        auto *matrix = reinterpret_cast<ultralight::Matrix *>(
            env->CallLongMethod(instance, runtime.object_with_handle.get_handle_method));
        return UltralightMatrix4x4JNI::create(env, matrix->GetMatrix4x4());
    }
    void UltralightMatrixJNI::transform(JNIEnv *env, jobject instance, jobject transformMatrix) {
        auto *matrix = reinterpret_cast<ultralight::Matrix *>(
            env->CallLongMethod(instance, runtime.object_with_handle.get_handle_method));
        auto *targetMatrix = reinterpret_cast<ultralight::Matrix *>(
            env->CallLongMethod(transformMatrix, runtime.object_with_handle.get_handle_method));

        matrix->Transform(*targetMatrix);
    }
    jobject UltralightMatrixJNI::create(JNIEnv *env, ultralight::Matrix matrix) {
        return env->NewObject(
            runtime.ultralight_matrix.clazz, runtime.ultralight_matrix.constructor, new ultralight::Matrix(matrix));
    }
    jlong UltralightMatrixJNI::construct(JNIEnv *env, jclass caller_class) {
        return reinterpret_cast<jlong>(new ultralight::Matrix());
    }
    void UltralightMatrixJNI::_delete(JNIEnv *env, jclass caller_class, jlong handle) {
        delete reinterpret_cast<ultralight::Matrix *>(handle);
    }
} // namespace ultralight_java
