import core.LR1Builder;
import core.YaccParser;
import dto.LR1;
import dto.ParseResult;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LR1Test {

    @Test
    public void calculateFirstSetTest(){
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
        List<Integer> testSeq = new ArrayList<>();
        testSeq.add(-4);
        testSeq.add(-4);
        // testSeq.add(102);
        System.out.println(lr1.calculateFirstSet(testSeq));
    }

    @Test
    // 在VisualizeUtilsTest中测试过了
    public void innerExpandTest(){

    }

}
