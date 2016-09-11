package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BasePortableGUIManager;
import be.nabu.eai.developer.managers.util.MovablePane;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.module.services.vm.VMServiceGUIManager;
import be.nabu.eai.module.workflow.gui.RectangleWithHooks;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.line.Line;
import be.nabu.jfx.control.tree.drag.MouseLocation;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class WorkflowGUIManager extends BasePortableGUIManager<Workflow, BaseArtifactGUIInstance<Workflow>> {

	static {
		URL resource = VMServiceGUIManager.class.getClassLoader().getResource("workflow.css");
		if (resource != null) {
			MainController.registerStyleSheet(resource.toExternalForm());
		}
	}
	
	private Map<String, RectangleWithHooks> states = new HashMap<String, RectangleWithHooks>();
	private Map<String, Line> transitions = new HashMap<String, Line>();
	private AnchorPane drawPane;
	private Line draggingLine;
	
	public WorkflowGUIManager() {
		super("Workflow", Workflow.class, new WorkflowManager());
	}

	@Override
	public void display(MainController controller, AnchorPane pane, Workflow artifact) throws IOException, ParseException {
		SplitPane split = new SplitPane();

		VBox vbox = new VBox();
		split.setOrientation(Orientation.VERTICAL);
		
		drawPane = new AnchorPane();
		
		for (WorkflowState state : artifact.getConfiguration().getStates()) {
			drawState(artifact, state);
		}
		
		for (WorkflowState state : artifact.getConfiguration().getStates()) {
			Iterator<WorkflowTransition> iterator = state.getTransitions().iterator();
			while (iterator.hasNext()) {
				WorkflowTransition transition = iterator.next();
				if (!states.containsKey(transition.getTargetStateId())) {
					MainController.getInstance().notify(new ValidationMessage(Severity.ERROR, "Can not draw transition from " + state + " to " + transition.getTargetStateId()));
					iterator.remove();
					MainController.getInstance().setChanged();
				}
				else {
					drawTransition(artifact, state, transition);
				}
			}
		}
		
		// contains buttons to add actions
		HBox buttons = new HBox();
		
		Button addState = new Button("Add State");
		addState.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void handle(ActionEvent arg0) {
				Set properties = new LinkedHashSet(Arrays.asList(new Property [] {
					new SimpleProperty<String>("Name", String.class, false)
				}));
				final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
				EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Add New State", new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						String name = updater.getValue("Name");
						if (name != null) {
							WorkflowState state = new WorkflowState();
							state.setId(UUID.randomUUID().toString());
							state.setName(name);
							artifact.getConfig().getStates().add(state);
							drawState(artifact, state);
							MainController.getInstance().setChanged();
						}
					}
				});
			}
		});
		
		buttons.getChildren().addAll(addState);
		
		vbox.getChildren().addAll(buttons, drawPane);
		
		// contains the transition mapping
		AnchorPane mapPane = new AnchorPane();
		
		split.getItems().addAll(vbox, mapPane);
		
		AnchorPane.setBottomAnchor(split, 0d);
		AnchorPane.setTopAnchor(split, 0d);
		AnchorPane.setLeftAnchor(split, 0d);
		AnchorPane.setRightAnchor(split, 0d);
		pane.getChildren().add(split);
	}

	private void drawState(final Workflow workflow, final WorkflowState state) {
//		Paint.valueOf("#ddffcf"), Paint.valueOf("#195700")
		RectangleWithHooks rectangle = new RectangleWithHooks(state.getX(), state.getY(), 200, 100, "state", "correct");
//		rectangle.getPane().setManaged(false);
		rectangle.getContent().getChildren().add(new Label(state.getName()));
		
		MovablePane movable = MovablePane.makeMovable(rectangle.getContainer());
		// make sure we update position changes
		movable.xProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				state.setX(arg2.doubleValue());
				MainController.getInstance().setChanged();
			}
		});
		movable.yProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				state.setY(arg2.doubleValue());
				MainController.getInstance().setChanged();
			}
		});
		
		Scene scene = MainController.getInstance().getStage().getScene();
		rectangle.getContent().addEventHandler(MouseEvent.DRAG_DETECTED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.isControlDown()) {
					draggingLine = new Line();
					draggingLine.startXProperty().bind(rectangle.rightAnchorXProperty());
					draggingLine.startYProperty().bind(rectangle.rightAnchorYProperty());
					draggingLine.endXProperty().bind(MouseLocation.getInstance(scene).xProperty().subtract(drawPane.localToSceneTransformProperty().get().getTx()));
					draggingLine.endYProperty().bind(MouseLocation.getInstance(scene).yProperty().subtract(drawPane.localToSceneTransformProperty().get().getTy()));
					Dragboard dragboard = rectangle.getContainer().startDragAndDrop(TransferMode.LINK);
					Map<DataFormat, Object> content = new HashMap<DataFormat, Object>();
					content.put(TreeDragDrop.getDataFormat("connect"), state.getId());
					dragboard.setContent(content);
					event.consume();
					drawPane.getChildren().add(draggingLine);
				}
			}
		});
		rectangle.getContent().addEventHandler(DragEvent.DRAG_OVER, new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				Object content = event.getDragboard().getContent(TreeDragDrop.getDataFormat("connect"));
				if (content != null) {
					System.out.println(state.getId() + " accepts request from " + content);
					event.acceptTransferModes(TransferMode.ANY);
					event.consume();
				}
			}
		});
		rectangle.getContent().addEventHandler(DragEvent.DRAG_DROPPED, new EventHandler<DragEvent>() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void handle(DragEvent event) {
				Object content = event.getDragboard().getContent(TreeDragDrop.getDataFormat("connect"));
				if (content != null) {
					Set properties = new LinkedHashSet(Arrays.asList(new Property [] {
						new SimpleProperty<String>("Name", String.class, false)
					}));
					final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
					EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Add New State", new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							String name = updater.getValue("Name");
							if (name != null) {
								WorkflowTransition transition = new WorkflowTransition();
								transition.setId(UUID.randomUUID().toString());
								transition.setTargetStateId(state.getId());
								transition.setName(name);
								for (WorkflowState state : workflow.getConfig().getStates()) {
									if (state.getId().equals(content)) {
										state.getTransitions().add(transition);
										drawTransition(workflow, state, transition);
									}
								}
								MainController.getInstance().setChanged();
							}
						}
					});
					if (draggingLine != null) {
						drawPane.getChildren().remove(draggingLine);
						draggingLine = null;
					}
					event.setDropCompleted(true);
					event.consume();
				}
			}
		});
		scene.addEventHandler(DragEvent.DRAG_DONE, new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				if (draggingLine != null) {
					drawPane.getChildren().remove(draggingLine);
					draggingLine = null;
				}
			}
		});
		
		
		drawPane.getChildren().add(rectangle.getContainer());
		// TODO: on click > show properties of state on the right side
		states.put(state.getId(), rectangle);
	}
	
	private void drawTransition(final Workflow workflow, final WorkflowState state, final WorkflowTransition transition) {
		Line line = new Line();
		line.eventSizeProperty().set(10);
		// the line starts at the outgoing point of the state
		line.startXProperty().bind(states.get(state.getId()).rightAnchorXProperty());
		line.startYProperty().bind(states.get(state.getId()).rightAnchorYProperty());
		line.endXProperty().bind(states.get(transition.getTargetStateId()).leftAnchorXProperty());
		line.endYProperty().bind(states.get(transition.getTargetStateId()).leftAnchorYProperty());
		// TODO: draw all transitions, add click handlers etc
		line.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE) {
					state.getTransitions().remove(transition);
					drawPane.getChildren().remove(line);
					workflow.getMappings().remove(transition.getId());
					MainController.getInstance().setChanged();
				}
			}
		});
		line.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				line.requestFocus();
			}
		});
		line.getStyleClass().add("connectionLine");
		transitions.put(transition.getId(), line);
		drawPane.getChildren().add(line);
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
