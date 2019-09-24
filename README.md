[toc]

## 一、读写字节码
### 1. 获取类文件对象CtClass

Javassist是一个用于处理Java字节码的库。Java字节码存储在一个class结尾的二进制文件中。每一个class文件都包含了一个Java类或接口。

**javassist.CtClass**是class文件的一个抽象代表。一个**CtClass**（编译期类）对象处理一个class文件。例如：

    // 见chapter.one.Test1
    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("test.Rectangle");
    cc.setSuperclass(pool.get("test.Point"));
    cc.writeFile();
    
这个程序首先定义了个**ClassPool**对象，它控制着字节码的修改。**ClassPool**对象是**CtClass**对象的一个容器，它代表一个Class文件。它会读取Class（test.Rectangle）文件，然后构造一个**CtClass**对象。为了修改一个类，用户必须用**ClassPool**对象的**get()**方法来获取**CtClass**对象。上面展示的例子中，**CtClass**的实例**cc**代表类**test.Rectanle**。**ClassPool**实例通过 **getDefault()** 方法实例化，它采用默认的搜索路径方式。

从实现的角度看，**ClassPool**是**CtClass**对象的一个Hash表，**ClassPool**使用类名作为键。当使用**classPool.get()** 方法时，会搜索Hash表，根据类名找出相应的**CtClass**对象。如果该对象没找到，就会读取类文件，然后构造一个**CtClass**对象，将其存到Hash表中，并返回结果。

**CtClass**对象可以被修改（第四章会详细介绍）。上面的例子中，它将**test.Point**作为自己的父类。在调用**writeFile()** 后，该修改就会反映到源class文件中。

### 2. 获取字节码

**writeFile()** 将**CtClass**对象转化为一个Class文件，并把它写到本地磁盘上。Javassist也提供了一个方法，用于直接获取被修改的字节码。可以调用**toBytecode()** 方法获取：

    byte[] b = cc.toBytecode();
    
你也可以直接加载**CtClass**:

    Class clazz = cc.toClass();
    
**toClass()** 请求当前线程的上下文类加载器来加载**CtClass**代表的class文件，它返回**java.lang.Class**对象。更多细节见第三章。

### 3. 定义一个新的类

要定义一个新的类，必须使用**ClassPool**对象，调用其**makeClass()** 方法：

    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.makeClass("Point");
    
这段代码定义了一个类名为**Point**的类，它没有任何成员。**Point**的成员方法可以通过声明在**CtNewMethod**中的工厂方法来创建，使用**CtClass**中的**addMethod()** 方法可以实现。

**makeClass()** 不能创建一个新的接口，需要用**makeInterface()**。接口的成员方法是使用**CtNewMethod**的**abstractMethod()** 。注意接口的方法是抽象方法。

### 4. 冻结类

如果一个**CtClass**对象已经转化成了class文件，比如通过**writeFile()** 、**toClass()** 、 **toBytecode()** , Javassist会冻结**CtClass**对象。之后对于**CtClass**对象的修改都是不允许的。这个是为了警告开发者，他们尝试修改的Class文件已经被加载了，JVM不允许再次加载该Class。

冻结的类可以如果想要修改，可以进行解冻，这样就允许修改了，如下：

    CtClasss cc = ...;
        :
    cc.writeFile();  // 会引起类冻结
    cc.defrost();   // 解冻
    cc.setSuperclass(...);    // OK 因为这个类已经被解冻了
    
在**defrost()** 被调用之后，该**CtClass**对象可以再次被修改。

如果**ClassPool.doPruning**被设置为true，当**CtClass**被冻结时，Javassist会修剪它的数据结构。为了减少内存消耗，会删除那个对象中不需要的属性(attribute_info structures)。例如，Code_attribute结构(方法体)会被删除。因此，在**CtClass**对象被修剪之后，方法的字节码是不可访问的，除了方法名称，签名和注释（我也不知道这里的annotations指的是注解还是注释）。被修剪的**CtClass**对象不能再次被解冻（defrost）。**ClassPool.doPruning** 的默认是false.

    CtClasss cc = ...;
    cc.stopPruning(true);
        :
    cc.writeFile();     // 转化为一个Class文件
    // cc 没有被修剪.
    
该**CtClass**对象**cc**没有被修剪。因此它还可以在调用**writeFile()** 之后调用**defrost()** 解冻。

> 在Debug的时候，你可能想暂停修剪和冻结，然后把修改后的class文件写到磁盘上, 可以使用**debugWriteFile()**方法来达到目的。 它会停止修剪，然后写Class文件,并且再次开始修剪（如果一开始就开始修剪的话）。 

### 5. 类路径搜索

**ClassPool.getDefault** 默认会搜索JVM下面相同路径的类，并返回ClassPool。但是，如果一个程序运行在Web应用服务器上，像JBoss和Tomcat那种，**ClassPool**对象可能就找不到用户指定的类了，因为web应用服务使用了多个系统类加载器。这种情况下，需要给**ClassPool**注册一个额外的Class路径。如下：

    pool.insertClassPath(new ClassClassPath(this.getClass()));  // 假设pool是ClassPool的一个实例
    
这句代码注册了一个类的类路径，这个类是**this**指向的那个类。你可以使用任意**Class**代替**this.getClass()**。

你也可以注册一个文件夹作为类路径。例如，下面这段代码增添可以了文件夹**/usr/local/javalib**到搜索路径中：

    ClassPool pool = ClassPool.getDefault();
    pool.insertClassPath("/usr/local/javalib");
   
搜索路径不仅可以是目录，甚至可以是URL：

    ClassPool pool = ClassPool.getDefault();
    ClassPath cp = new URLClassPath("www.javassist.org", 80, "/java/", "org.javassist.");
    pool.insertClassPath(cp);
    
该代码增添了**http://www.javassist.org:80/java/** 到类文件搜索路径下。该URL仅仅搜索**org.javassist.** 包下的class文件。例如，要加载**org.javassist.test.Main** 这个类，javassist会从这个地址下获取该类文件：

    http://www.javassist.org:80/java/org/javassist/test/Main.class
    
此外，你也可以直接给**ClassPool**对象一个byte数组，然后用这个数组构建**CtClass**对象。要这样做，用**ByteArrayClassPath**, 例如：

    ClassPool cp = ClassPool.getDefault();
    byte[] b = a byte array;
    String name = class name;
    cp.insertClassPath(new ByteArrayClassPath(name, b));
    CtClass cc = cp.get(name);
    
获得的**CtClass**对象表示一个由**b**指定的类文件定义的类。如果调用**get()** ，**ClassPool**会从**ByteArrayClassPath**中读取一个Class文件，指定的Class的名字就是上面的**name**变量。

如果你不知道这个类的全限定名，你你可以使用**ClassPool**中的**makeClass()** :

    ClassPool cp = ClassPool.getDefault();
    InputStream ins = an input stream for reading a class file;
    CtClass cc = cp.makeClass(ins);
    
**makeClass()** 返回一个通过输入流构建出来的**CtClass**。你可以使用**makeClass()** 给**ClassPool** 对象提供一个比较急的Class文件。如果搜索路径包含了一个很大的jar包，这可以提高性能。因为**ClassPool**对象会一个一个找，它可能会重复搜索整个jar包中的每一个class文件。**makeClass()** 可以优化这个搜索。**makeClass()**构造出来的类会保存在**ClassPool**对象中，你下次再用的时候，不会再次读Class文件。

