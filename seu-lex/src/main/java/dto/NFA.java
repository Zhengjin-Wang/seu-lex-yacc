package dto;

import constant.SpAlpha;
import lombok.Data;

/**
 * 根据LexParser获取到的ParseResult生成的NFA
 */
@Data
public class NFA { // 正则表达式转NFA时，遇到转义字符\，当作一个操作符，可以操作[ \（lex操作符），也可以操作n r t（用作转义），如果是其他普通字符，则保持原字符
                    // 只有遇到n r t才替换为对应转义后字符，其他就保留原字符

    public NFA(){

    }
    public NFA(char c){
        if(c == SpAlpha.ANY){

        }
        else{

        }
    }


}

