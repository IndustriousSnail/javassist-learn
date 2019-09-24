
    
## 六、泛型

**Javassist**的底层API完全支持了Java5中的泛型。另一方面，顶层API，例如 **CtClass**， 不能直接支持泛型。然而，这个对于字节码转换不是一个严重的问题。

Java中的泛型是通过消除技术实现的。 在编译之后，所有的类型参数都将消失。例如，假定你的源码声明了一个参数化的类型 **Vector<String** :

    Vector<String> v = new Vector<String>();
      :
    String s = v.get(0);
    
编译后的字节码就等同于下面：

    Vector v = new Vector();
      :
    String s = (String)v.get(0);
    
所以当你写一个字节码转换器时，你可以删除所有的类型参数。因为被嵌在Javassist中的编译器不支持泛型，所以对于使用Javassis插入的代码，你必须插入一个显式的类型转换。例如通过 **CtMethod.make()** 插入的代码。如果源码是被正常的Java编译器编译的话，比如javac，你就不需要做类型转换了。

例如，如果你有这么一个类：

    public class Wrapper<T> {
      T value;
      public Wrapper(T t) { value = t; }
    }
    
你想增添一个接口 **Getter<T>** 到类 **Wrapper<T>** 中：

    public interface Getter<T> {
      T get();
    }
    
那么你真正增添的是 **Getter** (类型参数<T>被丢弃了)，并且你必须向 **Wrapper** 类增添的方法就是下面这样一个简单的方法：

    public Object get() { return value; }
    
注意，不需要类型参数。因为 **get** 返回的是 **Object** ,所以在调用方需要显示的增加类型转换。例如，如果类型参数**T**是**String**, 那么 **(String)** 必须像下面这样被插入：

    Wrapper w = ...
    String s = (String)w.get();
    
如果编译器是正常的Java编译器，那么不需要显式的指定类型转换，它会自动插入类型转换代码。

## 七、可变参数(int... args)

目前，Javassist不直接支持可变参数。所以要让一个方法拥有可变参数，你必须显式的设置方法修饰符。但是这是容易的。假定现在你想创建下面的这个方法：

    public int length(int... args) { return args.length; }
    
上面的代码使用Javassist可以这样创建：

    CtClass cc = /* target class */;
    CtMethod m = CtMethod.make("public int length(int[] args) { return args.length; }", cc);
    m.setModifiers(m.getModifiers() | Modifier.VARARGS);
    cc.addMethod(m);
    
参数类型 **int...** 被变成了 **int[]** , 并且 **Modifier.VARARGS** 被增添到了方法修饰符中。

要在**Javassist**中的源码文本中调用该方法，你必须这样写：

    length(new int[] { 1, 2, 3 });
    
不能使用Java原生的调用方式：

    length(1, 2, 3);


## 八、J2ME

如果你要修改的文件是J2ME环境的，那么你必须执行预校验。预校验是生成堆栈映射（stack map）的基础，它与JDK1.6中的堆栈映射表很像。只有**javassist.bytecode.MethodInfo.doPreverify**为true的时候，Javassist才会为J2ME维护堆栈映射。

你也可以手工的为修改的方法生成一个堆栈映射。比如下面这个，**m** 是一个 **CtMethod** 对象，你可以调用下面方法来生成一个堆栈映射：

    m.getMethodInfo().rebuildStackMapForME(cpool);
    
这里， **cpool** 是一个 **ClassPool** 对象， 可以通过调用**CtClass**的 **getClassPool()** 方法获取。**ClassPool** 对象负责从给定路径找到class文件，这个前面章节已经说过了。要获取所有的**CtMethod**对象，可以调用**CtClass**的**getDeclaredMethods**方法。

## 九、拆箱和装箱

Java中的拆箱和装箱是个语法糖。是没有字节码的。所以Javassist的编译器不支持它们。例如，下面这个语句在Java中是合法的：

    Integer i = 3;
    
因为装箱是暗中执行。 对于Javassist来说，然而，你必须显式的将**int**转换为**Integer**：

    Integer i = new Integer(3);
    
## 十、Debug

把 **CtClass.debugDump** 的值设置成一个目录，那么Javassist修改和生成的所有class文件都将会被保存在该目录下。要是不想弄，把 **CtClass.debugDump** 设置为null就行了。默认值也是null。

例如：

    CtClass.debugDump = "./dump";
    
Javassist修改的所有class文件都将存储在 **./dump** 目录下。