## 二、ClassPool详解
### 1. ClassPool简介

**ClassPool**对象是多个**CtClass**对象的容器。一旦**CtClass**对象被创建，它就会永远被记录再**ClassPool**对象中。这是因为编译器之后在编译源码的时候可能需要访问**CtClass**对象。

例如，假定有一个新方法**getter()** 被增添到了表示**Point**类的**CtClass**对象。稍后，程序会试图编译代码，它包含了对**Point**方法的**getter()** 调用，并会使用编译后代码作为一个方法的方法体，它将会被增添到另一个类**Line**中。如果表示**Point**类的**CtClass**对象丢了的话，编译器就不能编译调用**getter()** 的方法了（注意：原始类定义中不包含**getter()** ）。因此，为了正确编译这样一个方法调用，**ClassPool**在程序过程中必须示种包含所有的**CtClass**对象。

    ClassPool classPool = ClassPool.getDefault();
    CtClass point = classPool.makeClass("Point");
    point.addMethod(getterMethod);  // Point增添了getter方法
    CtClass line = ...; // Line方法
    // line 调用point的getter方法
    

### 2. 避免内存溢出

某种特定的**ClassPool**可能造成巨大的内存消耗，导致OOM，比如**CtClass**对象变得非常的（这个发生的很少，因为Javassist已经尝试用不同的方法减少内存消耗了，比如冻结类）。为了避免该问题，你可以从**ClassPool**中移除不需要的**CtClass**对象。只需要调用**CtClass**的**detach()** 方法就行了：

    CtClass cc = ... ;
    cc.writeFile();
    cc.detach();  // 该CtClass已经不需要了，从ClassPool中移除
    
在调用**detach()** 之后，这个**CtClass**对象就不能再调用任何方法了。但是你可以依然可以调用**classPool.get()** 方法来创建一个相同的类。如果你调用**get()** ，**ClassPool**会再次读取class文件，然后创建一个新的**CtClass**对象并返回。

另一种方式是new一个新的**ClassPool**,旧的就不要了。这样旧的**ClassPool**就会被垃圾回收，它的**CtClass**也会被跟着垃圾回收。可以使用以下代码完成：

    ClassPool cp = new ClassPool(true);  // true代表使用默认路径
    // 如果需要的话，可以用appendClassPath()添加一个额外的搜索路径。
    
上面这个**new ClassPool**和**ClassPool.getDefault()** 的效果是一样。注意，**ClassPool.getDefault()** 是一个单例的工厂方法，它只是为了方便用户创建提供的方法。这两种创建方式是一样的，源码也基本是一样的，只不过**ClassPool.getDefault()**是单例的。

注意，**new ClassPool(true)** 是一个很方便的构造函数，它构造了一个**ClassPool**对象，然后给他增添了系统搜索路径。它构造方法的调用就等同于下面的这段代码：

    ClassPool cp = new ClassPool();
    cp.appendSystemPath();  // 你也可以通过appendClassPath()增添其他路径
    
### 3. 级联ClassPool

如果一个程序运行在Web应用服务器上，你可能需要创建多个**ClassPool**实例。为每一个类加载器（ClassLoader）创建一个**ClassPool**（也就是容器）。这时程序在创建**ClassPool**对象的时候就不能再用**getDefault()** 了，而是要用**ClassPool**的构造函数。

多个**ClassPool**对象可以像**java.lang.ClassLoader**那样进行级联。例如：

    ClassPool parent = ClassPool.getDefault();
    ClassPool child = new ClassPool(parent);
    child.insertClassPath("./classes");
    
如果调用了**child.get()** ，child的**ClassPool**首先会代理parent的**ClassPool**，如果parent的**ClassPool**中没有找到要找的类，才会试图到child中的**./classes**目录下找。

如果**child.childFirstLookup**设置为了true，child的**ClassPool**就会首先到自己路径下面找，之后才会到parent的路径下面找。

    ClassPool parent = ClassPool.getDefault();
    ClassPool child = new ClassPool(parent);
    child.appendSystemPath();         // 这默认使用相同的类路径
    child.childFirstLookup = true;    // 改变child的行为。
    
### 4. 更改类名的方式定义新类

一个“新类”可以从一个已经存在的类copy出来。可以使用以下代码：

    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("Point");
    cc.setName("Pair");
    
这段代码首先获取了**Point**的**CtClass**对象。然后调用**setName()** 方法给对象一个新的名字**Pair**。在这个调用之后，**CtClass**表示的类中的所有**Point**都会替换为**Pair**。类定义的其他部分不会变。

既然**setName()** 改变了**ClassPool**对象中的记录。从实现的角度看，**ClassPool**是一个hash表，**setName()** 改变了关联这个**CtClass**对象的**key**值。这个**key**值从原名称**Point**变为了新名称**Pair**。

因此，如果之后调用**get("Point")** ，就不会再返回上面的**cc**引用的对象了。**ClassPool**对象会再次读取class文件，然后构造一个新的**CtClass**对象。这是因为**Point**这个**CtClass**在**ClassPool**中已经不存在了。请看下面代码：

    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("Point");
    CtClass cc1 = pool.get("Point");   // 此时，cc1和cc是完全一样的。
    cc.setName("Pair");
    CtClass cc2 = pool.get("Pair");    // cc2和cc是完全一样的
    CtClass cc3 = pool.get("Point");   // cc3和cc是不一样的，因为cc3是重新读取的class文件
    
**cc1**和**cc2**引用的是相同的实例，和**cc**指向的是同一地址。但是，**cc3**却不是。注意，在执行**cc.setName("Pair")** 之后，**cc**和**cc1**引用的是同一地址，所以它们的**CtClass**都是代表**Pair**类。

**ClassPool**对象用于维护**CtClass**对象和类之间的一一映射关系。Javassist不允许两个不同的**CtClass**对象代表相同的类，除非你用两个**ClassPool**。这个是程序转换一致性的重要特性。

要创建**ClassPool**的副本，可以使用下面的代码片段（这个上面已经提到过了）：

    ClassPool cp = new ClassPool(true);

如果你又两个**ClassPool**对象，那么你就可以从这两个对象中获取到相同class文件但是不同的**CtClass**对象。你可以对那两个**CtClass**进行不同方式的修改，然后生成两个版本的Class。

### 5. 重命名冻结类的方式定义新类

一旦**CtClass**对象转化为Class文件后，比如**writeFile()** 或是 **toBytecode()** 之后，Javassist会拒绝**CtClass**对象进一步的修改。因此，在**CtClass**对象转为文件之后，你将不能再通过**setNme()** 的方式将该类拷贝成一个新的类了。比如，下面的这段错误代码：

    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("Point");
    cc.writeFile();
    cc.setName("Pair");    // 错， 因为cc已经调用了writeFile()
    
为了解除这个限制，你应该调用**ClassPool** 的 **getAndRename()** 方法。 例如：

    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("Point");
    cc.writeFile();
    CtClass cc2 = pool.getAndRename("Point", "Pair"); 
    
如果调用了**getAndRename**，**ClassPool**首先为了创建代表**Pair**的**CtClass**而去读取**Point.class**。然而，它在记录**CtClass**到hash表之前，会把**CtClass**由**Point**重命名为**Pair**。因此**getAndRename()** 可以在**writeFile()** 或 **toBytecode()** 之后执行。

