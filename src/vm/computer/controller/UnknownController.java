package vm.computer.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.Machine;

import java.util.ArrayList;
import java.util.List;


public class UnknownController {
    @FXML
    private TextArea textArea;

    @FXML
    private TextField textField;

    public Machine machine;
    public String address;
    public String type;
    private String readeText = "";

    private List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    public UnknownController(Machine machine, String address, String type){
        this.machine = machine;
        this.address = address;
        this.type = type;
    }

    @FXML
    public void initialize() {
        textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN) {
                // Navigate history down
                if (historyIndex < commandHistory.size() - 1) {
                    historyIndex++;
                    textField.setText(commandHistory.get(historyIndex));
                }
            } else if (event.getCode() == KeyCode.UP) {
                // Navigate history up
                if (historyIndex > 0) {
                    historyIndex--;
                    textField.setText(commandHistory.get(historyIndex));
                }
            }else if (event.getCode() == KeyCode.ENTER) {
                // Add the entered command to history
                String enteredCommand = textField.getText();
                if (!enteredCommand.isEmpty()) {
                    commandHistory.add(enteredCommand);
                    historyIndex = commandHistory.size();
                    handleInput();
                    textField.clear();
                }
            }
        });
        textArea.appendText("This is a console off " + type + " component who is unimplemented \n");
        textArea.appendText("type push <event> <args ...> to push event \n");
        textArea.appendText("when method called you can answer a output here \n");
    }

    private void handleInput() {
        String input = textField.getText();
        if (!input.isEmpty()) {
            // Display the input in the TextArea
            textArea.appendText("> " + input + "\n");

            // Process the input and display the response
            String response = processInput(input);
            textArea.appendText(response + "\n");
        }
    }

    private String processInput(String input) {
        if(machine.luaThread == null) return "Error : please run the computer";
        // Implement your own logic to process the input
        String[] tokens = input.split("\\s+");

        if (tokens[0].equals("push")) {
            if (tokens.length >= 2) {
                LuaState luaState = new LuaState();
                luaState.pushString(tokens[1]);
                luaState.pushString(address);
                int i = 2;
                while (i < tokens.length) {
                    if (tokens[i] == "true" || tokens[i] == "false") {
                        luaState.pushBoolean(tokens[i] == "true");
                    } else {
                        try {
                            luaState.pushNumber(Double.parseDouble(tokens[i]));
                        } catch (NumberFormatException e) {
                            luaState.pushString(tokens[i]);
                        }
                    }
                    i++;
                }
                machine.luaThread.pushSignal(luaState);
                return "Info  : Event pushed";
            } else {
                return "Error : require event name";
            }
        }if (readeText == null){
            readeText = input;
            return "Info  : Answer pushed !";
        }
        return "Error : Unknown command";
    }

    public void print(String txt) {
        textArea.appendText(txt + "\n");
    }

    public synchronized String read() throws InterruptedException {
        readeText = null;
        while (readeText == null){
            this.wait(10);
        }
        return readeText;
    }
}
