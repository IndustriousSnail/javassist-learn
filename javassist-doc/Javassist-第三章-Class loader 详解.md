##### 这是把官方的文档给翻译了，顺便学习，一共10章，可以到下面地址查看，里面可能有翻译不准的地方，欢迎指正

    https://github.com/IndustriousSnail/javassist-learn
    
如果一开始你就知道要修改哪个类，那么最简单的方式如下：

- 1.调用**ClassPool.get()** 来获取一个**CtClass**对象。
- 2.修改它
- 3.调用**writeFile()** 或 **toBytecode()** 来获取一个修改后的class文件

如果一个类是否要被修改是在加载时确定的，用户就必须让Javassist和类加载器协作。Javassist可以和类加载器一块儿使用，以便于可以在加载时修改字节码。用户可以自定义类加载器，也可以使用Javassist提供好的。

### 3.1. CtClass的 toClass() 方法

**CtClass**提供了一个方便的方法**toClass()**, 它会请求当前线程的上下文类加载器，让其加载**CtClass**对象所代表的那个类。要调用这个方法，必须要拥有权限。此外，该方法还会抛出**SecurityException**异常。

使用**toClass()** 方法样例：

    public class Hello {
        public void say() {
            System.out.println("Hello");
        }
    }
    
    public class Test {
        public static void main(String[] args) throws Exception {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get("Hello");
            // 获取say方法
            CtMethod m = cc.getDeclaredMethod("say");
            // 在方法第一行前面插入代码
            m.insertBefore("{ System.out.println(\"Hello.say():\"); }");
            Class c = cc.toClass();
            Hello h = (Hello)c.newInstance();
            h.say();
        }
    }

**Test.main()** 在**Hello**的**say()** 方法的方法体中插入了**println()** 的调用。然后构建了被修改后的**Hello**的实例，然后调用了该实例的**say()** 方法。

注意，上面这段程序有一个前提，就是**Hello**类在调用**toClass()** 之前没有被加载过。否则，在**toClass()** 请求加载被修改后的**Hello**类之前，JVM就会加载原始的**Hello**类。因此，加载被修改后的**Hello**类就会失败（抛出LinkageError）。例如:

    public static void main(String[] args) throws Exception {
        Hello orig = new Hello();
        ClassPool cp = ClassPool.getDefault();
        CtClass cc = cp.get("Hello");
        Class c = cc.toClass();  // 这句会报错
    }
    
**main**函数的第一行加载了**Hello**类，**cc.toClass()** 这行就会抛出异常。原因是类加载器不能同时加载两个不同版本的**Hello**类。

如果你的程序运行在JBOSS或Tomcat的应用服务器上，那么你再用**toClass()** 就有点不合适了。这种情况下，将会抛出**ClassCastException**异常。为了避免这个异常，你必须给**toClass()** 一个合适的类加载器。例如，假设**bean**是你的会话bean对象，那么这段代码：

    CtClass cc = ...;
    Class c = cc.toClass(bean.getClass().getClassLoader());
    
这段代码可以正常运行。你应该给**toClass()** 的类加载器是加载你程序的加载器（上面的例子中，就是bean对象的class的类加载器）。

**toClass()** 已经很方便了。你要是想更复杂的类加载器，你应该自定义类加载器。

### 3.2 Java中的类加载

在Java中，多个类加载器可以共存，它们可以创建自己的命名空间。不同的类加载器能够加载有着相同类名的不同的类文件。被加载过的两个类会被视为不同的东西。这个特点可以让我们在一个JVM中运行多个应用程序，尽管它们包含了有着相同名称的不同的类。

> JVM不允许动态重新加载一个类。一旦类加载加载过一个类之后，在运行期就不能在加载该类的另一个被修改过的版本。因此，你不能在JVM加载过一个类之后修改它的定义。但是，JPDA(Java Platform Debugger Architecture)提供了重新加载类的一些能力。详细请看3.6

如果两个不同的类加载器加载里一个相同的Class文件，那么JVM会生成两个不同的Class，虽然它们拥有相同的名字和定义。这两个Class会被视为两个不同的东西。因为这两个Class不是完全相同的，所以一个Class的实例不能赋值给另一个Class的变量。这两个类之间的类型转换会失败，抛出**ClassCastException**异常。

