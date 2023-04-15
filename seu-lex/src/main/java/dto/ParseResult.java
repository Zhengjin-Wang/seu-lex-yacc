package dto;

import lombok.Data;

import java.util.Map;

/*
    承载LexParser分割后的文本结果
 */
@Data
public class ParseResult {

    private String preCopy; // %{ 和 %} 之间的部分
    private Map<String, String> rawRegexAction; // 未进行别名替换
    private Map<String, String> regexAction; // 正则表达式的语义动作
    private String userCopy; // 用户定义部分的直接拷贝

    @Override
    public String toString(){
        String pre = String.format("preCopy:\n%s\n---------------\n", this.preCopy);
        StringBuilder mid = new StringBuilder("regex:\n");
        regexAction.forEach((k,v)->{
            mid.append("k:" + k + "\t" + "v:" + v+ "\n");
        });
        mid.append("---------------\n");
        String post = String.format("postCopy:\n%s", this.userCopy);
        return pre + mid.toString() + post;
    }

}
