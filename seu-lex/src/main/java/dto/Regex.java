package dto;

import constant.Range;
import constant.SpAlpha;
import lombok.Data;

/**
 *  正则表达式，以及它的变形，以便生成NFA
 *  只考虑ascii字符
 */
@Data
public class Regex {

    private String rawRegex; // 原始正则表达式
    private String escapeRegex; // 将将原始表达式中的转义（两个字符，如\n）替换为一个字符'\n'，\看作一个字符
    private String expandRegex; // 将[]等符号替换展开，让正则表达式只剩下双目运算符 | serial，单目运算符? * \ 以及括号还有.（代表全部字符）
    private String postFix; // 后缀表达式

    //把引号换成()，其中有[ ( *等操作符自动加上\，其他保持不变，即保留所有的转义字符\，统一到转换NFA时起作用
    //不支持\d \s \S （应该替换成[0-9]等)
    private String quoteReplace(String rawRegex){
        StringBuilder stringBuilder = new StringBuilder();
        boolean quote = false;
        boolean slash = false;
        for(int i = 0; i < rawRegex.length(); ++i){
            char c = rawRegex.charAt(i);
            if(i == rawRegex.length() - 1 && quote && c != '\"'){
                throw new RuntimeException("missing quote in regex");
            }
            if(slash){
                stringBuilder.append(c);
                slash = false;
                continue;
            }
            if(c == '\\'){
                if(i == rawRegex.length() - 1){
                    throw new RuntimeException("Invalid escape, \\ can't be the end of regex");
                }
                stringBuilder.append(c);
                slash = true;
            }
            else if(c == '\"'){
                if(!quote) {
                    stringBuilder.append('(');
                }
                else{
                    stringBuilder.append(')');
                }
                quote = !quote;
            }
            else if(quote && SpAlpha.isLexOperand(c)){
                stringBuilder.append('\\');
                stringBuilder.append(c);
            }
            else{
                //遇到不转义的.，直接替换为ANY，方便NFA转换，而\.则保留原.字符，这样转移操作只需要保留原字符就行，只要考虑t n r
                if(i > 0 && rawRegex.charAt(i-1) != '\\' && c == '.'){
                    stringBuilder.append(SpAlpha.ANY);
                }
                else {
                    stringBuilder.append(c);
                }
            }
        }
        return stringBuilder.toString();
    }

    // 这个地方应当有异常检测机制
    // 主要就是展开[]，变成(x|y)的形式
    // 单独的.在转NFA阶段会被处理为SpAlpha.ANY，\.会被处理为实际.字符
    // 没考虑[.]或[^.]的问题，但在[]加没转义的.没什么意义
    private String expand(String escapeRegex){
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder bracketContent = new StringBuilder();
        boolean slash = false;
        boolean bracket = false;
        boolean complement = false;
        for(int i = 0; i < escapeRegex.length(); ++i){
            char c = escapeRegex.charAt(i);
            if(slash){ // 任何情况下，遇到了转义字符，后边的字符直接append，忽视操作效果
                if(!bracket){
                    stringBuilder.append(c);
                }
                else{
                    bracketContent.append(c);
                    bracketContent.append('|');
                }
                slash = false;
                continue;
            }
            if(c == '\\'){
                if(i == escapeRegex.length() - 1){
                    throw new RuntimeException("Invalid escape, \\ can't be the end of regex");
                }
                if(!bracket){
                    stringBuilder.append(c);
                }
                else{
                    bracketContent.append(c);
                }
                slash = true;
            }
            else if(c == '['){
                if(bracket == true){
                    throw new RuntimeException("Invalid bracket.");
                }
                bracket = !bracket;

                bracketContent.append('(');
            }
            else if (c == ']'){
                if(bracket == false){
                    throw new RuntimeException("Invalid bracket.");
                }
                bracket = !bracket;

                bracketContent.deleteCharAt(bracketContent.length() - 1); // 去掉最后一个|
                bracketContent.append(')');

                if(!complement) {
                    stringBuilder.append(bracketContent);
                    bracketContent = new StringBuilder();
                }
                else{ // 统一处理取补的情况
                    StringBuilder complementChars = new StringBuilder();
                    for(int j = 1; j < bracketContent.length() - 1; ++j){ // 排除左右括号
                        char t = bracketContent.charAt(j);
                        if(t == '|'){
                            continue;
                        }
                        else if(t == '\\'){ // 假设每个\都有字符，[]是正确的
                            ++j;
                            complementChars.append(bracketContent.charAt(j)); // 只保留被转义的字符，没保留\
                        }
                        else{
                            complementChars.append(t);
                        }
                    }
                    String complementString = complementChars.toString();
                    StringBuilder finalBuilder = new StringBuilder();
                    finalBuilder.append('(');
                    for(char t = Range.ASCII_MIN; t <= Range.ASCII_MAX; ++t){
                        if(!complementString.contains(Character.toString(t))){
                            if(SpAlpha.isSpecialChars(t)){ // 需要先加一个转义符
                                finalBuilder.append('\\');
                            }
                            finalBuilder.append(t);
                            finalBuilder.append('|');
                        }
                    }
                    finalBuilder.deleteCharAt(finalBuilder.length()-1); // 去掉最后一个|
                    finalBuilder.append(')');
                    stringBuilder.append(finalBuilder);
                    bracketContent = new StringBuilder();
                    complement = false;
                }

            }
            else if (bracket){
                if(c == SpAlpha.ANY){
                    throw new RuntimeException("[.] is not supposed");
                }
                else if(c == '^'){
                    if (escapeRegex.charAt(i-1) != '[' && escapeRegex.charAt(i-1) != '\\'){
                        throw new RuntimeException("invalid position of ^ in []");
                    }
                    complement = true;
                    continue;
                }
                else if(c == '-'){ // 它的前一个字符已经被append了，且不考虑转义的影响

                    char begin = escapeRegex.charAt(i-1);
                    char end = escapeRegex.charAt(i+1);
                    ++i; // 跳过了a-b中的b
                    if(end == '\\'){
                        end = expandRegex.charAt(i+2);
                        ++i; // 跳过了\[-\] 中的\]
                    }

                    if(begin > end){
                        throw new RuntimeException("Invalid x-y in [], x > y");
                    }
                    for (char t = (char) (begin + 1); t <= end; ++t){
                        bracketContent.append(t);
                        bracketContent.append('|');
                    }

                }
                else {
                    bracketContent.append(c);
                    bracketContent.append('|');
                }
            }
            else {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

    private String toPostFix(String expandRegex){
        return "";
    }

    public Regex(String rawRegex){
        this.rawRegex = rawRegex;
        this.escapeRegex = quoteReplace(rawRegex);
        this.expandRegex = expand(escapeRegex);
        this.postFix = toPostFix(expandRegex);
    }

}
