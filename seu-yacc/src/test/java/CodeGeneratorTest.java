import core.CodeGenerator;
import core.LR1Builder;
import core.YaccParser;
import dto.LR1;
import dto.ParseResult;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class CodeGeneratorTest {

    @Test
    public void generateYTabHTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test3.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = LR1Builder.buildLR1(parseResult);
//        LR1 lalr = LR1Builder.buildLALRFromLR1(lr1);

        System.out.println(CodeGenerator.generateYTabH(lr1, parseResult));

    }

    @Test
    public void generateYTabCTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test3.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = LR1Builder.buildLR1(parseResult);
//        LR1 lalr = LR1Builder.buildLALRFromLR1(lr1);

//        System.out.println(CodeGenerator.generateYTabC(parseResult, lr1, lr1.getTransGraph()));

    }

    @Test
    public void generateTableTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test3.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = LR1Builder.buildLR1(parseResult);
//        LR1 lalr = LR1Builder.buildLALRFromLR1(lr1);

       // System.out.println(CodeGenerator.generateTable(lr1, lr1.getTransGraph()));

    }

    @Test
    public void generateNodeTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test3.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = LR1Builder.buildLR1(parseResult);
//        LR1 lalr = LR1Builder.buildLALRFromLR1(lr1);

        System.out.println(CodeGenerator.generateNode(lr1));
        for (List<Integer> list : lr1.getProductionIdToProduction().values()) {
            System.out.println(list);
        }
    }

    @Test
    public void generateActionSwitchTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test3.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = LR1Builder.buildLR1(parseResult);
//        LR1 lalr = LR1Builder.buildLALRFromLR1(lr1);

        System.out.println(CodeGenerator.generateActionSwitch(lr1));

    }

}
