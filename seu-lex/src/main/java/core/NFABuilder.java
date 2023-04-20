package core;

import constant.SpAlpha;
import dto.LexAction;
import dto.NFA;
import dto.ParseResult;
import dto.Regex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class NFABuilder {

    // 保证状态的唯一性
    private static Integer stateCount = 0;

    private static Integer getNewState(){
        return stateCount++;
    }

    private static NFA createAtomNFA(char c){
        int startState = getNewState();
        int endState = getNewState();
        return new NFA(c, startState, endState);
    }

    /**
     * 将输入NFA加一条新边（从开始态到每一个终态）
     * ```
     *      ______c______
     *     |             ↓
     *    开始 --...--> 接收
     *
     * ```
     */
    private static NFA addEdge(NFA nfa, char c){

        return new NFA();
    }

    /**
     * 将输入NFA加一条直达空串边（给?用的)
     * ```
     *      ______ε______
     *     |             ↓
     *    开始 --...--> 接收
     *
     * ```
     */
    private static NFA addEpsilonEdge(NFA nfa){

        return new NFA();
    }

    /**
     * 将输入NFA做Kleene闭包（星闭包），见龙书3.7.1节图3-34
     * ```
     *      ________________ε_______________
     *     |                                ↓
     * 新开始 -ε-> 旧开始 --...--> 旧接收 -ε-> 新接收
     *              ↑______ε______|
     * ```
     * 应当返回一个新对象
     */
    private static NFA kleene(NFA nfa){

        return new NFA();
    }

    /**
     * 串联两个NFA，丢弃所有动作
     * ```
     * NFA1 --epsilon--> NFA2
     * ```
     */
    private static NFA serial(NFA a, NFA b){

        return new NFA();
    }

    /**
     * 并联两个NFA（对应于|或运算），收束尾部，丢弃所有动作
     * ```
     *             ε  NFA1  ε
     * new_start <             > new_accept
     *             ε  NFA2  ε
     * ```
     */
    private static NFA parallel(NFA a, NFA b){

        return new NFA();
    }

    /**
     *
     * @param regex 用到的是postFix
     * @param lexAction 就是C代码，到达接受态时执行C代码就行，可能要把C代码复制到特定的地方
     * @return
     */
    private static NFA buildSingleNFA(Regex regex, LexAction lexAction){

        String postFix = regex.getPostFix();
        Stack<NFA> nfaStack = new Stack<>();
        boolean slash = false;
        /*
            在处理过程中，要考虑几点:
            1. SpAlpha.ANY意味着任意字符可以转移状态 （在转regex的时候已经把非转义的.替换为SpAlpha.ANY
            2. 遇到\直接把下一个字符视作常规字符，创建新NFA加入状态栈中
         */
        for(int i = 0; i < postFix.length(); ++i){
            char c = postFix.charAt(i);
            if(slash){
                if(c == 'n'){
                    c = '\n';
                }
                else if(c == 'r'){
                    c = '\r';
                }
                else if(c == 't'){
                    c = '\t';
                }
                NFA nfa = createAtomNFA(c);
                nfaStack.push(nfa);
                slash = false;
                continue;
            }
            if(c == '\\'){
                slash = true;
            }
            else if(!SpAlpha.isPostfixOp(c)){
                NFA nfa = createAtomNFA(c);
                nfaStack.push(nfa);
            }
            else{ // 是操作符
                if(c == '|'){
                    NFA a = nfaStack.pop();
                    NFA b = nfaStack.pop();
                    NFA nfa = parallel(a, b);
                    nfaStack.push(nfa);
                }
                else if(c == SpAlpha.CONCAT){
                    NFA a = nfaStack.pop();
                    NFA b = nfaStack.pop();
                    NFA nfa = serial(a, b);
                    nfaStack.push(nfa);
                }
                else if(c == '*'){
                    NFA nfa = nfaStack.pop();
                    nfa = kleene(nfa);
                    nfaStack.push(nfa);
                }
                else if(c == '+'){
                    NFA nfa = nfaStack.pop();
                    NFA nfaKleene = kleene(nfa);
                    nfa = serial(nfa, nfaKleene);
                    nfaStack.push(nfa);
                }
                else if(c == '?'){
                    NFA nfa = nfaStack.pop();
                    nfa = addEpsilonEdge(nfa);
                    nfaStack.push(nfa);
                }
            }
        }

        //表达式正确的情况下，这个地方应该只有一个元素
        return nfaStack.peek();
    }

    /**
     * 最后并联所有NFA
     * ```
     *             ε  NFA1
     * new_start <-   ...
     *             ε  NFAn
     * ```
     * @param nfas
     * @return
     */
    private static NFA parallelAll(List<NFA> nfas){

        return new NFA();
    }

    public static NFA buildNFA(ParseResult parseResult){

        Map<String, String> regexAction = parseResult.getRegexAction();
        List<NFA> nfas = new ArrayList<>();
        int orderCnt = 0;

        for(Map.Entry<String, String> entry: regexAction.entrySet()){
            String rawRegex = entry.getKey();
            String action = entry.getValue();

            Regex regex = new Regex(rawRegex);
            LexAction lexAction = new LexAction(orderCnt, action);
            NFA nfa = buildSingleNFA(regex, lexAction);

            nfas.add(nfa);
            ++orderCnt;
        }


        NFA finalNFA = parallelAll(nfas);

        return finalNFA;
    }

}
