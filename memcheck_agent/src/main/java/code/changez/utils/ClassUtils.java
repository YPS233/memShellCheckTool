package code.changez.utils;

import code.changez.common.ClassDumpTransformer;
import sun.instrument.InstrumentationImpl;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 检查是否实现高风险类接口
public class ClassUtils {
    public static Boolean lsReleaseRiskInterfaces(Class<?> clazz) {
        // 高风险的接口
        List<String> riskInterface = new ArrayList<String>();
        // filter型
        riskInterface.add("javax.servlet.Filter");
        // servlet型
        riskInterface.add("javax.servlet.Servlet");
        // listener型
        riskInterface.add("javax.servlet.ServletRequestListener");

        try {
            // 获取传入类的interface
            List<String> clazzInterfaces = new ArrayList<String>();
            for (Class<?> cls : clazz.getInterfaces()) {
                clazzInterfaces.add(cls.getName());
            }
            // 两个list有交集 返回true
            clazzInterfaces.retainAll(riskInterface);
            if (clazzInterfaces.size()>0) {
                return Boolean.TRUE;
            }

        } catch (Throwable ignored) {}

        return Boolean.FALSE;
    }


    // 检查父类是否属于高风险
    public static Boolean lsHasRiskSuperClass(Class<?> clazz) {
        //高风险的父类
        List<String> riskSuperClassesName = new ArrayList<String>();
        riskSuperClassesName.add("javax.servlet.http.HttpServlet");
        try {
            if ((clazz.getSuperclass() != null && riskSuperClassesName.contains(clazz.getSuperclass().getName()))){
                return Boolean.TRUE;
            }
        }catch (Throwable ignored) {}
        return Boolean.FALSE;
    }


    public static Boolean lsUseAnnotations(Class<?> clazz) {
        // 针对spring注册路由的一些注解
        List<String> riskAnnotations = new ArrayList<String>();
        riskAnnotations.add("org.springframework.stereotype.Controller");
        riskAnnotations.add("org.springframework.web.bind.annotation.RestController");
        riskAnnotations.add("org.springframework.web.bind.annotation.RequestMapping");
        riskAnnotations.add("org.springframework.web.bind.annotation.GetMapping");
        riskAnnotations.add("org.springframework.web.bind.annotation.PostMapping");
        riskAnnotations.add("org.springframework.web.bind.annotation.PatchMapping");
        riskAnnotations.add("org.springframework.web.bind.annotation.PutMapping");
        riskAnnotations.add("org.springframework.web.bind.annotation.Mapping");

        try {
            Annotation[] da = clazz.getDeclaredAnnotations();
            if (da.length > 0) {
                for (Annotation _da : da) {
                    // 比较当前类的注解 和黑名单 交集返回true
                    for (String _annotation : riskAnnotations) {
                        if (_da.annotationType().getName().equals(_annotation))
                            return Boolean.TRUE;
                    }
                }
            }
        }catch (Throwable ignored) {}
        return Boolean.FALSE;
    }

    // 高风险的类名
    public static Boolean lsRiskClassName(Class<?> clazz) {
        List<String> riskClassName = new ArrayList<String>();
        riskClassName.add("org.springframework.web.servlet.handler.AbstractHandlerMapping");
        try {
            if (riskClassName.contains(clazz.getName())) {
                return Boolean.TRUE;
            }
        }catch (Throwable ignored) {}
        return Boolean.FALSE;
    }

    // 高风险的包名
    public static Boolean lsContainRiskPackage(Class<?> clazz) {
        List<String> riskPackage = new ArrayList<String>();
        riskPackage.add("net.rebeyond.");
        riskPackage.add("com.metasploit");
        try {
            for(String packageName : riskPackage) {
                if (clazz.getName().startsWith(packageName)) {
                    return Boolean.TRUE;
                }
            }

        }catch (Throwable ignored) {}
        return Boolean.FALSE;
    }

    public static Class<?> dumpClass(Instrumentation inst, String classPattern, boolean isRegEx, String code) {
        Set<Class<?>> allClasses = SearchUtils.searchClassOnly(inst, classPattern, isRegEx, code);
        if(allClasses.iterator().hasNext()) {
            Class<?> c = allClasses.iterator().next();
            ClassDumpTransformer transformer = new ClassDumpTransformer(allClasses);
            InstrumentationUtils.retransformClasses(inst, transformer, allClasses);
            Map<Class<?>, File> classFiles = transformer.getDumpResult();
            transformer.dumpJavaIfNecessary(c, classFiles);
            return c;
        }else {
            LogUtils.logit(classPattern + " NoSuchElementException");
        }
        return null;
    }
}





