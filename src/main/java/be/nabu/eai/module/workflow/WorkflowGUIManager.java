package be.nabu.eai.module.workflow;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.MainController.PropertyUpdater;
import be.nabu.eai.developer.managers.base.BaseArtifactGUIInstance;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.EnumeratedSimpleProperty;
import be.nabu.eai.developer.managers.util.MovablePane;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.EAIDeveloperUtils.PropertyUpdaterListener;
import be.nabu.eai.module.services.vm.RepositoryExecutorProvider;
import be.nabu.eai.module.services.vm.VMServiceGUIManager;
import be.nabu.eai.module.services.vm.util.VMServiceController;
import be.nabu.eai.module.types.structure.StructureGUIManager;
import be.nabu.eai.module.workflow.gui.RectangleWithHooks;
import be.nabu.eai.module.workflow.transition.WorkflowTransitionMappingInterface;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.line.Line;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.jfx.control.tree.drag.MouseLocation;
import be.nabu.jfx.control.tree.drag.TreeDragDrop;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.vm.Pipeline;
import be.nabu.libs.services.vm.PipelineInterfaceProperty;
import be.nabu.libs.services.vm.SimpleVMServiceDefinition;
import be.nabu.libs.services.vm.api.ExecutorProvider;
import be.nabu.libs.services.vm.api.Step;
import be.nabu.libs.services.vm.api.VMService;
import be.nabu.libs.services.vm.step.Sequence;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class WorkflowGUIManager extends BaseJAXBGUIManager<WorkflowConfiguration, Workflow> {

	static {
		URL resource = VMServiceGUIManager.class.getClassLoader().getResource("workflow.css");
		if (resource != null) {
			MainController.registerStyleSheet(resource.toExternalForm());
		}
	}
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public WorkflowGUIManager() {
		super("Workflow", Workflow.class, new WorkflowManager(), WorkflowConfiguration.class);
	}
	
	private Map<String, RectangleWithHooks> states = new HashMap<String, RectangleWithHooks>();
	private Map<String, List<Node>> extensions = new HashMap<String, List<Node>>();
	private Map<String, List<Node>> transitions = new HashMap<String, List<Node>>();
	private AnchorPane drawPane;
	private Line draggingLine;
	private AnchorPane mapPane;
	private String dragMode = "move";

	@Override
	public void display(MainController controller, AnchorPane pane, Workflow artifact) {
		SplitPane split = new SplitPane();

		VBox vbox = new VBox();
		split.setOrientation(Orientation.VERTICAL);
		
		drawPane = new AnchorPane();
		drawPane.setPadding(new Insets(10, 10, 100, 100));
		
		// draw the states
		for (WorkflowState state : artifact.getConfig().getStates()) {
			drawState(artifact, state);
		}
		// draw extensions
		for (WorkflowState state : artifact.getConfig().getStates()) {
			if (state.getExtensions() != null) {
				Iterator<String> iterator = state.getExtensions().iterator();
				while (iterator.hasNext()) {
					String id = iterator.next();
					WorkflowState parent = artifact.getStateById(id);
					if (parent != null) {
						// draw possible extensions
						drawExtension(artifact, state, parent);
					}
					// can no longer find extended parent, remove the extension
					else {
						iterator.remove();
						MainController.getInstance().setChanged();
					}
				}
			}
		}
			
		for (WorkflowState state : artifact.getConfig().getStates()) {
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
							boolean nameTaken = false;
							String fieldName = EAIRepositoryUtils.stringToField(name);
							for (WorkflowState state : artifact.getConfig().getStates()) {
								if (EAIRepositoryUtils.stringToField(state.getName()).equals(fieldName)) {
									nameTaken = true;
									break;
								}
							}
							if (nameTaken) {
								Confirm.confirm(ConfirmType.ERROR, "Can not create state", "A state by the name '" + name + "' already exists", null);
							}
							else {
								WorkflowState state = new WorkflowState();
								state.setId(UUID.randomUUID().toString().replace("-", ""));
								state.setName(name);
								if (lastStateId != null) {
									WorkflowState stateById = artifact.getStateById(lastStateId);
									// may have been deleted already
									if (stateById != null) {
										state.setX(stateById.getX() + 200);
										state.setY(stateById.getY());
									}
								}
								artifact.getConfig().getStates().add(state);
								DefinedStructure value = new DefinedStructure();
								value.setName("state");
								value.setId(artifact.getId() + ".types.states." + EAIRepositoryUtils.stringToField(state.getName()));
								artifact.getStructures().put(state.getId(), value);
								lastStateId = state.getId();
								((WorkflowManager) getArtifactManager()).refreshChildren((ModifiableEntry) artifact.getRepository().getEntry(artifact.getId()), artifact);
								drawState(artifact, state);
								MainController.getInstance().setChanged();
							}
						}
					}
				});
			}
		});
		
		Button editProperties = new Button("Edit Workflow Properties");
		editProperties.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (!event.isConsumed()) {
					SplitPane split = new SplitPane();
					split.setOrientation(Orientation.HORIZONTAL);
					AnchorPane left = new AnchorPane();
					AnchorPane right = new AnchorPane();
					
					ScrollPane leftScrollPane = new ScrollPane();
					leftScrollPane.setFitToHeight(true);
					leftScrollPane.setFitToWidth(true);
					leftScrollPane.setContent(left);
					displayParent(right, artifact);
					split.getItems().addAll(leftScrollPane, right);
					mapPane.getChildren().clear();
					mapPane.getChildren().add(split);
					// add an editor for the transient state
					try {
						StructureGUIManager structureGUIManager = new StructureGUIManager();
						structureGUIManager.setActualId(artifact.getId());
						structureGUIManager.display(MainController.getInstance(), left, artifact.getStructures().get("properties"));
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
					AnchorPane.setLeftAnchor(split, 0d);
					AnchorPane.setTopAnchor(split, 0d);
					AnchorPane.setBottomAnchor(split, 0d);
					AnchorPane.setRightAnchor(split, 0d);
				}
			}
		});
		
		Separator separator = new Separator(Orientation.VERTICAL);
		separator.setPadding(new Insets(0,10,0,10));
		Button dragModeMove = new Button("Move");
		Button dragModeConnect = new Button("Transition");
		Button dragModeExtend = new Button("Extend");
		dragModeMove.getStyleClass().add("armed");
		dragModeMove.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				dragMode = "move";
				dragModeMove.getStyleClass().add("armed");
				dragModeConnect.getStyleClass().remove("armed");
				dragModeExtend.getStyleClass().remove("armed");
			}
		});
		dragModeConnect.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				dragMode = "connect";
				dragModeMove.getStyleClass().remove("armed");
				dragModeConnect.getStyleClass().add("armed");
				dragModeExtend.getStyleClass().remove("armed");
			}
		});
		dragModeExtend.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				dragMode = "extend";
				dragModeMove.getStyleClass().remove("armed");
				dragModeConnect.getStyleClass().remove("armed");
				dragModeExtend.getStyleClass().add("armed");
			}
		});
		
		buttons.getChildren().addAll(addState, editProperties, separator, dragModeMove, dragModeConnect, dragModeExtend);
		
		ScrollPane drawScrollPane = new ScrollPane();
		drawScrollPane.setContent(drawPane);
		vbox.getChildren().addAll(buttons, drawScrollPane);
		
		VBox.setVgrow(buttons, Priority.NEVER);
		VBox.setVgrow(drawScrollPane, Priority.ALWAYS);
		
		mapPane = new AnchorPane();
		
		split.getItems().addAll(vbox, mapPane);
		
		AnchorPane.setBottomAnchor(split, 0d);
		AnchorPane.setTopAnchor(split, 0d);
		AnchorPane.setLeftAnchor(split, 0d);
		AnchorPane.setRightAnchor(split, 0d);
		pane.getChildren().add(split);
	}
	
	private void displayParent(AnchorPane pane, Workflow artifact) {
		super.display(MainController.getInstance(), pane, artifact);
	}
	
	@Override
	public Collection<Property<?>> getModifiableProperties(Workflow instance) {
		Collection<Property<?>> modifiableProperties = super.getModifiableProperties(instance);
		Iterator<Property<?>> iterator = modifiableProperties.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getName().startsWith("states")) {
				iterator.remove();
			}
		}
		return modifiableProperties;
	}
	
	private String lastStateId;

	private void drawState(final Workflow workflow, final WorkflowState state) {
//		Paint.valueOf("#ddffcf"), Paint.valueOf("#195700")
		RectangleWithHooks rectangle = new RectangleWithHooks(state.getX(), state.getY(), 200, 45, "state", "correct");
//		rectangle.getPane().setManaged(false);
		Label label = new Label(state.getName());
		label.getStyleClass().add("workflow-name");
		rectangle.getContent().getChildren().add(label);
		
		rectangle.getContainer().prefWidthProperty().bind(label.prefWidthProperty());
		
		BooleanProperty locked = MainController.getInstance().hasLock(workflow.getId());
		MovablePane movable = MovablePane.makeMovable(rectangle.getContainer(), locked);
//		movable.setGridSize(10);
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
				lastStateId = state.getId();
				SplitPane split = new SplitPane();
				split.setOrientation(Orientation.HORIZONTAL);
				AnchorPane left = new AnchorPane();
				AnchorPane right = new AnchorPane();
				
				ScrollPane leftScrollPane = new ScrollPane();
				leftScrollPane.setFitToHeight(true);
				leftScrollPane.setFitToWidth(true);
				leftScrollPane.setContent(left);
				
				EnumeratedSimpleProperty<String> extensionProperty = new EnumeratedSimpleProperty<String>("extensions", String.class, false);
				if (workflow.getConfig().getStates() != null) {
					for (WorkflowState initialState : workflow.getInitialStates()) {
						if (state.getExtensions() == null || !state.getExtensions().contains(initialState.getId())) {
							extensionProperty.addAll(initialState.getName());
						}
					}
				}
				
				// TODO: extensions should really be enumerated and should persist ids instead of names...
				SimplePropertyUpdater createUpdater = EAIDeveloperUtils.createUpdater(state, null, "x", "y", "transitions.*", "id", "name", "extensions");
				createUpdater.setSourceId(workflow.getId());
				MainController.getInstance().showProperties(createUpdater, right, true);
			
				split.getItems().addAll(leftScrollPane, right);
				mapPane.getChildren().clear();
				mapPane.getChildren().add(split);
				// add an editor for the transient state
				try {
					StructureGUIManager structureGUIManager = new StructureGUIManager();
					structureGUIManager.setActualId(workflow.getId());
					structureGUIManager.display(MainController.getInstance(), left, workflow.getStructures().get(state.getId()));
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				AnchorPane.setLeftAnchor(split, 0d);
				AnchorPane.setTopAnchor(split, 0d);
				AnchorPane.setBottomAnchor(split, 0d);
				AnchorPane.setRightAnchor(split, 0d);
				
				// focus on the state for deletion if necessary
				rectangle.getContent().requestFocus();
				event.consume();
			}
		});
		rectangle.getContent().addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE && locked.get()) {
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
				if ((event.isControlDown() || event.isShiftDown() || !dragMode.equals("move")) && locked.get()) {
					draggingLine = new Line();
					draggingLine.startXProperty().bind(rectangle.rightAnchorXProperty());
					draggingLine.startYProperty().bind(rectangle.rightAnchorYProperty());
					draggingLine.endXProperty().bind(MouseLocation.getInstance(scene).xProperty().subtract(drawPane.localToSceneTransformProperty().get().getTx()));
					draggingLine.endYProperty().bind(MouseLocation.getInstance(scene).yProperty().subtract(drawPane.localToSceneTransformProperty().get().getTy()));
					Dragboard dragboard = rectangle.getContainer().startDragAndDrop(TransferMode.LINK);
					Map<DataFormat, Object> content = new HashMap<DataFormat, Object>();
					content.put(TreeDragDrop.getDataFormat(event.isControlDown() || event.isShiftDown() ? (event.isShiftDown() ? "extend" : "connect") : dragMode), state.getId());
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
				if (content != null && !workflow.isExtensionState(state.getId()) && !workflow.isExtensionState(state.getName())) {
					event.acceptTransferModes(TransferMode.ANY);
					event.consume();
				}
				if (!event.isConsumed()) {
					content = event.getDragboard().getContent(TreeDragDrop.getDataFormat("extend"));
					if (content != null) {
						boolean isAllowed = true;
						// there should not be any transitions _to_ the state you are about to extend
						for (WorkflowState potentialState : workflow.getConfig().getStates()) {
							if (potentialState.getTransitions() != null) {
								for (WorkflowTransition transition : potentialState.getTransitions()) {
									if (transition.getTargetStateId().equals(state.getId())) {
										isAllowed = false;
										break;
									}
								}
							}
							if (!isAllowed) {
								break;
							}
						}
						if (isAllowed) {
							event.acceptTransferModes(TransferMode.ANY);
							event.consume();
						}
					}
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
					EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Add New Transition", new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							String name = updater.getValue("Name");
							if (name != null) {
								boolean nameTaken = false;
								String fieldName = EAIRepositoryUtils.stringToField(name);
								states: for (WorkflowState state : workflow.getConfig().getStates()) {
									for (WorkflowTransition transition : state.getTransitions()) {
										if (EAIRepositoryUtils.stringToField(transition.getName()).equals(fieldName)) {
											nameTaken = true;
											break states;
										}
									}
								}
								if (nameTaken) {
									Confirm.confirm(ConfirmType.ERROR, "Can not create transition", "A transition by the name '" + name + "' already exists", null);
								}
								else {
									WorkflowTransition transition = new WorkflowTransition();
									transition.setId(UUID.randomUUID().toString().replace("-", ""));
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
	
											RectangleWithHooks rectangleWithHooks = states.get(state.getId());
											transition.setX(state.getX() + rectangleWithHooks.getContent().getWidth() + ((child.getX() - (state.getX() + rectangleWithHooks.getContent().getWidth())) / 2));
											transition.setY(state.getY() + (rectangleWithHooks.getContent().getHeight() / 2) - 5);
	
											
											Structure input = new Structure();
											input.setName("input");
											/*
											input.add(new ComplexElementImpl("workflow", (ComplexType) BeanResolver.getInstance().resolve(WorkflowInstance.class), input));
											input.add(new ComplexElementImpl("properties", workflow.getStructures().get("properties"), input));
											input.add(new ComplexElementImpl("state", workflow.getStructures().get(child.getId()), input));
											input.add(new ComplexElementImpl("transition", workflow.getStructures().get(transition.getId()), input));
											input.add(new ComplexElementImpl("history", (ComplexType) BeanResolver.getInstance().resolve(WorkflowTransitionInstance.class), input, new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0), new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
											*/
											
											Structure output = new Structure();
											output.setName("output");
											
											/*
											output.add(new ComplexElementImpl("properties", workflow.getStructures().get("properties"), output));
											output.add(new ComplexElementImpl("state", workflow.getStructures().get(state.getId()), output));
											output.add(new SimpleElementImpl<String>("workflowType", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
											output.add(new SimpleElementImpl<String>("contextId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
											output.add(new SimpleElementImpl<String>("groupId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
											output.add(new SimpleElementImpl<String>("log", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
											output.add(new SimpleElementImpl<String>("code", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
											output.add(new SimpleElementImpl<URI>("uri", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(URI.class), output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
											*/
											
											Pipeline pipeline = new Pipeline(input, output);
											
//											WorkflowTransitionServiceInterface iface = new WorkflowTransitionServiceInterface(workflow, child, transition);
											WorkflowTransitionMappingInterface iface = new WorkflowTransitionMappingInterface(workflow, child, transition);
											pipeline.setProperty(new ValueImpl<DefinedServiceInterface>(PipelineInterfaceProperty.getInstance(), iface));
											
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
						}
					});
					if (draggingLine != null) {
						drawPane.getChildren().remove(draggingLine);
						draggingLine = null;
					}
					event.setDropCompleted(true);
					event.consume();
				}
				else {
					Object extendContent = event.getDragboard().getContent(TreeDragDrop.getDataFormat("extend"));
					if (extendContent != null) {
						WorkflowState stateById = workflow.getStateById(extendContent.toString());
						if (stateById != null) {
							if (stateById.getExtensions() == null) {
								stateById.setExtensions(new ArrayList<String>());
							}
							if (!stateById.getExtensions().contains(state.getId())) {
								stateById.getExtensions().add(state.getId());
								drawExtension(workflow, stateById, state);
								MainController.getInstance().setChanged();
							}
						}
					}
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
		
		EventHandler<MouseEvent> highlightStates = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				states.get(state.getId()).getContent().getStyleClass().add("active-from");
				for (WorkflowTransition transition : state.getTransitions()) {
					for (Node shape : transitions.get(transition.getId())) {
						if (shape instanceof Line) {
							shape.getStyleClass().add("connectionLine-hover");
						}
						else if (shape instanceof AnchorPane) {
							for (Node child : ((AnchorPane) shape).getChildren()) {
								if (child instanceof Label) {
									child.setVisible(true);
								}
							}
						}
					}
					states.get(transition.getTargetStateId()).getContent().getStyleClass().add("active-to");
				}
			}
		};
		EventHandler<MouseEvent> unhighlightStates = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				states.get(state.getId()).getContent().getStyleClass().remove("active-from");
				for (WorkflowTransition transition : state.getTransitions()) {
					for (Node shape : transitions.get(transition.getId())) {
						if (shape instanceof Line) {
							shape.getStyleClass().remove("connectionLine-hover");
						}
						else if (shape instanceof AnchorPane) {
							for (Node child : ((AnchorPane) shape).getChildren()) {
								if (child instanceof Label) {
									child.setVisible(false);
								}
							}
						}
					}
					states.get(transition.getTargetStateId()).getContent().getStyleClass().remove("active-to");
				}
			}
		};
		rectangle.getContainer().addEventHandler(MouseEvent.MOUSE_ENTERED, highlightStates);
		rectangle.getContainer().addEventHandler(MouseEvent.MOUSE_EXITED, unhighlightStates);
		
		drawPane.getChildren().add(rectangle.getContainer());
		// TODO: on click > show properties of state on the right side
		states.put(state.getId(), rectangle);
	}
	
	public static class Endpoint {
		private DoubleExpression x, y;

		public Endpoint(DoubleExpression x, DoubleExpression y) {
			this.x = x;
			this.y = y;
		}
		public DoubleExpression xProperty() {
			return x;
		}
		public DoubleExpression yProperty() {
			return y;
		}
	}
	
	public static class EndpointPicker {
		private Endpoint[] possibleBindPoints;
		private Endpoint endpointToBind;
		
		private Endpoint lastWinner;
		
		private SimpleDoubleProperty x = new SimpleDoubleProperty();
		private SimpleDoubleProperty y = new SimpleDoubleProperty();

		public EndpointPicker(Endpoint endpointToBind, Endpoint...possibleBindPoints) {
			this.endpointToBind = endpointToBind;
			this.possibleBindPoints = possibleBindPoints;
			calculate();
			
			ChangeListener<Number> recalculationListener = new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
					calculate();
				}
			};
			endpointToBind.xProperty().addListener(recalculationListener);
			endpointToBind.yProperty().addListener(recalculationListener);
			
			for (Endpoint endpoint : possibleBindPoints) {
				endpoint.xProperty().addListener(recalculationListener);
				endpoint.yProperty().addListener(recalculationListener);
			}
		}
		
		private void calculate() {
			double minDistance = Double.MAX_VALUE;
			Endpoint winner = null;
			double x1 = endpointToBind.xProperty().get();
			double y1 = endpointToBind.yProperty().get();
			for (Endpoint possibleBindPoint : possibleBindPoints) {
				double x2 = possibleBindPoint.xProperty().get();
				double y2 = possibleBindPoint.yProperty().get();
				double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
				if (distance < minDistance) {
					winner = possibleBindPoint;
					minDistance = distance;
				}
			}
			if (lastWinner == null || !lastWinner.equals(winner)) {
				if (x.isBound()) {
					x.unbind();
				}
				x.bind(winner.xProperty());
				if (y.isBound()) {
					y.unbind();
				}
				y.bind(winner.yProperty());
				lastWinner = winner;
			}
		}
		
		public ReadOnlyDoubleProperty xProperty() {
			return x;
		}
		public ReadOnlyDoubleProperty yProperty() {
			return y;
		}
	}
	
	private void removeTransition(final Workflow workflow, final WorkflowState state, final WorkflowTransition transition) {
		if (transitions.containsKey(transition.getId())) {
			drawPane.getChildren().removeAll(transitions.get(transition.getId()));
			transitions.remove(transition.getId());
		}
		state.getTransitions().remove(transition);
		workflow.getStructures().remove(transition.getId());
		workflow.getMappings().remove(transition.getId());
		MainController.getInstance().setChanged();
	}
	
	private void removeExtension(final Workflow workflow, final WorkflowState child, final WorkflowState parent) {
		List<Node> list1 = extensions.get(child.getId());
		List<Node> list2 = extensions.get(parent.getId());
		if (list1 != null && list2 != null) {
			Iterator<Node> iterator = list1.iterator();
			while (iterator.hasNext()) {
				Node node = iterator.next();
				if (list2.contains(node)) {
					drawPane.getChildren().remove(node);
					list2.remove(node);
					iterator.remove();
					child.getExtensions().remove(parent.getId());
					MainController.getInstance().setChanged();
				}
			}
		}
	}
	private void drawExtension(final Workflow workflow, final WorkflowState child, final WorkflowState parent) {
		Line line = new Line();
		line.eventSizeProperty().set(5);
		
		EndpointPicker picker = getPicker(line.endXProperty(), line.endYProperty(), states.get(child.getId()));
		line.startXProperty().bind(picker.xProperty());
		line.startYProperty().bind(picker.yProperty());
		
		EndpointPicker picker2 = getPicker(line.startXProperty(), line.startYProperty(), states.get(parent.getId()));
		line.endXProperty().bind(picker2.xProperty());
		line.endYProperty().bind(picker2.yProperty());
		
		line.getStyleClass().add("extensionLine");
		
		line.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				line.requestFocus();
			}
		});
		BooleanProperty locked = MainController.getInstance().hasLock(workflow.getId());
		line.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE && locked.get()) {
					Confirm.confirm(ConfirmType.QUESTION, "Delete extension?", "Are you sure you want to delete the extension?", new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							removeExtension(workflow, child, parent);
						}
					});
				}
			}
		});
		
		if (!extensions.containsKey(child)) {
			extensions.put(child.getId(), new ArrayList<Node>());
		}
		if (!extensions.containsKey(parent)) {
			extensions.put(parent.getId(), new ArrayList<Node>());
		}
		extensions.get(child.getId()).add(line);
		extensions.get(parent.getId()).add(line);
		drawPane.getChildren().addAll(line);
	}
	
	private void drawTransition(final Workflow workflow, final WorkflowState state, final WorkflowTransition transition) {
		List<Node> shapes = new ArrayList<Node>();
		
		AnchorPane pane = new AnchorPane();
		VBox box = new VBox();
		Circle circle = new Circle(5);
		circle.getStyleClass().add("connectionLine");
		circle.setFill(Color.TRANSPARENT);
		Label label = new Label(transition.getName());
		if (transition.getQuery() != null && !transition.getQuery().trim().isEmpty()) {
			label.setText(label.getText() + ("true".equals(transition.getQuery()) ? "" : "\n" + transition.getQuery()));
		}
		label.getStyleClass().add("workflow-name");
		box.getChildren().addAll(circle);
		pane.getChildren().add(box);
		pane.setManaged(false);
		pane.setLayoutX(transition.getX());
		pane.setLayoutY(transition.getY());
		shapes.add(pane);
		
		AnchorPane labelPane = new AnchorPane();
		labelPane.setManaged(false);

		RectangleWithHooks stateRectangle = states.get(state.getId());
		DoubleBinding subtract = stateRectangle.getContainer().layoutYProperty().subtract(pane.layoutYProperty());
		labelPane.layoutXProperty().bind(circle.layoutXProperty().add(pane.layoutXProperty()).subtract(label.widthProperty().divide(2)));
//		labelPane.layoutYProperty().bind(circle.layoutYProperty().add(pane.layoutYProperty()).add(circle.radiusProperty().multiply(2)));

		// initial: if the value is positive, the rectangle is lower then the circle, we draw the text above
		if (subtract.get() >= 0) {
			labelPane.layoutYProperty().bind(pane.layoutYProperty().subtract(transition.getQuery() == null ? 40 : 60));
		}
		// otherwise we draw the text below
		else {
			labelPane.layoutYProperty().bind(circle.layoutYProperty().add(pane.layoutYProperty()).add(circle.radiusProperty().multiply(2)));
		}
		
		subtract.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number arg1, Number arg2) {
				// if the value is positive, the rectangle is lower then the circle, we draw the text above
				if (arg2.doubleValue() >= 0) {
					labelPane.layoutYProperty().unbind();
					labelPane.layoutYProperty().bind(pane.layoutYProperty().subtract(transition.getQuery() == null ? 40 : 60));
				}
				// otherwise we draw the text below
				else {
					labelPane.layoutYProperty().unbind();
					labelPane.layoutYProperty().bind(circle.layoutYProperty().add(pane.layoutYProperty()).add(circle.radiusProperty().multiply(2)));
				}
			}
		});
		
		
		labelPane.getChildren().add(label);
		label.setVisible(false);
		shapes.add(labelPane);
		
		circle.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				AnchorPane pane = new AnchorPane();
				try {
					VMServiceGUIManager serviceManager = new VMServiceGUIManager();
					serviceManager.setDisablePipelineEditing(true);
					serviceManager.setActualId(workflow.getId());
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
		
		BooleanProperty locked = MainController.getInstance().hasLock(workflow.getId());
		MovablePane movableCircle = MovablePane.makeMovable(pane, locked);
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
		// the picker will intelligently decide which endpoint in the rectangle to bind to
		EndpointPicker picker = getPicker(line1.endXProperty(), line1.endYProperty(), states.get(state.getId()));
		line1.startXProperty().bind(picker.xProperty());
		line1.startYProperty().bind(picker.yProperty());
//		line1.startXProperty().bind(states.get(state.getId()).rightAnchorXProperty());
//		line1.startYProperty().bind(states.get(state.getId()).rightAnchorYProperty());
		
		picker = getPicker(line1.startXProperty(), line1.startYProperty(), pane, circle);
		line1.endXProperty().bind(picker.xProperty());
		line1.endYProperty().bind(picker.yProperty());
//		line1.endXProperty().bind(pane.layoutXProperty());
//		line1.endYProperty().bind(pane.layoutYProperty().add(circle.layoutYProperty()).add(circle.centerYProperty()));
		
		Line line2 = new Line();
		
		EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				SplitPane split = new SplitPane();
				split.setOrientation(Orientation.HORIZONTAL);
				AnchorPane left = new AnchorPane();
				AnchorPane right = new AnchorPane();
				
				ScrollPane leftScrollPane = new ScrollPane();
				leftScrollPane.setFitToHeight(true);
				leftScrollPane.setFitToWidth(true);
				leftScrollPane.setContent(left);
				
				SimplePropertyUpdater createUpdater = EAIDeveloperUtils.createUpdater(transition, new PropertyUpdaterListener() {
					@Override
					public boolean updateProperty(Property<?> property, Object value) {
						if (property.getName().equals("startBatch")) {
							if (value != null && (Boolean) value) {
								if (workflow.getMappings().get(transition.getId()).getServiceInterface().getInputDefinition().get("batchId") == null) {
									Structure input = (Structure) workflow.getMappings().get(transition.getId()).getPipeline().get(Pipeline.INPUT).getType();
									input.add(new SimpleElementImpl<String>("batchId", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input));
									MainController.getInstance().setChanged();
								}
							}
							else {
								if (workflow.getMappings().get(transition.getId()).getServiceInterface().getInputDefinition().get("batchId") != null) {
									Structure input = (Structure) workflow.getMappings().get(transition.getId()).getPipeline().get(Pipeline.INPUT).getType();
									input.remove(input.get("batchId"));
									MainController.getInstance().setChanged();
								}
							}
						}
						if (property.getName().equals("query")) {
							if (value != null && !((String) value).trim().isEmpty()) {
								label.setText(transition.getName() + ("true".equals(value) ? "" : "\n" + value));
								if (!line1.getStyleClass().contains("indexQueryLine")) {
									line1.getStyleClass().add("indexQueryLine");
								}
								if (!line2.getStyleClass().contains("indexQueryLine")) {
									line2.getStyleClass().add("indexQueryLine");
								}
							}
							else {
								label.setText(transition.getName());
								if (line1.getStyleClass().contains("indexQueryLine")) {
									line1.getStyleClass().remove("indexQueryLine");
								}
								if (line2.getStyleClass().contains("indexQueryLine")) {
									line2.getStyleClass().remove("indexQueryLine");
								}
							}
						}
						return true;
					}
				}, "x", "y", "targetStateId", "id", "name", "target", "targetProperties");
				createUpdater.setSourceId(workflow.getId());
				MainController.getInstance().showProperties(createUpdater, right, true);
			
				split.getItems().addAll(leftScrollPane, right);
				
				if (transition.getQuery() != null && !transition.getQuery().trim().toLowerCase().equals("false")) {
					ExecutorProvider executorProvider = new RepositoryExecutorProvider(workflow.getRepository());
					AnchorPane additional = new AnchorPane();
					EnumeratedSimpleProperty<String> targetProperty = new EnumeratedSimpleProperty<String>("target", String.class, false);
					targetProperty.addAll(executorProvider.getTargets().toArray(new String[0]));
					HashSet<Property<?>> hashSet = new HashSet<Property<?>>(Arrays.asList(targetProperty));
					List<Property<?>> targetProperties = new ArrayList<Property<?>>();
					if (transition.getTarget() != null) {
						targetProperties.addAll(executorProvider.getTargetProperties(transition.getTarget()));
						hashSet.addAll(targetProperties);
					}
					PropertyUpdater updater = new PropertyUpdater() {
						@Override
						public Set<Property<?>> getSupportedProperties() {
							return hashSet;
						}
						@SuppressWarnings({ "unchecked", "rawtypes" })
						@Override
						public Value<?>[] getValues() {
							List<Value<?>> list = new ArrayList<Value<?>>(Arrays.asList(new Value<?> [] { 
								new ValueImpl<String>(targetProperty, transition.getTarget())
							}));
							java.util.Map<String, String> values = transition.getTargetProperties();
							if (values != null) {
								for (Property<?> property : targetProperties) {
									Object value = values.get(property.getName());
									if (value != null) {
										if (!String.class.isAssignableFrom(property.getValueClass()) && !((String) value).startsWith("=")) {
											value = ConverterFactory.getInstance().getConverter().convert(value, property.getValueClass());
										}
										list.add(new ValueImpl(property, value));
									}
								}
							}
							return list.toArray(new Value<?>[list.size()]);
						}
						@Override
						public boolean canUpdate(Property<?> property) {
							return true;
						}
						@Override
						public List<ValidationMessage> updateProperty(Property<?> property, Object value) {
							if (property.equals(targetProperty)) {
								transition.setTarget(value == null ? null : value.toString());
								hashSet.removeAll(targetProperties);
								targetProperties.clear();
								transition.setTargetProperties(null);
								if (transition.getTarget() != null) {
									targetProperties.addAll(executorProvider.getTargetProperties(transition.getTarget()));
									hashSet.addAll(targetProperties);
								}
							}
							else {
								if (transition.getTargetProperties() == null) {
									transition.setTargetProperties(new HashMap<String, String>());
								}
								if (value == null) {
									transition.getTargetProperties().remove(property.getName());
								}
								else {
									if (!String.class.isAssignableFrom(property.getValueClass()) && value != null && !(value instanceof String)) {
										value = ConverterFactory.getInstance().getConverter().convert(value, String.class);
									}
									transition.getTargetProperties().put(property.getName(), (String) value);
								}
							}
							MainController.getInstance().setChanged();
							return null;
						}
						@Override
						public boolean isMandatory(Property<?> property) {
							return true;
						}
					};
					MainController.getInstance().showProperties(updater, additional, true);
					split.getItems().add(additional);
				}
				
				mapPane.getChildren().clear();
				mapPane.getChildren().add(split);
				// add an editor for the transient state
				try {
					StructureGUIManager structureGUIManager = new StructureGUIManager();
					structureGUIManager.setActualId(workflow.getId());
					structureGUIManager.display(MainController.getInstance(), left, workflow.getStructures().get(transition.getId()));
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				AnchorPane.setLeftAnchor(split, 0d);
				AnchorPane.setTopAnchor(split, 0d);
				AnchorPane.setBottomAnchor(split, 0d);
				AnchorPane.setRightAnchor(split, 0d);
				
				// focus on the state for deletion if necessary
				line1.requestFocus();
				event.consume();
			}
		};
		
		line1.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEventHandler);
		line2.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEventHandler);
		
		EventHandler<KeyEvent> keyEventHandler = new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE && locked.get()) {
					Confirm.confirm(ConfirmType.QUESTION, "Delete transition?", "Are you sure you want to delete the transition '" + transition.getName() + "'", new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							removeTransition(workflow, state, transition);
						}
					});
				}
			}
		};
		line1.addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);
		line2.addEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);
		
		line1.getStyleClass().add("connectionLine");
		
		if (transition.getQuery() != null) {
			line1.getStyleClass().add("indexQueryLine");
		}
		
		line2.eventSizeProperty().set(5);
		picker = getPicker(line2.endXProperty(), line2.endYProperty(), pane, circle);
		line2.startXProperty().bind(picker.xProperty());
		line2.startYProperty().bind(picker.yProperty());
