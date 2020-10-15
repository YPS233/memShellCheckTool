package code.changez.shellcheck;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import code.changez.common.AnsiLog;
import code.changez.utils.ClassUtils;
import code.changez.utils.AnalysisUtils;
import code.changez.utils.LogUtils;
import code.changez.utils.PathUtils;

public class Agent {
    public static void agentmain(String agentArgs, Instrumentation instrument) {
        Class[] classes = instrument.getAllLoadedClasses();
        List<Class<?>> resultClasses = new ArrayList<Class<?>>();

        AnsiLog.info("==========Agent.jar is success attached " + agentArgs + "==========");
        AnsiLog.info("Found All Loaded Classes    : " + classes.length);
        List<Class<?>> suspiciousClassList = findAllSuspiciousClass(instrument, classes);
        int order = 1;
        String results = "All Suspicious Class    : " + resultClasses.size() + "\n\n";
        String high_level = "============================================================\nhigh risk level Class   : \n";
        String Absolutely_level = "============================================================\nAbsolutely risk level Class : \n";
        String normal_level = "============================================================\nnormal risk level Class : \n";
        for(Class cls : suspiciousClassList) {
//            AnsiLog.info("risk class name : {}", cls.getName());
            File dumpJavaFile = PathUtils.getStorePath(cls, false);
            String level = getClassRiskLevel(cls,dumpJavaFile);
            String tmp = "";
            try{
                tmp = String.format("order       : %d\nname        : %s\nrisk level  : %s\nlocation    : %s\nhashcode    : %s\nclassloader : %s\nextends     : %s\n\n", order, cls.getName(), level, dumpJavaFile.getAbsolutePath(), Integer.toHexString(cls.hashCode()), cls.getClassLoader().getClass().getName(), cls.getClassLoader());
            }catch (NullPointerException e){
                tmp = String.format("order       : %d\nname        : %s\nrisk level  : %s\nlocation    : %s\nhashcode    : %s\nclassloader : %s\nextends     : [NullPointerException]\n\n", order, cls.getName(), level, dumpJavaFile.getAbsolutePath(), Integer.toHexString(cls.hashCode()), cls.getClassLoader().getClass().getName());
            }
            if(level.equals("high")){
                high_level += tmp;
            }else if(level.equals("Absolutely")){
                Absolutely_level += tmp;
            }else {
                normal_level += tmp;
            }
            order += 1;
        }
        LogUtils.logit(results + Absolutely_level + high_level + normal_level);
        LogUtils.result(results + Absolutely_level + high_level + normal_level);
        AnsiLog.info(results + Absolutely_level + high_level + normal_level);

    }

    public static List<Class<?>> findAllSuspiciousClass (Instrumentation ins, Class<?>[] loadedClasses) {
        List<Class<?>> suspiciousClassList = new ArrayList<Class<?>>();
        List<String> loadedClassesNames = new ArrayList<String>();

        //获取所有风险类
        for(Class<?> clazz : loadedClasses) {
            loadedClassesNames.add(clazz.getName());
            //递归 检查class的父类 空或者java.lang.Object退出
            while (clazz != null && !clazz.getName().equals("java.lang.Object")) {
                if (ClassUtils.lsContainRiskPackage(clazz) ||
                        ClassUtils.lsUseAnnotations(clazz) ||
                        ClassUtils.lsHasRiskSuperClass(clazz) ||
                        ClassUtils.lsRiskClassName(clazz) ||
                        ClassUtils.lsReleaseRiskInterfaces(clazz)) {
                    if (loadedClassesNames.contains(clazz.getName())) {
                        suspiciousClassList.add(clazz);
                        ClassUtils.dumpClass(ins, clazz.getName(), false,
                                Integer.toHexString(clazz.getClassLoader().hashCode()));
                        break;
                    }
                    AnsiLog.info("cannot find " + clazz.getName() + " classes in instrumentation");
                }
                clazz = clazz.getSuperclass();
            }
        }
    return suspiciousClassList;
    }


    public static String getClassRiskLevel(Class<?> clazz, File dumpPath) {
        String riskLevel = "low";
        // 检测Class Loader目录下是否存在class文件
        if (AnalysisUtils.checkClassIsNotExists(clazz)) {
            riskLevel = "high";
        }
        // 反编译 检测java文件是否包含执行命令的危险函数
        if (AnalysisUtils.checkFileContentIsRisk(dumpPath)) {
            riskLevel = "Absolutely";
        }
        return riskLevel;
    }
}
