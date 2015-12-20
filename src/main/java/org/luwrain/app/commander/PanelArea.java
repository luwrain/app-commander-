/*
   Copyright 2012-2015 Michael Pozhidaev <michael.pozhidaev@gmail.com>

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

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.core.Registry;
import org.luwrain.app.commander.operations.TotalSize;

public class PanelArea extends CommanderArea implements CommanderArea.ClickHandler
{
    /*
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    */

    enum Side{LEFT, RIGHT};

    private Luwrain luwrain;
    private Actions actions;
    private Strings strings;
    private Side side = Side.LEFT;

    static private CommanderArea.Params createParams(Luwrain luwrain, Path startFrom)
    {
	final CommanderArea.Params params = new CommanderArea.Params();
	params.environment = new DefaultControlEnvironment(luwrain);
	params.selecting = true;
	params.filter = new CommanderFilters.NoHidden();
	params.comparator = new ByNameCommanderComparator();
	params.clickHandler = null;
	params.appearance = new DefaultCommanderAppearance(params.environment);
	return params;
    }

    PanelArea(Luwrain luwrain, Actions actions, Strings strings, 
	      File startFrom, Side side)
    {
	super(createParams(luwrain, startFrom.toPath()), startFrom.toPath());
	this.luwrain = luwrain;
	this.actions = actions;
	this.strings = strings;
	this.side = side;
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(strings, "strings");
	NullCheck.notNull(actions, "actions");
	setClickHandler(this);
    }

@Override public boolean onCommanderClick(Path current, Path[] selected)
    {
	if (selected == null)
	    return false;
	if (selected != null && selected.length == 1 && 
	    selected[0].toString().toLowerCase().endsWith(".zip"))
	{
	    if (openZip(selected[0]))
		return true;
	}
    	final String fileNames[] = new String[selected.length];
	for(int i = 0;i < selected.length;++i)
	    fileNames[i] = selected[i].toString();
	luwrain.openFiles(fileNames);
	return true;
    }

    @Override public boolean onKeyboardEvent(KeyboardEvent event)
    {
	NullCheck.notNull(event, "event");
	if (!event.isCommand() && !event.isModified())
	    switch(event.getCharacter())
	    {
	    case ' ':
		return calcSize();
	    case '=':
		setFilter(new CommanderFilters.AllFiles());
		refresh();
		return true;
	    case '-':
		setFilter(new CommanderFilters.NoHidden());
		refresh();
		return true;
	    default:
		return super.onKeyboardEvent(event);
	    }
	if (!event.isCommand())
	    return super.onKeyboardEvent(event);
	if (event.getCommand() == KeyboardEvent.F1 && event.withLeftAltOnly())
	{
	    actions.selectLocationsLeft();
	    return true;
	}
	if (event.getCommand() == KeyboardEvent.F2 && event.withLeftAltOnly())
	{
	    actions.selectLocationsRight();
	    return true;
	}
	if (event.getCommand() == KeyboardEvent.ENTER && event.withControlOnly())
	    return onShortInfo(event);
	if (event.isModified())
	    return super.onKeyboardEvent(event);
	switch(event.getCommand())
	{
	case KeyboardEvent.TAB:
	    if (side == Side.LEFT)
		actions.gotoRightPanel(); else
		if (side == Side.RIGHT)
		{
		    if (actions.hasOperations())
			actions.gotoOperations(); else
			actions.gotoLeftPanel();
		}
	    return true;
	case KeyboardEvent.F5:
	    return actions.copy(side);
	case KeyboardEvent.F6:
	    return actions.move(side);
	case KeyboardEvent.F7:
	    return actions.mkdir(side);
	case KeyboardEvent.F8:
	    return actions.delete(side);
	case KeyboardEvent.DELETE:
	    return actions.delete(side);
	default:
	    return super.onKeyboardEvent(event);
	}
    }

    @Override public boolean onEnvironmentEvent(EnvironmentEvent event)
    {
	switch(event.getCode())
	{
	case EnvironmentEvent.OPEN:
	    if (event instanceof OpenEvent)
	    {
		final Path path = ((OpenEvent)event).path();
		if (Files.isDirectory(path))
		{
		    open(path, null);
	    return true;
		}
		return false;
	    }
	    return false;
	case EnvironmentEvent.INTRODUCE:
	    luwrain.playSound(Sounds.INTRO_REGULAR);
	    switch (side)
	    {
	    case LEFT:
		luwrain.say(strings.leftPanel() + " " + getAreaName());
		break;
	    case RIGHT:
		luwrain.say(strings.rightPanel() + " " + getAreaName());
		break;
	    }
	    return true;
	case EnvironmentEvent.CLOSE:
	    actions.closeApp();
	    return true;
	case EnvironmentEvent.ACTION:
	    if (ActionEvent.isAction(event, "read"))
	    {
		actions.openReader(side);
		return true;
	    }
	    return false;
	default:
	    return super.onEnvironmentEvent(event);
	}
    }

    @Override public Action[] getAreaActions()
    {
	return new Action[]{
	    new Action("open", "Открыть"),
	    new Action("read", "Просмотреть с указанием формата"),
	};
    }

    private boolean onShortInfo(KeyboardEvent event)
    {
	final File[] f = selectedAsFiles();
	if (f == null)
	    return false;
	long res = 0;
	try {
	    for(File ff: f)
		res += TotalSize.getTotalSize(ff);
	}
	catch (Throwable e)
	{
	    e.printStackTrace();
	    return false;
	}
	luwrain.message(strings.bytesNum(res));
	return true;
    }

    private boolean calcSize()
    {
	final File[] f = selectedAsFiles();
	if (f == null || f.length < 1)
	    return false;
	long res = 0;
	try {
	    for(File ff: f)
		res += org.luwrain.app.commander.operations2.TotalSize.getTotalSize(ff.toPath());
	}
	catch (Throwable e)
	{
	    e.printStackTrace();
	    luwrain.message("Невозможно получить необходимый доступ к файлам, возможно, недостаточно прав доступа", Luwrain.MESSAGE_ERROR);
	    return true;
	}
	luwrain.message(strings.bytesNum(res), Luwrain.MESSAGE_DONE);
	return true;
    }

    private boolean openZip(Path path)
    {
	final Map<String, String> prop = new HashMap<String, String>();
	prop.put("encoding", actions.settings().getZipFilesEncoding("UTF-8"));
	try {
	    final URI zipfile = URI.create("jar:file:" + path.toString().replaceAll(" ", "%20"));
	    final FileSystem fs = FileSystems.newFileSystem(zipfile, prop);
	    open(fs.getPath("/"), null);
	    return true;
	}
	catch(IOException e)
	{
	    e.printStackTrace();
	    return false;
	}
    }

    File[] selectedAsFiles()
    {
	return null;
    }


    
    File openedAsFile()
    {
	return null;
    }
}
