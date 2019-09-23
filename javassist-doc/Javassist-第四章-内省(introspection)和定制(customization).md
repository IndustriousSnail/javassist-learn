##### 这是把官方的文档给翻译了，顺便学习，一共10章，可以到下面地址查看，里面可能有翻译不准的地方，欢迎指正

    https://github.com/IndustriousSnail/javassist-learn
    

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