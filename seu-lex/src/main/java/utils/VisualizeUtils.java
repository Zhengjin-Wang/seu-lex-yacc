package utils;

import constant.SpAlpha;
import dto.FA;
import dto.NFA;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class VisualizeUtils {

    // 生成graphviz有向图说明文件的字符串
    private static String generateGraphvizString(FA fa){
        StringBuilder mainPart = new StringBuilder();

        // 设置字体
        mainPart.append("edge [fontname=\"SimHei\"]");

        // 设置终态颜色
        for (Integer endState : fa.getEndStates()) {
            String fillColor = String.format("%d [style=filled color=green]\n", endState);
            mainPart.append(fillColor);
        }

        if(fa instanceof NFA) {
            // endState的rank
            mainPart.append("{ rank=same;");
            for (Integer state : fa.getEndStates()) {
                mainPart.append(" " + state.toString());
            }
            mainPart.append(" }\n");
        }

        // 添加startState的rank
        String beginRank = String.format("{ rank=same; %d }\n", fa.getStartState());
        mainPart.append(beginRank);

//        Set<Integer> rankedStates = new HashSet<>();

        Map<Integer, Map<Character, List<Integer>>> transGraph = fa.getTransGraph();
        for(Integer begin: transGraph.keySet()){
            Map<Character, List<Integer>> outEdges = transGraph.get(begin);
//            List<Integer> curRankedState = new ArrayList<>();

            for(Character c: outEdges.keySet()){
                List<Integer> ends = outEdges.get(c);
                String displayStr = c.toString();
                if (c == ' ') displayStr = "[ws]";
                else if (c == '\t') displayStr = "[\\\\t]";
                else if (c == '\n') displayStr = "[\\\\n]";
                else if (c == '\r') displayStr = "[\\\\r]";
                else if (c == SpAlpha.ANY) displayStr = "OTHER"; // 这个时候ANY边是在其他显式边走不通（当前读入字符不在显式边字符里）的情况下走的，可以理解为OTHER，保证无论读入什么字符，这个状态都能转移出去
                for(Integer end:ends){
                    String edge = String.format("%d -> %d[label=\"%s\"]\n", begin, end, displayStr);
                    mainPart.append(edge);
//                    if(!rankedStates.contains(end)){
//                        rankedStates.add(end);
//                        curRankedState.add(end);
//                    }
                }
            }

//            // 添加rank的地方
//            if(curRankedState.size() != 0){
//                mainPart.append("{ rank=same;");
//                for (Integer state : curRankedState) {
//                    mainPart.append(" "+state.toString());
//                }
//                mainPart.append(" }\n");
//            }
        }

        String rsl = "digraph G{\n"
                + " rankdir=LR\n"
                + mainPart.toString()
                + "}";
        return rsl;
    }

    // 生成graphviz有向图说明文件
    private static File generateGraphvizFile(FA fa, String workDirPath, String outputFileName){

        String dotFileName = outputFileName + ".dot";
        String content = generateGraphvizString(fa);

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
    public static void visualizeFA(FA fa, String workDirPath, String outputFileName){

        System.setProperty("user.dir", workDirPath);
        File dotFile = generateGraphvizFile(fa, workDirPath, outputFileName);

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

    public static void visualizeFA(FA fa, String workDirPath){
        visualizeFA(fa, workDirPath, "FA_graph");
    }

    public static void visualizeFA(FA fa) {visualizeFA(fa, "./");}

}
