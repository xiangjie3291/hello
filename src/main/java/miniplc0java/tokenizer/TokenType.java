package miniplc0java.tokenizer;

public enum TokenType {
    /** 空 */
    None,

    /* 关键字 */
    /** FN_KW */
    FN_KW,
    /** LET_KW */
    LET_KW,
    /** CONST_KW */
    CONST_KW,
    /** AS_KW */
    AS_KW,
    /** WHILE_KW */
    WHILE_KW,
    /** IF_KW */
    IF_KW,
    /** ELSE_KW */
    ELSE_KW,
    /** RETURN_KW */
    RETURN_KW,
    /** BREAK_KW */
    BREAK_KW,
    /** CONTINUE_KW */
    CONTINUE_KW,


    /** 无符号整数 */
    UINT_LITERAL,
    /** 字符串常量 */
    STRING_LITERAL,
    /** 浮点数 */
    DOUBLE_LITERAL,
    /** Char */
    CHAR_LITERAL,

    /** 标识符 */
    IDENT,

    /* 计算符号 */
    /** 加号 */
    PLUS,
    /** 减号 */
    MINUS,
    /** 乘号 */
    MUL,
    /** 除号 */
    DIV,
    /** 等号 */
    ASSIGN,
    /** 相等 */
    EQ,
    /** 不等 */
    NEQ,
    /** 小于 */
    LT,
    /** 大于 */
    GT,
    /** 小于等于 */
    LE,
    /** 大于等于 */
    GE,
    /** 左小括号 */
    L_Paren,
    /** 右小括号 */
    R_Paren,
    /** 左中括号 */
    L_BRACE,
    /** 右中括号 */
    R_BRACE,
    /** 箭头 */
    ARROW,
    /** 逗号 */
    COMMA,
    /** 冒号 */
    COLON,
    /** 分号 */
    SEMICOLON ,



    /** 文件尾 */
    EOF;

    @Override
    public String toString() {
        switch (this) {
            case None:
                return "NullToken";


            case FN_KW:
                return "fn";
            case LET_KW:
                return "let";
            case CONST_KW:
                return "const";
            case AS_KW:
                return "as";
            case WHILE_KW:
                return "while";
            case IF_KW:
                return "if";
            case ELSE_KW:
                return "else";
            case RETURN_KW:
                return "return";
            case BREAK_KW:
                return "break";
            case CONTINUE_KW:
                return "continue";


            case UINT_LITERAL:
                return "UnsignedInteger";
            case STRING_LITERAL:
                return "StringConstant";
            case DOUBLE_LITERAL:
                return "Double";
            case CHAR_LITERAL:
                return "Char";

            case IDENT:
                return "ident";


            case PLUS:
                return "PlusSign";
            case MINUS:
                return "MinusSign";
            case MUL:
                return "MultiplicationSign";
            case DIV:
                return "DivisionSign";
            case ASSIGN:
                return "AssignSign";
            case EQ:
                return "EqualSign";
            case NEQ:
                return "NotequalSign";
            case LT:
                return "LessThan";
            case GT:
                return "GreatThan";
            case LE:
                return "LeSign";
            case GE:
                return "GeSign";
            case L_Paren:
                return "LeftParen";
            case R_Paren:
                return "RightParen";
            case L_BRACE:
                return "LeftBrace";
            case R_BRACE:
                return "RightBrace";
            case ARROW:
                return "ArrowSign";
            case COMMA:
                return "CommaSign";
            case COLON:
                return "ColonSign";
            case SEMICOLON:
                return "SemicolonSign";


            case EOF:
                return "EOF";

            default:
                return "InvalidToken";
        }
    }
    public static void main(String[] args) {
        TokenType[] tokenTypes=TokenType.values();
        for(int i=1;i<=8;i++){
            System.out.println(tokenTypes[i].toString());
        }
    }


}
