package code.changez.utils;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

public class AnalysisUtils {
    public static Boolean checkClassIsNotExists(Class<?> clazz) {
        String className = clazz.getName();
        String classNamePath = className.replace(".", "/") + ".class";
        URL isExists = clazz.getClassLoader().getResource(classNamePath);
        if (isExists == null) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public static Boolean checkFileContentIsRisk(File dumpPath) {
        List<String> riskKeyword = new ArrayList<String>();
        riskKeyword.add("javax.crypto.");
        riskKeyword.add("ProcessBuilder");
        riskKeyword.add("getRuntime");
        riskKeyword.add("ProcessImpl");
        riskKeyword.add("shell");
        String content = PathUtils.getFileContent(dumpPath);
        for (String keyword : riskKeyword) {
            if (content.contains(keyword)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}
