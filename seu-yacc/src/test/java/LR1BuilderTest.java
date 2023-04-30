import core.LR1Builder;
import core.YaccParser;
import dto.LR1;
import dto.LR1Item;
import dto.LR1State;
import dto.ParseResult;
import org.junit.Test;
import utils.VisualizeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LR1BuilderTest {

    public static final String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";

    @Test
    public void encodeProductionTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\minic.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = new LR1();
        LR1Builder.assignID(lr1, parseResult);
        LR1Builder.encodeProduction(lr1, parseResult);

        lr1.getSymbolToNumber().forEach((k, v)->{
            System.out.println(k + ":" + v);
        });
        System.out.println("--------------------");
        lr1.getNonTerminalToProductionIds().forEach(
                (k, v)->{
                    System.out.println(k);
                    System.out.println(v);
                }
        );
        System.out.println(lr1.getProductionIdToProduction());
    }

    @Test
    public void getFirstSetTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\minic.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = new LR1();
        LR1Builder.assignID(lr1, parseResult);
        LR1Builder.encodeProduction(lr1, parseResult);
        LR1Builder.calculateFirstSet(lr1);
        System.out.println(lr1.getNonTerminalFirstSet());
    }

    @Test
    public void calculateNonTerminalHasEpsilonTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        System.out.println(parseResult.getTerminals());
        System.out.println(parseResult.getNonTerminals());
        LR1 lr1 = new LR1();
        LR1Builder.assignID(lr1, parseResult);
        LR1Builder.encodeProduction(lr1, parseResult);
        LR1Builder.calculateNonTerminalHasEpsilon(lr1);
        lr1.getSymbolToNumber().forEach((k, v)->{
            if(!(v>=0 && v < 128)) {
                System.out.println(k + ":" + v);
            }
        });
        System.out.println("--------------------");
        lr1.getNonTerminalToProductionIds().forEach(
                (k, v)->{
                    System.out.println(k);
                    System.out.println(lr1.getNumberToSymbol().get(k));
                    System.out.println(v);
                }
        );
        System.out.println(lr1.getProductionIdToProduction());
        System.out.println(lr1.getNonTerminalHasEpsilon());
    }

    @Test
    public void calculateFirstSetTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\minic.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        System.out.println(parseResult.getTerminals());
        System.out.println(parseResult.getNonTerminals());
        LR1 lr1 = new LR1();
        LR1Builder.assignID(lr1, parseResult);
        LR1Builder.encodeProduction(lr1, parseResult);
        LR1Builder.calculateFirstSet(lr1);
        lr1.getSymbolToNumber().forEach((k, v)->{
            if(!(v>=0 && v < 128)) {
                System.out.println(k + ":" + v);
            }
        });
        System.out.println("--------------------");
        lr1.getNonTerminalToProductionIds().forEach(
                (k, v)->{
                    System.out.println(k + "\t" + lr1.getNumberToSymbol().get(k));
                    System.out.println(v);
                }
        );
        System.out.println(lr1.getProductionIdToProduction());
        lr1.getNonTerminalFirstSet().forEach( (k, v)->{
            System.out.println(k + ":" + v);
        });
    }

    @Test
    // 只测试了node的生成，还没测试edge的生成，需要完成outerExpand函数
    public void buildLR1Test(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\minic.y");
        ParseResult parseResult = YaccParser.getParseResult(file);

        LR1 lr1 = LR1Builder.buildLR1(parseResult);
//        for (LR1State lr1State : lr1.getStateToStateId().keySet()) {
//            System.out.println(lr1State.getStateId());
//            for (LR1Item item : lr1State.getItems()) {
//                System.out.println(lr1.getProductionIdToProduction().get(item.getProductionId()));
//                System.out.println(item);
//            }
//            System.out.println("--------------------------------------");
//        }
        System.out.println(lr1.getStateToStateId().size());
//        VisualizeUtils.visualizeLR1(lr1, workDir);
    }

    @Test
    // 只测试了node的生成，还没测试edge的生成，需要完成outerExpand函数，已完成
    public void generateTransGraphTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test2.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = new LR1();
        LR1Builder.assignID(lr1, parseResult);
        LR1Builder.encodeProduction(lr1, parseResult);
        LR1Builder.calculateFirstSet(lr1);
        LR1Builder.generateLR1Dfa(lr1);
//        for (LR1State lr1State : lr1.getStateToStateId().keySet()) {
//            System.out.println(lr1State.getStateId() + " : " + lr1State.getItems());
//        }
        lr1.getTransGraph().forEach((k,v)->{
            System.out.println(k);
            System.out.println(lr1.getStateIdToState().get(k).getLr1StateCore());
            StringBuilder sb = new StringBuilder();
            for (Integer symbol : v.keySet()) {
                String symbolName = lr1.getNumberToSymbol().get(symbol);
                sb.append("-" + symbolName + "-> " + v.get(symbol) + ", ");
            }
            System.out.println(sb);
        });
        // VisualizeUtils.visualizeLR1(lr1, workDir);
    }

    @Test
    // 只测试了node的生成，还没测试edge的生成，需要完成outerExpand函数，已完成
    public void buildLALRFromLR1Test(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test2.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = new LR1();
        LR1Builder.assignID(lr1, parseResult);
        LR1Builder.encodeProduction(lr1, parseResult);
        LR1Builder.calculateFirstSet(lr1);
        LR1Builder.generateLR1Dfa(lr1);
        LR1Builder.buildLALRFromLR1(lr1);
//        for (LR1State lr1State : lr1.getStateToStateId().keySet()) {
//            System.out.println(lr1State.getStateId() + " : " + lr1State.getItems());
//        }
        System.out.println("LALR graph size: " + lr1.getLalrTransGraph().size());
        lr1.getLalrTransGraph().forEach((k,v)->{
            System.out.println(k);
            System.out.println(lr1.getStateIdToState().get(k).getLr1StateCore());
            StringBuilder sb = new StringBuilder();
            for (Integer symbol : v.keySet()) {
                String symbolName = lr1.getNumberToSymbol().get(symbol);
                sb.append("-" + symbolName + "-> " + v.get(symbol) + ", ");
            }
            System.out.println(sb);
        });
        VisualizeUtils.visualizeLR1(lr1, workDir);
    }

}
