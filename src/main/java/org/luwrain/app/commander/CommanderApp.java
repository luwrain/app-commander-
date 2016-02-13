/*
   Copyright 2012-2016 Michael Pozhidaev <michael.pozhidaev@gmail.com>

   This file is part of the LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.app.commander;

import java.util.*;
import java.io.*;
import java.nio.file.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.popups.*;

class CommanderApp implements Application, Actions
{
    static private final String STRINGS_NAME = "luwrain.commander";

    static private final int NORMAL_LAYOUT_INDEX = 0;
    static private final int OPERATIONS_LAYOUT_INDEX = 1;
    static private final int INFo_LAYOUT_INDEX = 2;

    private Luwrain luwrain;
    private final Base base = new Base();
    private Strings strings;
    private PanelArea leftPanel;
    private PanelArea rightPanel;
    private OperationArea operationsArea;
    private SimpleArea infoArea;
    private AreaLayoutSwitch layouts;
    private Path startFrom;

    public CommanderApp()
    {
	startFrom = null;
    }

    public CommanderApp(String arg)
    {
	NullCheck.notNull(arg, "arg");
	if (!arg.isEmpty())
	    this.startFrom = Paths.get(arg); else
	    this.startFrom = null;
    }

    @Override public boolean onLaunch(Luwrain luwrain)
    {
	final Object o =  luwrain.i18n().getStrings(STRINGS_NAME);
	if (o == null || !(o instanceof Strings))
	    return false;
	strings = (Strings)o;
	this.luwrain = luwrain;
	if (!base.init(luwrain, strings))
	    return false;
	createAreas();
	layouts = new AreaLayoutSwitch(luwrain);
	layouts.add(new AreaLayout(AreaLayout.LEFT_RIGHT, leftPanel, rightPanel));
	layouts.add(new AreaLayout(AreaLayout.LEFT_RIGHT_BOTTOM, leftPanel, rightPanel, operationsArea));
	layouts.add(new AreaLayout(infoArea));
	return true;
    }

    private void createAreas()
    {
	final Actions actions = this;

	leftPanel = new PanelArea(luwrain, this, strings, 
				  startFrom != null?startFrom:luwrain.launchContext().userHomeDirAsPath(),
				  PanelArea.Side.LEFT);
	rightPanel = new PanelArea(luwrain, this, strings, 
				   startFrom != null?startFrom:luwrain.launchContext().userHomeDirAsPath(),
				   PanelArea.Side.RIGHT);
	operationsArea = new OperationArea(luwrain, this, strings);

	infoArea = new SimpleArea(new DefaultControlEnvironment(luwrain), strings.infoAreaName()){
		@Override public boolean onKeyboardEvent(KeyboardEvent event)
		{
		    NullCheck.notNull(event, "event");
		    return super.onKeyboardEvent(event);
		}
		@Override public boolean onEnvironmentEvent(EnvironmentEvent event)
		{
		    NullCheck.notNull(event, "event");
		    switch(event.getCode())
		    {
		    case CLOSE:
			actions.closeApp();
			return true;
		    default:
			return super.onEnvironmentEvent(event);
		    }
		}
	    };
    }

    @Override public void selectLocationsLeft()
    {
	final File f = Popups.mountedPartitionsAsFile(luwrain, Popup.WEAK);
	if (f == null)
	    return;
	leftPanel.open(f.toPath(), null);
	luwrain.setActiveArea(leftPanel);
    }

    @Override public void selectLocationsRight()
    {
	final File f = Popups.mountedPartitionsAsFile(luwrain, Popup.WEAK);
	if (f == null)
	    return;
	rightPanel.open(f.toPath(), null);
	luwrain.setActiveArea(rightPanel);
    }

    @Override public boolean openReader(PanelArea.Side panelSide)
    {
	File[] files = null;
	switch(panelSide)
	{
	case LEFT:
	    files = leftPanel.selectedAsFiles();
	    break;
	case RIGHT:
	    files = rightPanel.selectedAsFiles();
	    break;
	default:
	    return false;
	}
	if (files == null || files.length < 1)
	    return false;
	base.openReader(files);
	return true;
    }

    @Override public boolean copy(PanelArea.Side panelSide)
    {
	final PanelArea fromPanel = getPanel(panelSide);
	final PanelArea toPanel = getAnotherPanel(panelSide);
	final Path copyFromDir = fromPanel.opened();
	final Path[] filesToCopy = fromPanel.selected();
	final Path copyTo = toPanel.opened();
	if (filesToCopy == null || filesToCopy.length < 1|| 
	    copyFromDir == null || copyTo == null)
	    return false;
	base.copy(operationsArea, copyFromDir, filesToCopy, copyTo);
	return true;
    }

    @Override public boolean move(PanelArea.Side panelSide)
    {
	return false;
	/*
	File[] filesToMove = null;
	File moveTo = null;
	switch(panelSide)
	{
	case LEFT:
	    filesToMove = leftPanel.selectedAsFiles();
	    moveTo= rightPanel.openedAsFile(); 
	    break;
	case RIGHT:
	    filesToMove = rightPanel.selectedAsFiles();
	    moveTo= leftPanel.openedAsFile(); 
	    break;
	default:
	    return false;
	}
	if (filesToMove == null || filesToMove.length < 1|| moveTo == null)
	    return false;
	moveTo = Popups.file(luwrain,
			     strings.movePopupName(),
			     strings.movePopupPrefix(filesToMove),
			     moveTo,
			     FilePopup.ANY, 0);
	if (moveTo == null)
	    return true;
 	operations.launch(new Move(operations, strings.moveOperationName(filesToMove, moveTo), filesToMove, moveTo));
	return true;
	*/
    }

    @Override public boolean mkdir(PanelArea.Side panelSide)
    {
	final PanelArea area = getPanel(panelSide);
	final Path createIn = area.opened();
	if (createIn == null)
	    return false;
	final Path created = base.mkdir(createIn);
	if (created == null)
	    return true;
	refreshPanels();
	area.find(created.getFileName().toString(), false);
	return true;
    }

    @Override public boolean delete(PanelArea.Side panelSide)
    {
	/*
	File[] filesToDelete = panelSide == PanelArea.Side.LEFT?leftPanel.selectedAsFiles():rightPanel.selectedAsFiles();
	if (filesToDelete == null || filesToDelete.length < 1)
	    return false;
	YesNoPopup popup = new YesNoPopup(luwrain, strings.delPopupName(),
					strings.delPopupText(filesToDelete), false);
	luwrain.popup(popup);
	if (popup.closing.cancelled())
	    return true;
	if (!popup.result())
	    return true;
 	operations.launch(Operations.delete(operations, strings.delOperationName(filesToDelete), 
filesToDelete));
	*/
	return true;
    }

    @Override public void refreshPanels()
    {
	leftPanel.refresh();
	rightPanel.refresh();
    }

    private PanelArea getPanel(PanelArea.Side side)
    {
	switch(side)
	{
	case LEFT:
	    return leftPanel;
	case RIGHT:
	    return rightPanel;
	default:
	    throw new IllegalArgumentException("Unknown panel side");
	}
    }

    private PanelArea getAnotherPanel(PanelArea.Side side)
    {
	switch(side)
	{
	case LEFT:
	    return rightPanel;
	case RIGHT:
	    return leftPanel;
	default:
	    throw new IllegalArgumentException("Unknown panel side");
	}
    }

    @Override public AreaLayout getAreasToShow()
    {
	return layouts.getCurrentLayout();
    }

    @Override public void gotoLeftPanel()
    {
	luwrain.setActiveArea(leftPanel);
    }

    @Override public void gotoRightPanel()
    {
	luwrain.setActiveArea(rightPanel);
    }

    @Override public void gotoOperations()
    {
	luwrain.setActiveArea(operationsArea);
    }

    @Override public String getAppName()
    {
	return strings.appName();
    }

    @Override public void closeApp()
    {
	if (!operationsArea.allOperationsFinished())
	{
	    luwrain.message(strings.notAllOperationsFinished(), Luwrain.MESSAGE_ERROR);
	    return;
	}
	luwrain.closeApp();
    }

    @Override public boolean hasOperations()
    {
	return operationsArea.hasOperations();
    }

    @Override public boolean showInfoArea(CommanderArea.Entry entry)
    {
	if (entry == null || entry.parent())
	    return false;
	luwrain.message(entry.path().toString());
	return true;
    }

    @Override public Settings settings()
    {
	return base.settings();
    }
}
