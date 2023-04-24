package core;

import constant.Associativity;
import dto.ParseResult;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class YaccParser {

    /**
     * 分离第一部分的预置C代码和定义部分，并解析定义部分，设置到parseResult中
     * @param definitionPart
     * @param parseResult
     */
    public static void detachAndParseDefinitionPart(String definitionPart, ParseResult parseResult){

        String[] split = definitionPart.split("%}");
        if (split.length != 2){
            throw new RuntimeException("Invalid .y file format");
        }
        //拷贝第一部分的直接代码部分
        String[] part1Split = split[0].split("%\\{");
        parseResult.setPreCopy(part1Split[1]);

        String definition = split[1];
        // 后续进行解析并对parseResult进行设置

        // 如果没有%start，将grammar部分出现的第一个非终结符作为start

        String[] lines = definition.split("[\n\r]");
        Map<String,String> aliasToCType = new HashMap<>();
        int priority = 0;

        for (int i = 0; i < lines.length; ++i) {

            String line = lines[i];
            line = line.trim();
            if(line.length() == 0 || line.charAt(0) != '%'){  // 只解析开头为%的行
                continue;
            }

            String[] lineSplit = line.split("[\t ]");
            String type = lineSplit[0];  // 识别定义类型

            if(type.equals("%token")){
                for (int j = 1; j < lineSplit.length; ++j){
                    String token = lineSplit[j].trim();
                    if(token.length() != 0){
                        if(token.charAt(0) == '/') break; // 开始注释
                        parseResult.getTokens().add(token);
                    }
                }
            }
            else if(type.equals("%union")){ // %union之后必有{
                String[] splitArray = line.split("\\{");
                if(splitArray.length > 1 && splitArray[1].trim().length() > 0){
                    line = splitArray[1].trim();
                }
                else{
                    ++i;
                    line = lines[i].trim();
                }
                do{

                    String[] pair = line.split("[\t ]");

                    String cType = pair[0];

                    String endPart = pair[pair.length - 1];
                    String alias = endPart.split(";")[0];

                    aliasToCType.put(alias, cType); // 加入映射

                    // 判断行尾}
                    if(endPart.split(";").length > 1 && endPart.split(";")[1].contains("}")){
                        break;
                    }

                    ++i;
                    line = lines[i].trim();
                    if(line.trim().charAt(0) == '}') break;

                }while (true);
            }
            else if(type.equals("%type")){ // 应该保证%union出现在%type之前，且%type中的type都在%union中出现过
                String cType = null;
                for (int j = 1; j < lineSplit.length; ++j){
                    String token = lineSplit[j].trim();
                    if(token.length() != 0){
                        if(token.charAt(0) == '/') break; // 开始注释
                        if (token.charAt(0) == '<'){
                            String alias = token.substring(1, token.length() - 1);
                            cType = aliasToCType.get(alias);
                        }
                        else {
                            parseResult.getSymbolToCType().put(token, cType);
                        }
                    }
                }
            }
            else if(type.equals("%start")){
                parseResult.setStartSymbol(lineSplit[lineSplit.length - 1]);
            }
            else if(type.equals("%left")){
                for (int j = 1; j < lineSplit.length; ++j){
                    String token = lineSplit[j].trim();
                    if(token.length() != 0){ // 是一个有效token
                        if(token.charAt(0) == '/') break; // 开始注释
                        parseResult.getSymbolPriority().put(token, priority);
                        parseResult.getSymbolAssociativity().put(token, Associativity.LEFT);
                    }
                }
                ++priority;
            }
            else if(type.equals("%right")){
                for (int j = 1; j < lineSplit.length; ++j){
                    String token = lineSplit[j].trim();
                    if(token.length() != 0){ // 是一个有效token
                        if(token.charAt(0) == '/') break; // 开始注释
                        parseResult.getSymbolPriority().put(token, priority);
                        parseResult.getSymbolAssociativity().put(token, Associativity.RIGHT);
                    }
                }
                ++priority;
            }
            else if(type.equals("%nonassoc")){
                for (int j = 1; j < lineSplit.length; ++j){
                    String token = lineSplit[j].trim();
                    if(token.length() != 0){ // 是一个有效token
                        if(token.charAt(0) == '/') break; // 开始注释
                        parseResult.getSymbolPriority().put(token, priority);
                        parseResult.getSymbolAssociativity().put(token, Associativity.NONASSOC);
                    }
                }
                ++priority;
            }

        }
        // System.out.println(aliasToCType);
        // System.out.println(parseResult.getTokens());
        // System.out.println(parseResult.getSymbolToCType());
        // System.out.println(parseResult.getStartSymbol());
        // System.out.println(parseResult.getSymbolPriority());
        // System.out.println(parseResult.getSymbolAssociativity());

//        System.out.println(parseResult.getPreCopy());
//        System.out.println(parseResult.getUserCopy());
    }

    /**
     * 解析规则部分的文法定义，将结果设置到parseResult中
     * @param grammar
     * @param parseResult
     */
    public static void parseGrammarPart(String grammar, ParseResult parseResult){
        // 出现的终结符必须在%token中定义，否则视为非终结符
        // 所有非终结符和终结符都是字母构成的字符串，且用空格分割
        // 当出现|后为空，说明这是一个epsilon产生式
        // 所有非终结符必须出现在产生式左部，否则异常
    }


    public static ParseResult getParseResult(String rawStr) throws RuntimeException{

        ParseResult parseResult = new ParseResult();

        // 进行分割
        String[] split = rawStr.split("%%");
        if (split.length != 3){
            throw new RuntimeException("Invalid .y file format");
        }

        //第三部分是用户定义，直接拷贝
        parseResult.setUserCopy(split[2]); // 如果定义部分或者规则部分或者用户部分出现%%怎么办呢? 比如字符串里包含%% （待解决）

        detachAndParseDefinitionPart(split[0], parseResult);

        parseGrammarPart(split[1], parseResult);


        return parseResult;
    }

    public static ParseResult getParseResult(File file) throws RuntimeException{

        //读取文件内容
        String rawStr = null;
        try {
            FileReader fr = new FileReader(file);
            char[] buffer = new char[(int) file.length()];
            int charsRead = fr.read(buffer);
            rawStr = new String(buffer, 0, charsRead);
        }catch (Exception e){
            e.printStackTrace();
        }

        ParseResult parseResult = YaccParser.getParseResult(rawStr);


        return parseResult;
    }

}
