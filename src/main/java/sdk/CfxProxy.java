package sdk;

import conflux.web3j.jsonrpc.Cfx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class CfxProxy implements InvocationHandler {
    static Logger log = LoggerFactory.getLogger("");
    private final Cfx cfx;

    public CfxProxy(Cfx cfx) {
        this.cfx = cfx;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoke {}", method.getName());
        switch (method.getName()) {
            case "getNonce":
                args[0] = "cfx:aathrdjwhfsjzt88577vz42r4hkh41vmt6y2dtbuzj";
                break;
        }
        return method.invoke(cfx, args);
    }
}
