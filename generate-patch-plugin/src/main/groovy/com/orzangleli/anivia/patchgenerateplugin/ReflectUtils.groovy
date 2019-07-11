package com.orzangleli.anivia.patchgenerateplugin

import javassist.*
import javassist.bytecode.AccessFlag

class ReflectUtils {

    public static final Boolean INLINE_R_FILE = true;
    public static int invokeCount = 0;

    public
    static String setFieldString(CtField field, String patchClassName, String modifiedClassName) {
        boolean isStatic = isStatic(field.modifiers)
        StringBuilder stringBuilder = new StringBuilder("{");
        if (isStatic) {
            println("setFieldString static field " + field.getName() + "  declaringClass   " + field.declaringClass.name)
            if (AccessFlag.isPublic(field.modifiers)) {
                stringBuilder.append("\$_ = \$proceed(\$\$);");
            } else {
                if (field.declaringClass.name.equals(patchClassName)) {
                    stringBuilder.append("com.orzangleli.anivia.support.util.AdvancedReflectUtils" + ".setStaticFieldValue(\"" + field.name + "\"," + modifiedClassName + ".class,\$1);");
                } else {
                    stringBuilder.append("com.orzangleli.anivia.support.util.AdvancedReflectUtils" + ".setStaticFieldValue(\"" + field.name + "\"," + field.declaringClass.name + ".class,\$1);");
                }
            }
        } else {
            stringBuilder.append("java.lang.Object instance;");
            stringBuilder.append("java.lang.Class clazz;");
            stringBuilder.append(" if(\$0 instanceof " + patchClassName + "){");
            stringBuilder.append("instance=((" + patchClassName + ")\$0)." + "originalClass" + ";")
            stringBuilder.append("}else{");
            stringBuilder.append("instance=\$0;");
            stringBuilder.append("}");
            stringBuilder.append("com.orzangleli.anivia.support.util.AdvancedReflectUtils" + ".setFieldValue(\"" + field.name + "\",instance,\$1,${field.declaringClass.name}.class);");
        }
        stringBuilder.append("}")
//        println field.getName() + "  set  field repalce  by  " + stringBuilder.toString()
        return stringBuilder.toString();
    }

    public
    static String getFieldString(CtField field, String patchClassName, String modifiedClassName) {

        boolean isStatic = isStatic(field.modifiers);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        if (isStatic) {
            if (AccessFlag.isPublic(field.modifiers)) {
                //deal with android R file
                if (INLINE_R_FILE && isRFile(field.declaringClass.name)) {
                    println("getFieldString static field " + field.getName() + "   is R file macthed   " + field.declaringClass.name)
                    stringBuilder.append("\$_ = " + field.constantValue + ";");
                } else {
                    stringBuilder.append("\$_ = \$proceed(\$\$);");
                }
            } else {

                if (field.declaringClass.name.equals(patchClassName)) {
                    stringBuilder.append("\$_=(\$r) " + "com.orzangleli.anivia.support.util.AdvancedReflectUtils" + ".getStaticFieldValue(\"" + field.name + "\"," + modifiedClassName + ".class);");

                } else {
                    stringBuilder.append("\$_=(\$r) " + "com.orzangleli.anivia.support.util.AdvancedReflectUtils" + ".getStaticFieldValue(\"" + field.name + "\"," + field.declaringClass.name + ".class);");
                }
            }
        } else {
            stringBuilder.append("java.lang.Object instance;");
            stringBuilder.append(" if(\$0 instanceof " + patchClassName + "){");
            stringBuilder.append("instance=((" + patchClassName + ")\$0)." + "originalClass" + ";")
            stringBuilder.append("}else{");
            stringBuilder.append("instance=\$0;");
            stringBuilder.append("}");

            stringBuilder.append("\$_=(\$r) " + "com.orzangleli.anivia.support.util.AdvancedReflectUtils" + ".getFieldValue(\"" + field.name + "\",instance,${field.declaringClass.name}.class);");
        }
        stringBuilder.append("}");
//        println field.getName() + "  get field repalce  by  " + stringBuilder.toString() + "\n"
        return stringBuilder.toString();
    }

    static boolean isRFile(String s) {
        if (s.lastIndexOf("R") < 0) {
            return false;
        }
        return Constants.RFileClassSet.contains(s.substring(s.indexOf("R")));
    }

    static boolean isStatic(int modifiers) {
        return (modifiers & AccessFlag.STATIC) != 0;
    }
}
