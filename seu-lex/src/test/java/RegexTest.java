import dto.Regex;
import org.junit.Test;

public class RegexTest {

    @Test
    public void quoteReplaceTest(){
        String s = "\"[]*?+()|.^{},-\\\"]\"\"\"[]";
        Regex regex = new Regex(s);
        System.out.println(regex.getEscapeRegex());
    }
}