## 三、Class loader详解
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


## 四、内省(introspection)和定制(customization)

### 简介

**CtClass** 提供了自省的方法。**Javassist**的自省能力是能够兼容Java的反射API的。**CtClass**提供了**getName()**，**getSuperclass()**，**getMethods()** 等等方法。它也提供了修改类定义的方法。它允许增添一个新的属性，构造函数以及方法。甚至可以改变方法体。

**CtMethod***对象代表一个方法。**CtMethod**提供了一些修改方法定义的方法。注意，如果一个方法是从父类继承过来的，那么相同的 **CtMethod**对象也会代表父类中声明的方法。**CtMethod**对象会对应到每一个方法定义中。

例如，如果**Point**类声明了**move()** 方法，并且它的子类**ColorPoint** 没有重写**move()** 方法，那么**Point**和**ColorPoint**的**move()** 方法会具有相同的**CtMethod**对象。如果用**CtMethod**修改了方法定义，那么该就该就会在两个类中都生效。如果你只想修改**ColorPoint**中的**move()** 方法，你必须要增添一个**Point.move()** 方法的副本到**ColorPoint**中去。可以使用**CtNewethod.copy()** 来获取**CtMethod**对象的副本。

> Javassist不允许移除方法或者属性，但是允许你重命名它们。所以如果你不再需要方法或属性的时候，你应该将它们重命名或者把它们改成私有的，可以调用**CtMethod**的**setName()** 和 **setModifiers()** 来实现。

Javassist不允许给一个已经存在的方法增添额外的参数。如果你非要这么做，你可以增添一个同名的新方法，然后把这个参数增添到新方法中。例如，如果你想增添一个额外的**int**参数给**newZ** 方法：

    void move(int newX, int newY) { x = newX; y = newY; }
    
假设这个是在**Point**类中的，那么你应该增添以下的代码到**Point**中
    
    void move(int newX, int newY, int newZ) {
        // do what you want with newZ.
        move(newX, newY);
    }

Javassist也提供了一个底层API，用于直接编辑一个原生class文件。例如，**CtClass**中的**getClassFile**就会返回一个**ClassFile**对象，它代表了一个原生Class文件。**CtMethod**中的**getMethodInfo()** 会返回一个**MethodInfo**对象，它代表一个Class文件中的**method_info**结构。底层API使用了JVM的一些特定词汇，用户需要了解class文件和字节码的一些知识。更多详情，可以参考第五章。

只要标识符是$开头的，那么在修改class文件的时候就需要**javassist.runtime**包用于运行时支持。那些特定的标识符会在下面进行说明。要是没有标识符，可以不需要**javassist.runtime**和其他的运行时支持包。更多详细内容，可以参考**javassist.runtime**包的API文档。

### 4.1 在方法的开头和结尾插入代码。

**CtMethod**和**CtConstructor**提供了**insertBefore()**,**insertAfter()**,**addCatch()** 方法。它们被用于在已经存在的方法上面插入代码片段。用户可以把它们的代码以文本的形式写进去。Javassist包含了一个简单的编译器，可以处理这些源码文本。它能够把这些代码编译成字节码，然后将它们内嵌到方法体中。

往指定行插入代码也是有可能的（前提是class文件中包含行号表）。**CtMethod**和**CtConstructor**中的**insertAt()** 就可以将代码插入指定行。它会编译代码文本，然后将其编译好的代码插入指定行。

**insertBefore()**,**insertAfter()**,**addCatch()**,**insertAt()** 这些方法接受一个字符串来表示一个语句(statements)或代码块(block)。一句代码可以是一个控制结构，比如if、while，也可以是一个以分号(;)结尾的表达式。代码块是一组用 **{}** 括起来的语句。因此，下面的每一行代码都是一个合法的语句或代码块。
    
    System.out.println("Hello");
    { System.out.println("Hello"); }
    if (i < 0) { i = -i; }
    
语句和代码块都可以引用属性或方法。如果方法使用 **-g** 选项（class文件中包含局部变量）进行编译，它们也可以引用自己插入方法的参数。否则，它们只能通过特殊的变量 **$0,$1,$2...** 来访问方法参数，下面有说明。虽然在代码块中声明一个新的局部变量是允许的，但是在方法中访问它们确是不允许的。然而，如果使用 **-g** 选项进行编译， 就允许访问。

传递到**insertBefore()**, **insertAfter()** 等方法中的String字符串会被Javassist的编译器编译。因为该编译器支持语言扩展，所以下面的这些以$开头的标识符就具有了特殊意义：


|标识符|含义|英语含义|
|:---|:---:|:---:|
|$0, $1, $2, ...| **this** 和实参|	**this** and actual parameters|
|$args|参数数组。**$args** 的类型是 **Object[]** |	An array of parameters. The type of $args is Object[].|
|$$|所有实参，例如m($$)等同于m($1,$2,...)|	All actual parameters.For example, m($$) is equivalent to m($1,$2,...)|
|$cflow(...)|cflow变量|	cflow variable|
|$r|返回值类型。用于强制类型转换表达式|The result type. It is used in a cast expression.|
|$w|包装类型。用于强制类型转换表达式|The wrapper type. It is used in a cast expression.|
|$_|结果值|The resulting value|
|$sig|**java.lang.Class**对象的数组，表示参数类型|	An array of java.lang.Class objects representing the formal parameter types.|
|$type|**java.lang.Class**对象的数组，表示结果类型|A java.lang.Class object representing the formal result type.|
|$class|**java.lang.Class**对象的数组，表示当前被编辑的类|	A java.lang.Class object representing the class currently edited.|

#### 4.1.1 $0, $1, $2, ...

传递给目标方法的参数可以通过**$1,$2,...** 访问，而不是通过原先的参数名称。**$1** 代表第一个参数，**$2**代表第二个参数，以此类推。那些变量的类型和参数的类型是一样的。**$0**代表**this**，如果是静态方法的话，**$0**不能用。

假定有一个**Point**类如下：

    class Point {
        int x, y;
        void move(int dx, int dy) { x += dx; y += dy; }
    }
    
要想在调用**move()** 时打印**dx**和**dy**的值，可以执行下面代码：

    ClassPool pool = ClassPool.getDefault();
    CtClass cc = pool.get("Point");
    CtMethod m = cc.getDeclaredMethod("move");
    m.insertBefore("{ System.out.println($1); System.out.println($2); }");
    cc.writeFile();
    
注意需要用 **{}** 括起来，如果只有一行语句，可以不用括。

修改后的**Point**类的定义长这个样子：

    class Point {
        int x, y;
        void move(int dx, int dy) {
            { System.out.println(dx); System.out.println(dy); }
            x += dx; y += dy;
        }
    }

**$1** 和 **$2** 分别被**dx**和**dy**给替换了。

**$1, $2, $3** 是可以被更新的，如果它们被赋予了新值，那么它们对应的变量也会被赋予新值。

#### 4.1.2 $args

变量 **$arg** 表示所有参数的一个数组。数组中的类型都是 **Object** 。如果参数是基本数据类型比如**int**，那么该参数就会被转换成包装类型比如**java.lang.Integer**，然后存储到 **$args**中。因此，**$args[0]** 就等于 **$1** ,除非它是个基本类型（int不等于Integer）。注意，**$args[0]** 不等于 **$0** 。 **$0** 是**this**。

