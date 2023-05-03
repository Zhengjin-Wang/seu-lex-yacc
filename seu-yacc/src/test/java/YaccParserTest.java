import core.LR1Builder;
import core.YaccParser;
import dto.LR1;
import dto.ParseResult;
import org.junit.Test;

import java.io.File;

public class YaccParserTest {

    @Test
    public void ParserTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\minic.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
    }

    @Test
    public void UnionTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\minic.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        System.out.println(parseResult.getUnionString());
        System.out.println(parseResult.getSymbolToUnionAttr());
        LR1 lr1 = LR1Builder.buildLR1(parseResult);
        System.out.println(lr1.getUnionString());
        System.out.println(lr1.getSymbolToUnionAttr());

    }


}
