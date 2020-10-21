package miniplc0java.analyser;

import miniplc0java.error.*;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
    }

    /**
     * 查看下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     * 
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    /**
     * 添加一个符号
     * 
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        if (this.symbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset()));
        }
    }

    /**
     * 设置符号为已赋值
     * 
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void declareSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

    /**
     * <程序> ::= 'begin'<主过程>'end'
     */
    private void analyseProgram() throws CompileError {
        // 示例函数，示例如何调用子程序
        // 'begin'
        expect(TokenType.Begin);

        //<主过程>
        analyseMain();

        // 'end'
        expect(TokenType.End);

        expect(TokenType.EOF);
    }

    /**
     <主过程> ::= <常量声明><变量声明><语句序列>
     */
    private void analyseMain() throws CompileError {
      //  throw new Error("Not implemented");
        // <常量声明>
        analyseConstantDeclaration();
        // <变量声明>
        analyseVariableDeclaration();
        // <语句序列>
        analyseStatementSequence();
    }

    /**
      <常量声明> ::= {<常量声明语句>}
      <常量声明语句> ::= 'const'<标识符>'='<常表达式>';'
     */
    private void analyseConstantDeclaration() throws CompileError {
        // 示例函数，示例如何解析常量声明
        // 如果下一个 token 是 const 就继续
        while (nextIf(TokenType.Const) != null) {
            // 变量名
            var nameToken = expect(TokenType.Ident);

            /*
            不能重定义：同一个标识符，不能被重复声明
             */
            if(symbolTable.containsKey(nameToken.getValue().toString())){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration,nameToken.getStartPos());
            }

            // 等于号
            expect(TokenType.Equal);

            // 常表达式
            int val=analyseConstantExpression();

            // 分号
            expect(TokenType.Semicolon);

            //语法正确，将该常量添加到符号表
            addSymbol(nameToken.getValue().toString(),true,true,nameToken.getStartPos());
            //将常表达式的值存入栈中，与该标识符的栈偏移相对应
            instructions.add(new Instruction(Operation.LIT,val));

        }
    }

    /**
     <变量声明> ::= {<变量声明语句>}
     <变量声明语句> ::= 'var'<标识符>['='<表达式>]';'
     */
    private void analyseVariableDeclaration() throws CompileError {
       // throw new Error("Not implemented");
        // 如果下一个 token 是 var 就继续
        boolean flag=false;
        while (nextIf(TokenType.Var) != null) {
            // 变量名
            var nameToken = expect(TokenType.Ident);

            /*
            不能重定义：同一个标识符，不能被重复声明
             */
            if(symbolTable.containsKey(nameToken.getValue().toString())){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration,nameToken.getStartPos());
            }

            // 等于号、表达式
            if (nextIf(TokenType.Equal) != null) {
                analyseExpression();
                flag=true;
            }

            // 分号
            expect(TokenType.Semicolon);

            if(flag){
                addSymbol(nameToken.getValue().toString(),true,false, nameToken.getStartPos());
            }else{
                addSymbol(nameToken.getValue().toString(),false,false, nameToken.getStartPos());
            }
        }
    }

    /**
     <语句序列> ::= {<语句>}
     */
    private void analyseStatementSequence() throws CompileError {
      //  throw new Error("Not implemented");
        //<赋值语句>、<输出语句>、<空语句>
        while (check(TokenType.Ident)||check(TokenType.Print)||check(TokenType.Semicolon)){
            // 调用相应的处理函数
            analyseStatement();
        }
    }

    /**
     <语句> ::= <赋值语句>|<输出语句>|<空语句>
     */
    private void analyseStatement() throws CompileError {
      //  throw new Error("Not implemented");
        if(check(TokenType.Ident)){
            analyseAssignmentStatement();
        }else if(check(TokenType.Print)){
            analyseOutputStatement();
        }else if(check(TokenType.Semicolon)){
            expect(TokenType.Semicolon);
        }else{
            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Print, TokenType.Semicolon), next());
        }
    }

    /**
     <常表达式> ::= [<符号>]<无符号整数>
     */
    private int analyseConstantExpression() throws CompileError {
     //   throw new Error("Not implemented");
        boolean negate;
        if (nextIf(TokenType.Minus) != null) {
            negate = true;
        } else {
            nextIf(TokenType.Plus);
            negate = false;
        }

        Token token=expect(TokenType.Uint);

        int val=Integer.parseInt(token.getValue().toString());
        if (negate) {
            val*=-1;
        }
        return val;
    }

    /**
     <表达式> ::= <项>{<加法型运算符><项>}
     */
    private void analyseExpression() throws CompileError {
     //   throw new Error("Not implemented");
        analyseItem();
        while(check(TokenType.Plus)||check(TokenType.Minus)){
            TokenType type=next().getTokenType();
            analyseItem();
            if(type==TokenType.Plus){
                instructions.add(new Instruction(Operation.ADD));
            }else if(type==TokenType.Minus){
                instructions.add(new Instruction(Operation.SUB));
            }
        }
    }

    /**
    <赋值语句> ::= <标识符>'='<表达式>';'
    */
    private void analyseAssignmentStatement() throws CompileError {
     //   throw new Error("Not implemented");
        Token token=expect(TokenType.Ident);

        /*
         不能使用没有声明过的标识符
         不能给常量赋值：被声明为常量的标识符，不能出现在赋值语句的左侧
        */
        if(!symbolTable.containsKey(token.getValue().toString())){
            throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
        }else if(isConstant(token.getValue().toString(), token.getStartPos())){
            throw new AnalyzeError(ErrorCode.AssignToConstant, token.getStartPos());
        }

        //等号
        expect(TokenType.Equal);
        //表达式
        analyseExpression();
        //分号
        expect(TokenType.Semicolon);

        //将栈顶的值赋值给变量
        instructions.add(new Instruction(Operation.STO,getOffset(token.getValue().toString(),token.getStartPos())));
        //将变量设置为已赋值
        declareSymbol(token.getValue().toString(), token.getStartPos());
    }

    /**
     <输出语句> ::= 'print' '(' <表达式> ')' ';'
     */
    private void analyseOutputStatement() throws CompileError {
        expect(TokenType.Print);
        expect(TokenType.LParen);
        analyseExpression();
        expect(TokenType.RParen);
        expect(TokenType.Semicolon);
        instructions.add(new Instruction(Operation.WRT));
    }

    /**
     <项> ::= <因子>{<乘法型运算符><因子>}
     */
    private void analyseItem() throws CompileError {
     //   throw new Error("Not implemented");
        analyseFactor();
        while(check(TokenType.Mult)||check(TokenType.Div)){
            TokenType type=next().getTokenType();
            analyseFactor();
            if(type==TokenType.Mult){
                instructions.add(new Instruction(Operation.MUL));
            }else if(type==TokenType.Div){
                instructions.add(new Instruction(Operation.DIV));
            }
        }
    }

    /**
     <因子> ::= [<符号>]( <标识符> | <无符号整数> | '('<表达式>')' )
     */
    private void analyseFactor() throws CompileError {
        boolean negate;
        if (nextIf(TokenType.Minus) != null) {
            negate = true;
            // 计算结果需要被 0 减
            instructions.add(new Instruction(Operation.LIT, 0));
        } else {
            nextIf(TokenType.Plus);
            negate = false;
        }

        if (check(TokenType.Ident)) {
            // 调用相应的处理函数
            /*
            不能使用没有声明过的标识符;
            不能使用未初始化的变量：不能参与表达式的运算，也不能出现在赋值语句的右侧;
             */
            Token token=expect(TokenType.Ident);
            if(!symbolTable.containsKey(token.getValue().toString())){
                throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
            }else if(!symbolTable.get(token.getValue().toString()).isInitialized){
                throw new AnalyzeError(ErrorCode.NotInitialized, token.getStartPos());
            }
            //将该标识符的值存入栈顶
            instructions.add(new Instruction(Operation.LOD, getOffset(token.getValue().toString(), token.getStartPos())));
        } else if (check(TokenType.Uint)) {
            // 调用相应的处理函数
           Token token=expect(TokenType.Uint);
            //将该无符号整数的值存入栈顶
            instructions.add(new Instruction(Operation.LIT, Integer.parseInt(token.getValue().toString())));
        } else if (check(TokenType.LParen)) {
            // 调用相应的处理函数
            expect(TokenType.LParen);
            analyseExpression();
            expect(TokenType.RParen);
        } else {
            // 都不是，摸了
            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
        }

        if (negate) {
            instructions.add(new Instruction(Operation.SUB));
        }
      //  throw new Error("Not implemented");
    }
}