如果一个 **Object** 数组赋值给了 **$args** , 那么参数的每一个元素都会一一赋值。如果某个参数是基本类型，那么相应的元素必须是包装类型。该值会从包装类型自动拆箱转换成基本数据类型。

#### 4.1.3 $$

$$ 是一个以逗号分隔参数的缩写。例如，如果**move()** 方法的参数是3个。那么：

    move($$)
    
就等于：

    move($1, $2, $3)
    
如果**move()** 没有接受任何参数，那么**move($$)** 就等于**move()** 。

$$ 也可以跟其他参数一起使用，比如你写这样一个表达式：

    exMove($$, context)
    
这个表达式就等通于下面：

    exMove($1, $2, $3, context)
    
$$ 能够支持泛型表示。一般与**$procced**一起使用，后面会说。

#### 4.1.3 $cflow

**$cflow** 意思就是控制流（control flow）。该只读变量返回特定方法进行递归调用时的深度。

假定**CtMethod**实例**cm**代表下面这个方法：

    int fact(int n) {
        if (n <= 1)
            return n;
        else
            return n * fact(n - 1);
    }
    
要使用**$cflow**，首先要声明**$cflow**要用于监控**fact()** 方法的调用。

    CtMethod cm = ...;
    cm.useCflow("fact");

**useCflow()** 的参数是声明**$cflow**变量的标识符。任何合法的Java名称都能作为标识符。因此标识符也可以包含点(.)。例如，**my.Test.fact**就是一个合法的标识符。

那么，**$cflow(fact)** 表示该方法递归调用时的深度。当该方法在方法内部递归调用时，第一次被调用时**$cflow(fact)** 的值是0而不是1。例如：

    cm.insertBefore("if ($cflow(fact) == 0)"
                  + "    System.out.println(\"fact \" + $1);");
                  
将**fact**加入了显示参数的代码。因为**$cflow(face)** 被检查，所以如果在内部递归调用fact方法，则不会打印参数。

在当前线程的当前最顶层的堆栈帧下，**$cflow**的值是**cm**关联的指定方法的堆栈深度。**$cflow**也能够在其他的方法下面访问。

#### 4.1.4 $r

**$r** 代表方法的返回值类型。他必须用于强制转换表达式中的转换类型。例如，这是它的一个典型用法：

    Object result = ... ;
    $_ = ($r)result;
    
如果返回值类型是一个基本数据类型，那么 **($r)** 就会遵循特殊的语义。首先，如果被转换对象的类型就是基本类型，那么 **($r)** 就会基本类型到基本类型的转换。但是，如果被转换对象的类型是包装类型，那么**$r**就会从包装类型转为基本数据类型。例如，如果返回值类型为**int**，那 **($r)** 就会将其从**java.lang.Integer**转为**int**。

如果返回值类型为**void**，那么 **($r)** 不会进行类型转换。 它什么都不做。然而，如果调用的方法返回值为**void**的话，那么 **($r)** 的结果就是**null**。例如，如果**foo()** 方法的返回值为**void**，那么：

    $_ = ($r)foo();
    
这是一个合法语句。

类型转换符 **($r)** 在**return**语句中也是很有用的。即使返回值类型为**void**，下面的**return**语句也是合法的：

     return ($r)result;
     
这里，**result**是某个本地变量。因为 **($r)** 是**void**的，所以返回值就被丢弃了。**return**语句也被视为没有返回值，就等同于下面：

    return;
    
    
#### 4.1.5 $w

**$w** 表示一个包装类型。它必须用于强制类型转换表达式中。**($w)** 把一个基本数据类型转换为包装类型。例如：

    Integer i = ($w)5;
    
所用的包装类型（Integer）取决于 **($w)** 后面表达式的类型。如果表达式类型为**double**，那么包装类型应为**java.lang.Double**。

如果 **($w)** 后面的表达式不是基本数据类型的话，那么 **($w)** 将不起作用。

#### 4.1.6 $_

**CtMethod**和**CtConstructor**中的**insertAfter()** 在方法最后插入代码时，不只是 **$1,$2..** 这些可以用，你也可用**$_**。

**$_** 表示方法的返回值。而该变量的类型取决于该方法的返回值类型。如果方法的返回值类型为**void**，那么 **$_**的值是**null**，类型为**Object**。

只有方法不报错，运行正常的情况下，**insertAfter()** 中的代码才会运行。如果你想让方法在抛出异常的时候也能执行**insertAfter()** 中的代码，那你就把该方法的第二个参数**asFinally**设置为true.

如果方法中抛出了异常，那么**insertAfter()** 中的代码也会在**finally**语句中执行。这时 **$_** 的值是**0**或**null**。插入的代码执行完毕后，抛出的异常还会抛给原来的调用者。注意，**$_**的值不会抛给原来的调用者，它相当于没用了（抛异常的时候没有返回值）。

#### 4.1.7 $sig

**$sig**是一个**java.lang.Class**对象的数组，数组的内容是按找参数顺序，记录每个参数的类型。

#### 4.1.8 $type

**$type** 是一个**java.lang.Class**对象，它表示返回值类型。如果是构造函数，则它是**Void.class**的引用。

#### 4.1.9 $class

**$class** 值是 **java.lang.Class** 对象，代表修改的方法所对应的那个类。**$class** 是 **$0** 的类型（$0是this）。

#### 4.1.10 addCatch()

**addCatch()** 往方法体插入了的代码片段会在方法抛出异常的时候执行。在源码中，你可以用**$e** 来表示抛出异常是的异常变量。

例如，这段代码：

    CtMethod m = ...;
    CtClass etype = ClassPool.getDefault().get("java.io.IOException");
    m.addCatch("{ System.out.println($e); throw $e; }", etype);
    
把**m**代表的方法编译之后，就成了下面这样：

    try {
        // 原本的代码
    }
    catch (java.io.IOException e) {
        System.out.println(e);
        throw e;
    }

注意，插入的代码以**throw**或**return**语句结尾。

### 4.2 修改方法体

**CtMethod**和**CtConstructor**提供了**setBody()** 方法，该方法用于取代整个方法体。它们会把你提供的源码编译成字节码，然后完全替代之前方法的方法体。如果你传递的源码参数为**null**，那么被替换的方法体只会包含一条**return**语句。

在**setBody()** 方法传递的源码中，以$开头的标识符会有一些特殊含义（这个跟上面是一样的）：

|标识符|含义|英语含义|
|:---|:---:|:---:|
|$0, $1, $2, ...| **this** 和实参|	**this** and actual parameters|
|$args|参数数组。**$args** 的类型是 **Object[]** |	An array of parameters. The type of $args is Object[].|
|$$|所有实参，例如m($$)等同于m($1,$2,...)|	All actual parameters.For example, m($$) is equivalent to m($1,$2,...)|
|$cflow(...)|cflow变量|	cflow variable|
|$r|返回值类型。用于强制类型转换表达式|The result type. It is used in a cast expression.|
|$w|包装类型。用于强制类型转换表达式|The wrapper type. It is used in a cast expression.|
|$sig|**java.lang.Class**对象的数组，表示参数类型|	An array of java.lang.Class objects representing the formal parameter types.|
|$type|**java.lang.Class**对象的数组，表示结果类型|A java.lang.Class object representing the formal result type.|
|$class|**java.lang.Class**对象的数组，表示当前被编辑的类|	A java.lang.Class object representing the class currently edited.|

