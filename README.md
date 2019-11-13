注：Anivia 是基于 Robust 核心原理重写的一个热修复框架，仅供学习交流，不可用于生产使用。

# 美团Robust热修复框架原理解析

## 一、热修复框架现状
目前热修复框架主要有QQ空间补丁、HotFix、Tinker、Robust等。热修复框架按照原理大致可以分为三类：

1. 基于 multidex机制 干预 ClassLoader 加载dex
2. native 替换方法结构体
3. instant-run 插桩方案

QQ空间补丁和Tinker都是使用的方案一；
阿里的AndFix使用的是方案二；
美团的Robust使用的是方案三。

### 1. QQ空间补丁原理

把补丁类生成 `patch.dex`，在app启动时，使用反射获取当前应用的`ClassLoader`，也就是 `BaseDexClassLoader`，反射获取其中的`pathList`，类型为`DexPathList`， 反射获取其中的`Element[] dexElements`, 记为`elements1`;然后使用当前应用的`ClassLoader`作为父`ClassLoader`，构造出 `patch.dex` 的 `DexClassLoader`,通用通过反射可以获取到对应的`Element[] dexElements`，记为`elements2`。将`elements2`拼在`elements1`前面，然后再去调用加载类的方法`loadClass`。

>**隐藏的技术难点 CLASS_ISPREVERIFIED 问题**
> 
> apk在安装时会进行dex文件进行验证和优化操作。这个操作能让app运行时直接加载odex文件，能够减少对内存占用，加快启动速度，如果没有odex操作，需要从apk包中提取dex再运行。
>  
> 在验证过程，如果某个类的调用关系都在同一个dex文件中，那么这个类会被打上`CLASS_ISPREVERIFIED`标记，表示这个类已经预先验证过了。但是再使用的过程中会反过来校验下，如果这个类被打上了`CLASS_ISPREVERIFIED`但是存在调用关系的类不在同一个dex文件中的话，会直接抛出异常。
> 
> 为了解决这个问题，QQ空间给出的解决方案就是，准备一个 AntilazyLoad 类，这个类会单独打包成一个 hack.dex，然后在所有的类的构造方法中增加这样的代码：
> ```java
>if (ClassVerifier.PREVENT_VERIFY) {
>    System.out.println(AntilazyLoad.class);
>}
>```
> 这样在 odex 过程中，每个类都会出现 AntilazyLoad 在另一个dex文件中的问题，所以odex的验证过程也就不会继续下去，这样做牺牲了dvm对dex的优化效果了。

### 2. Tinker 原理

对于Tinker，修复前和修复后的apk分别定义为apk1和apk2，tinker自研了一套dex文件差分合并算法，在生成补丁包时，生成一个差分包 patch.dex，后端下发patch.dex到客户端时，tinker会开一个线程把旧apk的class.dex和patch.dex合并，生成新的class.dex并存放在本地目录上，重新启动时，会使用本地新生成的class.dex对应的elements替换原有的elements数组。

### 3. AndFix 原理

AndFix的修复原理是替换方法的结构体。在native层获取修复前类和修复后类的指针，然后将旧方法的属性指针指向新方法。由于不同系统版本下的方法结构体不同，而且davilk与art虚拟机处理方式也不一样，所以需要针对不同系统针对性的替换方法结构体。

```
// AndFix 代码目录结构
jni
├─ Android.mk
├─ Application.mk
├─ andfix.cpp
├─ art
│  ├─ art.h
│  ├─ art_4_4.h
│  ├─ art_5_0.h
│  ├─ art_5_1.h
│  ├─ art_6_0.h
│  ├─ art_7_0.h
│  ├─ art_method_replace.cpp
│  ├─ art_method_replace_4_4.cpp
│  ├─ art_method_replace_5_0.cpp
│  ├─ art_method_replace_5_1.cpp
│  ├─ art_method_replace_6_0.cpp
│  └─ art_method_replace_7_0.cpp
├─ common.h
└─ dalvik
   ├─ dalvik.h
   └─ dalvik_method_replace.cpp
```

## 二、美团 Robust 热修复方案原理

下面，进入今天的主题，Robust热修复方案。首先，介绍一下 Robust 的实现原理。

以 State 类为例
```java
public long getIndex() {
    return 100L;
}
```

插桩后的 State 类
```java
public static ChangeQuickRedirect changeQuickRedirect;
public long getIndex() {
    if(changeQuickRedirect != null) {
        //PatchProxy中封装了获取当前className和methodName的逻辑，并在其内部最终调用了changeQuickRedirect的对应函数
        if(PatchProxy.isSupport(new Object[0], this, changeQuickRedirect, false)) {
            return ((Long)PatchProxy.accessDispatch(new Object[0], this, changeQuickRedirect, false)).longValue();
        }
    }
    return 100L;
}
```

