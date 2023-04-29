import core.LR1Builder;
import core.YaccParser;
import dto.LR1;
import dto.LR1State;
import dto.ParseResult;
import org.junit.Test;
import utils.VisualizeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VisualizeUtilsTest {

    public static final String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";

    @Test
    public void generateGraphvizStringTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test.y");
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


        LR1Builder.generateLR1Dfa(lr1);
        String s = VisualizeUtils.generateGraphvizString(lr1);
        System.out.println(s);

    }

    @Test
    // 只测试了node的生成，还没测试edge的生成，需要完成outerExpand函数
    public void visualizeLR1Test(){
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
        VisualizeUtils.visualizeLR1(lr1, workDir);
    }

}