> 注意，这里不能用 **$_** 。

### 4.2.1 修改现有的表达式

**Javassist**允许只修改方法体中的某一个表达式。**javassist.expr.ExprEditor**类用于替换方法体中的某一个表达式。用户可以定义**ExprEditor**的子类来说明表达式应该如何被修改。

使用**ExprEditor**对象，用户需要调用**CtMethod**或**CtClass**中的**instrument()** 方法，例如：

    CtMethod cm = ... ;
    cm.instrument(
        new ExprEditor() {
            public void edit(MethodCall m)
                          throws CannotCompileException
            {
                if (m.getClassName().equals("Point")
                              && m.getMethodName().equals("move"))
                    m.replace("{ $1 = 0; $_ = $proceed($$); }");
            }
        });
        
该功能为，搜索方法体中，所有对**Point**类的**move()** 方法的调用，都将其替换为如下代码块：

    { $1 = 0; $_ = $proceed($$); }

因此，**move()** 的第一个参数总是**0**。注意，被替换的代码不是表达式，而是一个语句或代码块，并且不能包含try-catch。

**instrument()** 方法会搜索方法体。如果它找到了像是“方法调用、属性访问、对象创建”的表达式，那么它就会调用**ExprEditor**对象的**edit()** 方法。**edit()** 的参数就代表了被找到的那个表达式。**edit()** 方法可以通过该对象来检查和替换表达式。

调用**MethodCall**对象**m**的**replace**方法来将其替换为一个语句或代码块。如果给了的是 **{}**，那么该表达式就会从方法体中移除。如果你想在该表达式的前后加一些逻辑，你可以这样写：

    { before-statements;
      $_ = $proceed($$);
      after-statements; }
      
不管是方法调用，属性访问还是对象创建或者是其他，第二条语句都可以是：

    $_ = $proceed();
    
如果表达式是个读访问，或者：

    $proceed($$);

如果表达式是个写访问。

如果使用 **-g** 选项编译源码，那么在**replace()** 中也是可以直接使用局部变量的（前提是class文件中包含那个局部变量）。

### 4.2.2 javassist.expr.MethodCall

**MethodCall**对象代表一个方法的调用。它的**replace()** 方法会把方法调用替换成另一个语句或代码块。它接受一个源码文本来代表要替换的代码，文本中以$开头的表示符具有特殊的含义，就跟**insertBefore()** 的差不多。

|标识符|含义|英语含义|
|:---|:---:|:---:|
|$0|方法调用的目标对象。<br>它不等于**this**，它是调用方的**this**对象。<br>如果是静态方法，**$0** 是null.|The target object of the method call.<br>This is not equivalent to this, which represents the caller-side this object.<br>$0 is null if the method is static.|
|$1, $2, ...|方法调用的参数|The parameters of the method call.|
|$_|方法调用的返回值|The resulting value of the method call.|
|$r|方法调用的返回值类型|The result type of the method call.|
|$class|**java.lang.Class**对象，表示声明该方法的类|	A java.lang.Class object representing the class declaring the method.|
|$sig|**java.lang.Class**对象的数组，表示参数类型|	An array of java.lang.Class objects representing the formal parameter types.|
|$type|**java.lang.Class**对象的数组，表示结果类型|A java.lang.Class object representing the formal result type.|
|$proceed|表达式中原始方法的名称|The name of the method originally called in the expression.|

上面的“方法调用”的意思就是**MethodCall**代表的那个对象。

其他的标识符，像 **$w**，**$args**，$$，也是可以用的。

除非返回类型是 **void**，否则，代码文本中你必须要给**$_** 赋值，而且类型要对的上。如果返回类型是**void**，那么**$_** 的类型是**Object，你也不用给他赋值。

**$proceed**不是一个 **String**，而是一个特殊的语法。它后面必须跟一个被括号 **()** 包围的参数列表。


#### 4.2.3 javassist.expr.ConstructorCall

**ConstructorCall**对象代表一个构造函数的调用，像**this()** ，并且**super()** 包含在该构造方法体中。**ConstructorCall**的**replace()** 方法可以将一句语句或代码块替换掉原本的构造方法体。它接受源码文本代表要替换的代码，它之中的以$开头的标识符具有一些特殊含义，就行**insertBefore**的那样：

|标识符|含义|英语含义|
|:---|:---:|:---:|
|$0|构造方法调用的目标对象。它就等于**this**|The target object of the constructor call. This is equivalent to this.|
|$1, $2, ...|构造方法调用的参数|The parameters of the constructor call.|
|$class|**java.lang.Class**对象，表示声明该构造函数的类|A java.lang.Class object representing the class declaring the constructor.|
|$sig|**java.lang.Class**对象的数组，表示参数类型|	An array of java.lang.Class objects representing the formal parameter types.|
|$proceed|表达式中原始方法的名称|The name of the method originally called in the expression.|

这里，“构造函数调用”的意思就是**ConstructorCall**对象代表的那个方法。

**$w,$args,$$**等标识符也是可以用的

因为任何构造函数都要调用它的父类构造函数或是自己其他的构造函数，所以被替换的语句要包含一个构造函数的调用，通用使用**$proceed()**.

**$proceed**不是一个**String**，而是一个特殊的语法。它后面必须跟一个被括号 **()** 包围的参数列表。


#### 4.2.4 javassist.expr.FieldAccess

**FieldAccess**对象代表属性访问。如果找到了属性访问，那么**ExprEditor**的**edit()** 就会接收到。**FieldAccesss**的**replace()** 方法接受一个源码文本，用于替换原本属性访问的代码。

在源码文本中，以$的标识符具有一些特殊的含义：

|标识符|含义|英语含义|
|:---|:---:|:---:|
|$0|包含该变量的那个对象。<br>它不等与**this**，**this**是访问该变量的那个方法对应的类对象。<br>如果变量为静态变量，**$0**为null|The object containing the field accessed by the expression. This is not equivalent to this.<br>this represents the object that the method including the expression is invoked on.<br>$0 is null if the field is static.|
|$1|如果表达式是写访问，那么它代表将要被写入的值。<br>否则**$1**不可用|The value that would be stored in the field if the expression is write access. <br>Otherwise, $1 is not available.|
|$_|如果表达式是读访问，它代表读取到的值。<br>否则，存储在**$_的值将丢失。|The resulting value of the field access if the expression is read access. <br>Otherwise, the value stored in $_ is discarded.|
|$r|如果表达式是读访问，它代表变量的类型。<br>否则，**$r**是**void**|The type of the field if the expression is read access. <br>Otherwise, $r is void.|
|$class|**java.lang.Class**对象，表示声明该属性的类|A java.lang.Class object representing the class declaring the field.|
|$type|**java.lang.Class**对象的数组，表示该变量的|	A java.lang.Class object representing the field type.|
|$proceed|表达式中原始方法的名称|The name of a virtual method executing the original field access. .|

**$w,$args,$$**等标识符也是可以用的。

如果表达式是读访问，必须要在源码文本中给**$_**赋值，而且类型要对的上。


#### 4.2.5 javassist.expr.NewExpr

