package chapter.one;

import javassist.*;

import java.io.IOException;

public class Test1 {

    public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {
        {
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.get("test.Rectangle");
            cc.setSuperclass(pool.get("test.Point"));  // 修改test.Rectangle的父类为test.Point
            cc.writeFile();

            byte[] b = cc.toBytecode();  // 获取字节码
            Class clazz = cc.toClass();  // 获取Class对象
        }

        {
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.makeClass("Point");  // 创建一个新的类，名为Point
        }

        {
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(new ClassClassPath(new Test1().getClass()));
        }

        try{

        }catch (Throwable e) {

        }

    }
}
