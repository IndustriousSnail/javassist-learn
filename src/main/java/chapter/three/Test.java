package chapter.three;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

class Hello {
    public void say() {
        System.out.println("Hello");
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get("chapter.three.Hello");
            CtMethod m = cc.getDeclaredMethod("say");
            m.insertBefore("{ System.out.println(\"Hello.say():\"); }");
            Class c = cc.toClass();
            Hello h = (Hello)c.newInstance();
            h.say();
        }

        {
            Hello orig = new Hello();
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get("chapter.three.Hello");
            try {
                Class c = cc.toClass();
                Hello h = (Hello)c.newInstance();
                h.say();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}