**NewExpr**对象表示使用**new**关键字创建新对象（不包括数组创建）。如果找到了对象时，**ExprEditor**的**edit()** 方法就会被执行。可以使用**NewExpr**的**replace()** 方法来替换原本的代码。

在源代码文本中，以$开头的标识符具有特殊含义：

|标识符|含义|英语含义|
|:---|:---:|:---:|
|$0|null|null|
|$1,$2,...|构造器的参数|The parameters to the constructor.|
|$_|创建对象的结果值。新创建的对象必须存储到这个变量中。|	The resulting value of the object creation. <br>A newly created object must be stored in this variable.|
|$r|创建对象的类型|The type of the created object.|
|$sig|**java.lang.Class**对象的数组，表示参数类型|	An array of java.lang.Class objects representing the formal parameter types.|
|$type|**java.lang.Class**对象，表示创建对象的那个类的类型|java.lang.Class object representing the class of the created object.|
|$proceed|表达式中原始方法的名称|The name of a virtual method executing the original object creation. .|

**$w,$args,$$**等标识符也是可以用的。


#### 4.2.6 javassist.expr.NewArray

**NewArray**代表**new**关键字创建数组。如果找到了数组的创建， **ExprEditor**的**edit()** 方法就会被执行。**NewArray**的**replace()** 方法能够替换创建数组的代码。

在源代码文本里，以$开头的标识符有以下特殊含义：

|标识符|含义|英语含义|
|:---|:---:|:---:|
|$0|null|null|
|$1,$2,...|每个维度的大小|The size of each dimension.|
|$_|数组创建的返回值。新创建的数组必须要存到该变量中|The resulting value of the array creation. <br>A newly created array must be stored in this variable.|
|$r|被创建的数组的类型|The type of the created array.|
|$sig|**java.lang.Class**对象的数组，表示参数类型|	An array of java.lang.Class objects representing the formal parameter types.|
|$type|**java.lang.Class**对象，表示被创建的数组的类|A java.lang.Class object representing the class of the created array.|
|$proceed|表达式中原始方法的名称|	The name of a virtual method executing the original array creation.|

**$w,$args,$$**等标识符也是可以用的。

例如，如果按照下面的方式创建数组：

    String[][] s = new String[3][4];
    
那么**$1**和**$2**的值分别是**3**和**4**，**$3**不可用。

如果数组是按照下面的方式创建的：

    String[][] s = new String[3][];
    
**$1**的值是**3**，**$2**不可用。

#### 4.2.7 javassist.expr.Instanceof

**Instanceof**对象代表了一个**instanceof**语句。如果**instanceof**语句被发现，**ExprEditor**的**edit()** 就会被执行。**Instanceof**的**replace()** 方法会替换它原本的代码。

在源代码文本中，以$开头的标识符具有特殊的含义：

|标识符|含义|英语含义|
|:---|:---:|:---:|
|$0|null|null|
|$1|instanceof操作符左边变量的值|The value on the left hand side of the original instanceof operator.|
|$_|表达式的结果值。**$_** 的类型为**boolean**|The resulting value of the expression. The type of $_ is boolean.|
|$r|instanceof操作符右边的类型|The type on the right hand side of the instanceof operator.|
|$type|**java.lang.Class**对象，表示instanceof操作符右边的类型|A java.lang.Class object representing the type on the right hand side of the instanceof operator.|
|$proceed|表达式中原始方法的名称。<br>它接受一个参数(类型为**java.lang.Object**)。<br>如果类型对的上，返回true，否则为false|The name of a virtual method executing the original instanceof expression. <br>It takes one parameter (the type is java.lang.Object) and returns true <br>if the parameter value is an instance of the type on the right hand side of <br>the original instanceof operator. Otherwise, it returns false.|

**$w,$args,$$**等标识符也是可以用的。

#### 4.2.8 javassist.expr.Cast

**Cast**对象代表一个强制类型转换表达式。如果找到了强制类型转换的表达式，**ExprEditor**的**edit()** 方法将会被执行。**Cast**的**replace()** 方法会替换原来的代码。

在源代码文本里，以$开头的标识符有以下特殊含义：

|标识符|含义|英语含义|
|:---|:---:|:---:|
|$0|null|null|
|$1|被类型转换的那个变量的值|The value the type of which is explicitly cast.|
|$_|表达式结果的值。**$_**的类型是被强制转换后的类型，就是**()** 包起来的那个。|	The resulting value of the expression. The type of $_ is the same as the type <br>after the explicit casting, that is, the type surrounded by **()**.|
|$r|被强制转换后的类型，或者说是被 **()** 包起来的那个类型。|the type after the explicit casting, or the type surrounded by **()** .|
|$type|**java.lang.Class**对象，表示与**$r**相同的那个类型|A java.lang.Class object representing the same type as $r.|
|$proceed|表达式中原始方法的名称。<br>他接受一个**java.lang.Object**类型的参数，并在强制转换成功后返回它。|The name of a virtual method executing the original type casting. <br>It takes one parameter of the type java.lang.Object and returns it after <br>the explicit type casting specified by the original expression.|

**$w,$args,$$** 等标识符也是可以用的。

#### 4.2.9 javassist.expr.Handler

**Handler**对象代表了**try-catch**语句中的**catch**语句。如果找到了**catch**语句，**edit()** 方法就会被执行。**Handler**的**insertBefore()** 可以在**catch**语句的最开始插入代码。

在源代码文本中，以$开头的标识符具有特殊的含义：

|标识符|含义|英语含义|
|:---|:---:|:---:|
|$1|**catch**语句捕获的异常对象|	The exception object caught by the catch clause.|
|$r|捕获异常的异常类型。用于强制类型转换|	the type of the exception caught by the catch clause. It is used in a cast expression.|
|$w|包装类型，用于强制类型转换|The wrapper type. It is used in a cast expression.|
|$type|**java.lang.Class**对象，表示catch捕获的异常对象的类型|A java.lang.Class object representing <br>the type of the exception caught by the catch clause.|

如果给 **$1** 赋了新的异常对象，它会将其作为捕获异常传递给原始的 **catch** 语句。


### 4.3 增加新方法或新属性

#### 4.3.1 增加新方法

Javassist允许用户从零开始创建一个新的方法和构造函数。**CtNewMethod**和**CtNewConstructor**提供了几个工厂方法，它们都是静态方法用于创建**CtMethod**或**CtConstructor**对象。尤其是**make()** 方法，它可以直接传递源代码，用于创建**CtMethod**和**CtConstructor**对象。

例如这个程序：

    CtClass point = ClassPool.getDefault().get("Point");
    CtMethod m = CtNewMethod.make(
                     "public int xmove(int dx) { x += dx; }",
                     point);
    point.addMethod(m);
    
该代码给**Point**类增添了一个**public**方法**xmove()** 。其中**x**是**Point**类原本就有的一个**int**属性。

传递给**make()** 的代码也可以包含以 $ 开头的标识符，就跟**setBody()** 方法是一样的，除了**$_** 之外。如果你还把**make()** 传递了目标对象和目标方法，你也可以使用**$proceed**。 例如：

    CtClass point = ClassPool.getDefault().get("Point");
    CtMethod m = CtNewMethod.make(
                     "public int ymove(int dy) { $proceed(0, dy); }",
                     point, "this", "move");
                     
这个程序创建的**ymove()** 的定义如下：

    public int ymove(int dy) { this.move(0, dy); }

