package VM;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class ScreenWidget extends Pane {
    public ImageView imageView;
    public Label label;
    public double scale = 1;
    
    private double
        lastX = -1,
        lastY,
        currentGPUWidth,
        currentGPUHeight;
    
    public ScreenWidget() {
        label = new Label("My cool machine");
        label.setPrefHeight(20);
        label.setPadding(new Insets(0, 0, 0, 5));
        Main.addStyleSheet(label, "screenLabel.css");

        label.setOnMousePressed(event -> {
            lastX = event.getScreenX();
            lastY = event.getScreenY();
        });

        label.setOnMouseDragged(event -> {
            if (lastX >= 0) {
                setLayoutX(getLayoutX() + event.getScreenX() - lastX);
                setLayoutY(getLayoutY() + event.getScreenY() - lastY);
                
                lastX = event.getScreenX();
                lastY = event.getScreenY();
            }
        });

        label.setOnScroll(event -> {
            scale += event.getDeltaY() > 0 ? 0.05 : -0.05;
            applyScale();
        });
        
        setPadding(new Insets(40));

        imageView = new ImageView();
        imageView.setLayoutY(label.getPrefHeight());
        imageView.setPreserveRatio(false);
        imageView.setSmooth(false);
        
        Main.addStyleSheet(this, "screenWidget.css");
        this.setId("eblo");
        
        // Эффектики
//        imageView.setEffect(new Bloom(0.74));
//        setEffect(new DropShadow(BlurType.THREE_PASS_BOX, new Color(0, 0, 0, 0.6), 40, 0, 0, 8));
        
        // Добавляем говнище на экранчик
        getChildren().addAll(label, imageView);
    }
    
    private void applyScale() {
        double
            width = currentGPUWidth * scale,
            height = currentGPUHeight * scale;
        
        setPrefSize(width, height + label.getPrefHeight());

        imageView.setFitWidth(width);
        imageView.setFitHeight(height);

        label.setPrefWidth(width);
    }
    
    public void setResolution(double width, double height) {
        currentGPUWidth = width;
        currentGPUHeight = height;

        applyScale();
    }
}
