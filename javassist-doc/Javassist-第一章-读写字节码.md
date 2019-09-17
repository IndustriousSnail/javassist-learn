##### 这是把官方的文档给翻译了，顺便学习，一共10章，可以到下面地址查看

    https://github.com/IndustriousSnail/javassist-learn
    

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