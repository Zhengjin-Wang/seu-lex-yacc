package utils;

import dto.NFA;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VisualizeUtils {

    // 生成graphviz有向图说明文件的字符串
    public static String generateGraphvizString(NFA nfa){
        StringBuilder mainPart = new StringBuilder();

        for (Integer endState : nfa.getEndStates()) {
            String fillColor = String.format("%d [style=filled color=green]\n", endState);
            mainPart.append(fillColor);
        }

        Map<Integer, Map<Character, List<Integer>>> transGraph = nfa.getTransGraph();
        for(Integer begin: transGraph.keySet()){
            Map<Character, List<Integer>> outEdges = transGraph.get(begin);
            for(Character c: outEdges.keySet()){
                List<Integer> ends = outEdges.get(c);
                for(Integer end:ends){
                    String edge = String.format("%d -> %d[label=\" %s\"]\n", begin, end, c.toString());
                    mainPart.append(edge);
                }
            }
        }

        String rsl = "digraph G{\n"
                + mainPart.toString()
                + "}";
        return rsl;
    }

    // 生成graphviz有向图说明文件
    public static File generateGraphvizFile(NFA nfa, String workDirPath){

        String dotFileName = "NFA_graph.dot";
        String content = generateGraphvizString(nfa);

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
    public static void visualizeNFA(NFA nfa, String workDirPath){

        System.setProperty("user.dir", workDirPath);
        File dotFile = generateGraphvizFile(nfa, workDirPath);

        try {
            // 执行cmd命令
            File pngFile = new File(workDirPath, "NFA_graph.png");
            String dotFileName = "\"" + dotFile.getAbsolutePath() + "\"";
            String pngFileName = "\"" + pngFile.getAbsolutePath() + "\"";

            String cmd = String.format("dot -Tpng %s -o %s", dotFileName, pngFileName);
            Process process = Runtime.getRuntime().exec(cmd);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
