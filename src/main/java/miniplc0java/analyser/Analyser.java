package miniplc0java.analyser;

import miniplc0java.Struct.FunctionDef;
import miniplc0java.Struct.GlobalDef;
import miniplc0java.Struct.Parameter;
import miniplc0java.Struct.Symbol;
import miniplc0java.error.*;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.AuxiliaryFunction;
import miniplc0java.util.OperatorPrecedence;

import java.io.*;
import java.util.*;

public class Analyser {

    Tokenizer tokenizer;


    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表(变量或常量) */
    List<Symbol> SymbolTable = new ArrayList<>();

    /** 函数表 */
    HashMap<String, FunctionDef> FunctionTable = new HashMap<>();

    /** 全局表 */
    List<GlobalDef> GlobalTable = new ArrayList<>();

    /** 指令集列表 */
    List<Instruction> InstructionList ;

    /** 全局指令集列表 */
    List<Instruction> GlobalInstructionList = new ArrayList<>() ;

    /** 全局变量中的位置 */
    int globalOffset = 0;

    /**
     *  函数id，调用函数时用
     *  为0的函数id被开始程序占用
     * */
    int functionId = 1;

    /** 函数参数偏移 */
    int paramOffset = 0;

    /** 判断函数是否为void，此时不需要返回 */
    Boolean isVoid =false;

    /** 判断函数是否有返回 */
    Boolean haveReturn = false;

    /** 用于判断返回的类型是否符合函数 */
    String returnType = "";

    /** 函数当前的参数列表 */
    List<Parameter> params= new ArrayList<>();

    /** 算符优先矩阵 */
    int[][] priority = OperatorPrecedence.getPriority();




    /**
     *  用于存储函数的局部变量大小
     *  以及同时用于表达局部变量在栈中的偏移
     * */
    int loc_slots = 0;

    /** 用于存储函数的返回值大小 */
    int return_slots = 0;

    /** 创建一个符号栈 */
    Stack<TokenType> stack = new Stack<>();

