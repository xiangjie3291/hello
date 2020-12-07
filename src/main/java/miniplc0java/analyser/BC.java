package miniplc0java.analyser;

import miniplc0java.Struct.FunctionDef;
import miniplc0java.Struct.GlobalDef;
import miniplc0java.instruction.Instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BC{
    List<GlobalDef> globals;
    List<Map.Entry<String, FunctionDef>> functions;
    List<Byte> BcOut;

    public BC (List<GlobalDef> globals , List<Map.Entry<String, FunctionDef>> functions){
        this.globals = globals;
        this.functions = functions;
        this.BcOut = new ArrayList<>();
        BcOut.addAll(IntToBytes(0x72303b3e)); //magic
        BcOut.addAll(IntToBytes(0x00000001)); //version
        BcAddGlobals(); //globals
        BcAddFunctions(); //functions
    }

    public List<Byte> getBcOut() {
        return BcOut;
    }

    private void BcAddGlobals() {
        BcOut.addAll(IntToBytes(globals.size())); //globalCounts
        for (GlobalDef global : globals) {
            BcOut.add(ByteIntToBytes(global.getIsConst()));//is_const: u8
            List<Byte> globalValueCount;// value count
            List<Byte> globalValue;//value items
            //不存在全局名称,为变量或常量,64位的0,八个字节
            if (global.getValueCount() == 0) {
                globalValueCount = IntToBytes( 8);
                globalValue = LongToBytes( 0);
            }
            //存在，为函数
            else {
                globalValue = ListCharToBytes(global.getValue());
                globalValueCount = IntToBytes(globalValue.size());
            }
            BcOut.addAll(globalValueCount);
            BcOut.addAll(globalValue);
        }
    }

    private void BcAddFunctions() {
        List<Byte> functionsCount=IntToBytes( functions.size());
        BcOut.addAll(functionsCount);
        for(Map.Entry<String, FunctionDef> functionDef:functions){
            BcOut.addAll(IntToBytes(functionDef.getValue().getFunctionId()));//name: u32
            BcOut.addAll(IntToBytes(functionDef.getValue().getReturn_slots()));//return_slots: u32
            BcOut.addAll(IntToBytes(functionDef.getValue().getParam_slots()));//param_slots: u32
            BcOut.addAll(IntToBytes(functionDef.getValue().getLoc_slots()));//loc_slots: u32

            //函数体
            List<Instruction> instructions = functionDef.getValue().getInstructions();
            BcOut.addAll(IntToBytes(instructions.size())); //functions.count

            //Array<Instruction>
            for(Instruction instruction : instructions){
                BcOut.add(ByteIntToBytes(instruction.getOpt()));// opcode: u8
                if(instruction.getByteNum() == 4){
                    BcOut.addAll(IntToBytes((int)instruction.getX()));// param: u32
                }
                else if(instruction.getByteNum() == 8){
                    BcOut.addAll(LongToBytes(instruction.getX())); // param: u64
                }
            }
        }

    }



    /* List<Char>转为bytes */
    private List<Byte> ListCharToBytes(List<Character> value) {
        List<Byte>  AB=new ArrayList<>();
        for (char ch : value) {
            AB.add((byte) (ch & 0xff));
        }
        return AB;
    }

    /* char转化为bytes */
    private Byte CharToBytes(char value) {
        return ((byte)(value&0xff));
    }


    /* 将字符串转化为字节码 */
    private List<Byte> StringToBytes(String valueString) {
        List<Byte>  AB=new ArrayList<>();
        for (int i=0;i<valueString.length();i++){
            char ch=valueString.charAt(i);
            AB.add((byte)(ch&0xff));
        }
        return AB;
    }


    /* 8位整数，截断一次 */
    private Byte ByteIntToBytes(int target) {
        return (byte) (target  & 0xFF );
    }

    /* int类型转化为bytes，截断4次 */
    private List<Byte> IntToBytes(int target) {
        ArrayList<Byte> bytes = new ArrayList<>();
        int start = 8 * 3;
        for(int i = 0 ; i < 4; i++){
            bytes.add((byte) (( target >> ( start - i * 8 )) & 0xFF ));
        }
        return bytes;
    }


    /* long类型转化为bytes，截断8次 */
    private List<Byte> LongToBytes(long target) {
        ArrayList<Byte> bytes = new ArrayList<>();
        int start = 8 * 7;
        for(int i = 0 ; i < 8; i++){
            bytes.add((byte) (( target >> ( start - i * 8 )) & 0xFF ));
        }
        return bytes;
    }




}