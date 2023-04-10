import core.LexParser;
import dto.ParseResult;

import java.io.File;

public class LexMain {
    public static void main(String[] args) {
        System.out.println("--------seu-lex test--------");
        if(args.length == 0){
            System.out.println("Missing argument .l file");
        }
        else if(args.length != 1){
            System.out.println("Too many arguments found");
        }
        else{
            File file = new File(args[0]);
            if(!file.canRead() || !file.isFile()){
                System.out.println("Not a valid file");
                return;
            }
            System.out.println("Running...");
            ParseResult parseResult = LexParser.getParseResult(file);
        }
    }
}