    /**
     *  语句、表达式、函数当前的层次
     *  0层即为全局变量
     * */
    int level = 0;



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
//    /** 下一个变量的栈偏移 */
//    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.InstructionList = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return InstructionList;
    }


    /**
     * 表达式,修改左递归后：
     * 表达式 ->
     *         (
     *         '-' 表达式 |
     *         IDENT ( '=' 表达式 | '(' call_param_list ')' | 空 ) |
     *         literal_expr |
     *         '(' 表达式 ')'
     *         ){
     *             运算符 表达式 | 'as' ty
     *         }
     *
     * call_param_list -> expr (',' expr)*
     * */
    private String analyseExpression() throws CompileError {
        String Type1="";
        /* negate_expr */
        if(check(TokenType.MINUS)){
           Type1 = analyseNegateExpression();
        }
        /* assign_expr| call_expr | ident_expr */
        else if(check(TokenType.IDENT)){
          Type1 = analyseAssign_Call_IdentExpression();
        }
        /* group_expr */
        else if(check(TokenType.L_Paren)){
          Type1 = analyseGroupExpression();
        }
        /* literal_expr */
        else if(isLiteralExpr()){
            /* 继续执行 */
            /* 识别字面量 */
            Token tmp = next();
            if(tmp.getTokenType()==TokenType.UINT_LITERAL){
                /* int类型，四字节 */
                if(level==0){
                    GlobalInstructionList.add(new Instruction(Operation.push , (Integer) tmp.getValue(), 8));
                }else {
                    InstructionList.add(new Instruction(Operation.push, (Integer) tmp.getValue(), 8));
                    System.out.println("----------------------"+tmp.getValueString()+"----------------");
                }
                Type1 = "int";
                //returnType = "int";
            }else if(tmp.getTokenType()==TokenType.DOUBLE_LITERAL){
                /* todo: double类型，未实现 */
                if(level==0){
                    GlobalInstructionList.add(new Instruction(Operation.push, (long) tmp.getValue(), 8));
                }else {
                    InstructionList.add(new Instruction(Operation.push, (long) tmp.getValue(), 8));
                }
            }else if (tmp.getTokenType() == TokenType.STRING_LITERAL) {
                GlobalTable.add(new GlobalDef(tmp.getValueString(), 1, AuxiliaryFunction.ChangeToBinary(tmp.getValueString())));
                if(level==0) {
                    GlobalInstructionList.add(new Instruction(Operation.push, globalOffset, 8));
                }else{
                    InstructionList.add(new Instruction(Operation.push, globalOffset, 8));
                }

                globalOffset++;
               // returnType = "string";
                Type1 = "int";
            }
        }

            while (isBinaryOperator() || check(TokenType.AS_KW)) {
                /* operator_expr */
                if (isBinaryOperator()) {
                    Token tmp = next();

                    if (!stack.empty()) {
                        int front = OperatorPrecedence.getOrder(stack.peek());
                        int next = OperatorPrecedence.getOrder(tmp.getTokenType());
                        if (priority[front][next] > 0) {
                            TokenType type = stack.pop();
                            Instruction.AddToInstructionListInt(type, InstructionList);
                        }
                    }
                    stack.push(tmp.getTokenType());
                    String Type2 = analyseExpression();
                    if (!Type1.equals(Type2)){
                        throw new AnalyzeError(ErrorCode.TypeError, tmp.getStartPos());
                    }
                }
                /* as_expr
                 *  todo: 没写，摸了
                 *  */
                else {
                    expect(TokenType.AS_KW);
                    analyseTy();
                }
            }
//        /* 都不是，报错 */
//        else{
//            throw new ExpectedTokenError(List.of(TokenType.MINUS, TokenType.IDENT, TokenType.L_Paren), next());
//        }
        return Type1;

    }

    /**
     * negate_expr -> '-' expr
     * */
    //finish
    private String analyseNegateExpression() throws CompileError{
        String Type1 = "";
        expect(TokenType.MINUS);
        Token tmp = peek();
        /* 根据表达式分析后得到的表达式类型判断 */
        Type1 =  analyseExpression();
        if(Type1.equalsIgnoreCase("int")||Type1.equalsIgnoreCase("double")){
            /* 满足需求,添加指令 */
            InstructionList.add(new Instruction(Operation.neg));
        }else {
            throw new AnalyzeError(ErrorCode.TypeError, tmp.getStartPos());

        }
        /* 表达式的类型不变，不需要操作 */
        return Type1;
    }

    /**
     * assign_expr -> l_expr '=' expr
     * call_expr -> IDENT '(' call_param_list? ')'
     * ident_expr -> IDENT
     *
     * 左值表达式
     * l_expr -> IDENT
     * */
    private String analyseAssign_Call_IdentExpression()throws CompileError{
        String Type1 = "";
        Token tmp = expect(TokenType.IDENT);

        //偷偷查看下一个token
        Token var = peek();
        if(check(TokenType.ASSIGN)){
            expect(TokenType.ASSIGN);
            String type = "";
            /* 在当层或之前层查找该变量 */
            Symbol symbol = AuxiliaryFunction.CanBeUsed(SymbolTable, level, tmp.getValueString());
            /* 在函数列表中查找该变量 */
            Parameter param = AuxiliaryFunction.isParameter(params, tmp.getValueString());
            /* 找不到该变量 */
            if(symbol==null&&param==null){
                throw new AnalyzeError(ErrorCode.NotDeclared, tmp.getStartPos());
            }
            else {
                //为符号
                if(symbol!=null) {
                    type = symbol.getType();
                    /* 该变量类型与表达式类型不同,或l_expr的类型为void */
                    if (type.equals("void")) {
                        throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
                    }
                    /* 找到一个常量 */
                    else if (symbol.isConstant() == 1) {
                        throw new AnalyzeError(ErrorCode.AssignToConstant, tmp.getStartPos());
                    }
                    /* 都不是 */
                    else {
                        /* 判断该变量为全局还是局部 */
                        //为全局
                        if(symbol.getLevel()==0){
                            InstructionList.add(new Instruction(Operation.globa, symbol.getOffset(), 4));
                        }
                        //为局部
                        else{
                            InstructionList.add(new Instruction(Operation.loca, symbol.getOffset(), 4));
                        }
                    }
                }
                //为参数
                else{
                    type = param.getType();
                    if (type.equals("void")) {
                        throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
                    }
                    int offset = AuxiliaryFunction.getParamOffset(param.getName(), params);
                    InstructionList.add(new Instruction(Operation.arga,paramOffset+offset, 4));
                }
            }

            String Type2 = "";
            if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_Paren)||isLiteralExpr()) {
                Type2 = analyseExpression();
            }

            if(!type.equals(Type2)){
                throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
            }

            while (!stack.empty()) {
                Instruction.AddToInstructionListInt(stack.pop(), InstructionList);
            }

            InstructionList.add(new Instruction(Operation.store));

            Type1 = "void";

        }

        else if(check(TokenType.L_Paren)){
            expect(TokenType.L_Paren);

            stack.push(TokenType.L_Paren);
            /* 判断是否为已经定义的函数或库函数 */
            FunctionDef function = FunctionTable.get(tmp.getValueString());
            Instruction instruction;

            if(function!=null||AuxiliaryFunction.isLibraryFunction(tmp.getValueString())){
                int offset;

                /* 库函数 */
                if(AuxiliaryFunction.isLibraryFunction(tmp.getValueString())){
                    offset=globalOffset;
                    //库函数允许重复，直接添加进全局
                    GlobalTable.add(new GlobalDef(tmp.getValueString(),1, AuxiliaryFunction.ChangeToBinary(tmp.getValueString())));
                    globalOffset++;
                    instruction = new Instruction(Operation.callname, offset, 4);

                    Type1 = AuxiliaryFunction.TypeReturnOfLibrary(tmp.getValueString());
                }
                /* 为自己创建的函数 */
                /* 非库函数才需要返回值空间 */
                else{
                    offset = function.getFunctionId();
                    instruction = new Instruction(Operation.call,offset, 4);

                    Type1 = function.getType();
                }
            }else{
                throw new AnalyzeError(ErrorCode.NotDeclared, tmp.getStartPos());
            }

            if(AuxiliaryFunction.hasReturn(tmp.getValueString(), FunctionTable)){
                InstructionList.add(new Instruction(Operation.stackalloc,1, 4));
            }else{
                InstructionList.add(new Instruction(Operation.stackalloc,0, 4));
            }


            if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_Paren)||isLiteralExpr()) {
                /* 函数参数均正确 */
                analyseCallParamList(tmp.getValueString());
            }
            expect(TokenType.R_Paren);


            //弹栈
            while (stack.peek() != TokenType.L_Paren) {
                TokenType tokenType = stack.pop();
                Instruction.AddToInstructionListInt(tokenType, InstructionList);
            }
            stack.pop();

            InstructionList.add(instruction);

        }

        else{
            Symbol symbol = AuxiliaryFunction.CanBeUsed(SymbolTable, level, tmp.getValueString());
            Parameter parameter = AuxiliaryFunction.isParameter(params, tmp.getValueString());
            if (symbol==null&&parameter==null)
                throw new AnalyzeError(ErrorCode.NotDeclared);
            Instruction instruction;
            //局部变量
            int id;
            if (symbol!=null) {
                /* 全局 */
                if(symbol.getLevel()==0){
                    id = symbol.getOffset();
                    instruction = new Instruction(Operation.globa, id,4);
                    InstructionList.add(instruction);
                }
                /* 局部 */
                else {
                    id = symbol.getOffset();
                    instruction = new Instruction(Operation.loca, id,4);
                    InstructionList.add(instruction);
                }
                Type1 = symbol.getType();
            }
            //参数
            else {
                id = AuxiliaryFunction.getParamOffset(parameter.getName(), params);
                instruction = new Instruction(Operation.arga, paramOffset + id,4);
                InstructionList.add(instruction);
                Type1 = parameter.getType();
            }
            InstructionList.add(new Instruction(Operation.load));
        }
        return Type1;
    }

    /**
     * call_param_list -> expr (',' expr)*
     * 同时判断参数列表与函数的参数列表是否一一对应
     * */
    private int analyseCallParamList(String name) throws CompileError{
        List<String> TypeList = new ArrayList<>();
        int count = 0;
        String Type = analyseExpression();
        TypeList.add(Type);
        while (!stack.empty() && stack.peek() != TokenType.L_Paren) {
            Instruction.AddToInstructionListInt(stack.pop(), InstructionList);
        }
        count++;

        while(nextIf(TokenType.COMMA)!=null){
            Type = analyseExpression();
            TypeList.add(Type);
            while (!stack.empty() && stack.peek() != TokenType.L_Paren) {
                Instruction.AddToInstructionListInt(stack.pop(), InstructionList);
            }
            count++;
        }

        List<String> ParamTypeList = AuxiliaryFunction.TypeReturn(name, FunctionTable);
        if(ParamTypeList.size()==TypeList.size()){
            for (int i=0 ;i<TypeList.size();i++){
                if(!TypeList.get(i).equals(ParamTypeList.get(i))){
                    throw new AnalyzeError(ErrorCode.ParamError);
                }
            }
        }else{
            throw new AnalyzeError(ErrorCode.ParamError);
        }


        return count;
    }

    /**
     * group_expr -> '(' expr ')'
     * */
    private String analyseGroupExpression() throws CompileError{
        String Type = "";

        expect(TokenType.L_Paren);
        stack.push(TokenType.L_Paren);

        Type = analyseExpression();
        expect(TokenType.R_Paren);

        while (stack.peek() != TokenType.L_Paren) {
            TokenType type = stack.pop();
            Instruction.AddToInstructionListInt(type, InstructionList);
        }
        return Type;
    }

    /**
     * 语句：
     * stmt ->
     *       expr_stmt | decl_stmt | if_stmt | while_stmt | break_stmt |
     *       continue_stmt | return_stmt | block_stmt | empty_stmt
     *
     *let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
     *const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
     *decl_stmt -> let_decl_stmt | const_decl_stmt
     * */

    private void analyseStatement(String Type) throws CompileError {
        //throw new Error("Not implemented");
        Token var = peek();
        if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_Paren)||isLiteralExpr()){

            analyseExpression();
            //将栈中剩余的操作符弹出
            while (!stack.empty()) {
                TokenType tokenType = stack.pop();
                Instruction.AddToInstructionListInt(tokenType, InstructionList);
            }

            expect(TokenType.SEMICOLON);
        }else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            analyseDeclStmt();
        }else if(check(TokenType.IF_KW)){
            analyseIfStmt(Type);
        }else if(check(TokenType.WHILE_KW)){
            analyseWhileStmt(Type);
        }else if(check(TokenType.BREAK_KW)){
            analyseBreakStmt();
        }else if(check(TokenType.CONTINUE_KW)){
            analyseContinueStmt();
        }else if(check(TokenType.RETURN_KW)){
            analyseReturnStmt(Type);
        }else if(check(TokenType.L_BRACE)){
            analyseBlockStmt(Type);
        }else if(check(TokenType.SEMICOLON)){
            analyseEmptyStmt();
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput, var.getStartPos());
        }
    }

    /**
     * let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
     * const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
     * decl_stmt -> let_decl_stmt | const_decl_stmt
     */

    private void analyseDeclStmt()throws CompileError{
        if(check(TokenType.LET_KW)){
            expect(TokenType.LET_KW);
           Token tmp = expect(TokenType.IDENT);

           /* 判断该变量的名称是否已经被使用, 可能为函数名、变量或函数参数 */
           if(AuxiliaryFunction.isDefinedSymbol(FunctionTable, SymbolTable, level, tmp.getValueString())||
                   AuxiliaryFunction.isParameter(params, tmp.getValueString())!=null){
               throw new AnalyzeError(ErrorCode.DuplicateDeclaration, tmp.getStartPos());
           }

            expect(TokenType.COLON);
            String ty = analyseTy();
            if(!ty.equals("int")&&!ty.equals("double")){
                throw new AnalyzeError(ErrorCode.NotValidReturn, tmp.getStartPos());
            }
            /* 将该变量填入符号表，分为全局和局部 */
            /* 全局 */
            if(level==0){
                SymbolTable.add(new Symbol(0, 0, tmp.getValueString(), level, ty, globalOffset));
                GlobalTable.add( new GlobalDef(tmp.getValueString(), 0));
            }
            /* 局部 */
            else{
                SymbolTable.add(new Symbol(0, 0, tmp.getValueString(), level, ty, loc_slots));
            }

            if(check(TokenType.ASSIGN)){
                expect(TokenType.ASSIGN);
                /* 将该变量赋值 */
                AuxiliaryFunction.initializeSymbol(SymbolTable, tmp.getValueString(), level, tmp.getStartPos());
                if(level == 0){
                    GlobalInstructionList.add(new Instruction(Operation.globa,globalOffset,4));
                }else{
                    InstructionList.add(new Instruction(Operation.loca,loc_slots,4));
                }
                 String Type = analyseExpression();

                /* 判断表达式的返回类型是否符合函数 */
                if(!Type.equals(ty)){
                    throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
                }
                while (!stack.empty()) {
                    TokenType tokenType = stack.pop();
                    Instruction.AddToInstructionListInt(tokenType, InstructionList);
                }

                if(level==0) {
                    GlobalInstructionList.add(new Instruction(Operation.store));
                }else{
                    InstructionList.add(new Instruction(Operation.store));
                }

            }
            expect(TokenType.SEMICOLON);

        }else if(check(TokenType.CONST_KW)){
            expect(TokenType.CONST_KW);
            Token tmp = expect(TokenType.IDENT);

            /* 判断该变量的名称是否已经被使用 */
            if(AuxiliaryFunction.isDefinedSymbol(FunctionTable, SymbolTable, level, tmp.getValueString())){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, tmp.getStartPos());
            }

            expect(TokenType.COLON);
            String ty = analyseTy();

            expect(TokenType.ASSIGN);

            /* 将该变量填入符号表，分为全局和局部 */
            /* 全局 */
            if(level==0){
                SymbolTable.add(new Symbol(1, 1, tmp.getValueString(), level, ty, globalOffset));
                GlobalTable.add(new GlobalDef(tmp.getValueString(),1));
                GlobalInstructionList.add(new Instruction(Operation.globa,globalOffset,4));
            }
            /* 局部 */
            else{
                SymbolTable.add(new Symbol(1, 1, tmp.getValueString(), level, ty, loc_slots));
                InstructionList.add(new Instruction(Operation.loca,loc_slots,4));
            }

            String Type = analyseExpression();

            /* 判断表达式的返回类型是否符合函数 */
            if(!Type.equals(ty)){
                throw new AnalyzeError(ErrorCode.InvalidAssignment, tmp.getStartPos());
            }

            while (!stack.empty()) {
                TokenType tokenType = stack.pop();
                Instruction.AddToInstructionListInt(tokenType, InstructionList);
            }

            InstructionList.add(new Instruction(Operation.store));

            expect(TokenType.SEMICOLON);
        }

        /* 执行结束，将使用过的偏移增加，供下次使用 */
        if(level == 0){
            globalOffset++;
        }else{
            loc_slots++;
        }
    }


    /**
     * if_stmt -> 'if' expr block_stmt
     *            ('else' 'if' expr block_stmt)* ('else' block_stmt)?
     * */
    private void analyseIfStmt(String Type) throws CompileError{
        expect(TokenType.IF_KW);
        analyseExpression();
        //弹栈
        while (!stack.empty()) {
            TokenType tokenType = stack.pop();
            Instruction.AddToInstructionListInt(tokenType, InstructionList);
        }

        //满足，跳过br进行语句块中的指令
        InstructionList.add(new Instruction(Operation.brTrue, 1,4));
        //不满足，通过br跳转
        Instruction jump_ifInstruction = new Instruction(Operation.br, 0,4);
        InstructionList.add(jump_ifInstruction);
        int if_start = InstructionList.size();

        analyseBlockStmt(Type);

        /* todo:ret指令对跳转有影响吗？（应该是不影响的） */
        /* 当if成立时，可能需要通过jumpInstruction直接跳过else  */
        Instruction jump_elseInstruction = new Instruction(Operation.br, 0,4);
        InstructionList.add(jump_elseInstruction);
        int else_start = InstructionList.size();

        /* 当if不成立时，跳转到else if、else语句开始处，直接跳过if */
        int jump = InstructionList.size() - if_start;
        jump_ifInstruction.setX(jump);

        if (check( TokenType.ELSE_KW)) {
            expect(TokenType.ELSE_KW);
            if (check(TokenType.IF_KW))
                analyseIfStmt(Type);
            else {
                analyseBlockStmt(Type);
                /* else语句，跳转0，直接执行下一段语句即可 */
                InstructionList.add(new Instruction(Operation.br,0,4));
            }
        }

        /* 将跳转的数值设为所有的else if、else的指令数目 */
        jump = InstructionList.size() - else_start;
        jump_elseInstruction.setX(jump);

    }

    /**
     * while_stmt -> 'while' expr block_stmt
     * */
    private void analyseWhileStmt(String Type) throws CompileError{
        expect(TokenType.WHILE_KW);

        /* 语句进行前的指令集长度 */
        int size_first = InstructionList.size();

        analyseExpression();

        while (!stack.empty()) {
            TokenType tokenType = stack.pop();
            Instruction.AddToInstructionListInt(tokenType, InstructionList);
        }


        /* 当表达式计算结果满足，brTure,跳过br即可 */
        InstructionList.add(new Instruction(Operation.brTrue,1,4));
        /* 不满足，无条件跳转，br，其中br跳转的值需要后续计算 */
        Instruction br = new Instruction(Operation.br,0,4);
        InstructionList.add(br);

        /* while开始执行时的指令集长度 */
        int size_second = InstructionList.size();

        /* 分析block_stmt */
        analyseBlockStmt(Type);

        /* 跳转回while语句最初的位置 */
        Instruction comeBack = new Instruction(Operation.br, 0,4);
        InstructionList.add(comeBack);
        /* 语句进行后的指令集长度 */
        int size_third = InstructionList.size();

        /* 回到最开始、跳过while的偏移 */
        int back = size_first - size_third;
        int jump = size_third - size_second;

        comeBack.setX(back);
        br.setX(jump);

    }

    /**
     * break_stmt -> 'break' ';'
     * */
    private void analyseBreakStmt() throws CompileError{
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
    }

    /**
     * continue_stmt -> 'continue' ';'
     * */
    private void analyseContinueStmt() throws CompileError{
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
    }

    /**
     * return_stmt -> 'return' expr? ';'
     * */

    private void analyseReturnStmt(String Type) throws CompileError{
        expect(TokenType.RETURN_KW);
        Token var = peek();

        if(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_Paren)||isLiteralExpr()){
            if((Type.equals("int")||Type.equals("double"))){
                InstructionList.add(new Instruction(Operation.arga,0,4));
                returnType = analyseExpression();
                if(!Type.equals(returnType)){
                    throw new AnalyzeError(ErrorCode.NotValidReturn, var.getStartPos());
                }
                while (!stack.empty()) {
                    Instruction.AddToInstructionListInt(stack.pop(), InstructionList);
                }
                InstructionList.add(new Instruction(Operation.store));
                haveReturn = true ;
            }else{
                throw new AnalyzeError(ErrorCode.NotValidReturn, var.getStartPos());
            }
        }
        expect(TokenType.SEMICOLON);
        while (!stack.empty()) {
            Instruction.AddToInstructionListInt(stack.pop(), InstructionList);
        }
        InstructionList.add(new Instruction(Operation.ret));

    }

    /**
     * block_stmt -> '{' stmt* '}'
     * */
    private void analyseBlockStmt(String Type) throws CompileError{
        expect(TokenType.L_BRACE);
        /* 进入block_stmt,层数增加 */
        level++;
        while(check(TokenType.MINUS)||check(TokenType.IDENT)||check(TokenType.L_Paren)||isLiteralExpr()||
                check(TokenType.LET_KW)||check(TokenType.CONST_KW)||check(TokenType.IF_KW)||check(TokenType.WHILE_KW)
                ||check(TokenType.BREAK_KW)||check(TokenType.CONTINUE_KW)||check(TokenType.RETURN_KW)||check(TokenType.L_BRACE)||
                check(TokenType.SEMICOLON)){
            analyseStatement(Type);
        }
        expect(TokenType.R_BRACE);
        /* 函数分析完毕，将该层的局部变量全部删除 */
        AuxiliaryFunction.clearSymbolTable(SymbolTable, level);
        /* block_stmt分析完成 */
        level--;
    }

    /**
     * empty_stmt -> ';'
     * */
    private void analyseEmptyStmt() throws CompileError{
        expect(TokenType.SEMICOLON);
    }

    /**
     * 函数部份
     * function_param -> 'const'? IDENT ':' ty
     * function_param_list -> function_param (',' function_param)*
     * function -> 'fn' IDENT '(' function_param_list? ')' '->' ty block_stmt
     * */

    //finish (maybe)
    private void analyseFunction() throws CompileError{
        /* 开始前，将各判断函数初始化 */
        initJudgeVar();

        /* 开始分析 */
        expect(TokenType.FN_KW);

        Token tmp=expect(TokenType.IDENT);

        expect(TokenType.L_Paren);

        /* 如果存在参数，获得参数列表 */
        if(check(TokenType.CONST_KW)||check(TokenType.IDENT)){
           params = analyseFunctionParamList();
        }

        expect(TokenType.R_Paren);
        expect(TokenType.ARROW);

        /* 得到返回类型 */
        String type = analyseTy();

        /* 当判断为void时，函数语句中不应该有返回 */
        if(type.equals("void")){isVoid = true;}

        /* 函数返回情况正常 */
        if(!isVoid){
            return_slots = 1;
            paramOffset = 1; //由于具有返回值，因此参数从1开始偏移，否则从0开始偏移
        }else{
            return_slots = 0;
        }

        if (AuxiliaryFunction.isFunction(GlobalTable, tmp.getValueString())){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, tmp.getStartPos());
        }
        FunctionDef function = new FunctionDef(0,return_slots,params.size(),0,null, functionId, type, tmp.getValueString(), AuxiliaryFunction.ChangeToBinary(tmp.getValueString()), params);

        /* 添加FunctionDef */
        FunctionTable.put(function.getName(), function);

        /*
        *  进入block stmt
        *  传入Type,用于后续判断语句返回类型是否符合函数需求
        * */
        /* Block_stmt中会修改loc_slots变量 */
        analyseBlockStmt(type);


        /* 返回类型和函数类型不同 | 函数为void时有返回 | 函数不为void时没有返回 */
        if((!returnType.equals(type)) || isVoid&&haveReturn || ((!isVoid)&&(!haveReturn))){
           // System.out.println(returnType);
            //System.out.println(type);
            throw new AnalyzeError(ErrorCode.NotValidReturn, tmp.getEndPos());
        }

        if (type.equals("void")) {
            InstructionList.add(new Instruction(Operation.ret));
        }

        function.setOffset(globalOffset);
        function.setLoc_slots(loc_slots);
        function.setInstructions(InstructionList);

        /* 将函数添加至global中 */
        AuxiliaryFunction.addGlobalTable(GlobalTable, 1, tmp.getValueString(), tmp.getStartPos());
        /* 添加一个Function，产生偏移 */
        globalOffset++;
        functionId++;

        /* 当前函数分析结束，准备下一次函数分析 */
        initJudgeVar();
    }

     // function_param -> 'const'? IDENT ':' ty
    //finish
    private Parameter analyseFunctionParam() throws CompileError{
        if(check(TokenType.CONST_KW)){
            expect(TokenType.CONST_KW);
        }
        Token name = expect(TokenType.IDENT);
        expect(TokenType.COLON);
        String Ty = analyseTy();
        //均符合，返回参数param
        return new Parameter(Ty, name.getValueString());
    }

    //返回参数列表
    //finish
    private List<Parameter> analyseFunctionParamList() throws CompileError{
        List<Parameter> tmp = new ArrayList<>();
        tmp.add(analyseFunctionParam());
        while(check(TokenType.COMMA)){
            Token var = expect(TokenType.COMMA);
            Parameter param = analyseFunctionParam();
            //判断变量是否再列表中存在同名
            if(AuxiliaryFunction.isRepeatParam(tmp, param)){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, var.getEndPos());
            }else {
                tmp.add(param);
            }
        }
        return tmp;
    }


    /**
     * 程序部份
     * program -> item*
     * */

    //todo(需要添加最开始的函数内容)
    public void analyseProgram() throws CompileError {
        while(check(TokenType.FN_KW)||check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            analyseItem();
        }
        /* 查找main函数 */
        FunctionDef main = FunctionTable.get("main");
        if(main==null){
            throw new AnalyzeError(ErrorCode.NoMain);
        }

        GlobalTable.add(new GlobalDef("_start", 1, AuxiliaryFunction.ChangeToBinary("_start")));
        Instruction tmp = new Instruction(Operation.stackalloc, 0,4);
        GlobalInstructionList.add(tmp);
        if(main.getType().equals("int") ||main.getType()=="double"){
            tmp.setX(1);
            GlobalInstructionList.add(new Instruction(Operation.call, functionId - 1,4));
            GlobalInstructionList.add(new Instruction(Operation.popn, 1,4));
        }else{
            GlobalInstructionList.add(new Instruction(Operation.call, functionId - 1,4));
        }
        FunctionTable.put("_start", new FunctionDef(globalOffset, 0, 0, 0, GlobalInstructionList,
                0, "void","_start", AuxiliaryFunction.ChangeToBinary("_start"), null));
        globalOffset++;
    }
    /**
     * item -> function | decl_stmt
     */
    //finish
    private void analyseItem() throws CompileError {
        Token var = peek();
        if(check(TokenType.FN_KW)){
            analyseFunction();
        }else if(check(TokenType.LET_KW)||check(TokenType.CONST_KW)){
            analyseDeclStmt();
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput, var.getStartPos());
        }
    }

    /**
     * ty -> IDENT
     */
    //finish
    private String analyseTy() throws CompileError{
        Token var = expect(TokenType.IDENT);
        if (var.getValue().equals("int")||var.getValue().equals("void")||var.getValue().equals("double")){
            return var.getValue().toString();
        }else{
            throw new AnalyzeError(ErrorCode.InvalidInput, var.getStartPos());
        }
    }


    /* 判断是否为运算符 */
    private boolean isBinaryOperator() throws TokenizeError {
        if(check(TokenType.PLUS)||check(TokenType.MINUS)||
                check(TokenType.MUL)||check(TokenType.DIV)||
                check(TokenType.ASSIGN)||check(TokenType.EQ)||
                check(TokenType.NEQ)||check(TokenType.LT)||check(TokenType.GT)
                ||check(TokenType.LE)||check(TokenType.GE)) {
            return true;
        }
        return false;
    }
    /* 判断是否为字面量 */
    private boolean isLiteralExpr() throws TokenizeError {
        if(check(TokenType.UINT_LITERAL)||check(TokenType.STRING_LITERAL)||
                check(TokenType.DOUBLE_LITERAL)||check(TokenType.CHAR_LITERAL)) {
            return true;
        }
        return false;
    }

    /**
     * 每定义一个函数之前，将全局中的判断变量及指令集初始化
     * */
    private void initJudgeVar(){
        isVoid = false;
        haveReturn = false;
        returnType = "void";
        InstructionList = new ArrayList<>();
        paramOffset = 0;
        loc_slots = 0;
        return_slots = 0;
        params = new ArrayList<>();
    }

    public HashMap<String, FunctionDef> getFunctionTable() {
        return FunctionTable;
    }

    public List<GlobalDef> getGlobalTable() {
        return GlobalTable;
    }

    public static void main(String[] args) throws IOException, CompileError {


//            String a = "1.0e10";
//            double b = Double.parseDouble(a);
//            long l = Double.doubleToLongBits(b);
//            System.out.println(b);
//            System.out.println(Long.toBinaryString(l));


            InputStream inputStream = new FileInputStream(args[0]);
            //InputStream outputStream = new FileInputStream(args[1]);
            Scanner scanner = new Scanner(inputStream);

            var iter = new StringIter(scanner);
            iter.readAll();
            System.out.println(iter.getLinesBuffer());
            Analyser tmp = new Analyser(new Tokenizer(iter));

            tmp.analyseProgram();
//
            for (GlobalDef globalDef : tmp.getGlobalTable()) {
                System.out.println(globalDef);
            }

            List<Map.Entry<String, FunctionDef>> FunctionList = new ArrayList<Map.Entry<String, FunctionDef>>(tmp.getFunctionTable().entrySet());


            /* FunctionId 升序排列 */
            Collections.sort(FunctionList, new Comparator<Map.Entry<String, FunctionDef>>() {
                public int compare(Map.Entry<String, FunctionDef> o1, Map.Entry<String, FunctionDef> o2) {
                    return (o1.getValue().getFunctionId() - o2.getValue().getFunctionId());
                }
            });

            for (Map.Entry<String, FunctionDef> functionDef : FunctionList) {
                System.out.println(functionDef.getValue().getName());
                System.out.println(functionDef);
            }

            BC output = new BC(tmp.getGlobalTable(), FunctionList);
//            System.out.println();
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(args[1])));
            List<Byte> bytes = output.getBcOut();
            byte[] resultBytes = new byte[bytes.size()];
            StringBuilder test = new StringBuilder();
            for (int i = 0; i < bytes.size(); ++i) {
                resultBytes[i] = bytes.get(i);
                test.append(bytes.get(i).toString());
            }
            out.write(resultBytes);
            System.out.println(bytes);
        }


}