这里面**this.move**替换了**$proceed**。

Javassist还提供了一些其他方法用于创建新方法。你可以先创建一个抽象方法，之后再给它一个方法体:

    CtClass cc = ... ;
    CtMethod m = new CtMethod(CtClass.intType, "move",
                              new CtClass[] { CtClass.intType }, cc);
    cc.addMethod(m);
    m.setBody("{ x += $1; }");
    cc.setModifiers(cc.getModifiers() & ~Modifier.ABSTRACT);
    
你给class增添过抽象方法之后，Javassist就会把这个类变成抽象类，所以在你调用**setBody()** 方法后，需要显式的把该class改变成非抽象类。

#### 4.3.2 相互递归方法

如果一个类没有增添某一个方法，那么Javassist是不允许调用它的。（但是Javassist编译自己调用自己的递归方法）。要给一个类增添相互递归的方法，你需要先增添一个抽象方法。假定你向增添**m()** 和 **n()** 方法到**cc**代表的类中。

    CtClass cc = ... ;
    CtMethod m = CtNewMethod.make("public abstract int m(int i);", cc);
    CtMethod n = CtNewMethod.make("public abstract int n(int i);", cc);
    cc.addMethod(m);
    cc.addMethod(n);
    m.setBody("{ return ($1 <= 0) ? 1 : (n($1 - 1) * $1); }");
    n.setBody("{ return m($1); }");
    cc.setModifiers(cc.getModifiers() & ~Modifier.ABSTRACT);
    
你必须首先把它们弄成两个抽象方法，然后增添到class中。然后你就能给他们增加方法体，方法体中也可以进行互相调用。最后你必须把类改成非抽象类，因为你**addMethod()** 的时候，javassist自动把该类改成了抽象类。

#### 4.3.3 增添属性

Javassist也允许用户创建一个新的属性：

    CtClass point = ClassPool.getDefault().get("Point");
    CtField f = new CtField(CtClass.intType, "z", point);
    point.addField(f);
    
这个程序给**Point**类增添一个名为**z**的属性。

如果增添的属性需要进行值初始化，则上面的程序就要改成这样：

    CtClass point = ClassPool.getDefault().get("Point");
    CtField f = new CtField(CtClass.intType, "z", point);
    point.addField(f, "0");    // 初始化的值是0.
    
Now，**addField()** 方法接受了第二个参数，它代表了计算初始值表达式的源码文本。该源码文本可以是任何Java表达式，前提是表达式的结果类型和属性类型匹配。注意，表达式不以分号(;)结尾，意思就是**0**后面不用跟分号。

除此之外，上面的代码也可用下面这个简单的代码代替：

    CtClass point = ClassPool.getDefault().get("Point");
    CtField f = CtField.make("public int z = 0;", point);
    point.addField(f);
    
#### 4.3.4 删除属性

要删除属性或方法，可以调用**CtClass**中的**removeField()** 或 **removeMethod()**。也可以调用**removeConstructor()** 删除构造函数。


### 4.4 注解

**CtClass, CtMethod, CtField, CtConstructor** 提供了一个很方便的方法**getAnnotations()** 来读取注解。它返回一个注解类型对象。

例如，假定下列注解：
    
    public @interface Author {
        String name();
        int year();
    }
    
这个注解被这样使用：

    @Author(name="Chiba", year=2005)
    public class Point {
        int x, y;
    }
    
那么，这个注解的值可以通过**getAnnotations()** 方法获取。它返回一个包含了注解类型对象的数组：

    CtClass cc = ClassPool.getDefault().get("Point");
    Object[] all = cc.getAnnotations();
    Author a = (Author)all[0];
    String name = a.name();
    int year = a.year();
    System.out.println("name: " + name + ", year: " + year);
    
这段代码的输出是：

    name: Chiba, year: 2005
    
因为**Point**只包含了 **@Author** 一个注解，所以 **all** 数组的长度是1，**all[0]** 是**Author**对象。该注解的属性值可以使用**Author**对象的**name()** 和 **year()** 方法获取。

要使用**getAnnotation()**, 当前class路径下必须要包含注解类型，像**Author**。*它们也必须在ClassPool对象中可访问*。如果注解类型的Class文件没有找到，Javassist就不能获取该注解类型成员的默认值。

### 4.5 运行时类支持

大多数情况下，由Javassist修改的类不需要Javassist去运行。然而，有些Javassist编译器生成的字节码需要运行时类支持，那些都在**javassist.runtime**包中（详细内容请参考该包的API文档）。注意，**javassist.runtime**包只负责管Javassist修改的类的运行时支持。其他Javassist修改后的类不会在运行时使用。


### 4.6 Import

源码中所有的类名都必须是完全限定的（它们必须导入包名）。然而，**java.lang**包时一个特例；例如，**Javassist**编译器可以解析**Object**也可以解析**java.lang.Object**.

为了告知编译器当解析类时搜索其他的包，可以调用**ClassPool**的**importPackage()** 方法。 例如：

    ClassPool pool = ClassPool.getDefault();
    pool.importPackage("java.awt");
    CtClass cc = pool.makeClass("Test");
    CtField f = CtField.make("public Point p;", cc);
    cc.addField(f);
    
第二行告诉编译器要导入**java.awt**包。因此，第三行不会抛出异常。编译器会把**Point**看作**java.awt.Point**.

注意，**importPackage()** 不会影响**ClassPool**的**get()** 方法。只有编译器会任务导入了包。**get()** 方法的参数必须总是全限定名。

### 4.7 局限性

在当前实现中，Javassist的编译器存在几个局限性。这些局限性有：

- J2SE 5.0中提到的语法（包括枚举和泛型）还没有得到支持。注解由Javassist的底层API支持。参见**javassist.bytecode.annotation**包（和**getAnnotations()** 以及**CtBehavior** ）。泛型也只是部分支持。后面的章节有详细介绍。
- 数组初始化的时候，以逗号分割，大括号 **{}** 包围的初始化方式还不支持。除非数组的长度时1.
- 不支持内部类和匿名类。注意，这只是编译器的局限。它不能编译包含在匿名类定义中的源码。Javassist可以读取并修改内部/匿名类的类文件。
- 不支持**continue**和**break**关键字。
- 编译器不能正确的实现Java方法的多态。如果方法在一个类中具有相同的名字，但是却有不同的参数列表，编译器可能会出错。例如：


    class A {} 
    class B extends A {} 
    class C extends B {} 
    
    class X { 
        void foo(A a) { .. } 
        void foo(B b) { .. } 
    }
    
如果被编译的表达式是**x.foo(new C())**, **x**是**X**的一个实例，编译器可能会生成一个对**foo(A)** 的调用，虽然编译器可以正确的编译**foo((B)new C())**.

- 建议用户使用 **#** 作为类名与静态方法或属性之间的分给。例如，在Java中：


    javassist.CtClass.intType.getName()
    
在javassist.CtClass中的静态字段intType指示的对象上调用getName()方法。在Javassist中，用户可以写上面的表达式，但是还是建议按照下面这样写：

    javassist.CtClass#intType.getName()
    
这样，编译器就可以很快的解析这个表达式。


## 五、字节码API

**由于我没有字节码知识基础，所以本章的翻译可能会有很多不准的地方。**    
    

### 简介

