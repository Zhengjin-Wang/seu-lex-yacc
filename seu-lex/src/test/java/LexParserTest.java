import core.LexParser;
import dto.LexAction;
import dto.NFA;
import dto.ParseResult;
import dto.Regex;
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
    public void actionOrderTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\c99.l");
        ParseResult parseResult = LexParser.getParseResult(file);
        Map<String, String> regexAction = parseResult.getRegexAction();
        for(Map.Entry<String, String> entry: regexAction.entrySet()){
            String rawRegex = entry.getKey();
            String action = entry.getValue();

            System.out.println(rawRegex + " :: " + action);
        }
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