//		line2.startXProperty().bind(pane.layoutXProperty().add(circle.radiusProperty().multiply(2)));
//		line2.startYProperty().bind(pane.layoutYProperty().add(circle.layoutYProperty()).add(circle.centerYProperty()));
		
		picker = getPicker(line2.startXProperty(), line2.startYProperty(), states.get(transition.getTargetStateId()));
		line2.endXProperty().bind(picker.xProperty());
		line2.endYProperty().bind(picker.yProperty());
//		line2.endXProperty().bind(states.get(transition.getTargetStateId()).leftAnchorXProperty());
//		line2.endYProperty().bind(states.get(transition.getTargetStateId()).leftAnchorYProperty());
		line2.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE && locked.get()) {
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
		
		if (transition.getQuery() != null) {
			line2.getStyleClass().add("indexQueryLine");
		}
		
		shapes.add(line1);
		shapes.add(line2);
		
		List<Shape> arrow1 = EAIDeveloperUtils.drawArrow(line1, 0.5);
		shapes.addAll(arrow1);
		List<Shape> arrow2 = EAIDeveloperUtils.drawArrow(line2, 0.5);
		shapes.addAll(arrow2);
		
		EventHandler<MouseEvent> highlightStates = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				states.get(state.getId()).getContent().getStyleClass().add("active-from");
				states.get(transition.getTargetStateId()).getContent().getStyleClass().add("active-to");
				line1.getStyleClass().add("connectionLine-hover");
				line2.getStyleClass().add("connectionLine-hover");
				label.setVisible(true);
			}
		};
		EventHandler<MouseEvent> unhighlightStates = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				states.get(state.getId()).getContent().getStyleClass().remove("active-from");
				states.get(transition.getTargetStateId()).getContent().getStyleClass().remove("active-to");
				line1.getStyleClass().remove("connectionLine-hover");
				line2.getStyleClass().remove("connectionLine-hover");
				label.setVisible(false);
			}
		};
		line1.addEventHandler(MouseEvent.MOUSE_ENTERED, highlightStates);
		line1.addEventHandler(MouseEvent.MOUSE_EXITED, unhighlightStates);
		line2.addEventHandler(MouseEvent.MOUSE_ENTERED, highlightStates);
		line2.addEventHandler(MouseEvent.MOUSE_EXITED, unhighlightStates);
		
		transitions.put(transition.getId(), shapes);
		drawPane.getChildren().addAll(line1, line2, pane, labelPane);
		drawPane.getChildren().addAll(arrow1);
		drawPane.getChildren().addAll(arrow2);
	}

	private static EndpointPicker getPicker(ReadOnlyDoubleProperty x, ReadOnlyDoubleProperty y, AnchorPane pane, Circle circle) {
		Endpoint endpoint = new Endpoint(x, y);
		Endpoint left = new Endpoint(pane.layoutXProperty(), pane.layoutYProperty().add(circle.layoutYProperty()).add(circle.centerYProperty()));
		Endpoint right = new Endpoint(pane.layoutXProperty().add(circle.radiusProperty().multiply(2)), pane.layoutYProperty().add(circle.layoutYProperty()).add(circle.centerYProperty()));
		Endpoint top = new Endpoint(pane.layoutXProperty().add(circle.radiusProperty()), pane.layoutYProperty());
		Endpoint bottom = new Endpoint(pane.layoutXProperty().add(circle.radiusProperty()), pane.layoutYProperty().add(circle.radiusProperty().multiply(2)));
		return new EndpointPicker(endpoint, left, right, top, bottom);
	}
	
	public static EndpointPicker getPicker(ReadOnlyDoubleProperty x, ReadOnlyDoubleProperty y, RectangleWithHooks rectangle) {
		Endpoint endpoint = new Endpoint(x, y);
		Endpoint bottom = new Endpoint(rectangle.bottomAnchorXProperty(), rectangle.bottomAnchorYProperty());
		Endpoint top = new Endpoint(rectangle.topAnchorXProperty(), rectangle.topAnchorYProperty());
		Endpoint left = new Endpoint(rectangle.leftAnchorXProperty(), rectangle.leftAnchorYProperty());
		Endpoint right = new Endpoint(rectangle.rightAnchorXProperty(), rectangle.rightAnchorYProperty());
		return new EndpointPicker(endpoint, bottom, top, left, right);
	}
	
	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	// currently we don't support detaching cause the mouse drawing is limited with the movable panes linked to the scenes
	// in theory we can group on something else than the scene, but we need a generic drop listener on the whole scene just to be sure?
	@Override
	protected BaseArtifactGUIInstance<Workflow> newGUIInstance(Entry entry) {
		return new BaseArtifactGUIInstance<Workflow>(this, entry) {
			@Override
			public boolean isDetachable() {
				return false;
			}
		};
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