我们生成一个 StatePatch 类, 创一个实例并反射赋值给 State 的 changeQuickRedirect 变量。

```java
public class StatePatch implements ChangeQuickRedirect {
    @Override
    public Object accessDispatch(String methodSignature, Object[] paramArrayOfObject) {
        String[] signature = methodSignature.split(":");
        // 混淆后的 getIndex 方法 对应 a
        if (TextUtils.equals(signature[1], "a")) {//long getIndex() -> a
            return 106;
        }
        return null;
    }

    @Override
    public boolean isSupport(String methodSignature, Object[] paramArrayOfObject) {
        String[] signature = methodSignature.split(":");
        if (TextUtils.equals(signature[1], "a")) {//long getIndex() -> a
            return true;
        }
        return false;
    }
}
```
当我们执行出问题的代码 getState 时，会转而执行 StatePatch 中逻辑。这就 Robust 的核心原理，由于没有干扰系统加载dex过程,所以这种方案兼容性最好。

## 三、Robust 实现细节

Robust 的实现方案很简单，如果只是这么简单了解一下，有很多细节问题，我们不去接触就不会意识到。
Robust 的实现可以分成三个部分：插桩、生成补丁包、加载补丁包。下面先从插桩开始。

### 1. 插桩

Robust 预先定义了一个配置文件 `robust.xml`，在这个配置文件可以指定是否开启插桩、哪些包下需要插桩、哪些包下不需要插桩，在编译 Release 包时，RobustTransform 这个插件会自动遍历所有的类，并根据配置文件中指定的规则，对类进行以下操作：

1. 类中增加一个静态变量 `ChangeQuickRedirect changeQuickRedirect`
2. 在方法前插入一段代码，如果是需要修补的方法就执行补丁包中的方法，如果不是则执行原有逻辑。

常用的字节码操纵框架有：

- ASM
- AspectJ
- BCEL
- Byte Buddy
- CGLIB
- Cojen
- Javassist
- Serp

美团 Robust 分别使用了ASM、Javassist两个框架实现了插桩修改字节码的操作。个人感觉 javaassist 更加容易理解一些，下面的代码分析都以 javaassist 操作字节码为例进行阐述。

```java
for (CtBehavior ctBehavior : ctClass.getDeclaredBehaviors()) {
    // 第一步： 增加 静态变量 changeQuickRedirect
    if (!addIncrementalChange) {
        //insert the field
        addIncrementalChange = true;
        // 创建一个静态变量并添加到 ctClass 中
        ClassPool classPool = ctBehavior.getDeclaringClass().getClassPool();
        CtClass type = classPool.getOrNull(Constants.INTERFACE_NAME);  // com.meituan.robust.ChangeQuickRedirect
        CtField ctField = new CtField(type, Constants.INSERT_FIELD_NAME, ctClass);  // changeQuickRedirect
        ctField.setModifiers(AccessFlag.PUBLIC | AccessFlag.STATIC);
        ctClass.addField(ctField);
    }
    // 判断这个方法需要修复
    if (!isQualifiedMethod(ctBehavior)) {
        continue;
    }
    // 第二步： 方法前插入一段代码 ...
}
```

对于方法前插入一段代码，
```java
// Robust 给每个方法取了一个唯一id
methodMap.put(ctBehavior.getLongName(), insertMethodCount.incrementAndGet());
try {
    if (ctBehavior.getMethodInfo().isMethod()) {
        CtMethod ctMethod = (CtMethod) ctBehavior;
        boolean isStatic = (ctMethod.getModifiers() & AccessFlag.STATIC) != 0;
        CtClass returnType = ctMethod.getReturnType();
        String returnTypeString = returnType.getName();
        // 这个body 就是要塞到方法前面的一段逻辑
        String body = "Object argThis = null;";
        // 在 javaassist 中 $0 表示 当前实例对象，等于this
        if (!isStatic) {
            body += "argThis = $0;";
        }
        String parametersClassType = getParametersClassType(ctMethod);
        // 在 javaassist 中 $args 表达式代表 方法参数的数组，可以看到 isSupport 方法传了这些参数：方法所有参数，当前对象实例，changeQuickRedirect，是否是静态方法，当前方法id，方法所有参数的类型，方法返回类型
        body += "   if (com.meituan.robust.PatchProxy.isSupport($args, argThis, " + Constants.INSERT_FIELD_NAME + ", " + isStatic +
                ", " + methodMap.get(ctBehavior.getLongName()) + "," + parametersClassType + "," + returnTypeString + ".class)) {";
        // getReturnStatement 负责返回执行补丁包中方法的代码
        body += getReturnStatement(returnTypeString, isStatic, methodMap.get(ctBehavior.getLongName()), parametersClassType, returnTypeString + ".class");
        body += "   }";
        // 最后，把我们写出来的body插入到方法执行前逻辑
        ctBehavior.insertBefore(body);
    }
} catch (Throwable t) {
    //here we ignore the error
    t.printStackTrace();
    System.out.println("ctClass: " + ctClass.getName() + " error: " + t.getMessage());
}
```

