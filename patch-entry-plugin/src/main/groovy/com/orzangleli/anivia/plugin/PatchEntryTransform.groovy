package com.orzangleli.anivia.plugin

import com.android.SdkConstants
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

class PatchEntryTransform extends Transform implements Plugin<Project> {

    private final static Logger logger = Logging.getLogger(PatchEntryTransform)
    private final static ClassPool classPool = ClassPool.getDefault()
    private Project project
    private final static PatchEntryProcessor patchEntryProcessor = new PatchEntryProcessor()
    private List<String> mAllAvailableConfigLines

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
        project.extensions.create('anivia', AniviaExtension)

        List<String> totalLines = new ArrayList<>()
        InputStream inputStream = this.getClass().getResourceAsStream("/anivia-android.txt")
        BufferedReader innerReader = new BufferedReader(new InputStreamReader(inputStream))
        String str = null
        while ((str = innerReader.readLine()) != null) {
            if (str != null) {
                totalLines.addAll(str)
            }
        }
        inputStream.close()
        if (project.anivia.aniviaFile != null) {
            File outFile = new File(project.projectDir.absolutePath + File.separator + project.anivia.aniviaFile)
            InputStream outFileInputStream = new FileInputStream(outFile)
            BufferedReader outFileReader = new BufferedReader(new InputStreamReader(outFileInputStream))
            while ((str = outFileReader.readLine()) != null) {
                if (str != null) {
                    totalLines.addAll(str)
                }
            }
            outFileInputStream.close()
        }
        mAllAvailableConfigLines = getAvailableLines(totalLines)
        for (String line: mAllAvailableConfigLines) {
            System.out.println("each line ：" + line)
        }
        System.out.println("------------------Anivia 插桩插件 注册成功----------------------")
    }

    List<String> getAvailableLines(List<String> totalLines) {
        List<String> lines = new ArrayList<>()
        for (String line : totalLines) {
            if (line != null && "" != line.trim() && !line.startsWith("#")) {
                lines.add(line)
            }
        }
        return lines
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
        Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
        boolean isIncremental) throws IOException, TransformException, InterruptedException {
        super.transform(context, inputs, referencedInputs, outputProvider, isIncremental)
        System.out.println("------------------Anivia 插桩 开始----------------------")

        File jarFile = outputProvider.getContentLocation("main", getOutputTypes(), getScopes(),
            Format.JAR);
        if(!jarFile.getParentFile().exists()){
            jarFile.getParentFile().mkdirs();
        }
        if(jarFile.exists()){
            jarFile.delete();
        }


        //step1:将所有类的路径加入到ClassPool中
        project.android.bootClasspath.each {
            classPool.appendClassPath((String) it.absolutePath)
        }

        List<CtClass> allClasses = appendAllClasses(inputs, classPool)
        patchEntryProcessor.setClassPool(classPool)
        patchEntryProcessor.setAllAvailableConfigLines(mAllAvailableConfigLines)

        patchEntryProcessor.injectCode(allClasses, jarFile)

        System.out.println("------------------Anivia 输出文件："+ jarFile.absolutePath + " ----------------------")
        System.out.println("------------------Anivia 插桩 结束----------------------")
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