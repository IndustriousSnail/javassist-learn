[toc]

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

