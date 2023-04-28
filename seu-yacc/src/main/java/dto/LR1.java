package dto;

import lombok.Data;

import java.util.*;

@Data
// 需要设置一个S'->S, $ 保证S'只出现在左部
public class LR1 {

    private Integer epsilon; // epsilon的终结符序号
    private Integer dollar; // dollar的终结符序号

    // symbol(包括终结符和非终结符）的标号和字符串形式的双向映射，0~127是ASCII字符，大于等于128是自定义终结符，小于等于-1是自定义非终结符
    private Map<Integer, String> numberToSymbol = new HashMap<>(); // 方便可视化
    private Map<String, Integer> symbolToNumber = new HashMap<>();

    // 非终结符的产生式，可有多个产生式，每个产生式有多个symbol，产生空串的产生式就是一个长度为0的List，key应该<0
    // key 非终结符编号 value 产生式编号集合
    private Map<Integer, List<Integer>> productions = new HashMap<>();
    // key 非终结符编号 value 对应first集的终结符编号结合
    private Map<Integer, Set<Integer>> first = new HashMap<>();// 每个非终结符的first集，可能存在epsilon

    // key 是产生式编号，value是编码后的产生式
    private Map<Integer, List<Integer>> numberToProduction = new HashMap<>(); // 由产生式序号得到产生式
    // private Map<List<Integer>, Integer> productionToNumber = new HashMap<>(); // 由产生式得到产生式序号
    // key 是产生式编号，value是动作C代码
    private Map<Integer, String> productionAction = new HashMap<>(); // 有语义动作的产生式才会出现在这个map里，目前只支持语义动作出现在产生式最后，而且只有$$=$1+$2这种赋值操作

    // 终结符编号-优先级
    private Map<Integer, Integer> symbolPriority = new HashMap<>();
    // 产生式编号-优先级
    private Map<Integer, Integer> productionPriority = new HashMap<>();

    // 根据一串序列计算first，作为拓展后的预测符
    public Set<Integer> first(List<Integer> sequence){
        Set<Integer> firstSet = new HashSet<>();

        return firstSet;
    }
}
