package webp.plugin

import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.regex.*

class WebpConvertBuildPlugin implements Plugin<Project> {
    WebpInfo config;

    @Override
    void apply(Project project) {
        def hasApp = project.plugins.withType(AppPlugin)

        def variants = hasApp ? project.android.applicationVariants : project.android.libraryVariants

        config = project.extensions.create("webpinfo", WebpInfo);
        println "variants:" + variants
        project.afterEvaluate {

            variants.all { variant ->
                println "variant:" + variant
                println variant.getVariantData().getVariantConfiguration()
                def flavor = variant.getVariantData().getVariantConfiguration().getFlavorName()
                def buildType = variant.getVariantData().getVariantConfiguration().getBuildType().name
                if (config.skipDebug == true && "${buildType}".contains("debug")) {
                    printlog "skipDebug webpConvertPlugin Task!!!!!!"
                    return
                }
                println project.tasks.getNames()
                project.tasks.getNames().each { taskname ->
                    Pattern pattern = Pattern.compile("process([-0-9a-zA-Z]+)${variant.name.capitalize()}Resources");
                    Matcher m = pattern.matcher(taskname);
                    while (m.find()) {
                        def cpu = m.group(1);
                        println "cpu: ${cpu}"
                        println "process${cpu}${variant.name.capitalize()}Resources"
                        def dx = project.tasks.findByName("process${cpu}${variant.name.capitalize()}Resources")
                        println dx
                        def webpConvertPlugin = "webpConvertPlugin${cpu}${variant.name.capitalize()}"
                        project.task(webpConvertPlugin) << {
                            String resPath = "${project.buildDir}/intermediates/${flavor}/res/merged/${buildType}"
                            println "resPath:" + resPath
                            def dir = new File("${resPath}")
                            dir.eachDirMatch(~/drawable[a-z0-9-]*/) { drawDir ->
                                println "drawableDir:" + drawDir
                                def file = new File("${drawDir}")
                                file.eachFile { filename ->
                                    def name = filename.name
                                    def f = new File("${project.projectDir}/webp_white_list.txt")
                                    if (!f.exists()) {
                                        f.createNewFile()
                                    }
                                    def isInWhiteList = false
                                    f.eachLine { whiteName ->
                                        if (name.equals(whiteName)) {
                                            isInWhiteList = true
                                        }
                                    }
                                    if (!isInWhiteList) {
                                        if (name.endsWith(".jpg") || name.endsWith(".png")) {
                                            if (!name.contains(".9")) {

                                                def picName = name.split('\\.')[0]
                                                def suffix = name.split('\\.')[1]
                                                printlog "find target pic >>>>>>>>>>>>>" + name
                                                printlog "picName:" + picName

                                                def cmd = "cwebp -q 75 -m 6 ${filename} -o ${drawDir}/${picName}.webp"
                                                def p = cmd.execute()
                                                try {
                                                    def lines = p.getInputStream().readLines()
                                                    lines.each { line ->
                                                        printlog line
                                                    }
                                                    lines = p.getErrorStream().readLines()
                                                    lines.each { line ->
                                                        printlog line
                                                    }
                                                } catch (Exception e) {
                                                    println "del ${cmd} failed"
                                                    e.printStackTrace()
                                                }

                                                try {
                                                    //printlog filename
                                                    def del_res = filename.delete();
                                                    printlog "${del_res} del ${filename}"
                                                } catch (Exception e) {
                                                    println "cmd  ${filename} failed"
                                                    e.printStackTrace()
                                                }
                                                println "del ${filename}"
                                                //"del ${filename}".execute()
                                                printlog "delete:" + "${filename}"
                                                printlog "generate:" + "${drawDir}/${picName}.webp"

                                            }
                                        }
                                    }

                                }
                            }
                        }

                        project.tasks.findByName(webpConvertPlugin).dependsOn dx.taskDependencies.getDependencies(dx)
                        dx.dependsOn project.tasks.findByName(webpConvertPlugin)
                    }
                }

            }
        }

    }

    void printlog(String msg) {
        if (config.isShowLog == true) {
            println msg
        }
    }
}


class WebpInfo {
    boolean skipDebug
    boolean isShowLog
}
