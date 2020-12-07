package miniplc0java.Struct;

import miniplc0java.tokenizer.TokenType;

/* 符号表 */
public class Symbol {
    int isConstant;//是否为常量
    int isInitialized;//是否初始化

    String name;
    /**
     * level层次为0时即为全局变量
     * */
    int level; //嵌套层次
    String Type; //变量或常量的类型(int|void|double|string)
    int offset; //在栈中的偏移
    /**
     * @param isConstant
     * @param level
     * @param Type
     */
    public Symbol(int isConstant, int isInitialized , String name,int level ,String Type ,int offset) {
        this.isConstant = isConstant;
        this.isInitialized = isInitialized;
        this.name = name;
        this.level = level;
        this.Type = Type;
        this.offset = offset;
    }

    /**
     * @return the isConstant
     */
    public int isConstant() {
        return isConstant;
    }

    /**
     * @return the isInitialized
     */
    public int isInitialized() {
        return isInitialized;
    }

    public int getLevel(){
        return level;
    }

    public String getType() {
        return Type;
    }

    /**
     * @param isConstant the isConstant to set
     */
    public void setConstant(int isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(int isInitialized) {
        this.isInitialized = isInitialized;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOffset() {
        return offset;
    }
}
