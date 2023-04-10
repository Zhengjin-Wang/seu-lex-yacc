package core;

import dto.ParseResult;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Lilac
 */
public class LexParser {

    /**
     * 返回regex alias的map，并将预定义部分拷贝到parseResult
     * @param definationPart
     * @param parseResult
     * @return
     * @throws RuntimeException
     */
    private static Map<String, String> detachDefinationPart(String definationPart, ParseResult parseResult) throws RuntimeException{
        //默认 %{ %} 在文件开始位置，也就是在定义规则之前，而且一定存在
        String[] split = definationPart.split("%}");
        if (split.length != 2){
            throw new RuntimeException("Invalid .l file format");
        }

        String regexAlias = split[1]; // 要保证每行一个别名及正则表达式匹配只占一行

        //拷贝第一部分的直接代码部分
        String[] part1Split = split[0].split("%\\{");
        parseResult.setPreCopy(part1Split[1]);

        Map<String, String> aliasMap = new HashMap<>();
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
        Map<String, String> rawRegexAction = new HashMap<>();

        return rawRegexAction;
    }

    /**
     * 将alias替换到规则部分的正则表达式部分
     * @param rawRegexAction
     * @param aliasMap
     * @return
     */
    private static Map<String, String> getRegexAction(Map<String, String> rawRegexAction, Map<String, String> aliasMap){
        Map<String, String> regexAction = new HashMap<>();

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
        Map<String, String> aliasMap = LexParser.detachDefinationPart(split[0], parseResult);

        Map<String, String> rawRegexAction = LexParser.getRawRegexAction(split[1]);

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