再来看看 `getReturnStatement` 方法，

```java
 private String getReturnStatement(String type, boolean isStatic, int methodNumber, String parametersClassType, String returnTypeString) {
        switch (type) {
            case Constants.CONSTRUCTOR:
                return "    com.meituan.robust.PatchProxy.accessDispatchVoid( $args, argThis, changeQuickRedirect, " + isStatic + ", " + methodNumber + "," + parametersClassType + "," + returnTypeString + ");  ";
            case Constants.LANG_VOID:
                return "    com.meituan.robust.PatchProxy.accessDispatchVoid( $args, argThis, changeQuickRedirect, " + isStatic + ", " + methodNumber + "," + parametersClassType + "," + returnTypeString + ");   return null;";
            // 省略了其他返回类型处理
        }
 }
 ```

`PatchProxy.accessDispatchVoid` 最终调用了 `changeQuickRedirect.accessDispatch`。

至此插桩环节就结束了。

### 2. 生成补丁包

Robust 定义了一个 Modify 注解，

```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface Modify {
    String value() default "";
}
```

对于要修复的方法，直接在方法声明时增加 `Modify`注解

```
@Modify
public String getTextInfo() {
    getArray();
    //return "error occur " ;
    return "error fixed";
}
```
在编译期间，Robust逐一遍历所有类，如果这个类有方法需要修复，Robust 会生一个 xxPatch 的类：
1. 第一步 根据bug类 clone 出 Patch 类， 然后再删除不需要打补丁的类。（为什么使用删除方法而不是新增方法？ 删除更简单）
2. 第二步 为 Patch 创建一个构造方法，用来接收bug类的实例对象。
3. 遍历 Patch 类中的所有方法，使用 ExprEditor + 反射 修改表达式。
4. 删除 Patch 类中所有的变量和父类。

这里举个例子，为什么这里的处理这么麻烦。

```java
public class Test {
    private int num = 0;
    public void increase() {
        num += 1;
    }
    public void decrease() {
        // 这里减错了
        num -= 2;
    }
    public static void main(String[] args) {
        Test t1 = new Test();
        // 执行完 num=1
        t1.increase();
        // 执行完 num=2
        t1.increase();
        // 执行完 num=0， decrease 方法出现了bug，我们本意是减1，结果减2了
        t1.decrease();
    }
}
```
所以当我们下发补丁时，对num进行减1的操作也是针对t1对象的num操作。这就是为什么我们需要创建一个构造方案接受bug类实例对象。再来说下，我们如何在 TestPatch 类中把所有对 TestPatch 变量和方法等调用迁移到 Test 上。这就需要使用到 ExprEditor (表达式编辑器)。

```java
// 这个 method 就是 TestPatch 修复后的那个方法
method.instrument(
    new ExprEditor() {
        // 处理变量访问
        public void edit(FieldAccess f) throws CannotCompileException {
            if (Config.newlyAddedClassNameList.contains(f.getClassName())) {
                return;
            }
            Map memberMappingInfo = getClassMappingInfo(f.getField().declaringClass.name);
            try {
                // 如果是 读取变量，那么把 f 使用replace方法，替换成括号里的返回的表达式
                if (f.isReader()) {
                    f.replace(ReflectUtils.getFieldString(f.getField(), memberMappingInfo, temPatchClass.getName(), modifiedClass.getName()));
                }
                // 如果是 写数据到变量
                else if (f.isWriter()) {
                    f.replace(ReflectUtils.setFieldString(f.getField(), memberMappingInfo, temPatchClass.getName(), modifiedClass.getName()));
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }
    }
)
```

ReflectUtils.getFieldString 方法调用的结果是生成一串类似这样的字符串：

`\$_=(\$r) com.meituan.robust.utils.EnhancedRobustUtils.getFieldValue(fieldName, instance, clazz)`

这样在 TestPatch 中对变量 num 的调用，在编译期间都会转为通过反射对 原始bug类对象 t1 的 num 变量调用。

