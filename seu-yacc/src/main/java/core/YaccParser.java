package core;

import constant.Associativity;
import dto.ParseResult;
import utils.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                        parseResult.getTerminals().add(token);
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

                    do {
                        ++i;
                        line = lines[i].trim();
                        if(line.length() > 0) break;
                    }while (true);

                    // System.out.println(line);

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

    }

    public static void setProductionPriority(ParseResult parseResult){
        for (String leftPart : parseResult.getProductions().keySet()) {

            List<List<String>> rightPartSet = parseResult.getProductions().get(leftPart);
            for (List<String> rightPart : rightPartSet) { // 遍历不同的右部
                List<String> production = new ArrayList();
                production.add(leftPart);
                Integer priority = null; // 默认优先级为null

                for (int i = 0; i < rightPart.size(); i++) {
                    String symbol = rightPart.get(i);
                    if(symbol.equals("%prec")){
                        String prioritySymbol = rightPart.get(i+1);
                        priority = parseResult.getSymbolPriority().get(prioritySymbol);
                        if(priority == null) throw new RuntimeException("Priority symbol not found");
                        break;
                    }
                    if (parseResult.getSymbolPriority().containsKey(symbol)){
                        priority = parseResult.getSymbolPriority().get(symbol); // 直接将最后一个出现的具有优先级的终结符当作产生式的运算符，不知道对不对
                    }
                    production.add(symbol);
                }

                // 加入list和优先级列表中
                parseResult.getProductionList().add(production);
                parseResult.getProductionPriority().put(production, priority);

            }
        }

        for (String leftPart : parseResult.getExtendedProductions().keySet()) {

            List<List<String>> rightPartSet = parseResult.getExtendedProductions().get(leftPart);
            for (List<String> rightPart : rightPartSet) { // 遍历不同的右部
                List<String> production = new ArrayList();
                production.add(leftPart);
                Integer priority = null;

                for (int i = 0; i < rightPart.size(); i++) {
                    String symbol = rightPart.get(i);
                    if(symbol.equals("%prec")){
                        String prioritySymbol = rightPart.get(i+1);
                        priority = parseResult.getSymbolPriority().get(prioritySymbol);
                        if(priority == null) throw new RuntimeException("Priority symbol not found");
                        break;
                    }
                    if (parseResult.getSymbolPriority().containsKey(symbol)){
                        priority = parseResult.getSymbolPriority().get(symbol); // 直接将最后一个出现的具有优先级的终结符当作产生式的运算符，不知道对不对
                    }
                    production.add(symbol);
                }

                // 加入list和优先级列表中
                parseResult.getExtendedProductionList().add(production);
                parseResult.getExtendedProductionPriority().put(production, priority);

            }
        }
//        for (List<String> strings : parseResult.getExtendedProductionList()) {
//            System.out.println(strings);
//        }


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

        boolean isProduction = false;
        boolean isRightPart = false;
        boolean isAction = false;
        StringBuilder stringBuilder = new StringBuilder();
        String leftPart = new String();

        String symbol = new String();
        List<List<String>> productions = new ArrayList<>();
        List<String> production = new ArrayList<>();

        List<List<String>> extendedProductions = new ArrayList<>();
        List<String> extendedProduction = new ArrayList<>();

        for (int i = 0; i < grammar.length(); ++i) {

            char c = grammar.charAt(i);

            if(c == '/' && !isAction && i > 0 && grammar.charAt(i-1) != '\''){ // 不在''中的/一律视作注释
                while(grammar.charAt(i) != '\n') ++i;
                continue;
            }

            if(!isProduction && StringUtils.isEmptyChar(c)){ // 不是产生式部分，遇到空字符跳过
                continue;
            }

            isProduction = true;

            if(!isRightPart) {
                if (!StringUtils.isEmptyChar(c) && c != ':'){ // 处于左部，不是空字符也不是:，是左部内容
                    stringBuilder.append(c);
                }
                else {
                    if (leftPart.length() == 0){ // 只给leftPart赋一次值
                        leftPart = stringBuilder.toString();
                        stringBuilder = new StringBuilder();
                    }
                    if(c == ':'){
                        isRightPart = true;
                        // 找到第一个非空白符
                        ++i;
                        while(StringUtils.isEmptyChar(grammar.charAt(i))) ++i;
                        --i;
                        continue;
                    }
                }
            }
            else{ // 正在右部

                if(!isAction){ // 不是{}中的内容
                    if(c == '\''){ // 单引号说明是一个ascii字符，放入production即可，对其他操作屏蔽
                        if(symbol.length() != 0){ // 刷新一下symbol
                            symbol = new String();
                        }
                        stringBuilder.append(c);
                        ++i;
                        stringBuilder.append(grammar.charAt(i));
                        ++i;
                        stringBuilder.append(grammar.charAt(i));
                    }
                    else if(!StringUtils.isEmptyChar(c) && c != '|' && c != '{' && c != ';'){ // 是一个symbol
                        if(symbol.length() != 0){ // 刷新一下symbol
                            symbol = new String();
                        }
                        stringBuilder.append(c);
//                        System.out.println((int) c);
//                        System.out.println(stringBuilder.toString());
                    }
                    else{
                        if (symbol.length() == 0){ // 第一次遇到空白符，会将StringBuilder内容刷入symbol，放进production，一直维持直到遇到下一个非空白符，代表要读入token
                            symbol = stringBuilder.toString(); // 如果c是一个;且之前没有symbol，那这就是一个空字符，代表epsilon
//                            System.out.println(symbol);
                            production.add(symbol);
                            extendedProduction.add(symbol);
                            stringBuilder = new StringBuilder();
                        }

                        if(c == '{'){ // 进入action状态
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(c);
                            isAction = true;
                        }
                        else if(c == '|') { // 结束了一个产生式

                            // 检查production中是否出现了%prec，若出现应该重定义优先级

                            productions.add(production);
                            extendedProductions.add(extendedProduction);
                            symbol = new String();
                            stringBuilder = new StringBuilder();
                            production = new ArrayList<>();
                            extendedProduction = new ArrayList<>();
                            ++i;
                            while(StringUtils.isEmptyChar(grammar.charAt(i))) ++i;
                            --i;
                        }
                        else if(c == ';'){ // 整个产生式集合的分析结束了
                            if (production.get(0).length() == 0){ // 此时这是一个空产生式
                                production = new ArrayList<>();
                                extendedProduction = new ArrayList<>();
                            }
                            productions.add(production); // 得加入最后一个产生式
                            extendedProductions.add(extendedProduction);

                            parseResult.getNonTerminals().add(leftPart);
                            parseResult.getProductions().put(leftPart, productions);
                            parseResult.getExtendedProductions().put(leftPart, extendedProductions);

                            leftPart = new String();
                            symbol = new String();
                            stringBuilder = new StringBuilder();
                            production = new ArrayList<>();
                            extendedProduction = new ArrayList<>();
                            productions = new ArrayList<>();
                            extendedProductions = new ArrayList<>();

                            isProduction = false;
                            isRightPart = false;
                        }

                    }
                }
                else{ // {}中的内容
                    stringBuilder.append(c);
                    if(c == '}'){ // 这就要求{}里边的内容不能含有}，如printf("}")，一般也不应该出现
                        extendedProduction.add(stringBuilder.toString()); // 这里加的是具体动作代码，实际可以转换为哑元M->ε，规约时执行动作
                        stringBuilder = new StringBuilder();
                        isAction = false;
                    }
                }

            }


        }

//        System.out.println(parseResult.getNonTerminals());
////        System.out.println(parseResult.getProductions());
////        System.out.println(parseResult.getExtendedProductions());
//        for (String key : parseResult.getProductions().keySet()) {
//            System.out.println("左部: " + key);
//            for (List<String> strings : parseResult.getProductions().get(key)) {
//                System.out.println(strings);
//            }
//        }

        setProductionPriority(parseResult);

//        parseResult.getProductionPriority().forEach((k,v)->{
//            System.out.println(k + ", 优先级:" + v);
//        });

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
