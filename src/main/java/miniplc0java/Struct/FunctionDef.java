package miniplc0java.Struct;

import miniplc0java.instruction.Instruction;

import java.util.List;

public class FunctionDef {
    int offset; //在全局变量中的位置
    int return_slots;
    int param_slots;
    int loc_slots;
    List<Instruction> Instructions;

    int FunctionId; //函数的标号
    String type; //函数返回值类型
    String name; //函数名
    List<Character> names; //函数名字节格式
    List<Parameter> parameters; //变量列表

    public FunctionDef(int offset, int returnSlots, int param_slots ,int localSlots, List<Instruction> Instructions ,
                       int FunctionId ,String type,String name, List<Character> names ,List<Parameter> parameters) {
        this.offset = offset;
        this.return_slots = returnSlots;
        this.param_slots = param_slots;
        this.loc_slots = localSlots;
        this.Instructions = Instructions;
        this.FunctionId = FunctionId;
        this.type = type;
        this.name = name;
        this.names = names;
        this.parameters = parameters;
    }

    public int getOffset() {
        return offset;
    }

    public int getLoc_slots() {
        return loc_slots;
    }

    public int getParam_slots() {
        return param_slots;
    }


    public int getReturn_slots() {
        return return_slots;
    }

    public List<Character> getNames() {
        return names;
    }

    public List<Instruction> getInstructions() {
        return Instructions;
    }

    public String getType() {
        return type;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public int getFunctionId() {
        return FunctionId;
    }

    public String getName() {
        return name;
    }

    public void setInstructions(List<Instruction> instructions) {
        Instructions = instructions;
    }

    public void setLoc_slots(int loc_slots) {
        this.loc_slots = loc_slots;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setParam_slots(int param_slots) {
        this.param_slots = param_slots;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFunctionId(int functionId) {
        FunctionId = functionId;
    }

    public void setReturn_slots(int return_slots) {
        this.return_slots = return_slots;
    }

    public void setNames(List<Character> names) {
        this.names = names;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public String toString() {
        return "FunctionDef{\n" +
                "   id=" + offset +
                ",\n    returnSlots=" + return_slots +
                ",\n    paramSlots=" + param_slots +
                ",\n    localSlots=" + loc_slots +
                "    body=" + Instructions +'\n'+
                '}';
    }
}
