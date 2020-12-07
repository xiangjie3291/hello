package miniplc0java.Struct;

/* 参数 */
public class Parameter {
    String Type; //int|void|double
    String Name; //命名

    public Parameter(String ty, String valueString) {
        this.Type = ty;
        this.Name = valueString;
    }

    public String getName() {
        return Name;
    }

    public String getType() {
        return Type;
    }
}
