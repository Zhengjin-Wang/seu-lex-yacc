import core.CodeGenerator;
import core.LR1Builder;
import core.TableGenerator;
import core.YaccParser;
import dto.LR1;
import dto.ParseResult;
import org.junit.Test;
import utils.VisualizeUtils;

import java.io.File;
import java.util.List;

public class TableGeneratorTest {

    public static final String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";

    @Test
    public void initializerTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test2.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
//        LR1 lr1 = LR1Builder.buildLR1(parseResult);
//        TableGenerator tableGenerator = new TableGenerator(lr1, lr1.getTransGraph());
         LR1 lr1 = LR1Builder.buildLALR(parseResult);
        TableGenerator tableGenerator = new TableGenerator(lr1, lr1.getLalrTransGraph());


        int row = 0;

        int col = tableGenerator.getActionTable()[0].length;
        System.out.print("table\t\t");
        for (int i = 0; i < col; i++) {
            if(lr1.getOccurredSymbols().contains(i)){
                System.out.print(lr1.getNumberToSymbol().get(i) + "\t");
            }
        }
        col = tableGenerator.getNonTerminalCount();
        for (int i = 0; i < col; i++) {
            Integer symbolId = TableGenerator.convertGotoTableColumnIndexToSymbolId(i);
            System.out.print(lr1.getNumberToSymbol().get(symbolId) + "\t");
        }
        System.out.println();

        for (int[] ints : tableGenerator.getActionTable()) {

            System.out.print("row:" + row+ "\t\t");
            int j = 0;
            for (int anInt : ints) {

                if(lr1.getOccurredSymbols().contains(j)) {
                    System.out.print(anInt + "\t");
                }
                ++j;
            }
            for (int nextState : tableGenerator.getGotoTable()[row]) {
                System.out.print(nextState + "\t");
            }
            System.out.println();
            ++row;
        }
    }

    @Test
    public void initializerLALRTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test2.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
//        LR1 lr1 = LR1Builder.buildLR1(parseResult);
//        TableGenerator tableGenerator = new TableGenerator(lr1, lr1.getTransGraph());
        LR1 lr1 = LR1Builder.buildLALR(parseResult);
        TableGenerator tableGenerator = new TableGenerator(lr1, lr1.getLalrTransGraph());

        System.out.println("productions:");
        for (Integer pid : lr1.getProductionIdToProduction().keySet()) {
            System.out.print(pid + "\t");
            List<Integer> production = lr1.getProductionIdToProduction().get(pid);
            int left = production.get(0);
            System.out.print(lr1.getNumberToSymbol().get(left) + " -> ");
            for (int i = 1; i < production.size(); i++) {
                int symbol = production.get(i);
                System.out.print(lr1.getNumberToSymbol().get(symbol) + " ");
            }
            System.out.println();

        }


        int row = 0;

        int col = tableGenerator.getActionTable()[0].length;
        System.out.print("table\t\t");
        for (int i = 0; i < col; i++) {
            if(lr1.getOccurredSymbols().contains(i)){
                System.out.print(lr1.getNumberToSymbol().get(i) + "\t");
            }
        }
        col = tableGenerator.getNonTerminalCount();
        for (int i = 0; i < col; i++) {
            Integer symbolId = TableGenerator.convertGotoTableColumnIndexToSymbolId(i);
            System.out.print(lr1.getNumberToSymbol().get(symbolId) + "\t");
        }
        System.out.println();

        for (int[] ints : tableGenerator.getActionTable()) {
            int state = tableGenerator.getTableRowIndexToStateId().get(row);
            System.out.print("sta:" + state+ "\t\t");
            int j = 0;
            for (int anInt : ints) {

                if(lr1.getOccurredSymbols().contains(j)) {
                    if(anInt > 0){
                        anInt = tableGenerator.getTableRowIndexToStateId().get(anInt);
                    }
                    System.out.print(anInt + "\t");
                }
                ++j;
            }
            for (int nextState : tableGenerator.getGotoTable()[row]) {
                if(nextState > 0){
                    nextState = tableGenerator.getTableRowIndexToStateId().get(nextState);
                }
                System.out.print(nextState + "\t");
            }
            System.out.println();
            ++row;
        }

        VisualizeUtils.visualizeLR1(lr1, workDir);


    }

}
