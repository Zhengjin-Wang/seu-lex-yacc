import core.LexParser;
import dto.ParseResult;
import org.junit.Test;

import java.io.File;
import java.util.Map;

public class LexParserTest {

    @Test
    public void AliasExtractTest(){
//        ParseResult rsl = new ParseResult();
//        String text = "%{ int c = 1; \n " +
//                "%}\n"+
//                "a   \t [1-2] \n" +
//                " b    [1-2] \n";
//        Map map = LexParser.detachDefinationPart(text, rsl);
//        map.forEach((k,v) ->{
//            System.out.println("*" + k +"*" + " " + "*" +v + "*");
//        });
//        System.out.println(rsl.getPreCopy());
    }

    @Test
    public void ParserTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test.l");
        ParseResult parseResult = LexParser.getParseResult(file);
        System.out.println(parseResult);
    }

    @Test
    public void rawRegexParseTest(){
//        String t = "\"[\\\"\"[a] print(1);\n" +
//                "[x] pint";
//        Map m = LexParser.getRawRegexAction(t);
//        m.forEach((k,v)->{
//            System.out.println("k:" + k + "\t" + "v:" + v);
//        });
    }

}
