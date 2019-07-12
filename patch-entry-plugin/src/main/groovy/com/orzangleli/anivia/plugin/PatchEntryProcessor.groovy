package com.orzangleli.anivia.plugin

import com.android.build.api.transform.TransformInput
import com.sun.xml.bind.v2.TODO
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtBehavior
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import javassist.bytecode.AccessFlag
import javassist.expr.Cast
import javassist.expr.ConstructorCall
import javassist.expr.ExprEditor
import javassist.expr.Handler
import javassist.expr.Instanceof
import javassist.expr.MethodCall
import javassist.expr.NewArray
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.lang.reflect.Modifier
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import com.orzangleli.anivia.support.generator.*

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class PatchEntryProcessor {
    private final static Logger logger = Logging.getLogger(PatchEntryTransform);
    private ClassPool classPool

    public void setClassPool(ClassPool cp) {
        classPool = cp
    }

    public void injectCode(List<CtClass> ctClasses, File jarFile) {
        ZipOutputStream outStream = new JarOutputStream(new FileOutputStream(jarFile));
        for (CtClass ctClass : ctClasses) {
            System.out.println("class : " + ctClass)
            List<CtBehavior> ctBehaviors = ctClass.getDeclaredBehaviors()
            boolean hasAddField = false
            for (CtBehavior ctBehavior : ctBehaviors) {
                if (ctBehavior != null) {
                    // 添加一个静态内部变量
                    if (!hasAddField) {
                        hasAddField = true
                        ClassPool classPool = ctBehavior.getDeclaringClass().getClassPool()
                        CtClass fieldType = classPool.get("com.orzangleli.anivia.support.Patchable")
                        System.out.println("fieldType: " + fieldType)
                        System.out.println("fieldType in class: " + ctBehavior.getDeclaringClass().getName())
                        CtField patchableFiled = new CtField(fieldType, "patchable", ctClass)
                        patchableFiled.setModifiers(AccessFlag.STATIC | AccessFlag.PUBLIC)
                        ctClass.addField(patchableFiled)
                    }
                    // 遍历每个方法插桩
                    if (!isQualifiedMethod(ctBehavior)) {
                        continue
                    }
                    // 只对普通方法做处理
                    if (ctBehavior.getMethodInfo().isMethod()) {
                        CtMethod ctMethod = (CtMethod) ctBehavior
                        // 是否是静态方法
                        boolean isStaticMethod = (ctMethod.getModifiers() & AccessFlag.STATIC) != 0
                        String body = "Object argThis = null;"
                        if (!isStaticMethod) {
                            body += "argThis = \$0;"
                        }
                        CtClass[] paramTypes = ctMethod.getParameterTypes()
                        String returnTypeName = ctMethod.getReturnType().getName()
                        String paramTypeNames = convertClassArrayToStringArray(paramTypes)
                        String []generatorParams = getGeneratorParams(ctMethod.getDeclaringClass().getName(), ctMethod.getName(), paramTypeNames)
                        String methodId = MethodIdGenerator.getInstance().generate(generatorParams)
                        // isPatchable(String methodId, boolean isStaticMethod, Object object, Patchable patchable, Object[] paramValues, Class[] paramTypes, Class returnType) {
                        body += "com.orzangleli.anivia.PatchProxy p = null;"
                        body += "if (com.orzangleli.anivia.PatchProxy.isPatchable(\""+ methodId +"\", " + isStaticMethod + ", argThis, patchable, \$args, " + paramTypeNames +", \"" + returnTypeName + "\")) { ";
                        body += getReturnStatement(methodId, isStaticMethod, paramTypeNames, returnTypeName)
                        body += " }"
                        System.out.println("body --> " + body)
                        ctMethod.insertBefore(body)
                    }
                }
            }

            zipFile(ctClass.toBytecode(), outStream, ctClass.getName().replaceAll("\\.", "/") + ".class");
        }
        outStream.close()
    }

    protected void zipFile(byte[] classBytesArray, ZipOutputStream zos, String entryName) {
        try {
            ZipEntry entry = new ZipEntry(entryName)
            zos.putNextEntry(entry)
            zos.write(classBytesArray, 0, classBytesArray.length)
            zos.closeEntry()
            zos.flush()
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean shouldProcessClass(String className) {
        if(!className.endsWith(".class")) {
            return false;
        }

        // xx/BuildConfig.class R$dimen.class R$id.class R$integer.class R$drawable.class
        // R$layout.class  R$string.class  R$style.class R$styleable.class
        if(className.endsWith("BuildConfig.class") || className.endsWith("R.class") || className.contains("R\$")) {
            return false;
        }
        // TODO 增加 黑名单 自定义 排除一些类

        className = className.replaceAll("/", "\\.")
        className = className.replaceAll("\\.class", "")
        CtClass ctClass = classPool.get(className)
        if (ctClass.isFrozen()) {
            return false
        }
        return true;
    }

    public boolean shouldProcessJar(String jarName) {
        return true;
    }


    public void processJar(File inputJar, File outPutJar) {
        JarOutputStream target = null;
        JarFile jarfile = null;
        try{
            target = new JarOutputStream(new FileOutputStream(outPutJar));
            jarfile = new JarFile(inputJar);
            logger.debug("Jarfile:"+jarfile.getName());
            Enumeration<? extends JarEntry> entryList = jarfile.entries();
            while(entryList.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) entryList.nextElement();
                logger.debug("JarEntry:" + jarEntry.getName());
                JarEntry newEntry = new JarEntry(jarEntry.getName());
                target.putNextEntry(newEntry);
                if(!jarEntry.isDirectory()) {
                    if(jarEntry.getName().endsWith(".class")) {
                        def classPath = inputJar.getAbsolutePath() + File.separator + jarEntry.getName()
                        System.out.println("inputJar classpath = " + classPath)
                        CtClass ctClass = classPool.getCtClass(classPath)
                        if (ctClass.isFrozen()) {
                            continue
                        }
                        List<CtBehavior> ctBehaviors = ctClass.getDeclaredBehaviors()
                        boolean hasAddField = false
                        for (CtBehavior ctBehavior : ctBehaviors) {
                            if (ctBehavior != null) {
                                // 添加一个静态内部变量
                                if (!hasAddField) {
                                    hasAddField = true
                                    ClassPool classPool = ctBehavior.getDeclaringClass().getClassPool()
                                    Class fieldType = classPool.getOrNull("com.orzangleli.anivia.support.Patchable")
                                    CtField patchableFiled = new CtField(fieldType, "patchable", ctClass)
                                    patchableFiled.setModifiers(AccessFlag.STATIC | AccessFlag.PUBLIC)
                                    ctClass.addField(patchableFiled)
                                }
                                // 遍历每个方法插桩
                                if (!isQualifiedMethod(ctBehavior)) {
                                    continue
                                }
                                // 只对普通方法做处理
                                if (ctBehavior.getMethodInfo().isMethod()) {
                                    CtMethod ctMethod = (CtMethod) ctBehavior
                                    // 是否是静态方法
                                    boolean isStaticMethod = (ctMethod.getModifiers() & AccessFlag.STATIC) != 0
                                    String body = "Object argThis = null;"
                                    if (!isStaticMethod) {
                                        body += "argThis = \$0;"
                                    }

                                    CtClass[] paramTypes = ctMethod.getParameterTypes()
                                    String returnTypeName = ctMethod.getReturnType().getName()
                                    String paramTypeNames = convertClassArrayToStringArray(paramTypes)
                                    String []generatorParams = getGeneratorParams(ctMethod.getDeclaringClass().getName(), ctMethod.getName(), paramTypeNames)
                                    String methodId = MethodIdGenerator.getInstance().generate(generatorParams)
                                    // isPatchable(String methodId, boolean isStaticMethod, Object object, Patchable patchable, Object[] paramValues, Class[] paramTypes, Class returnType) {
                                    body += "com.orzangleli.anivia.PatchProxy p = null;"
                                    body += "if (com.orzangleli.anivia.PatchProxy.isPatchable(\""+ methodId +"\", " + isStaticMethod + ", argThis, patchable, \$args, " + paramTypeNames +", \"" + returnTypeName + "\")) { ";
                                    body += getReturnStatement(methodId, isStaticMethod, paramTypeNames, returnTypeName)
                                    body += " }"
                                    System.out.println("body0 --> " + body)
                                    ctMethod.insertBefore(body)
                                }
                            }
                        }

                        // 将修改后的类，写到目标文件中
                        byte[] bytes = ctClass.toBytecode()
                        System.out.println(new String(bytes))
                        target.write(bytes);

//                        ClassReader classReader = new ClassReader(getBytes(jarfile, jarEntry));
//                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
//                        ClassVisitor cv = new CostMethodClassVisitor(classWriter, jarEntry.getName(),
//                            getBundleName(inputJar.absolutePath));
//                        classReader.accept(cv, ClassReader.EXPAND_FRAMES);
//                        byte[] bytes = classWriter.toByteArray();
//                        target.write(bytes);
                    } else {
                        target.write(getBytes(jarfile, jarEntry));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("processJar => " + e.getMessage());
        } finally {
            try {
                if (target != null) {
                    target.closeEntry();
                    target.close();
                }
                if(jarfile != null) {
                    jarfile.close();
                }
            } catch (Throwable e) {
            }
        }
    }


    public byte[] processClass(File file, String className) {
        className = className.replaceAll("/", "\\.")
        className = className.replaceAll("\\.class", "")
        System.out.println("class path: " + file.getAbsolutePath())
        System.out.println("className : " + className)
        System.out.println("class 1: " + classPool.getOrNull(className))
        System.out.println("class 2: " + classPool.getCtClass(className))
        CtClass ctClass = classPool.get(className)
        System.out.println("class : " + ctClass)
        List<CtBehavior> ctBehaviors = ctClass.getDeclaredBehaviors()
        boolean hasAddField = false
        for (CtBehavior ctBehavior : ctBehaviors) {
            if (ctBehavior != null) {
                // 添加一个静态内部变量
                if (!hasAddField) {
                    hasAddField = true
                    ClassPool classPool = ctBehavior.getDeclaringClass().getClassPool()
                    CtClass fieldType = classPool.get("com.orzangleli.anivia.support.Patchable")
                    System.out.println("fieldType: " + fieldType)
                    System.out.println("fieldType in class: " + ctBehavior.getDeclaringClass().getName())
                    CtField patchableFiled = new CtField(fieldType, "patchable", ctClass)
                    patchableFiled.setModifiers(AccessFlag.STATIC | AccessFlag.PUBLIC)
                    ctClass.addField(patchableFiled)
                }
                // 遍历每个方法插桩
                if (!isQualifiedMethod(ctBehavior)) {
                    continue
                }
                // 只对普通方法做处理
                if (ctBehavior.getMethodInfo().isMethod()) {
                    CtMethod ctMethod = (CtMethod) ctBehavior
                    // 是否是静态方法
                    boolean isStaticMethod = (ctMethod.getModifiers() & AccessFlag.PUBLIC) != 0
                    String body = "Object argThis = null;"
                    if (!isStaticMethod) {
                        body += "argThis = \$0;"
                    }
                    CtClass[] paramTypes = ctMethod.getParameterTypes()
                    String returnTypeName = ctMethod.getReturnType().getName()
                    String paramTypeNames = convertClassArrayToStringArray(paramTypes)
                    String []generatorParams = getGeneratorParams(ctMethod.getDeclaringClass().getName(), ctMethod.getName(), paramTypeNames)
                    String methodId = MethodIdGenerator.getInstance().generate(generatorParams)
                    // isPatchable(String methodId, boolean isStaticMethod, Object object, Patchable patchable, Object[] paramValues, Class[] paramTypes, Class returnType) {
                    body += "com.orzangleli.anivia.PatchProxy p = null;"
                    body += "if (com.orzangleli.anivia.PatchProxy.isPatchable(\""+ methodId +"\", " + isStaticMethod + ", argThis, patchable, \$args, " + paramTypeNames +", \"" + returnTypeName + "\")) { ";
                    body += getReturnStatement(methodId, isStaticMethod, paramTypeNames, returnTypeName)
                    body += " }"
                    System.out.println("body --> " + body)
                    ctMethod.insertBefore(body)
                }
            }
        }

        // 将修改后的类，写到目标文件中
        byte[] bytes = ctClass.toBytecode()
        return bytes
    }


    private boolean isQualifiedMethod(CtBehavior it) throws CannotCompileException {
        if (it.getMethodInfo().isStaticInitializer()) {
            return false;
        }

        // synthetic 方法暂时不aop 比如AsyncTask 会生成一些同名 synthetic方法,对synthetic 以及private的方法也插入的代码，主要是针对lambda表达式
        if ((it.getModifiers() & AccessFlag.SYNTHETIC) != 0 && !AccessFlag.isPrivate(it.getModifiers())) {
            return false;
        }
        if (it.getMethodInfo().isConstructor()) {
            return false;
        }

        if ((it.getModifiers() & AccessFlag.ABSTRACT) != 0) {
            return false;
        }
        if ((it.getModifiers() & AccessFlag.NATIVE) != 0) {
            return false;
        }
        if ((it.getModifiers() & AccessFlag.INTERFACE) != 0) {
            return false;
        }

        if (it.getMethodInfo().isMethod()) {
            if (AccessFlag.isPackage(it.getModifiers())) {
                it.setModifiers(AccessFlag.setPublic(it.getModifiers()));
            }
            boolean flag = isMethodWithExpression((CtMethod) it);
            if (!flag) {
                return false;
            }
        }
       return true;
    }

    /**
     * 判断是否有方法调用， 没有被调用的方法不需要插桩
     *
     * @return 是否插桩
     */
    private boolean isMethodWithExpression(CtMethod ctMethod) throws CannotCompileException {
        boolean isCallMethod = false;
        if (ctMethod == null) {
            return false;
        }

        ctMethod.instrument(new ExprEditor() {
            /**
             * Edits a <tt>new</tt> expression (overridable).
             * The default implementation performs nothing.
             *
             * @param e the <tt>new</tt> expression creating an object.
             */
            //            public void edit(NewExpr e) throws CannotCompileException { isCallMethod = true; }

            /**
             * Edits an expression for array creation (overridable).
             * The default implementation performs nothing.
             *
             * @param a the <tt>new</tt> expression for creating an array.
             * @throws CannotCompileException
             */
            public void edit(NewArray a) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits a method call (overridable).
             *
             * The default implementation performs nothing.
             */
            public void edit(MethodCall m) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits a constructor call (overridable).
             * The constructor call is either
             * <code>super()</code> or <code>this()</code>
             * included in a constructor body.
             *
             * The default implementation performs nothing.
             *
             * @see #edit(javassist.expr.NewExpr)
             */
            public void edit(ConstructorCall c) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits an instanceof expression (overridable).
             * The default implementation performs nothing.
             */
            public void edit(Instanceof i) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits an expression for explicit type casting (overridable).
             * The default implementation performs nothing.
             */
            public void edit(Cast c) throws CannotCompileException {
                isCallMethod = true;
            }

            /**
             * Edits a catch clause (overridable).
             * The default implementation performs nothing.
             */
            public void edit(Handler h) throws CannotCompileException {
                isCallMethod = true;
            }
        });
        return isCallMethod;
    }

    /**
     * 根据传入类型判断调用PathProxy的方法
     *
     * @param type         返回类型
     * @param isStatic     是否是静态方法
     * @param methodNumber 方法数
     * @return 返回return语句
     */
    private String getReturnStatement(String methodId, boolean isStaticMethod, String paramTypes, String returnType) {
        switch (returnType) {
            case Constants.CONSTRUCTOR:
                return "com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\");  return null;";
            case Constants.LANG_VOID:
                return "com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\");  return null;";

            case Constants.VOID:
                return "com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\");  return ;";

            case Constants.LANG_BOOLEAN:
                return "return ((java.lang.Boolean)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\"));";
            case Constants.BOOLEAN:
                return "return ((java.lang.Boolean)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\")).booleanValue();";

            case Constants.INT:
                return "return ((java.lang.Integer)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\")).intValue();";
            case Constants.LANG_INT:
                return "return ((java.lang.Integer)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\"));";

            case Constants.LONG:
                return "return ((java.lang.Long)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\")).longValue();";
            case Constants.LANG_LONG:
                return "return ((java.lang.Long)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\"));";

            case Constants.DOUBLE:
                return "return ((java.lang.Double)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\")).doubleValue();";
            case Constants.LANG_DOUBLE:
                return "return ((java.lang.Double)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\"));";

            case Constants.FLOAT:
                return "return ((java.lang.Float)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\")).floatValue();";
            case Constants.LANG_FLOAT:
                return "return ((java.lang.Float)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\"));";

            case Constants.SHORT:
                return "return ((java.lang.Short)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\")).shortValue();";
            case Constants.LANG_SHORT:
                return "return ((java.lang.Short)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\"));";

            case Constants.BYTE:
                return "return ((java.lang.Byte)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\")).byteValue();";
            case Constants.LANG_BYTE:
                return "return ((java.lang.Byte)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\"));";

            case Constants.CHAR:
                return "return ((java.lang.Character)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\")).charValue();";
            case Constants.LANG_CHARACTER:
                return "return ((java.lang.Character)com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\"));";

            default:
                return "return (" + returnType + ")com.orzangleli.anivia.PatchProxy.transferAction(\""+ methodId +"\", " + isStaticMethod +", argThis, patchable, \$args, " + paramTypes +", \"" + returnType + "\");";
        }
    }

    public String convertClassArrayToStringArray(CtClass[] classes) {
        if (classes == null || classes.length == 0) {
            return " null "
        }
        StringBuilder paramType = new StringBuilder()
        paramType.append("new String[] {")
        for (int i=0; i<classes.length; i++) {
            paramType.append("\"" + classes[i].getName() + "\",")
        }
        if (',' == paramType.charAt(paramType.length() - 1)) {
            paramType.deleteCharAt(paramType.length() - 1);
        }
        paramType.append("}")
        return paramType
    }

    public String[] getGeneratorParams(String className, String methodName, String[] paramTypes) {
        int size = 0
        if (paramTypes == null) {
            size = 2
        } else {
            size = 2 + paramTypes.length
        }
        String []result = new String[size]
        result[0] = className
        result[1] = methodName
        for (int i = 2; i < size; i++) {
            result[i] = "\"" + paramTypes[i-2] + "\""
        }
        return result
    }

    private static byte[] getBytes(ZipFile jarfile, ZipEntry entry) throws IOException {
        InputStream inputStream = jarfile.getInputStream(entry);
        Throwable throwable = null;
        byte[] bytes;
        try {
            bytes = IOUtils.readFully(inputStream, (int)entry.getSize(), true);
        } catch (Throwable var13) {
            throwable = var13;
            throw var13;
        } finally {
            if(inputStream != null) {
                if(throwable != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var12) {
                        throwable.addSuppressed(var12);
                    }
                } else {
                    inputStream.close();
                }
            }
        }

        return bytes;
    }

}