例如，下面这个代码片段就会抛出该异常：

    MyClassLoader myLoader = new MyClassLoader();
    Class clazz = myLoader.loadClass("Box");
    Object obj = clazz.newInstance();
    Box b = (Box)obj;    // 这里总是会抛出ClassCastException异常.

**Box**类被两个类加载器所加载。假定**CL**类加载器加载了这段代码片段。因为该代码中引用了**MyClassLoader**,**Class**,**Object**，所以**CL**也会加载这些类(除非它代理了其它啊类加载器)。因此，**b** 变量的类型是**CL**加载的**Box**。但是**obj**变量的类型是**myLoader**加载的**Box**，虽然都是**Box**，但是不一样。所以，最后一段代码一定会抛出**ClassCastException**，因为**b**和**obj**是两个不同版本的**Box**。

多个类加载形成了一个树型结构。除了启动加载器之外，其他的类加载器都有一个父类加载，子类加载器通常由父类加载器加载。由于加载类的请求可以沿着加载器的层次结构进行委托，所以你请求加载类的加载器，并不一定真的是由这个加载器加载的，也可能换其他加载器加载了。因此（举例），请求加载类**C**的加载器可能不是真正加载类**C**的加载器。不同的是，我们将前面的加载器称为**C**的发起者（initiator），后面的加载器称为C实际的加载器(real loader)。

除此之外，如果类加载器**CL**请求加载一个类**C**（C的发起者）委托给了它的父类加载器**PL**，那么类加载器**CL**也不会加载类**C**定义中引用的任何其他类。对于那些类，**CL**不是它们的发起者，相反，父加载器**PL**则会称为它们的发起者，并且回去加载它们。类**C**定义中引用的类，由类C的实际的加载器去加载。

要理解上面的行为，可以参考下面代码：

    public class Point {    // PL加载该类
        private int x, y;
        public int getX() { return x; }
            :
    }
    
    public class Box {      // L是发起者，但实际的加载器是PL
        private Point upperLeft, size;
        public int getBaseX() { return upperLeft.x; }
            :
    }
    
    public class Window {    // 该类被加载器L加载
        private Box box;
        public int getBaseX() { return box.getBaseX(); }
    }
    
假定类加载器**L**加载**Window**类。加载**Window**的发起者和实际加载者都是**L**。因为**Window**的定义引用了类**Box**，所以JVM会让 **L**去加载**Box**类。这里，假定**L**将该任务委托给了父类加载器**PL**，所以加载**Box**的发起者是**L**，但实际加载者是**PL**。这种情况下，**PL**作为**Box**的实际加载者，就会去加载**Box**中定义中引用的**Point**类，所以**Point**的发起者和实际加载者都是**PL**。因此加载器**L**从来都没有请求过加载**Point**类。

把上面的例子稍微改一下：

    public class Point {
        private int x, y;
        public int getX() { return x; }
            :
    }
    
    public class Box {      // 发起者是L，但实际加载者是PL
        private Point upperLeft, size;
        public Point getSize() { return size; }
            :
    }
    
    public class Window {    // Window由加载器L加载
        private Box box;
        public boolean widthIs(int w) {  // 增加了方法，方法中有对Point类的引用。
            Point p = box.getSize();
            return w == p.getX();
        }
    }
    
上面中，**Window**也引用了**Point**。这样，如果加载器**L**需要加载**Point**的话，**L**也必须委托给**PL**。*你必须避免让两个类加载器重复加载同一个类*。两个加载器中的一个必须委托给另一个加载器。

如果当**Point**被加载时，**L**没有委托给**PL**，那么**widthIs()**就会抛出**ClassCastException**。因为**Window**里的**Point**是**L**加载的，而**Box**中的**Point**是**PL**加载器加载的。你用**box.getSize()** 返回的**PL.Point**给**L的Point**，那么就会JVM就会认为它们是不同的实例，进而抛出异常。

这样有些不方便，但是需要有这种限制。比如：

    Point p = box.getSize();
    