Javassist也提供了底层API用于直接编辑class文件。要使用了该API，你需要Java字节码和class文件格式的详细知识，这样你就可以利用API对class文件想怎么改就怎么改。

如果你只想生成一个简单的class文件，你可以使用**javassist.bytecode.ClassFileWriter**。它比**javassist.bytecode.ClassFile**快的多，虽然它的API最小。

### 5.1 获取 ClassFile 对象

一个**javassist.bytecode.ClassFile**对象代表一个Class文件。可以使用**CtClass**中的**getClassFile()** 获取该对象。

除此之外，你也可以用根据一个Class文件直接构造该**javassist.bytecode.ClassFile**对象。例如：

    BufferedInputStream fin
        = new BufferedInputStream(new FileInputStream("Point.class"));
    ClassFile cf = new ClassFile(new DataInputStream(fin));
    
该代码片段创建了一个来自**Point.class**的**ClassFile**对象。

你也可以从零开始创建一个新文件。例如：

    ClassFile cf = new ClassFile(false, "test.Foo", null);
    cf.setInterfaces(new String[] { "java.lang.Cloneable" });
     
    FieldInfo f = new FieldInfo(cf.getConstPool(), "width", "I");
    f.setAccessFlags(AccessFlag.PUBLIC);
    cf.addField(f);
    
    cf.write(new DataOutputStream(new FileOutputStream("Foo.class")));
    
该代码生成了一个class文件**Foo.class**，它包含了以下实现：

    package test;
    class Foo implements Cloneable {
        public int width;
    }
    
### 5.2 增添或删除成员

**ClassFile**提供了**addField()** 和**addMethod()** ，用于增添属性或方法（注意在字节码中，构造函数被视为一个方法）。它也提供了**addAttribute()** 用于增添一个属性到class文件中。

注意，**FiledInfo**, **MethodInfo** 和 **AttributeInfo** 对象包含了对**ConstPool**(常量池表)对象的引用。**ConstPool**对象必须是**ClassFile**对象和被增添到**ClassFile**对象的**FieldInfo**（或**MethodInfo**等）的公共对象。换句话说，一个**FieldInfo**(或**MethodInfo**等)对象不能在不同的**ClassFile**对象之间共享。

要从**ClassFile**对象中移除一个属性或方法，你必须先获取该类所有属性的**java.util.List**，可以使用**getField()** 和**getMethod()** ，它们都返回list。属性和方法都可以使用该**List**对象的**remove()** 方法进行移除。一个属性(Attribute)可以通过相同的方法进行移除。调用**FieldInfo**或**MethodInfo**的**getAttribute()** 来获取属性列表，然后从返回的list中移除它。

### 5.3 遍历方法体

要检查方法体中的每个字节码指令，**CodeIterator**是很有用的。要获取这个对象，可以这样做：

    ClassFile cf = ... ;
    MethodInfo minfo = cf.getMethod("move");    // we assume move is not overloaded.
    CodeAttribute ca = minfo.getCodeAttribute();
    CodeIterator i = ca.iterator();
    
**CodeIterator**对象可以让你从开头到结尾一行一行的访问每一个字节码指令。下面是**CodeIterator**一部分的方法API:

- **void begin()** ： 移动到第一个指令
- **void move(int index)**：移动到指定index位置的指令
- **boolean hasNext()**：如果还有指令，则返回true
- **int next()**：返回下一个指令的index。注意，他不会返回下一个指令的字节码。
- **int byteAt(int index)**: 返回该位置的无符号8bit(unsigned 8bit)值
- **int u16bitAt(int index)**: 返回该位置的无符号16bit（unsigned 16bit）值。
- **int write(byte[] code, int index)**: 在该位置写byte数组。
- **void insert(int index, byte[] code)**，在该位置插入byte数组。分支偏移量等会自动调节。

> 这里我不是很会翻译，可以直接看原版
- void begin()
<br>Move to the first instruction.
- void move(int index)
<br>Move to the instruction specified by the given index.
- boolean hasNext()
<br>Returns true if there is more instructions.
- int next()
<br>Returns the index of the next instruction.
<br>Note that it does not return the opcode of the next instruction.
- int byteAt(int index)
<br>Returns the unsigned 8bit value at the index.
- int u16bitAt(int index)
<br>Returns the unsigned 16bit value at the index.
- int write(byte[] code, int index)
<br>Writes a byte array at the index.
- void insert(int index, byte[] code)
<br>Inserts a byte array at the index. Branch offsets etc. are automatically adjusted.

下面这段代码基本包含了上面所介绍的所有API:

    CodeIterator ci = ... ;
    while (ci.hasNext()) {
        int index = ci.next();
        int op = ci.byteAt(index);
        System.out.println(Mnemonic.OPCODE[op]);
    }
    
### 5.4 生成字节码序列

**Bytecode**对象代表一串字节码指令。它是一个可增长的**bytecode**数组。例如：

    ConstPool cp = ...;    // constant pool table
    Bytecode b = new Bytecode(cp, 1, 0);
    b.addIconst(3);
    b.addReturn(CtClass.intType);
    CodeAttribute ca = b.toCodeAttribute();
    
这将生产代码属性，表示以下字节码序列：

    iconst_3
    ireturn
    
你也可以调用**Bytecode**中的**get()** 方法获取包含该序列的byte数组。获取到的数组可以插入到其他的代码属性中。

**Bytecode**提供了一些方法来增添特定的指令到字节码序列中。它提供了**addOpcode()** 用于增添8bit操作码，也提供了**addIndex()** 方法用于增添一个索引。每个操作码的8bit值都被定义在**Opcode**接口中。

**addOpcode()** 和其他用于增添特殊指令的方法，是自动维护最大堆栈深度，除非控制流不包括分支。可以通过**Bytecode**对象的**getMaxStack()** 值获取。它也会在**Bytecode**对象构造的**CodeAttribute**对象上反应出来。要重新计算方法体的堆栈深度，调用**CodeAttribute**的**computeMaxStack()** 方法。

**Bytecode**可以用于构造方法，例如：

    ClassFile cf = ...
    Bytecode code = new Bytecode(cf.getConstPool());
    code.addAload(0);
    code.addInvokespecial("java/lang/Object", MethodInfo.nameInit, "()V");
    code.addReturn(null);
    code.setMaxLocals(1);
    
    MethodInfo minfo = new MethodInfo(cf.getConstPool(), MethodInfo.nameInit, "()V");
    minfo.setCodeAttribute(code.toCodeAttribute());
    cf.addMethod(minfo);
    
这段代码创建了默认的构造函数，然后将其增添到了**cf**指定的class中。**Bytecode**对象首先被转换成了**CodeAttribute**对象，然后增添到了**minfo**指定的方法中。该方法最终被增添到了**cf**类文件中。

### 5.5 注解（Meta tags）

注解作为运行时不可见（或可见）的注解属性被存储在class文件中。它们的属性可以通过**ClassFile**,**MethodInfo**或**FieldInfo**对象获取，调用那些对象的**getAttribute(AnnotationsAttribute.invisibleTag)** 方法。 更详细的内容参见**javassist.bytecode.AnnotationsAttribute** 和**javassist.bytecode.annotation**包的javadoc手册。

Javassist也让你通过顶层API访问注解。如果你想通过**CtClass**访问注解，可以调用**getAnnotations()** 方法。

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