ExprEditor 除了变量访问 FieldAccess， 还有这些情况需要特殊处理。

```java
public void edit(NewExpr e) throws CannotCompileException {
}

public void edit(MethodCall m) throws CannotCompileException {
}

public void edit(FieldAccess f) throws CannotCompileException {
}

public void edit(Cast c) throws CannotCompileException {
}

```

需要处理的情况太多了，以致于Robust的作者都忍不住吐槽：
`shit !!too many situations need take into  consideration`

生成完 Patch 类之后，Robust 会从模板类的基础上生成一个这个类专属的 ChangeQuickRedirect 类， 模板类代码如下：

```java
public class PatchTemplate implements ChangeQuickRedirect {
    public static final String MATCH_ALL_PARAMETER = "(\\w*\\.)*\\w*";

    public PatchTemplate() {
    }

    private static final Map<Object, Object> keyToValueRelation = new WeakHashMap<>();

    @Override
    public Object accessDispatch(String methodName, Object[] paramArrayOfObject) {
        return null;
    }

    @Override
    public boolean isSupport(String methodName, Object[] paramArrayOfObject) {
        return true;
    }

}
```

以Test类为例，生成 ChangeQuickRedirect 类名为 TestPatchController, 在编译期间会在 isSupport 方法前加入过滤逻辑，

```java
// 根据方法的id判断是否是补丁方法执行
public boolean isSupport(String methodName, Object[] paramArrayOfObject) {
    return "23:".contains(methodName.split(":")[3]);
}
```

以上两个类生成后，会生成一个维护 bug类 -->  ChangeQuickRedirect 类的映射关系

```java
public class PatchesInfoImpl implements PatchesInfo {
    public List getPatchedClassesInfo() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new PatchedClassInfo("com.meituan.sample.Test", "com.meituan.robust.patch.TestPatchControl"));
        EnhancedRobustUtils.isThrowable = false;
        return arrayList;
    }
}
```

以一个类的一个方法修复生成补丁为例，补丁包中包含三个文件：
- TestPatch
- TestPatchController
- PatchesInfoImpl

生成的补丁包是jar格式的，我们需要使用 jar2dex 将 jar 包转换成 dex包。

### 3. 加载补丁包

当线上app反生bug后，可以通知客户端拉取对应的补丁包，下载补丁包完成后，会开一个线程执行以下操作：

1. 使用 DexClassLoader 加载外部 dex 文件，也就是我们生成的补丁包。
2. 反射获取 PatchesInfoImpl 中补丁包映射关系，如PatchedClassInfo("com.meituan.sample.Test", "com.meituan.robust.patch.TestPatchControl")。
3. 反射获取 Test 类插桩生成 changeQuickRedirect 对象，实例化 TestPatchControl，并赋值给 changeQuickRedirect

至此，bug就修复了，无需重启实时生效。

### 4. 一些问题

**a. Robust 导致Proguard 方法内联失效**
    
Proguard是一款代码优化、混淆利器，Proguard 会对程序进行优化，如果某个方法很短或者只被调用了一次，那么Proguard会把这个方法内部逻辑内联到调用处。
Robust的解决方案是找到内联方法，不对内联的方法插桩。

**b. lambada 表达式修复**

对于 lambada 表达式无法直接添加注解，Robust 提供了一个 RobustModify 类，modify 方法是空方法，再编译期间使用 ExprEditor 检测是否调用了 RobustModify 类，如果调用了，就认为这个方法需要修复。

```
new Thread(
        () -> {
            RobustModify.modify();
            System.out.print("Hello");
            System.out.println(" Hoolee");
        }
).start();
```

**c. Robust 生成方法id是通过编译期间遍历所有类和方法，递增id实现的**

一个方法，可以通过类名 + 方法名 + 参数类型唯一确定。我自己的方案是把这三个数据组装成 `类名@方法名#参数类型md5`,支持 lambada 表达式（`com.orzangleli.demo.Test#lambda$execute$0@2ab6d5a5d73bad3848b7be22332e27ea`）。我自己基于 Robust 的核心原理，仿写了一个热修复框架 Anivia.

![image.png](https://i.loli.net/2019/10/22/a1jTiMgnxNpRGFK.png)

[https://github.com/hust201010701/Anivia](https://github.com/hust201010701/Anivia)

## 四、总结

首先要认可国内不同热修复方案的开发者和组织做出的工作，做好热修复解决方案不是一件简单的事。
其次，从别人解决热修复方案实施过程遇到问题上来看，这些开发者遇到问题后，追根溯源，会去找导致这个问题的本质原因，然后才思考解决方案，这一点很值得我们学习。







