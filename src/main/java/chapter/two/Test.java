package chapter.two;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class Test {

    public static void main(String[] args) throws NotFoundException {
        {
            ClassPool cp = new ClassPool(true);
        }

        {
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.get("Point");
            cc.setName("Pair");
        }
    }

}
