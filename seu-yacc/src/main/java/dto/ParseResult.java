package dto;

import constant.Associativity;
import lombok.Data;

import java.util.*;

@Data
public class ParseResult {

    private String preCopy; // %{ 和 %} 之间的部分
    private String userCopy; // 用户定义部分的直接拷贝

    // part1获取的信息
    private String startSymbol; // 文法开始符号
    private List<String> terminals = new ArrayList<>(); // 终结符集合
    private Map<String, String> symbolToCType = new HashMap<>(); // 终结符或终结符有具体值时，对应的C语言中的值类型
    private Map<String, Integer> symbolPriority = new HashMap<>(); // 终结符的优先级，数值越大优先级越高，key不一定在terminals中，有可能只出现在规则部分，只是用来声明优先级用的
    private Map<String, Associativity> symbolAssociativity = new HashMap<>(); // 终结符的结合性

    // part2获取的信息
    private Set<String> nonTerminals = new HashSet<>(); // 非终结符集合
    private Map<String, List<List<String>>> productions = new HashMap<>(); // 产生式，一个非终结符可能有多个产生式，一个产生式也会包含多个元素
    private Map<String, List<List<String>>> extendedProductions = new HashMap<>(); // 含有动作的产生式

    // 处理优先级后的信息
    private List<List<String>> productionList = new ArrayList<>(); // 所有的产生式，产生式长度至少为1，第一个是左部非终结符，如果是空串产生式，则只有一个字符串
    private Map<List<String>, Integer> productionPriority = new HashMap<>(); // 产生式对应的优先级，只在移进规约冲突的时候起作用，如果不能判断优先级，则优先级为0



}
