package miniplc0java.Struct;

import java.util.List;

public class GlobalDef {
    String name;
    int isConst;
    int valueCount;
    List<Character> values;

    public GlobalDef(String name,int isConst) {
        this.name = name;
        this.isConst = isConst;
        this.valueCount = 0;
        this.values = null;
    }


    public GlobalDef(String name,int isConst,List<Character> values) {
        this.name = name;
        this.isConst = isConst;
        this.valueCount = values.size();
        this.values = values;
    }

    public int getIsConst() {
        return isConst;
    }

    public int getValueCount() {
        return valueCount;
    }

    public List<Character> getValue() {
        return values;
    }

    public void setIsConst(int isConst) {
        this.isConst = isConst;
    }

    public void setValue(List<Character> value) {
        this.values = value;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Global{" +
                "isConst=" + isConst +
                ", valueCount=" + valueCount +
                ", valueItems=" + values +
                '}';
    }
}
