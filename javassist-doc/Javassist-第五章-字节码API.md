##### 这是把官方的文档给翻译了，顺便学习，一共10章，可以到下面地址查看，里面可能有翻译不准的地方，欢迎指正

    https://github.com/IndustriousSnail/javassist-learn
    
    

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