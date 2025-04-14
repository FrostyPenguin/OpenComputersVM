package vm.computer.components.base;

import org.json.JSONObject;

import javafx.event.Event;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import vm.computer.Machine;

public class ComponentWindowed extends ComponentBase{
    public Stage stage;
    public ComponentWindowed(Machine machine, String address, JSONObject obj) {
        super(machine, address, obj);

        stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);

        stage.setTitle(type + "@" + address);

        stage.setMinWidth(200);
        stage.setMinHeight(200);

        //prevent from stacking
        stage.setX(machine.xOffset);
        stage.setY(machine.yOffset);
        //stage.initOwner(machine.stage);
        machine.xOffset += 50;
        machine.yOffset += 50;

        //stage.setAlwaysOnTop(true);
        machine.stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) stage.setAlwaysOnTop(true);
            stage.setAlwaysOnTop(false);
        });

        stage.setOnCloseRequest(Event::consume);
        stage.show();
    }

    public void closeScreenWindows(){
        stage.close();
    }
}
