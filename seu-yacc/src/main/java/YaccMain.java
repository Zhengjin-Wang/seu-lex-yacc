import core.YaccParser;
import dto.ParseResult;

import java.io.File;

public class YaccMain {
    public static void main(String[] args) {

        System.out.println("--------seu-yacc--------");
        if(args.length == 0){
            System.out.println("Missing argument .y file");
        }
        else {
            File file = new File(args[0]);
            if (!file.canRead() || !file.isFile()) {
                System.out.println("Not a valid file");
                return;
            }
            System.out.println("Running...");
            ParseResult parseResult = YaccParser.getParseResult(file);
        }

    }
}
