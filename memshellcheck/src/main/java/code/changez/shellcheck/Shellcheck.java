package code.changez.shellcheck;
import code.changez.utils.PathUtils;
import code.changez.utils.ProcessUtils;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import code.changez.common.AnsiLog;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.*;

public class Shellcheck {
    public static File agent_work_directory = new File(PathUtils.getCurrentDirectory(), ".copagent");

    public static void main(String[] args) throws Exception {

        String attach_jar_path = null;
        String current_jar_path = PathUtils.getCurrentJarPath();

        // add "-Xbootclasspath/a" command line start memshellcheck.jar
        boolean is_boot_start = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-Xbootclasspath/a");

        if(is_boot_start){
            attach_jar_path = args[0];

            attach(attach_jar_path);

            AnsiLog.info("Result store in : {}", new File(agent_work_directory, "result.txt"));
            System.exit(0);
        }else {
            Options options = new Options();
            options.addOption("h", "help", false, "print options information");
            options.addOption("a", "agent", true, "path of agent.jar");
            // options.addOption("f", "filter", true, "class name regex to filter");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmdLine = parser.parse(options, args);

            if (cmdLine.hasOption("help")) {
                new HelpFormatter().printHelp("java -jar memshellcheck.jar", options, true);
                System.exit(0);
            }

            if (cmdLine.hasOption("agent")) {
                attach_jar_path = cmdLine.getOptionValue("agent");
            }


            AnsiLog.info("========= attach tomcat start =========");
            /*
             * java <opts> -jar memcheck.jar </path/to/agent.jar>
             * */
            List<String> opts = new ArrayList<String>();
            opts.add("-jar");
            opts.add(current_jar_path);
            opts.add(attach_jar_path);

            // real start memshellcheck.jar process
            ProcessUtils.startProcess(opts);
            // attach(attach_jar_path);
        }
        System.exit(0);
    }

    public static void attach(String agent_jar_path) throws  Exception{
        VirtualMachine virtualMachine = null;
        // 冒号： 遍历list，取每一个元素
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()){
            // 这里遍历找的是tomcat的java进程，减少遍历的量 正常来说weblogic等其他中间件也可以
            if (descriptor.displayName().contains("catalina") || descriptor.displayName().equals("")) {
                try {
                    AnsiLog.info("Try to attach process " + descriptor + ", please wait a moment ...");
                    virtualMachine = VirtualMachine.attach(descriptor);
                    Properties targetSystemProperties = virtualMachine.getSystemProperties();
                    if (descriptor.displayName().equals("") && !targetSystemProperties.containsKey("catalina.home")){
                        continue;
                    }
                    //将当前tomcat pid 传到agent，作为检测结果的文件名，用来区分多个tomcat进程
                    String currentJvmName = "tomcat_" + descriptor.id();
                    Thread.sleep(1000);
//                    javaInfoWarning()
                    virtualMachine.loadAgent(agent_jar_path, currentJvmName);
                    AnsiLog.info("Attach process {} finished .", descriptor);
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    // detach
                    if(null != virtualMachine) {
                        virtualMachine.detach();
                        AnsiLog.info("mission complete,detach process {} success .", descriptor);
                    }

                }
            }
        }
    }
}
