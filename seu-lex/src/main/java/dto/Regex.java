package dto;

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
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

    //这个地方应当有异常检测机制
    private String expand(String escapeRegex){
        return "";
    }

    private String toPostFix(String postFix){
        return "";
    }

    public Regex(String rawRegex){
        this.rawRegex = rawRegex;
        this.escapeRegex = quoteReplace(rawRegex);
        this.expandRegex = expand(escapeRegex);
        this.postFix = toPostFix(postFix);
    }

}
