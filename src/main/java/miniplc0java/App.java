package miniplc0java;

import miniplc0java.Struct.FunctionDef;
import miniplc0java.Struct.GlobalDef;
import miniplc0java.analyser.Analyser;
import miniplc0java.analyser.BC;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Tokenizer;

import java.io.*;
import java.util.*;


public class App {
    public static void main(String[] args) throws Exception {

        try {
            String a = "1.0e10";
            double b = Double.parseDouble(a);
            long l = Double.doubleToLongBits(b);
            System.out.println(b);
            System.out.println(Long.toBinaryString(l));


            InputStream inputStream = new FileInputStream(args[0]);
            //InputStream outputStream = new FileInputStream(args[1]);
            Scanner scanner = new Scanner(inputStream);

            var iter = new StringIter(scanner);
            Analyser tmp = new Analyser(new Tokenizer(iter));
            tmp.analyseProgram();

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
            System.out.println();
            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(args[1])));
            List<Byte> bytes = output.getBcOut();
            byte[] resultBytes = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); ++i) {
                resultBytes[i] = bytes.get(i);
            }
            out.write(resultBytes);
        }catch (Exception  e){
            System.exit(-1);
        }



    }
}
