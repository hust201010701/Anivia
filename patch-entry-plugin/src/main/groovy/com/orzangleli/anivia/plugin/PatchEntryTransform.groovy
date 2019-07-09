package com.orzangleli.anivia.plugin

import javassist.ClassPool
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.internal.pipeline.TransformManager

import com.android.build.api.transform.*
import org.apache.commons.io.FileUtils
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import groovy.io.FileVisitResult

class PatchEntryTransform extends Transform implements Plugin<Project> {

    private final static Logger logger = Logging.getLogger(PatchEntryTransform)
    private final static ClassPool classPool = ClassPool.getDefault()
    private Project project

    @Override
    String getName() {
        return "PatchEntryTransform"
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
        System.out.println("------------------Anivia 插桩插件 注册成功----------------------")
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
        Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
        boolean isIncremental) throws IOException, TransformException, InterruptedException {
        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental)
        System.out.println("------------------Anivia 插桩 开始----------------------")

        //step1:将所有类的路径加入到ClassPool中
        project.android.bootClasspath.each {
            classPool.appendClassPath((String) it.absolutePath)
            if (it.absolutePath.contains("Patchable")) {
                System.out.println("Patchable: " + it.absolutePath)
            }
        }

        appendAllClasses(inputs, classPool)

        PatchEntryProcessor.setClassPool(classPool)

        inputs.each { TransformInput input ->
            /**
             * 遍历目录
             */
            input.directoryInputs.each { DirectoryInput directoryInput ->

                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes,
                    directoryInput.scopes, Format.DIRECTORY);
                //这里进行字节码注入处理
                logger.debug "process directory = ${directoryInput.file.absolutePath}"
                File tmpDestDir = new File(directoryInput.file.parentFile, "tmp")
                if(!tmpDestDir.exists()) {
                    tmpDestDir.mkdirs();
                }

                processDirectory(directoryInput.file, tmpDestDir);
                FileUtils.copyDirectory(tmpDestDir, dest);
                tmpDestDir.deleteDir();
            }

            /**
             * 遍历jar
             */
            input.jarInputs.each { JarInput jarInput ->

                String destName = jarInput.name;
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4);
                }
                File dest = outputProvider.getContentLocation(destName, jarInput.contentTypes,
                    jarInput.scopes, Format.JAR);
                //处理jar进行字节码注入处理 TODO
                logger.error("process jar = " + jarInput.file.absolutePath);
                if(PatchEntryProcessor.shouldProcessJar(jarInput.file.absolutePath)) {
                    File tmpDest = File.createTempFile(jarInput.file.name, ".tmp", jarInput.file.parentFile);
                    PatchEntryProcessor.processJar(jarInput.file, tmpDest);
                    FileUtils.copyFile(tmpDest, dest, true);
                    tmpDest.delete();
                } else {
                    logger.error("ignore process jar = " + jarInput.file.absolutePath);
                    FileUtils.copyFile(jarInput.file, dest, true)
                }
            }
        }


        System.out.println("------------------Anivia 插桩 结束----------------------")
    }

    private void processDirectory(File sourceDir, File destDir) {
        sourceDir.traverse { inputFile ->
            if (!inputFile.isDirectory()) {
                String relativePath = relativize(sourceDir, inputFile)
                File outputFile = new File(destDir, relativePath)
                if(PatchEntryProcessor.shouldProcessClass(relativePath)) {
                    def bytes = PatchEntryProcessor.processClass(inputFile, relativePath)
                    FileUtils.copyBytesToFile(bytes, outputFile)
                } else {
                    logger.error("ignore process classFile = " + inputFile.absolutePath)
                    FileUtils.copyFile(inputFile, outputFile, true)
                }
            }
            return FileVisitResult.CONTINUE
        }
    }

    public static String relativize(final File parent, final File child) {
        final URI relativeUri = parent.toURI().relativize(child.toURI())
        return relativeUri.toString()
    }

    public static void appendAllClasses(Collection<TransformInput> inputs, ClassPool classPool) {
        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath
                classPool.insertClassPath(dirPath)
                if (dirPath.contains("com.orzangleli.anivia")) {
                    System.out.println("Patchable: " + dirPath)
                }
            }

            it.jarInputs.each {
                classPool.insertClassPath(it.file.absolutePath)
                if (it.file.absolutePath.contains("Patchable")) {
                    System.out.println("Patchable: " + it.file.absolutePath)
                }
            }
        }
    }

}