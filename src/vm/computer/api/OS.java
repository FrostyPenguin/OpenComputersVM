//package vm.computer.api;
//
//import li.cil.repack.com.naef.jnlua.LuaState;
//import vm.computer.Machine;
//
//public class OS {
//	public OS(Machine machine) {
//        machine.lua.getGlobal("os");
//        
//		machine.lua.pushJavaFunction(args -> {
//
//
//            machine.lua.pushString("%H:%M");
//            machine.lua.pushNumber(System.currentTimeMillis() / 1000d);
//            
//            machine.lua.getGlobal("os");
//            machine.lua.getField(1, "date");
//		    
//		    machine.lua.call(2, 1);
//		    
//		    return 1;
//        });
//        machine.lua.setField(-2, "date");
//		
//		machine.lua.pop(1);
//	}
//}