如果这条语句没有抛出异常，那么**Window**的代码就有可能打破**Point**的封装。例如，**PL**加载的**Point**的**x**变量是private，但是**L**加载器加载的**Point**的**x**变量是public(下面的代码定义)，那么不就打破了封装定义。

    public class Point {
        public int x, y;    // not private
        public int getX() { return x; }
    }
    
要是想了解更多关于JAVA类加载器的细节，可以参考下面这个论文：

    Sheng Liang and Gilad Bracha, "Dynamic Class Loading in the Java Virtual Machine", 
    ACM OOPSLA'98, pp.36-44, 1998.
    

### 3.3 使用javassist.Loader

Javassist提供了一个类加载器**javasist.Loader**，该加载器使用一个**javassist.ClassPool**对象来读取类文件。

例如，**javassist.Loader**可以加载一个被**Javassist**修改过的特定类：

    import javassist.*;
    import test.Rectangle;
    
    public class Main {
      public static void main(String[] args) throws Throwable {
         ClassPool pool = ClassPool.getDefault();
         Loader cl = new Loader(pool);
    
         CtClass ct = pool.get("test.Rectangle");
         ct.setSuperclass(pool.get("test.Point"));
    
         Class c = cl.loadClass("test.Rectangle");
         Object rect = c.newInstance();
      }
    }

这段 程序修改了**test.Rectangle**，将它的父类设置为了**test.Point**。然后程序加载了修改后的类，并且创建了**test.Rectangle**的一个新实例。

如果用户想根据需要在类被加载的时候修改类，那么用户可以增添一个事件监听器给**javassist.Loader**。该事件监听器会在类加载器加载类时被通知。事件监听器必须实现下面这个接口：

    public interface Translator {
        public void start(ClassPool pool)
            throws NotFoundException, CannotCompileException;
        public void onLoad(ClassPool pool, String classname)
            throws NotFoundException, CannotCompileException;
    }
    
当使用**javassist.Loader**的**addTranslator()** 方法增添事件监听器时，**start()** 方法就会被调用。在**javassist.Loader**加载类之前，**onLoad()** 方法就会被调用。你可以在**onLoad()** 方法中修改要加载的类的定义。

例如，下面的事件监听器就在类被加载之前把它们都修改成public类。

    public class MyTranslator implements Translator {
        void start(ClassPool pool)
            throws NotFoundException, CannotCompileException {}
        void onLoad(ClassPool pool, String classname)
            throws NotFoundException, CannotCompileException
        {
            CtClass cc = pool.get(classname);
            cc.setModifiers(Modifier.PUBLIC);
        }
    }
    
注意**onLoad()**不必调用**toBytecode()**或**writeFile()**，因为**javassist.Loader**会调用这些方法来获取类文件。  

要想运行一个带有**Mytranslator**对象的*application*(带main方法，可以运行的)类**MyApp**，可以这样写：

    import javassist.*;
    
    public class Main2 {
      public static void main(String[] args) throws Throwable {
         Translator t = new MyTranslator();
         ClassPool pool = ClassPool.getDefault();
         Loader cl = new Loader();
         cl.addTranslator(pool, t);
         cl.run("MyApp", args);
      }
    }
    
然后这样运行这个程序：

    > java Main2 arg1 arg2...
    
这样**MyApp**和其他的应用程序类就会被**MyTranslator**转换了。

注意，像**MyApp**这样的应用类不能访问加载器的类，不如**Main2**，**MyTranslator**和**ClassPool**。因为他们是被不同的加载器加载的。应用类时**javassist.Loader**加载的，然而像**Main2**这些是被默认的java类加载器加载的。

**javassist.Loader**搜索类的顺序和**java.lang.ClassLoader.ClassLoader**不同。**JavaClassLoader**首先会委托父加载器进行加载操作，父加载器找不到的时候，才会由子加载器加载。而**javassist.Loader**首先尝试加载类，然后才会委托给父加载器。只有在下面这些情况才会进行委托：

- 调用**get()**方法后在**ClassPool**对象中找不到
- 使用**delegateLoadingOf()** 方法指定要父类加载器去加载

