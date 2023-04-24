package core;

import dto.ParseResult;
import utils.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * @author Lilac
 */
public class LexParser {

    /**
     * 返回regex alias的map，并将预定义部分拷贝到parseResult
     * @param definitionPart
     * @param parseResult
     * @return
     * @throws RuntimeException
     */
    private static Map<String, String> detachDefinitionPart(String definitionPart, ParseResult parseResult) throws RuntimeException{
        //默认 %{ %} 在文件开始位置，也就是在定义规则之前，而且一定存在
        String[] split = definitionPart.split("%}");
        if (split.length != 2){
            throw new RuntimeException("Invalid .l file format");
        }

        String regexAlias = split[1]; // 要保证每行一个别名及正则表达式匹配只占一行

        //拷贝第一部分的直接代码部分
        String[] part1Split = split[0].split("%\\{");
        parseResult.setPreCopy(part1Split[1]);

        Map<String, String> aliasMap = new LinkedHashMap<>();
        String[] regexAliases = regexAlias.split("\n");
        for(String s:regexAliases){
            s = s.strip();
            String alias = "";
            String regex = "";
            for(int i = 0; i < s.length(); ++i){
                if(s.charAt(i) == ' ' || s.charAt(i) == '\t'){
                    alias = s.substring(0, i);
                    alias = alias.strip();
                    regex = s.substring(i);
                    regex = regex.strip();
                    break;
                }
            }
            if (alias.equals("")) continue;
            aliasMap.put(alias, regex);
        }

        return aliasMap;
    }

    /**
     * 得到原始的regex-action映射（未替换alias）
     * @param rulePart
     * @return
     */
    private static Map<String, String> getRawRegexAction(String rulePart){
        Map<String, String> rawRegexAction = new LinkedHashMap<>();

        int status = 0; // 0表示不在regex-action解析，1表示在regex解析，3表示在action解析（单行），4表示在action解析（大括号)
        /*  0阶段读入非空白符进入1阶段
            1阶段在下边五个变量都是0才能进入下个阶段
            此时遇到\n直接进入0阶段
            遇到空白符进入2阶段
            2阶段遇到第一个非空白符进入3或4阶段
            遇到除{以外的符号进入3阶段，再遇到\n进入0阶段，清空stringbuilder
            遇到{进入4阶段，再遇到}进入0阶段，清空stringbuilder

         */

        //引号里的括号还有转义中的括号要看作无效啊
        int parenthesis = 0; // 遇左括号加1，遇右括号减一
        int bracket = 0;
        int curBracket = 0;
        boolean quote = false; // 遇引号改变状态
        boolean slash = false; // 正在转义
        //上边四个变量只有1阶段有效
        //只有三个括号计数都是0并且quote是false且转义是false，遇到空格或\t才改变状态
        StringBuilder regex = new StringBuilder();
        StringBuilder action = new StringBuilder();
        for(int i = 0; i < rulePart.length(); ++i){
            char c = rulePart.charAt(i);
            if(slash){
                if(status == 1) {
                    regex.append(c);
                }
                else{
                    action.append(c);
                }
                slash = false;
                continue;
            }
            if(quote){
                if(status == 1) {
                    regex.append(c);
                }
                else{
                    action.append(c);
                }
                if(c == '\\'){
                    slash = true;
                    continue;
                }
                if(c == '"'){
                    quote = false;
                }
                continue;
            }
            switch (status){
                case 0:
                    if(!StringUtils.isBlankChar(c)){
                        --i; // 回退，以便写入stringbuilder
                        status = 1;
                    }
                    break;
                case 1:
                    //先slash再quote，转义的优先级最高

                    if(parenthesis == 0 && bracket == 0 && curBracket == 0 && quote == false && slash == false){ //这时后边跟\n不跟action? 不考虑了，按空白符处理

                        if(StringUtils.isBlankChar(c)){
                            --i;
                            status = 2;
                            break;
                        }

                    }

                    if(c == '('){
                        ++parenthesis;
                    }
                    else if(c == ')'){
                        --parenthesis;
                        if(parenthesis < 0){
                            throw new RuntimeException("Invalid parenthesis input");
                        }
                    }
                    else if(c == '['){
                        ++bracket;
                    }
                    else if(c == ']'){
                        --bracket;
                        if(bracket < 0){
                            throw new RuntimeException("Invalid bracket input");
                        }
                    }
                    else if(c == '{'){
                        ++curBracket;
                    }
                    else if(c == '}'){
                        --curBracket;
                        if(curBracket < 0){
                            throw new RuntimeException("Invalid curBracket input");
                        }
                    }
                    else if(c == '"'){
                        quote = true;
                    }
                    else if(c == '\\'){
                        slash = true;
                    }

                    regex.append(c);

                    break;

                case 2:
                    if(!StringUtils.isBlankChar(c)){
                        --i; // 回退，以便写入stringbuilder
                        if(c == '{'){
                            status = 4;
                        }
                        else{
                            status = 3;
                        }

                    }
                    break;

                case 3:
                    if(c == '\n' || c == '\r'){
                        rawRegexAction.put(regex.toString(), action.toString());
                        regex = new StringBuilder();
                        action = new StringBuilder();
                        status = 0;
                        break;
                    }
                    else if(c == '"'){
                        quote = true;
                    }
                    else if(c == '\\'){
                        slash = true;
                    }

                    action.append(c);

                    break;

                case 4:
                    if(c == '}'){
                        action.append(c);
                        rawRegexAction.put(regex.toString(), action.toString());
                        regex = new StringBuilder();
                        action = new StringBuilder();
                        status = 0;
                        break;
                    }
                    else if(c == '"'){
                        quote = true;
                    }
                    else if(c == '\\'){
                        slash = true;
                    }

                    action.append(c);

                    break;

                default:
                    break;
            }
        }

        if(action.length() != 0){
            rawRegexAction.put(regex.toString(), action.toString());
        }

        return rawRegexAction;
    }

