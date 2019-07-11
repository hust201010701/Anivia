package com.orzangleli.anivia.patchgenerateplugin

import com.android.SdkConstants
import com.orzangleli.anivia.patchgenerateplugin.PatchGenerateProcessor
import javassist.ClassPool
import javassist.CtClass
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.internal.pipeline.TransformManager

import com.android.build.api.transform.*
import org.apache.commons.io.FileUtils
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import groovy.io.FileVisitResult

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher

class PatchGeneratePluginTransform extends Transform implements Plugin<Project> {

    private final static Logger logger = Logging.getLogger(PatchGeneratePluginTransform)
    private final static ClassPool classPool = ClassPool.getDefault()
    private Project project
    private final static PatchGenerateProcessor patchGenerateProcessor = new PatchGenerateProcessor()

    @Override
    String getName() {
        return "PatchGeneratePluginTransform"
    }

    //需要处理的数据类型，有两种枚举类型
    //CLASSES和RESOURCES，CLASSES代表处理的java的class文件，RESOURCES代表要处理java的资源
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    //    指Transform要操作内容的范围，官方文档Scope有7种类型：
    //
    //    EXTERNAL_LIBRARIES        只有外部库
    //    PROJECT                       只有项目内容
    //    PROJECT_LOCAL_DEPS            只有项目的本地依赖(本地jar)
    //    PROVIDED_ONLY                 只提供本地或远程依赖项
    //    SUB_PROJECTS              只有子项目。
    //    SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
    //    TESTED_CODE                   由当前变量(包括依赖项)测试的代码
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void apply(Project project) {
        this.project = project
        project.android.registerTransform(this)
        System.out.println("------------------Anivia 补丁包生成插件 注册成功----------------------")
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
        Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
        boolean isIncremental) throws IOException, TransformException, InterruptedException {
        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental)
        System.out.println("------------------Anivia 补丁包生成 开始----------------------")

        File jarFile = new File(project.buildDir.absolutePath + File.separator + "anivia" + File.separator + "patch.jar")
        System.out.println("jarFile:" + jarFile)
        if(!jarFile.getParentFile().exists()){
            jarFile.getParentFile().mkdirs();
        }
        if(jarFile.exists()){
            jarFile.delete();
        }

//        jarFile.createNewFile()

        //step1:将所有类的路径加入到ClassPool中
        project.android.bootClasspath.each {
            classPool.appendClassPath((String) it.absolutePath)
        }

        List<CtClass> allClasses = appendAllClasses(inputs, classPool)
        patchGenerateProcessor.setClassPool(classPool)

        patchGenerateProcessor.generatePatch(allClasses, jarFile)

        System.out.println("------------------Anivia 补丁包生成 输出文件："+ jarFile.absolutePath + " ----------------------")
        System.out.println("------------------Anivia 补丁包生成 结束----------------------")
    }

    public static List<CtClass> appendAllClasses(Collection<TransformInput> inputs, ClassPool classPool) {
        List<String> classNames = new ArrayList<>()
        List<CtClass> allClass = new ArrayList<>();
        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath
                classPool.insertClassPath(dirPath)
                org.apache.commons.io.FileUtils.listFiles(it.file, null, true).each {
                    if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        def className = it.absolutePath.substring(dirPath.length() + 1, it.absolutePath.length() - SdkConstants.DOT_CLASS.length()).replaceAll(
                            Matcher.quoteReplacement(File.separator), '.')
                        classNames.add(className)
                    }
                }
            }

            it.jarInputs.each {
                classPool.insertClassPath(it.file.absolutePath)
                def jarFile = new JarFile(it.file)
                Enumeration<JarEntry> classes = jarFile.entries();
                while (classes.hasMoreElements()) {
                    JarEntry libClass = classes.nextElement();
                    String className = libClass.getName();
                    if (className.endsWith(SdkConstants.DOT_CLASS)) {
                        className = className.substring(0, className.length() - SdkConstants.DOT_CLASS.length()).replaceAll('/', '.')
                        classNames.add(className)
                    }
                }
            }
        }

        for (String className : classNames) {
            allClass.add(classPool.get(className))
        }
        return allClass
    }

}