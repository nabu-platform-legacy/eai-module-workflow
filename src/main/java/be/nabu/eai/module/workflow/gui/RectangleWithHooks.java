/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.workflow.gui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

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
		container.setPrefWidth(width);
		container.setPrefHeight(height);
		container.setManaged(false);

		content = new VBox();
		for (String clazz : classes) {
			content.getStyleClass().add(clazz);
		}
		content.prefHeightProperty().bind(container.prefHeightProperty());
		content.prefWidthProperty().bind(container.prefWidthProperty());
//		content.setStyle("-fx-background-color: #ddffcf; -fx-border-color: #195700; -fx-border-width: 2px");
		
		container.getChildren().add(this.content);
				
		topAnchorX.bind(container.layoutXProperty().add(content.widthProperty().divide(2)));
		topAnchorY.bind(container.layoutYProperty());
		
		rightAnchorX.bind(container.layoutXProperty().add(content.widthProperty()));
		rightAnchorY.bind(container.layoutYProperty().add(content.heightProperty().divide(2)));
		
		bottomAnchorX.bind(container.layoutXProperty().add(content.widthProperty().divide(2)));
		bottomAnchorY.bind(container.layoutYProperty().add(content.heightProperty()));
		
		leftAnchorX.bind(container.layoutXProperty());
		leftAnchorY.bind(container.layoutYProperty().add(content.heightProperty().divide(2)));
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
