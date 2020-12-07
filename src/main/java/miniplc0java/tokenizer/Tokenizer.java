package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }
    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        /* 数字开头 */
        if (Character.isDigit(peek)) {
            return lexUInt();
        } else if (Character.isAlphabetic(peek)||peek == '_') {
            return lexIdentOrKeyword();
        } else if(peek == '"'){
            return lexStringLiteral();
        } else if(peek == '\''){
            return lexCharLiteral();
        }
        else {
//            if(peek == '/'){
//                return lexComment();
//            }
            return lexOperatorOrUnknown();
        }
    }

    /* 识别字符字面量 */
    private Token lexCharLiteral() throws TokenizeError{
      //  StringBuilder CatToken = new StringBuilder();
        Pos startPos = it.nextPos();
        Pos endPos;
        it.nextChar();
        char tmp = it.peekChar();//下一个指向0，当前指向'(开始)
        if(tmp != '\\' && tmp != '\''  && tmp != '\n' && tmp != '\r' && tmp != '\t'){
           // CatToken.append(tmp);
            it.nextChar();
            if(it.peekChar()=='\''){
                it.nextChar();
                return new Token(TokenType.CHAR_LITERAL,(long)tmp, startPos, it.nextPos());
            }else{
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
        }else if(tmp == '\\'){
           // CatToken.append(tmp); //读取一个'\'
            it.nextChar();
            tmp = it.peekChar();
            //读取斜线后的另一部分构成转义字符
            if(tmp == '\\' || tmp == '\''  || tmp == 'n' || tmp == 'r' || tmp == 't'){
                //CatToken.append(tmp);
                it.nextChar();
                if(it.peekChar()=='\''){
                    it.nextChar();
                    if(tmp=='\\'||tmp=='\'') {
                        return new Token(TokenType.CHAR_LITERAL, (long) tmp, startPos, it.nextPos());
                    }else if(tmp== 'n'){
                        return new Token(TokenType.CHAR_LITERAL, (long) '\n', startPos, it.nextPos());
                    }else if(tmp== 'r'){
                        return new Token(TokenType.CHAR_LITERAL, (long) '\r', startPos, it.nextPos());
                    }else {
                        return new Token(TokenType.CHAR_LITERAL, (long) '\t', startPos, it.nextPos());
                    }
                }else{
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
            }
            //没读到另一部分
            else{
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
        }else {
            throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }

    }

    /* 无符号整数(或浮点数形式) */
    private Token lexUInt() throws TokenizeError {
        StringBuilder CatToken = new StringBuilder();
        Pos startPos = it.nextPos();
        Pos endPos;
        int flag = 0;
        while (!it.isEOF() && (Character.isDigit(it.peekChar()))) {
            CatToken.append(it.peekChar());
            it.nextChar();
        }
        /*发现小数点，为浮点数 */
        if (it.peekChar() == '.') {
            flag = 1;
            CatToken.append(it.peekChar());
            it.nextChar();
            /* 小数点后必须跟数字 */
            if(Character.isDigit(it.peekChar())){
                while (!it.isEOF() && (Character.isDigit(it.peekChar()))) {
                    CatToken.append(it.peekChar());
                    it.nextChar();
                }
                /* 发现e或E，为科学计数法 */
                if (it.peekChar() == 'e' || it.peekChar() == 'E') {
                    CatToken.append(it.peekChar());
                    it.nextChar();
                    /* 可能有+或- */
                    if(it.peekChar() == '+' || it.peekChar() == '-'){
                        CatToken.append(it.peekChar());
                        it.nextChar();
                    }
                    /* 后必须跟数字 */
                    if(Character.isDigit(it.peekChar())) {
                        while (!it.isEOF() && (Character.isDigit(it.peekChar()))) {
                            CatToken.append(it.peekChar());
                            it.nextChar();
                        }
                    }else{
                        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                    }
                }
            }else{
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
        }
       endPos = it.currentPos();
       if (flag == 0) {
           Long num = Long.parseLong(CatToken.toString());
           return new Token(TokenType.UINT_LITERAL, num, startPos, endPos);
       } else {
           double b = Double.parseDouble(CatToken.toString());
           long l = Double.doubleToLongBits(b);
           /* 将double以long的字节形式传入 */
           return new Token(TokenType.DOUBLE_LITERAL,l, startPos, endPos);
       }
    }

    /* 关键字或标识符 */
    private Token lexIdentOrKeyword() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串
        StringBuilder CatToken=new StringBuilder();
        Pos startPos=it.nextPos();
        Pos endPos;
        while(!it.isEOF() && (Character.isDigit(it.peekChar())||Character.isAlphabetic(it.peekChar())||it.peekChar()=='_')){
            CatToken.append(it.peekChar());
            it.nextChar();
        }
        endPos=it.currentPos();
        TokenType[] tokenTypes=TokenType.values();
        TokenType x = null;
        for(int i=1;i<=10;i++){
            if(tokenTypes[i].toString().equalsIgnoreCase(CatToken.toString())){
                x=tokenTypes[i];
            }
        }
        if(x==null){
            return new Token(TokenType.IDENT,CatToken.toString(),startPos,endPos);
        }else {
            return new Token(x, x.toString(), startPos, endPos);
        }
    }

    /* 字符串常量 */
    private Token lexStringLiteral() throws TokenizeError{
        StringBuilder CatToken = new StringBuilder();
        Pos startPos=it.nextPos();
        Pos endPos;
        /* 已经判断第一个 "  */
        /* 指向 " 所在的位置，然后循环开始 */
        it.nextChar();
        while(!it.isEOF()){
            /* 判断下一个是否为转义字符 */
            if(it.peekChar()=='\\'){
                /* 将'\\'存入，并指向它 */
                //CatToken.append(it.peekChar());
                it.nextChar();
                /* 偷看'\\'后的一个字符 */
                char tmp= it.peekChar();
                /* 不为转义字符，抛出异常，为转义字符，读取后跳过本次循环*/
                if(tmp != '\\' && tmp != '\'' && tmp != '"' && tmp != 'n' && tmp != 'r' && tmp != 't'){
                    throw new TokenizeError(ErrorCode.InvalidInput, it.nextPos());
                }else{
                    if(tmp=='\\'||tmp=='\'') {
                        CatToken.append(it.peekChar());
                    }else if(tmp=='"') {
                        CatToken.append('\"');
                    }else if(tmp=='n') {
                        CatToken.append('\n');
                    }else if(tmp=='r') {
                        CatToken.append('\r');
                    }else {
                        CatToken.append('\t');
                    }
                    it.nextChar();
                    continue;
                }
            }
            /* 判断字符串是否结束,由于上个if已经包含转义字符情况，因此此时一定为函数结束 */
            if(it.peekChar()=='"'){
                it.nextChar();
                break;
            }
            CatToken.append(it.peekChar());
            it.nextChar();
        }
        endPos=it.currentPos();
        return new Token(TokenType.STRING_LITERAL,CatToken.toString(),startPos,endPos);
    }

    /* 计算符 */
    private Token lexOperatorOrUnknown() throws TokenizeError {
        /* 首先只用peek对下一个进行读取，判断成功后再next */
        Pos startpos;
        switch (it.peekChar()) {
            case '+':
                it.nextChar();
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());

            case '-':
                // 填入返回语句
                it.nextChar();
                startpos=it.previousPos();
                if(it.peekChar()=='>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", startpos, it.currentPos());
                }
                return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());

            case '*':
                // 填入返回语句
                it.nextChar();
                return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());

            case '/':
                // 填入返回语句
                it.nextChar();
                if(it.peekChar()=='/'){
                    while(it.peekChar()!='\n'){
                        it.nextChar();
                    }
                    return nextToken();
                }
                return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());

            case '=':
                // 填入返回语句
                it.nextChar();
                startpos=it.previousPos();
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", startpos, it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());

            case '!':
                // 填入返回语句
                it.nextChar();
                startpos=it.previousPos();
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", startpos, it.currentPos());
                }else {
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }

            case '<':
                // 填入返回语句
                it.nextChar();
                startpos=it.previousPos();
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", startpos, it.currentPos());
                }
                return new Token(TokenType.LT, '<', it.previousPos(), it.currentPos());

            case '>':
                // 填入返回语句
                it.nextChar();
                startpos=it.previousPos();
                if(it.peekChar()=='='){
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", startpos, it.currentPos());
                }
                return new Token(TokenType.GT, '>', it.previousPos(), it.currentPos());

            case '(':
                // 填入返回语句
                it.nextChar();
                return new Token(TokenType.L_Paren, '(', it.previousPos(), it.currentPos());

            case ')':
                // 填入返回语句
                it.nextChar();
                return new Token(TokenType.R_Paren, ')', it.previousPos(), it.currentPos());

            case '{':
                // 填入返回语句
                it.nextChar();
                return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());

            case '}':
                // 填入返回语句
                it.nextChar();
                return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());

            case ',':
                // 填入返回语句
                it.nextChar();
                return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());

            case ':':
                // 填入返回语句
                it.nextChar();
                return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());
            case ';':
                // 填入返回语句
                it.nextChar();
                return new Token(TokenType.SEMICOLON, ';', it.previousPos(), it.currentPos());

            // 填入更多状态和返回语句

            default:
                // 不认识这个输入，摸了
                it.nextChar();
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

//    /* 注释 */
//    private Token lexComment() throws TokenizeError{
//        it.nextChar();
//        if(it.peekChar()=='/'){
//            while(it.peekChar()!='\n'){
//                it.nextChar();
//            }
//            return nextToken();
//        }else{
//            throw new TokenizeError(ErrorCode.InvalidInput, it.nextPos());
//        }
//    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
