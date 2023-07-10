package sdk;

import javassist.*;

import java.io.IOException;

public class ClassPatcher {
    public static void changeCode() {

    }
    public static void changeCode1() {
        //jvm --add-opens java.base/java.lang=ALL-UNNAMED
        ClassPool pool = ClassPool.getDefault();
        try {
            pool.appendClassPath("./libs/conflux-web3j.jar");
            CtClass cc = pool.get("conflux.web3j.types.SendTransactionError");
            CtMethod m = cc.getDeclaredMethod("matches", new CtClass[]{pool.get("java.lang.String")});

//            m.insertBefore("java.lang.System.out.println(\"-------SendTransactionError:\"+message);");

            String catchCode = "return false;";
            m.addCatch(catchCode, pool.get("java.lang.NullPointerException"));
            cc.writeFile();
            cc.toClass();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }
}
