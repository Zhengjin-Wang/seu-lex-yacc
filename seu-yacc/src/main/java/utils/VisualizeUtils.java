package utils;

import dto.LR1;
import dto.LR1Item;
import dto.LR1ItemCore;
import dto.LR1State;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class VisualizeUtils {

    // 生成graphviz有向图说明文件的字符串
    public static String generateGraphvizString(LR1 lr1){

        StringBuilder mainPart = new StringBuilder();

        LR1State startState = lr1.getStartState();
        Queue<LR1State> queue = new ArrayDeque<>();
        Set<Integer> searchedLR1StateId = new HashSet<>();

        queue.add(startState);
        searchedLR1StateId.add(startState.getStateId());

        while(!queue.isEmpty()){
            LR1State curState = queue.poll();
            Integer stateId = curState.getStateId();

            // 先设置边的信息
            StringBuilder nodeLabel = new StringBuilder();

            nodeLabel.append("S" + stateId +"\\n\\n"); // 写上状态号

            // 将同一core的预测符合并
            Map<LR1ItemCore, List<Integer>> mergedItems = new LinkedHashMap<>();
            for (LR1Item item : curState.getItems()) {
                LR1ItemCore lr1ItemCore = item.getLr1ItemCore();
                if(!mergedItems.containsKey(lr1ItemCore)){
                    mergedItems.put(lr1ItemCore, new ArrayList<>());
                }
                mergedItems.get(lr1ItemCore).add(item.getPredictSymbol());
            }

            // 考虑提取出itemCore，然后把预测符作为一个集合

            for (LR1ItemCore item: mergedItems.keySet()) { // 写入每一项，一项占一行

                List<Integer> production = item.getProductionFromLR1(lr1);
                Integer left = production.get(0);

                String leftSymbolName = lr1.getNumberToSymbol().get(left);
                nodeLabel.append(leftSymbolName);
                nodeLabel.append(" -> ");

                for (int i = 1; i < production.size(); i++) {
                    if(item.getDotPos() == i){ // 这个地方前边要加·
                        nodeLabel.append("·");
                    }
                    Integer symbolId = production.get(i);
                    String symbolName = lr1.getNumberToSymbol().get(symbolId);
                    nodeLabel.append(symbolName);
                    nodeLabel.append(" ");
                }

                if(item.isReducible(lr1)){ // 能规约的话，在最后加一个点
                    nodeLabel.append("· ");
                }

                // 去掉最后一个空格
                nodeLabel.deleteCharAt(nodeLabel.length() - 1);

                nodeLabel.append(", ");

                for (Integer predictSymbol : mergedItems.get(item)) {
                    String predictSymbolName = lr1.getNumberToSymbol().get(predictSymbol);
                    nodeLabel.append(predictSymbolName + "|");
                }

                nodeLabel.deleteCharAt(nodeLabel.length() - 1); // 去掉最后一个|

                nodeLabel.append("\\n");

            }
            
            // 删除最后一个\n
            nodeLabel.deleteCharAt(nodeLabel.length() - 1);
            nodeLabel.deleteCharAt(nodeLabel.length() - 1);
            
            String nodeInfo = stateId + " " + "[label = \"" + nodeLabel.toString() + "\"]\n";
            mainPart.append(nodeInfo);

            // 设置边的信息
            Integer curStateId = curState.getStateId();
            for (Integer symbolId : curState.getEdges().keySet()) {
                String symbolName = lr1.getNumberToSymbol().get(symbolId);
                LR1State nextState = curState.getEdges().get(symbolId);
                Integer nextStateId = nextState.getStateId();

                // 加入一个边信息
                mainPart.append(curStateId + " -> " + nextStateId + " [label = \"" + symbolName + "\"]\n");

                if(!searchedLR1StateId.contains(nextStateId)){
                    searchedLR1StateId.add(nextStateId);
                    queue.add(nextState);
                }
            }

        }



        String rsl = "digraph G{\n"
                + " rankdir=LR\n" // 从左到右排列
                + "node [shape=box]\n" // 设置节点为框格式
                + "node [fontname=\"SimHei\"]\n" // 中文支持，主要为了显示·
                + "edge [fontname=\"SimHei\"]\n" // 中文支持
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
