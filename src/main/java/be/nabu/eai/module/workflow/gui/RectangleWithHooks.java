package be.nabu.eai.module.workflow.gui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
	
	private Rectangle rectangle;
	
	public RectangleWithHooks(double x, double y, double width, double height, Paint paint) {
		rectangle = new Rectangle(x, y, width, height);
		rectangle.setFill(paint);

		topAnchorX.bind(rectangle.layoutXProperty().add(rectangle.widthProperty().divide(2)));
		topAnchorY.bind(rectangle.layoutYProperty());
		
		rightAnchorX.bind(rectangle.layoutXProperty().add(rectangle.widthProperty()));
		rightAnchorY.bind(rectangle.layoutYProperty().add(rectangle.heightProperty().divide(2)));
		
		bottomAnchorX.bind(rectangle.layoutXProperty().add(rectangle.widthProperty().divide(2)));
		bottomAnchorY.bind(rectangle.layoutYProperty().add(rectangle.heightProperty()));
		
		leftAnchorX.bind(rectangle.layoutXProperty());
		leftAnchorY.bind(rectangle.layoutYProperty().add(rectangle.heightProperty().divide(2)));
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
	
	public Rectangle getRectangle() {
		return rectangle;
	}
}
