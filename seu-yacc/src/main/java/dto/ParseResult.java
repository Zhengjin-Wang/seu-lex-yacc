package dto;

import constant.Associativity;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ParseResult {

    private String preCopy; // %{ 和 %} 之间的部分
    private String userCopy; // 用户定义部分的直接拷贝

    private String startSymbol;

    private List<String> tokens = new ArrayList<>();

    private Map<String, String> symbolToCType = new HashMap<>(); // 终结符或终结符有具体值时，对应的C语言中的值类型

    private Map<String, Integer> symbolPriority = new HashMap<>(); // 终结符的优先级，数值越大优先级越高

    private Map<String, Associativity> symbolAssociativity = new HashMap<>(); // 终结符的结合性

}
