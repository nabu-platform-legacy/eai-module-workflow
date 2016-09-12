package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
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
import javafx.scene.Node;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.controllers.VMServiceController;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BasePortableGUIManager;
import be.nabu.eai.developer.managers.util.MovablePane;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.module.services.vm.VMServiceGUIManager;
import be.nabu.eai.module.types.structure.StructureGUIManager;
import be.nabu.eai.module.workflow.gui.RectangleWithHooks;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.line.Line;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.jfx.control.tree.drag.MouseLocation;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.vm.Pipeline;
import be.nabu.libs.services.vm.SimpleVMServiceDefinition;
import be.nabu.libs.services.vm.api.Step;
import be.nabu.libs.services.vm.api.VMService;
import be.nabu.libs.services.vm.step.Sequence;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.Structure;
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
	private Map<String, List<Node>> transitions = new HashMap<String, List<Node>>();
	private AnchorPane drawPane;
	private Line draggingLine;
	private AnchorPane mapPane;
	
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
							DefinedStructure value = new DefinedStructure();
							value.setName("state");
							value.setId(artifact.getId() + ".types.states." + EAIRepositoryUtils.stringToField(state.getName()));
							artifact.getStructures().put(state.getId(), value);
							((WorkflowManager) getArtifactManager()).refreshChildren((ModifiableEntry) artifact.getRepository().getEntry(artifact.getId()), artifact);
							drawState(artifact, state);
							MainController.getInstance().setChanged();
						}
					}
				});
			}
		});
		
		Button editGlobalState = new Button("Edit Global State");
		editGlobalState.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (!event.isConsumed()) {
					mapPane.getChildren().clear();
					// add an editor for the transient state
					try {
						new StructureGUIManager().display(MainController.getInstance(), mapPane, artifact.getStructures().get("global"));
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
		
		buttons.getChildren().addAll(addState, editGlobalState);
		
		vbox.getChildren().addAll(buttons, drawPane);
		
		mapPane = new AnchorPane();
		
		split.getItems().addAll(vbox, mapPane);
		
		AnchorPane.setBottomAnchor(split, 0d);
		AnchorPane.setTopAnchor(split, 0d);
		AnchorPane.setLeftAnchor(split, 0d);
		AnchorPane.setRightAnchor(split, 0d);
		pane.getChildren().add(split);
	}

	private void drawState(final Workflow workflow, final WorkflowState state) {
//		Paint.valueOf("#ddffcf"), Paint.valueOf("#195700")
		RectangleWithHooks rectangle = new RectangleWithHooks(state.getX(), state.getY(), 200, 45, "state", "correct");
//		rectangle.getPane().setManaged(false);
		Label label = new Label(state.getName());
		label.getStyleClass().add("workflow-name");
		rectangle.getContent().getChildren().add(label);
		
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
		rectangle.getContent().addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mapPane.getChildren().clear();
				// add an editor for the transient state
				try {
					new StructureGUIManager().display(MainController.getInstance(), mapPane, workflow.getStructures().get(state.getId()));
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				// focus on the state for deletion if necessary
				rectangle.getContent().requestFocus();
				event.consume();
			}
		});
		rectangle.getContent().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE) {
					Confirm.confirm(ConfirmType.QUESTION, "Delete state?", "Are you sure you want to delete the state '" + state.getName() + "'", new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							// remove all transitions that reach this state
							for (WorkflowState child : workflow.getConfig().getStates()) {
								Iterator<WorkflowTransition> iterator = child.getTransitions().iterator();
								while (iterator.hasNext()) {
									WorkflowTransition next = iterator.next();
									if (state.getId().equals(next.getTargetStateId())) {
										iterator.remove();
										// the transition goes to the state-to-be-removed
										removeTransition(workflow, child, next);
									}
								}
							}
							// delete all transitions that start from this state
							Iterator<WorkflowTransition> iterator = state.getTransitions().iterator();
							while (iterator.hasNext()) {
								WorkflowTransition transition = iterator.next();
								iterator.remove();
								removeTransition(workflow, state, transition);
							}
							drawPane.getChildren().remove(states.get(state.getId()).getContainer());
							states.remove(state.getId());
							// remove the state
							workflow.getConfig().getStates().remove(state);
							// remove the structure associated with the transiens state (if any)
							workflow.getStructures().remove(state.getId());
							MainController.getInstance().setChanged();
						}
					});
				}
			}
		});
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
								
								
								for (WorkflowState child : workflow.getConfig().getStates()) {
									if (child.getId().equals(content)) {
										child.getTransitions().add(transition);

										DefinedStructure value = new DefinedStructure();
										value.setId(workflow.getId() + ".types.transitions." + EAIRepositoryUtils.stringToField(transition.getName()));
										value.setName("transition");
										workflow.getStructures().put(transition.getId(), value);
										((WorkflowManager) getArtifactManager()).refreshChildren((ModifiableEntry) workflow.getRepository().getEntry(workflow.getId()), workflow);

										transition.setX(state.getX() + ((child.getX() - state.getX()) / 2));
										transition.setY(state.getY());
										Structure input = new Structure();
										input.setName("input");
										input.add(new ComplexElementImpl("global", (ComplexType) workflow.getRepository().resolve(workflow.getId() + ".types.global"), input));
										input.add(new ComplexElementImpl("state", (ComplexType) workflow.getRepository().resolve(workflow.getId() + ".types.states." + EAIRepositoryUtils.stringToField(child.getName())), input));
										input.add(new ComplexElementImpl("transition", (ComplexType) workflow.getRepository().resolve(workflow.getId() + ".types.transitions." + EAIRepositoryUtils.stringToField(transition.getName())), input));
										
										Structure output = new Structure();
										output.setName("output");
										output.add(new ComplexElementImpl("global", (ComplexType) workflow.getRepository().resolve(workflow.getId() + ".types.global"), output));
										output.add(new ComplexElementImpl("state", (ComplexType) workflow.getRepository().resolve(workflow.getId() + ".types.states." + EAIRepositoryUtils.stringToField(state.getName())), output));
										
										Pipeline pipeline = new Pipeline(input, output);
										SimpleVMServiceDefinition service = new SimpleVMServiceDefinition(pipeline);
										service.setRoot(new Sequence());
										be.nabu.libs.services.vm.step.Map map = new be.nabu.libs.services.vm.step.Map();
										map.setParent(service.getRoot());
										service.getRoot().getChildren().add(map);
										
										workflow.getMappings().put(transition.getId(), service);
										drawTransition(workflow, child, transition);
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
	
	private void removeTransition(final Workflow workflow, final WorkflowState state, final WorkflowTransition transition) {
		if (transitions.containsKey(transition.getId())) {
			drawPane.getChildren().removeAll(transitions.get(transition.getId()));
			transitions.remove(transition.getId());
		}
		state.getTransitions().remove(transition);
		workflow.getMappings().remove(transition.getId());
		MainController.getInstance().setChanged();
	}
	
	private void drawTransition(final Workflow workflow, final WorkflowState state, final WorkflowTransition transition) {
		List<Node> shapes = new ArrayList<Node>();
		
		AnchorPane pane = new AnchorPane();
		VBox box = new VBox();
		Circle circle = new Circle(10);
		circle.getStyleClass().add("connectionLine");
		Label label = new Label(transition.getName());
		label.getStyleClass().add("workflow-name");
		box.getChildren().addAll(circle, label);
		pane.getChildren().add(box);
		pane.setManaged(false);
		pane.setLayoutX(transition.getX());
		pane.setLayoutY(transition.getY());
		shapes.add(pane);
		
		circle.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				AnchorPane pane = new AnchorPane();
				try {
					VMServiceGUIManager serviceManager = new VMServiceGUIManager();
					serviceManager.setDisablePipelineEditing(true);
					VMService service = workflow.getMappings().get(transition.getId());
					VMServiceController controller = serviceManager.displayWithController(MainController.getInstance(), pane, service);
					TreeItem<Step> root = serviceManager.getServiceTree().rootProperty().get();
					TreeItem<Step> treeItem = root.getChildren().get(0);
					serviceManager.getServiceTree().getSelectionModel().select(serviceManager.getServiceTree().getTreeCell(treeItem));
					Pane panMap = controller.getPanMap();
					mapPane.getChildren().clear();
					mapPane.getChildren().add(panMap);
					
					AnchorPane.setBottomAnchor(panMap, 0d);
					AnchorPane.setTopAnchor(panMap, 0d);
					AnchorPane.setRightAnchor(panMap, 0d);
					AnchorPane.setLeftAnchor(panMap, 0d);
					event.consume();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		
		MovablePane movableCircle = MovablePane.makeMovable(pane);
		movableCircle.xProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				transition.setX(arg2.doubleValue());
				MainController.getInstance().setChanged();
			}
		});
		movableCircle.yProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				transition.setY(arg2.doubleValue());
				MainController.getInstance().setChanged();
			}
		});
		
		Line line1 = new Line();
		line1.eventSizeProperty().set(5);
		// the line starts at the outgoing point of the state
		line1.startXProperty().bind(states.get(state.getId()).rightAnchorXProperty());
		line1.startYProperty().bind(states.get(state.getId()).rightAnchorYProperty());
		line1.endXProperty().bind(pane.layoutXProperty());
		line1.endYProperty().bind(pane.layoutYProperty().add(circle.layoutYProperty()).add(circle.centerYProperty()));
		
		// TODO: draw all transitions, add click handlers etc
		line1.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE) {
					Confirm.confirm(ConfirmType.QUESTION, "Delete transition?", "Are you sure you want to delete the transition '" + transition.getName() + "'", new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							removeTransition(workflow, state, transition);
						}
					});
				}
			}
		});
		line1.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				mapPane.getChildren().clear();
				// add an editor for the transient state
				try {
					new StructureGUIManager().display(MainController.getInstance(), mapPane, workflow.getStructures().get(transition.getId()));
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				// focus on the state for deletion if necessary
				line1.requestFocus();
				event.consume();
			}
		});
		line1.getStyleClass().add("connectionLine");
		
		Line line2 = new Line();
		line2.eventSizeProperty().set(5);
		line2.startXProperty().bind(pane.layoutXProperty().add(pane.widthProperty()));
		line2.startYProperty().bind(pane.layoutYProperty().add(circle.layoutYProperty()).add(circle.centerYProperty()));
		line2.endXProperty().bind(states.get(transition.getTargetStateId()).leftAnchorXProperty());
		line2.endYProperty().bind(states.get(transition.getTargetStateId()).leftAnchorYProperty());
		line2.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE) {
					removeTransition(workflow, state, transition);
				}
			}
		});
		line2.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				line2.requestFocus();
			}
		});
		line2.getStyleClass().add("connectionLine");
		
		shapes.add(line1);
		shapes.add(line2);
		transitions.put(transition.getId(), shapes);
		drawPane.getChildren().addAll(line1, line2, pane);
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
