package vm.computer;

import java.util.Stack;

import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;

public class Debugger {
    public Stack<String> callStack = new Stack<>();
    

    public Debugger(LuaState L){
        L.getGlobal("debug"); // Push 'debug' table onto the stack
        L.getField(-1, "sethook"); // Push 'debug.sethook' onto the stack

        // Push arguments for debug.sethook
        L.pushJavaFunction(args -> this.update(args));
        L.pushString("crl");
        L.pushInteger(0);

        // Call debug.sethook with 3 arguments and no return values
        L.call(3, 0);

        // Clean up the stack (remove 'debug' table)
        L.pop(1);
    }

    public int update(LuaState args){
        //printStack(args);
        String event  = args.checkString(1);
        switch (event) {
            case "tail call":
            case "call":
                handleCallEvent(args);
                break;
            case "return":
                handleReturnEvent(args);
                break;
            case "line":
                handleLineEvent(args);
                break;
            default:
                break;
        }
        return 0;
    }

    private void handleCallEvent(LuaState args) {
        // Use debug.getinfo to get the function name, source, and line
        args.getGlobal("debug");
        args.getField(-1, "getinfo");
        args.pushInteger(2); // Level 2 in the stack
        args.pushString("Snl"); // Request "source", "name", and "linedefined"
        args.call(2, 1); // Call debug.getinfo(level, "Snl")
    
        // Retrieve the function source
        args.getField(-1, "source");
        String functionSource = args.isString(-1) ? args.toString(-1) : "<unknown>";
        args.pop(1);
    
        if (functionSource.equals("=[C]") || functionSource.equals("=machine")) {
            // Clean up the remaining stack
            args.pop(2);
            return;
        }
    
        // Retrieve the function name
        args.getField(-1, "name");
        String functionName = args.isString(-1) ? args.toString(-1) : "<unknown>";
        args.pop(1);
    
        // Retrieve the line where the function is defined
        args.getField(-1, "linedefined");
        int lineDefined = args.isNumber(-1) ? args.toInteger(-1) : -1;
        args.pop(1);
    
        // Clean up the stack: remove the info table and debug table
        args.pop(2);
        
        callStack.push(String.format("'%s' defined at '%s' on line %d", functionName, functionSource, lineDefined));
    }
    
    
    

    private void handleReturnEvent(LuaState args) {
        if(callStack.isEmpty()) return;
        try{
        args.getGlobal("debug");
        args.getField(-1, "getinfo");
        args.pushInteger(2); 
        args.pushString("S");
        args.call(2, 1); // Call debug.getinfo(level, "S")

        args.getField(-1, "source");
        String functionSource = args.isString(-1) ? args.toString(-1) : "<unknown>";
        args.pop(3);
    
        if (functionSource.equals("=[C]") || functionSource.equals("=machine")) return;
        callStack.pop();
        }catch(Error e){
            System.out.println("Debug fail !");
        }
    }

    private void handleLineEvent(LuaState args) {
        //System.out.println("line triggered");

        // Print the current line number
        //int currentLine = ((int)args.checkNumber(2));
        //System.out.println("Executing Line: " + currentLine);
    }

    public void printStack(LuaState L){
		int i;
		int top = L.getTop();
		System.out.println("---- Begin Stack ----");
		System.out.print("Stack size: ");
		System.out.println(top);
		for(i = top; i >= 1; i--){
			LuaType t = L.type(i);
			switch (t) {
				case STRING:
					System.out.printf("%d -- (%d) ---- `%s'", i, i - (top + 1), L.toString(i));
					break;
				case BOOLEAN:
					System.out.printf("%d -- (%d) ---- `%s'", i, i - (top + 1), L.toBoolean(i) ? "true" : "false");
					break;
				case NUMBER:
					System.out.printf("%d -- (%d) ---- `%lf'", i, i - (top + 1), L.toNumber(i));
					break;
				default:
					System.out.printf("%d -- (%d) ---- `%lf'", i, i - (top + 1), L.typeName(i));
					break;
			}
			System.out.println();
		}
		System.out.println("---- End Stack ----");
	}

    
}
