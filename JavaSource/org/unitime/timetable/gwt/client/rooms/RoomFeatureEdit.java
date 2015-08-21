/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.gwt.client.rooms;

import java.util.List;

import org.unitime.timetable.gwt.client.page.UniTimeNotifications;
import org.unitime.timetable.gwt.client.page.UniTimePageLabel;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.SimpleForm;
import org.unitime.timetable.gwt.client.widgets.UniTimeHeaderPanel;
import org.unitime.timetable.gwt.client.widgets.UniTimeWidget;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.MouseClickListener;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable.TableEvent;
import org.unitime.timetable.gwt.command.client.GwtRpcService;
import org.unitime.timetable.gwt.command.client.GwtRpcServiceAsync;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.EventInterface.FilterRpcResponse.Entity;
import org.unitime.timetable.gwt.shared.RoomInterface.DepartmentInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.FeatureInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.FeatureTypeInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.RoomDetailInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.RoomPropertiesInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.RoomsPageMode;
import org.unitime.timetable.gwt.shared.RoomInterface.UpdateRoomFeatureRequest;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

/**
 * @author Tomas Muller
 */
public class RoomFeatureEdit extends Composite {
	private static final GwtRpcServiceAsync RPC = GWT.create(GwtRpcService.class);
	private static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);

	private SimpleForm iForm;
	private UniTimeHeaderPanel iHeader, iFooter;
	private RoomPropertiesInterface iProperties;
	private FeatureInterface iFeature;
	
	private UniTimeWidget<TextBox> iName;
	private UniTimeWidget<TextBox> iAbbreviation;
	private ListBox iType = null;
	private UniTimeWidget<ListBox> iDepartment;
	private CheckBox iGlobal;
	private int iDepartmentRow = -1;

	private RoomsTable iRooms = null;

	public RoomFeatureEdit(RoomPropertiesInterface properties) {
		iProperties = properties;

		iForm = new SimpleForm();
		iForm.addStyleName("unitime-RoomDepartmentsEdit");
		
		iHeader = new UniTimeHeaderPanel();
		ClickHandler createOrUpdateFeature = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (validate()) {
					UpdateRoomFeatureRequest request = new UpdateRoomFeatureRequest();
					request.setFeature(iFeature);
					for (int i = 1; i < iRooms.getRowCount(); i++) {
						RoomDetailInterface room = iRooms.getData(i);
						boolean wasSelected = (iFeature.getRoom(room.getUniqueId()) != null);
						boolean selected = iRooms.isRoomSelected(i);
						if (selected != wasSelected) {
							if (selected)
								request.addLocation(room.getUniqueId());
							else
								request.dropLocation(room.getUniqueId());
						}
					}
					LoadingWidget.getInstance().show(iFeature.getId() == null ? MESSAGES.waitSavingRoomFeature() : MESSAGES.waitUpdatingRoomFeature());
					RPC.execute(request, new AsyncCallback<FeatureInterface>() {
						@Override
						public void onFailure(Throwable caught) {
							LoadingWidget.getInstance().hide();
							String message = (iFeature.getId() == null ? MESSAGES.errorFailedToSaveRoomFeature(caught.getMessage()) : MESSAGES.errorFailedToUpdateRoomFeature(caught.getMessage()));
							iHeader.setErrorMessage(message);
							UniTimeNotifications.error(message);
						}

						@Override
						public void onSuccess(FeatureInterface result) {
							LoadingWidget.getInstance().hide();
							hide(true);
						}
					});
				} else {
					iHeader.setErrorMessage(MESSAGES.failedValidationCheckForm());
					UniTimeNotifications.error(MESSAGES.failedValidationCheckForm());
				}
			}
		};
		iHeader.addButton("create", MESSAGES.buttonCreateRoomFeature(), 100, createOrUpdateFeature);
		iHeader.addButton("update", MESSAGES.buttonUpdateRoomFeature(), 100, createOrUpdateFeature);
		iHeader.addButton("delete", MESSAGES.buttonDeleteRoomFeature(), 100, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (Window.confirm(MESSAGES.confirmDeleteRoomFeature())) {
					UpdateRoomFeatureRequest request = new UpdateRoomFeatureRequest();
					request.setDeleteFeatureId(iFeature.getId());
					LoadingWidget.getInstance().show(MESSAGES.waitDeletingRoomFeature());
					RPC.execute(request, new AsyncCallback<FeatureInterface>() {
						@Override
						public void onFailure(Throwable caught) {
							LoadingWidget.getInstance().hide();
							String message = MESSAGES.errorFailedToDeleteRoomFeature(caught.getMessage());
							iHeader.setErrorMessage(message);
							UniTimeNotifications.error(message);
						}

						@Override
						public void onSuccess(FeatureInterface result) {
							LoadingWidget.getInstance().hide();
							hide(true);
						}
					});
				}
			}
		});
		iHeader.addButton("back", MESSAGES.buttonBack(), 100, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide(false);
			}
		});

		iForm.addHeaderRow(iHeader);
		
		iName = new UniTimeWidget<TextBox>(new TextBox());
		iName.getWidget().setStyleName("unitime-TextBox");
		iName.getWidget().setMaxLength(60);
		iName.getWidget().setWidth("370px");
		iName.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iName.clearHint();
				iHeader.clearMessage();
			}
		});
		iForm.addRow(MESSAGES.propName(), iName);
		
		iAbbreviation = new UniTimeWidget<TextBox>(new TextBox());
		iAbbreviation.getWidget().setStyleName("unitime-TextBox");
		iAbbreviation.getWidget().setMaxLength(60);
		iAbbreviation.getWidget().setWidth("370px");
		iAbbreviation.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iAbbreviation.clearHint();
				iHeader.clearMessage();
			}
		});
		iForm.addRow(MESSAGES.propAbbreviation(), iAbbreviation);
		
		if (!iProperties.getFeatureTypes().isEmpty()) {
			iType = new ListBox();
			iType.addItem(MESSAGES.itemNoFeatureType(), "-1");
			for (FeatureTypeInterface type: iProperties.getFeatureTypes())
				iType.addItem(type.getLabel(), type.getId().toString());
			iForm.addRow(MESSAGES.propFeatureType(), iType);
		}
		
		iGlobal = new CheckBox();
		iForm.addRow(MESSAGES.propGlobalFeature(), iGlobal);
		iGlobal.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				iForm.getRowFormatter().setVisible(iDepartmentRow, !event.getValue());
			}
		});			
		
		iDepartment = new UniTimeWidget<ListBox>(new ListBox());
		iDepartment.getWidget().setStyleName("unitime-TextBox");
		iDepartmentRow = iForm.addRow(MESSAGES.propDepartment(), iDepartment);
		
		iForm.addHeaderRow(MESSAGES.headerRooms());
		
		iRooms = new RoomsTable(RoomsPageMode.COURSES, iProperties, true);
		iForm.addRow(iRooms);
		iRooms.addMouseClickListener(new MouseClickListener<RoomDetailInterface>() {
			@Override
			public void onMouseClick(TableEvent<RoomDetailInterface> event) {
				iHeader.clearMessage();
			}
		});
		
		iFooter = iHeader.clonePanel();
		iForm.addBottomRow(iFooter);
		
		initWidget(iForm);
	}
	
	private void hide(boolean refresh) {
		setVisible(false);
		onHide(refresh);
		Window.scrollTo(iLastScrollLeft, iLastScrollTop);
	}
	
	protected void onHide(boolean refresh) {
	}
	
	protected void onShow() {
	}
	
	private int iLastScrollTop, iLastScrollLeft;
	public void show() {
		UniTimePageLabel.getInstance().setPageName(iFeature.getId() == null ? MESSAGES.pageAddRoomFeature() : MESSAGES.pageEditRoomFeature());
		setVisible(true);
		iLastScrollLeft = Window.getScrollLeft();
		iLastScrollTop = Window.getScrollTop();
		onShow();
		Window.scrollTo(0, 0);
	}

	public void setFeature(FeatureInterface feature, String dept) {
		iHeader.clearMessage();
		iName.clearHint(); 
		iAbbreviation.clearHint();
		iDepartment.clearHint();
		if (feature == null) {
			iFeature = new FeatureInterface();
			iHeader.setEnabled("create", true);
			iHeader.setEnabled("update", false);
			iHeader.setEnabled("delete", false);
			iName.getWidget().setText("");
			iAbbreviation.getWidget().setText("");
			iDepartment.getWidget().clear();
			iGlobal.setValue(true, true);
			iGlobal.setEnabled(true);
			if (iType != null) iType.setSelectedIndex(0);
			if (iProperties.isCanAddDepartmentalRoomFeature()) {
				iDepartment.getWidget().addItem(MESSAGES.itemSelect(), "-1");
				iDepartment.getWidget().setSelectedIndex(0);
				for (DepartmentInterface department: iProperties.getDepartments()) {
					iDepartment.getWidget().addItem(department.getExtAbbreviationOrCode() + " - " + department.getExtLabelWhenExist(), department.getId().toString());
					if (dept != null && dept.equals(department.getDeptCode())) {
						iDepartment.getWidget().setSelectedIndex(iDepartment.getWidget().getItemCount() - 1);
						iGlobal.setValue(false, true);
					}
				}
			}
			if (!iProperties.isCanAddGlobalRoomFeature()) {
				iGlobal.setValue(false, true); iGlobal.setEnabled(false);
			} else if (!iProperties.isCanAddDepartmentalRoomGroup()) {
				iGlobal.setValue(true, true); iGlobal.setEnabled(false);
			}
		} else {
			iFeature = new FeatureInterface(feature);
			iHeader.setEnabled("create", false);
			iHeader.setEnabled("update", feature.canEdit());
			iHeader.setEnabled("delete", feature.canDelete());
			iName.getWidget().setText(feature.getLabel() == null ? "" : feature.getLabel());
			iAbbreviation.getWidget().setText(feature.getAbbreviation() == null ? "" : feature.getAbbreviation());
			if (iType != null) {
				if (feature.getType() == null) {
					iType.setSelectedIndex(0);
				} else {
					iType.setSelectedIndex(1 + iProperties.getFeatureTypes().indexOf(feature.getType()));
				}
			}
			iGlobal.setValue(!feature.isDepartmental(), true);
			iGlobal.setEnabled(false);
			iDepartment.getWidget().clear();
			if (feature.isDepartmental()) {
				for (DepartmentInterface department: iProperties.getDepartments())
					iDepartment.getWidget().addItem(department.getExtAbbreviationOrCode() + " - " + department.getExtLabelWhenExist(), department.getId().toString());
				int index = iProperties.getDepartments().indexOf(feature.getDepartment());
				if (index >= 0) {
					iDepartment.getWidget().setSelectedIndex(1 + index);
				} else {
					iDepartment.getWidget().addItem(feature.getDepartment().getExtAbbreviationOrCode() + " - " + feature.getDepartment().getExtLabelWhenExist(), feature.getDepartment().getId().toString());
					iDepartment.getWidget().setSelectedIndex(iDepartment.getWidget().getItemCount() - 1);
				}
			}
		}
	}
	
	public void setRooms(List<Entity> rooms) {
		iRooms.clearTable(1);
		iHeader.clearMessage();
		ValueChangeHandler<Boolean> clearErrorMessage = new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				iHeader.clearMessage();
			}
		};
		for (Entity e: rooms) {
			RoomDetailInterface room = (RoomDetailInterface)e;
			int row = iRooms.addRoom(room);
			boolean selected = (iFeature.getRoom(room.getUniqueId()) != null);
			iRooms.selectRoom(row, selected);
			iRooms.setSelected(row, selected);
			iRooms.getRoomSelection(row).addValueChangeHandler(clearErrorMessage);
		}
		int sort = RoomCookie.getInstance().getRoomsSortBy();
		if (sort != 0)
			iRooms.setSortBy(sort);
	}
	
	public boolean validate() {
		boolean result = true;
		iFeature.setLabel(iName.getWidget().getText());
		if (iFeature.getLabel().isEmpty()) {
			iName.setErrorHint(MESSAGES.errorNameIsEmpty());
			result = false;
		}
		iFeature.setAbbreviation(iAbbreviation.getWidget().getText());
		if (iFeature.getAbbreviation().isEmpty()) {
			iAbbreviation.setErrorHint(MESSAGES.errorAbbreviationIsEmpty());
			result = false;
		}
		if (iType != null) {
			iFeature.setType(iProperties.getFeatureType(Long.valueOf(iType.getValue(iType.getSelectedIndex()))));
		} else {
			iFeature.setType(null);
		}
		if (iFeature.getId() == null) {
			if (!iGlobal.getValue()) {
				iFeature.setDepartment(iProperties.getDepartment(Long.valueOf(iDepartment.getWidget().getValue(iDepartment.getWidget().getSelectedIndex()))));
				if (iFeature.getDepartment() == null) {
					iDepartment.setErrorHint(MESSAGES.errorNoDepartmentSelected());
					result = false;
				}
			} else {
				iFeature.setDepartment(null);
			}
		}
		return result;
	}
}
