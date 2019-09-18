package chapter.two;

import javassist.*;

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

        {

        }
    }

}
