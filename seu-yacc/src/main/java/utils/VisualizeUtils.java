package utils;

import dto.LR1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class VisualizeUtils {

    // 生成graphviz有向图说明文件的字符串
    private static String generateGraphvizString(LR1 lr1){
        StringBuilder mainPart = new StringBuilder();

        // 设置字体
        mainPart.append("edge [fontname=\"SimHei\"]\n");



        String rsl = "digraph G{\n"
                + " rankdir=LR\n"
                + mainPart.toString()
                + "}";
        return rsl;
    }

    // 生成graphviz有向图说明文件
    private static File generateGraphvizFile(LR1 lr1, String workDirPath, String outputFileName){

        String dotFileName = outputFileName + ".dot";
        String content = generateGraphvizString(lr1);

        File file = new File(workDirPath, dotFileName); // 文件名和路径
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    // 工作目录应当使用绝对路径
    public static void visualizeLR1(LR1 lr1, String workDirPath, String outputFileName){

        System.setProperty("user.dir", workDirPath);
        File dotFile = generateGraphvizFile(lr1, workDirPath, outputFileName);

        try {
            // 执行cmd命令
            File pngFile = new File(workDirPath, outputFileName + ".png");
            String dotFileName = "\"" + dotFile.getAbsolutePath() + "\"";
            String pngFileName = "\"" + pngFile.getAbsolutePath() + "\"";

            String cmd = String.format("dot -Tpng %s -Gdpi=150 -o %s", dotFileName, pngFileName);
            Process process = Runtime.getRuntime().exec(cmd);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void visualizeLR1(LR1 lr1, String workDirPath){
        visualizeLR1(lr1, workDirPath, "LR1_graph");
    }

    public static void visualizeLR1(LR1 lr1) { visualizeLR1(lr1, "./");}

}
