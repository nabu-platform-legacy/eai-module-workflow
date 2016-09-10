package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BasePortableGUIManager;
import be.nabu.eai.module.workflow.gui.RectangleWithHooks;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.line.Line;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class WorkflowGUIManager extends BasePortableGUIManager<Workflow, BaseArtifactGUIInstance<Workflow>> {

	private Map<WorkflowState, RectangleWithHooks> states = new HashMap<WorkflowState, RectangleWithHooks>();
	private Map<WorkflowTransition, Line> transitions = new HashMap<WorkflowTransition, Line>();
	
	public WorkflowGUIManager() {
		super("Workflow", Workflow.class, new WorkflowManager());
	}

	@Override
	public void display(MainController controller, AnchorPane pane, Workflow artifact) throws IOException, ParseException {
		SplitPane split = new SplitPane();

		VBox vbox = new VBox();
		split.setOrientation(Orientation.VERTICAL);
		
		// contains all the actions and lines
		AnchorPane drawPane = new AnchorPane();
		
		for (WorkflowState state : artifact.getConfiguration().getStates()) {
			drawState(state);
		}
		
		for (WorkflowState state : artifact.getConfiguration().getStates()) {
			for (WorkflowTransition transition : state.getTransitions()) {
				Line line = new Line();
				// the line starts at the outgoing point of the state
				line.startXProperty().bind(states.get(state).rightAnchorXProperty());
				line.startYProperty().bind(states.get(state).rightAnchorYProperty());
				// TODO: draw all transitions, add click handlers etc
			}
		}
		
		// contains buttons to add actions
		HBox buttons = new HBox();
		
		vbox.getChildren().addAll(buttons, drawPane);
		
		// contains the transition mapping
		AnchorPane mapPane = new AnchorPane();
		
		split.getItems().addAll(vbox, drawPane, mapPane);
		
		
		AnchorPane.setBottomAnchor(split, 0d);
		AnchorPane.setTopAnchor(split, 0d);
		AnchorPane.setLeftAnchor(split, 0d);
		AnchorPane.setRightAnchor(split, 0d);
		pane.getChildren().add(split);
	}

	private void drawState(final WorkflowState state) {
		RectangleWithHooks rectangle = new RectangleWithHooks(state.getX(), state.getY(), 200, 100, Paint.valueOf("#EAEAEA"));
		// make sure we update position changes
		rectangle.getRectangle().layoutXProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				state.setX(arg2.doubleValue());
				MainController.getInstance().setChanged();
			}
		});
		rectangle.getRectangle().layoutYProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				state.setY(arg2.doubleValue());
				MainController.getInstance().setChanged();
			}
		});
		states.put(state, rectangle);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected BaseArtifactGUIInstance<Workflow> newGUIInstance(Entry entry) {
		return new BaseArtifactGUIInstance<Workflow>(this, entry);
	}

	@Override
	protected void setEntry(BaseArtifactGUIInstance<Workflow> guiInstance, ResourceEntry entry) {
		guiInstance.setEntry(entry);
	}

	@Override
	protected Workflow newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new Workflow(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	protected void setInstance(BaseArtifactGUIInstance<Workflow> guiInstance, Workflow instance) {
		guiInstance.setArtifact(instance);
	}
	
	@Override
	public String getCategory() {
		return "Workflow";
	}
}