这个搜索顺序机制允许**Javassist**加载修改后的类。然而，如果它因为某些原因找不到修改后的类的话，就会委托父加载器去加载。一旦该类被父加载器加载，那么该类中引用的类也会用父加载器加载，并且它们不能再被修改了。回想下，之前类C的实际加载器加载了类C所有引用的类。如果你的程序加载一个修改过的类失败了，那么你就得想想是否那些类是否使用了被**javassist.Loader**加载的类。


### 3.4 自定义一个类加载器

一个简单的类加载器如下：

    import javassist.*;
    
    public class SampleLoader extends ClassLoader {
        /* Call MyApp.main().
         */
        public static void main(String[] args) throws Throwable {
            SampleLoader s = new SampleLoader();
            Class c = s.loadClass("MyApp");
            c.getDeclaredMethod("main", new Class[] { String[].class })
             .invoke(null, new Object[] { args });
        }
    
        private ClassPool pool;
    
        public SampleLoader() throws NotFoundException {
            pool = new ClassPool();
            pool.insertClassPath("./class"); // MyApp.class must be there.
        }
    
        /* Finds a specified class.
         * The bytecode for that class can be modified.
         */
        protected Class findClass(String name) throws ClassNotFoundException {
            try {
                CtClass cc = pool.get(name);
                // modify the CtClass object here
                byte[] b = cc.toBytecode();
                return defineClass(name, b, 0, b.length);
            } catch (NotFoundException e) {
                throw new ClassNotFoundException();
            } catch (IOException e) {
                throw new ClassNotFoundException();
            } catch (CannotCompileException e) {
                throw new ClassNotFoundException();
            }
        }
    }
    
**MyApp**是一个应用程序。要执行这段程序，首先要放一个class文件到 **./class** 目录下，该目录不能包含在类搜索路径下。否则，**MyApp.class**将会被默认的系统类加载器加载，也就是**SampleLoader**的父类加载器。你也可以把**insertClassPath**中的 **./class** 放入构造函数的参数中，这样你就可以选择自己想要的路径了。 运行java程序：

    > java SampleLoader
    
类加载器加载了类**MyApp**(./class/MyApp.class)，并且调用了**MyApp.main()** ，并传入了命令行参数。

这是使用Javassist最简单的方式。如果你想写个更复杂的类加载器，你可能需要更多的java类加载机制的知识。例如，上面的程序把**MyApp**的命名空间和**SampleLoader**的命名空间是分开的，因为它们两个类是由不同的类加载器加载的。因此，**MyApp**不能直接访问**SampleLoader**类。

### 3.5 修改系统类

除了系统类加载器，系统类不能被其他加载器加载，比如**java.lang.String**。因此，上面的**SampleLoader**和**javassist.Loader**在加载期间不能修改系统类。

如果你的程序非要那么做，请“静态的”修改系统类。例如，下面的程序给**java.lang.String**增添了**hiddenValue**属性。

    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("java.lang.String");
    CtField f = new CtField(CtClass.intType, "hiddenValue", cc);
    f.setModifiers(Modifier.PUBLIC);
    cc.addField(f);
    cc.writeFile(".");
    
这个程序会生成一个文件 **./java/lang/String.class**

用修改过的**String**类运行一下你的程序**MyApp**，按照下面：

    > java -Xbootclasspath/p:. MyApp arg1 arg2...
    
假定**MyApp**的代码是这样的：

    public class MyApp {
        public static void main(String[] args) throws Exception {
            System.out.println(String.class.getField("hiddenValue").getName());
        }
    }
    
如果被修改的**String**正常加载的话，**MyApp**就会打印**hiddenValue**。

> 应用最好不要使用该技术去重写**rt.jar**中的内容，这样会违反Java 2 Runtime Environment binary code 协议。

### 3.6 运行期重新加载一个类

启动JVM时，如果开启了JPDA(Java Platform Debugger Architecture)，那么class就可以动态的重新加载了。在JVM加载一个类之后，旧的类可以被卸载，然后重新加载一个新版的类。意思就是，类的定义可以在运行期动态的修改。但是，新类一定要能和旧类相互兼容。JVM不允许两个版本存在模式的改变，它们需要有相同的方法和属性。

**Javassist**提供了一个很方便的类，用于在运行期改变类。想了解更多信息，可以看**javassist.tools.HotSwapper**的API文档