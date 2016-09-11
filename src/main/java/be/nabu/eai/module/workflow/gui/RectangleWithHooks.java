package be.nabu.eai.module.workflow.gui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class RectangleWithHooks {
	
	private DoubleProperty topAnchorX = new SimpleDoubleProperty(),
			topAnchorY = new SimpleDoubleProperty();
	
	private DoubleProperty rightAnchorX = new SimpleDoubleProperty(),
			rightAnchorY = new SimpleDoubleProperty();
	
	private DoubleProperty bottomAnchorX = new SimpleDoubleProperty(),
			bottomAnchorY = new SimpleDoubleProperty();
	
	private DoubleProperty leftAnchorX = new SimpleDoubleProperty(),
			leftAnchorY = new SimpleDoubleProperty();
	
	private AnchorPane container;
	private VBox content;
	
	public RectangleWithHooks(double x, double y, double width, double height, String...classes) {
		container = new AnchorPane();
		container.setLayoutX(x);
		container.setLayoutY(y);
		container.minWidthProperty().bind(container.prefWidthProperty());
		container.maxWidthProperty().bind(container.prefWidthProperty());
		container.minHeightProperty().bind(container.prefHeightProperty());
		container.maxHeightProperty().bind(container.prefHeightProperty());
		container.setPrefWidth(200);
		container.setPrefHeight(100);
		container.setManaged(false);

		content = new VBox();
		for (String clazz : classes) {
			content.getStyleClass().add(clazz);
		}
		content.prefHeightProperty().bind(container.prefHeightProperty());
		content.prefWidthProperty().bind(container.prefWidthProperty());
		content.setStyle("-fx-background-color: #ddffcf; -fx-border-color: #195700; -fx-border-width: 2px");
		
		container.getChildren().add(this.content);
				
		topAnchorX.bind(container.layoutXProperty().add(container.widthProperty().divide(2)));
		topAnchorY.bind(container.layoutYProperty());
		
		rightAnchorX.bind(container.layoutXProperty().add(container.widthProperty()));
		rightAnchorY.bind(container.layoutYProperty().add(container.heightProperty().divide(2)));
		
		bottomAnchorX.bind(container.layoutXProperty().add(container.widthProperty().divide(2)));
		bottomAnchorY.bind(container.layoutYProperty().add(container.heightProperty()));
		
		leftAnchorX.bind(container.layoutXProperty());
		leftAnchorY.bind(container.layoutYProperty().add(container.heightProperty().divide(2)));
	}
	
	public ReadOnlyDoubleProperty topAnchorXProperty() {
		return topAnchorX;
	}
	public ReadOnlyDoubleProperty topAnchorYProperty() {
		return topAnchorY;
	}
	public ReadOnlyDoubleProperty rightAnchorXProperty() {
		return rightAnchorX;
	}
	public ReadOnlyDoubleProperty rightAnchorYProperty() {
		return rightAnchorY;
	}
	public ReadOnlyDoubleProperty bottomAnchorXProperty() {
		return bottomAnchorX;
	}
	public ReadOnlyDoubleProperty bottomAnchorYProperty() {
		return bottomAnchorY;
	}
	public ReadOnlyDoubleProperty leftAnchorXProperty() {
		return leftAnchorX;
	}
	public ReadOnlyDoubleProperty leftAnchorYProperty() {
		return leftAnchorY;
	}
	
	public AnchorPane getContainer() {
		return container;
	}

	public VBox getContent() {
		return content;
	}
	
}