    /**
     * 将alias替换到规则部分的正则表达式部分
     * @param rawRegexAction
     * @param aliasMap
     * @return
     */
    private static Map<String, String> getRegexAction(Map<String, String> rawRegexAction, Map<String, String> aliasMap){
        Map<String, String> regexAction = new LinkedHashMap<>();

//        aliasMap.forEach((k,v)->{
//            System.out.println("k:" + k + "\t" + "v:" + v+ "\n");
//        });

        rawRegexAction.forEach((k,v)->{
            int status = 0; // 1 是 {
            StringBuilder alias = new StringBuilder();
            StringBuilder newKey = new StringBuilder();
            boolean quote = false;
            boolean slash = false;
            for(int i = 0; i < k.length(); ++i){
                char c = k.charAt(i);
                if(slash){
                    newKey.append(c);
                    slash = false;
                    continue;
                }
                if(quote){
                    newKey.append(c);
                    if(c == '\\'){
                        slash = true;
                        continue;
                    }
                    if(c == '"'){
                        quote = false;
                    }
                    continue;
                }
                switch (status){
                    case 0:
                        if(c == '{'){
                            status = 1;
                        }
                        else{
                            newKey.append(c);
                            if(c == '\\'){
                                slash = true;
                            }
                            if(c == '"'){
                                quote = true;
                            }
                        }
                        break;
                    case 1:
                        if (c == '}'){
                            status = 0;
                            if(aliasMap.containsKey(alias.toString())){
                                newKey.append(aliasMap.get(alias.toString()));
                            }
                            else{ // 理论上来说，此时alias只能是数字,数字的形式，不然会报错
                                if(!Pattern.matches("[0-9]+(,[0-9]+)?" , alias.toString())){
                                    System.out.println(alias.toString());
                                    throw new RuntimeException("Wrong regex expression, incorrect format of {}, can only be {digit, digit} or {alias}");
                                }
                                String origin = "{" + alias.toString() + "}";
                                newKey.append(origin);
                            }
                            alias = new StringBuilder();
                        }
                        else{
                            alias.append(c);
                        }
                        if(c != '}' && i == k.length() - 1){
                            newKey.append("{");
                            newKey.append(alias.toString());
                        }

                        break;

                    default:
                        break;
                }
            }
            regexAction.put(newKey.toString(), v);
        });

        return regexAction;
    }

    public static ParseResult getParseResult(String rawStr) throws RuntimeException{
        ParseResult parseResult = new ParseResult();

        // 进行分割
        String[] split = rawStr.split("%%");
        if (split.length != 3){
            throw new RuntimeException("Invalid .l file format");
        }

        //第三部分是用户定义，直接拷贝
        parseResult.setUserCopy(split[2]); // 如果定义部分或者规则部分或者用户部分出现%%怎么办呢? 比如字符串里包含%% （待解决）

        //解析第一部分，分离 %{ %} 包围的用户定义部分和正则表达式别名alias部分
        Map<String, String> aliasMap = LexParser.detachDefinitionPart(split[0], parseResult);

        Map<String, String> rawRegexAction = LexParser.getRawRegexAction(split[1]);
        parseResult.setRawRegexAction(rawRegexAction);

        Map<String, String> regexAction = LexParser.getRegexAction(rawRegexAction, aliasMap);
        parseResult.setRegexAction(regexAction);

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

        ParseResult parseResult = LexParser.getParseResult(rawStr);

        return parseResult;
    }

}
